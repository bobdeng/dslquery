package cn.beagile.dslquery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static cn.beagile.dslquery.Conditions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ConditionsTest {
    QueryExecutor queryExecutor;
    ArgumentCaptor<SQLQuery> sqlQueryArgumentCaptor;
    private String fields = "view_query.name name_,view_query.age age_,view_query.json json_,view_query.another_name embeddingField_name_";

    @BeforeEach
    public void setup() {
        queryExecutor = mock(QueryExecutor.class);
        sqlQueryArgumentCaptor = ArgumentCaptor.forClass(SQLQuery.class);
        ArrayList<QueryResultBean> list = new ArrayList<>();
        list.add(new QueryResultBean("bob"));
        when(queryExecutor.list(any(), any())).thenReturn(Collections.singletonList(list));
        when(queryExecutor.nullsOrder(NullsOrder.NONE)).thenReturn("");
    }

    private static void expectSqlWithoudEnter(String expected, String sql) {
        assertEquals(expected, sql.replace("\n", ""));
    }

    @Test
    public void build_eq() {
        assertEquals("(name eq bob)", eq("name", "bob"));
    }

    @Test
    public void build_ne() {
        assertEquals("(name ne bob)", ne("name", "bob"));
    }

    @Test
    public void build_gt() {
        assertEquals("(age gt 18)", gt("age", 18));
    }

    @Test
    public void build_ge() {
        assertEquals("(age ge 18)", ge("age", 18));
    }

    @Test
    public void build_lt() {
        assertEquals("(age lt 18)", lt("age", 18));
    }

    @Test
    public void build_le() {
        assertEquals("(age le 18)", le("age", 18));
    }

    @Test
    public void build_starts_with() {
        assertEquals("(email sw test)", startsWith("email", "test"));
    }

    @Test
    public void build_ends_with() {
        assertEquals("(email ew test)", endsWith("email", "test"));
    }

    @Test
    public void build_contains() {
        assertEquals("(phone ct 123)", contains("phone", "123"));
    }

    @Test
    public void build_not_starts_with() {
        assertEquals("(name nsw bob)", notStartsWith("name", "bob"));
    }

    @Test
    public void build_not_ends_with() {
        assertEquals("(name new bob)", notEndsWith("name", "bob"));
    }

    @Test
    public void build_not_contains() {
        assertEquals("(name nct bob)", notContains("name", "bob"));
    }

    @Test
    public void build_is_null() {
        assertEquals("(name isn)", isNull("name"));
    }

    @Test
    public void build_not_null() {
        assertEquals("(name inn)", notNull("name"));
    }

    @Test
    public void build_is_empty() {
        assertEquals("(name emp)", isEmpty("name"));
    }

    @Test
    public void build_is_not_empty() {
        assertEquals("(name nem)", isNotEmpty("name"));
    }

    @Test
    public void build_in() {
        assertEquals("(name in %5B%22bob%22%2C%22alice%22%5D)", in("name", Arrays.asList("bob", "alice")));
    }

    @Test
    public void build_not_in() {
        assertEquals("(name ni %5B%22bob%22%2C%22alice%22%5D)", notIn("name", Arrays.asList("bob", "alice")));
    }

    @Test
    public void build_between() {
        assertEquals("(age bt 1%2C32)", between("age", 1, 32));
    }

    @Test
    public void build_not_between() {
        assertEquals("(age nbt 1%2C32)", notBetween("age", 1, 32));
    }

    @Test
    public void accept_non_string_value() {
        assertEquals("(age eq 18)", eq("age", 18));
        assertEquals("(age gt 18.5)", gt("age", 18.5));
    }

    @Test
    public void build_and() {
        assertEquals("(and(name eq bob)(age gt 18))", and(eq("name", "bob"), gt("age", 18)));
    }

    @Test
    public void build_or() {
        assertEquals("(or(name eq bob)(age gt 18))", or(eq("name", "bob"), gt("age", 18)));
    }

    @Test
    public void build_nested() {
        assertEquals("(and(name eq bob)(or(email sw test)(phone ct 123)))",
                and(eq("name", "bob"), or(startsWith("email", "test"), contains("phone", "123"))));
    }

    @Test
    public void build_deep_nested() {
        assertEquals("(or(and(a eq 1)(b eq 2))(and(c eq 3)(d eq 4)))",
                or(and(eq("a", "1"), eq("b", "2")), and(eq("c", "3"), eq("d", "4"))));
    }

    @Test
    public void where_multiple_implicit_and() {
        assertEquals("(and(name eq bob)(age gt 18))", where(eq("name", "bob"), gt("age", 18)));
    }

    @Test
    public void where_single_wrapped_passthrough() {
        assertEquals("(and(name eq bob)(age gt 18))", where(and(eq("name", "bob"), gt("age", 18))));
        assertEquals("(or(name eq bob)(age gt 18))", where(or(eq("name", "bob"), gt("age", 18))));
    }

    @Test
    public void where_single_unwrapped_wraps_with_and() {
        assertEquals("(and(name eq bob))", where(eq("name", "bob")));
    }

    @Test
    public void where_empty_returns_no_filter() {
        assertEquals("", where());
        assertEquals("", where((String) null));
    }

    @Test
    public void and_empty_rejected() {
        assertThrows(IllegalArgumentException.class, () -> and());
    }

    @Test
    public void or_empty_rejected() {
        assertThrows(IllegalArgumentException.class, () -> or());
    }

    @Test
    public void null_fragments_ignored() {
        assertEquals("(and(name eq bob))", and(eq("name", "bob"), null));
        assertEquals("(and(name eq bob))", where(eq("name", "bob"), null));
    }

    @Test
    public void accumulator_empty_build_returns_no_filter() {
        assertEquals("", Conditions.accumulator().build());
        assertEquals("", Conditions.orAccumulator().build());
    }

    @Test
    public void accumulator_add_conditions() {
        Conditions conditions = Conditions.accumulator();
        conditions.add(eq("name", "bob")).add(gt("age", 18));
        assertEquals("(and(name eq bob)(age gt 18))", conditions.build());
    }

    @Test
    public void accumulator_add_nested_or_group() {
        Conditions conditions = Conditions.accumulator();
        conditions.add(eq("name", "bob")).add(or(eq("status", "active"), eq("status", "trial")));
        assertEquals("(and(name eq bob)(or(status eq active)(status eq trial)))", conditions.build());
    }

    @Test
    public void accumulator_or_root() {
        Conditions conditions = Conditions.orAccumulator();
        conditions.add(eq("name", "bob")).add(eq("age", "18"));
        assertEquals("(or(name eq bob)(age eq 18))", conditions.build());
    }

    @Test
    public void accumulator_ignores_null_fragment() {
        Conditions conditions = Conditions.accumulator();
        conditions.add(eq("name", "bob"), null);
        assertEquals("(and(name eq bob))", conditions.build());
    }

    @Test
    public void accumulator_is_empty() {
        assertTrue(Conditions.accumulator().isEmpty());
        assertFalse(Conditions.accumulator().add(eq("name", "bob")).isEmpty());
        assertEquals(2, Conditions.accumulator().add(eq("name", "bob"), gt("age", 18)).size());
    }

    @Test
    public void encode_value_with_space() {
        assertEquals("(name eq bob+robert)", eq("name", "bob robert"));
    }

    @Test
    public void encode_value_with_special_chars() {
        assertEquals("(name eq 100%25)", eq("name", "100%"));
        assertEquals("(name eq a%2Cb)", eq("name", "a,b"));
    }

    @Test
    public void invalid_field_name() {
        assertThrows(RuntimeException.class, () -> eq("bad field", "x"));
        assertThrows(RuntimeException.class, () -> eq("", "x"));
        assertThrows(RuntimeException.class, () -> eq("1name", "x"));
    }

    @Test
    public void null_value_rejected() {
        assertThrows(IllegalArgumentException.class, () -> eq("name", null));
        assertThrows(IllegalArgumentException.class, () -> in("name", null));
        assertThrows(IllegalArgumentException.class, () -> between("age", null, 32));
        assertThrows(IllegalArgumentException.class, () -> between("age", 1, null));
    }

    @Test
    public void roundtrip_through_parser() {
        String dsl = where(and(eq("name", "bob"), gt("age", 18), or(startsWith("email", "test"), contains("phone", "123"))));
        assertEquals(dsl, new WhereParser().parse(dsl).toDSL());
    }

    @Test
    public void roundtrip_value_with_space() {
        String dsl = where(eq("name", "bob robert"));
        assertEquals(dsl, new WhereParser().parse(dsl).toDSL());
    }

    @Test
    public void roundtrip_field_reference_value() {
        String dsl = where(eq("tenantId", "@parent.tenantId"));
        assertEquals(dsl, new WhereParser().parse(dsl).toDSL());
    }

    @Test
    public void should_execute_query_with_conditions() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultBean.class);
        dslQuery.where(where(eq("name", "bob"), gt("age", 18))).query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        expectSqlWithoudEnter("select " + fields + " from view_query where ((view_query.name = :p0) and (view_query.age > :p1))", sqlQuery.getSql());
        Map<String, Object> params = sqlQuery.getParams();
        assertEquals("bob", params.get("p0"));
        assertEquals(18, params.get("p1"));
    }

    @Test
    public void should_execute_query_with_nested_or() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultBean.class);
        dslQuery.where(and(eq("name", "bob"), or(eq("age", 18), eq("age", 20)))).query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        expectSqlWithoudEnter("select " + fields + " from view_query where ((view_query.name = :p0) and ((view_query.age = :p1) or (view_query.age = :p2)))", sqlQuery.getSql());
        Map<String, Object> params = sqlQuery.getParams();
        assertEquals("bob", params.get("p0"));
        assertEquals(18, params.get("p1"));
        assertEquals(20, params.get("p2"));
    }

    @Test
    public void should_execute_query_with_in() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultBean.class);
        dslQuery.where(where(in("name", Arrays.asList("bob", "alice")))).query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        expectSqlWithoudEnter("select " + fields + " from view_query where ((view_query.name in (:p0)))", sqlQuery.getSql());
        assertEquals(Arrays.asList("bob", "alice"), sqlQuery.getParams().get("p0"));
    }

    @Test
    public void should_execute_query_with_between() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultBean.class);
        dslQuery.where(where(between("age", 18, 26))).query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        expectSqlWithoudEnter("select " + fields + " from view_query where ((view_query.age between :p0_0 and :p0_1))", sqlQuery.getSql());
        Map<String, Object> params = sqlQuery.getParams();
        assertEquals(18, params.get("p0_0"));
        assertEquals(26, params.get("p0_1"));
    }

    @Test
    public void should_execute_query_with_accumulated_conditions() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultBean.class);
        Conditions conditions = Conditions.accumulator();
        conditions.add(eq("name", "bob"));
        conditions.add(gt("age", 18));
        dslQuery.where(conditions.build()).query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        expectSqlWithoudEnter("select " + fields + " from view_query where ((view_query.name = :p0) and (view_query.age > :p1))", sqlQuery.getSql());
        Map<String, Object> params = sqlQuery.getParams();
        assertEquals("bob", params.get("p0"));
        assertEquals(18, params.get("p1"));
    }
}
