package cn.beagile.dslquery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static cn.beagile.dslquery.Conditions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class FilterRulesTest {
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
    public void build_from_form_with_values() {
        FilterRules rules = FilterRules.create()
                .on("name", value -> eq("name", value))
                .on("minAge", value -> ge("age", value))
                .on("status", value -> in("status", (Collection<?>) value));
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("name", "bob");
        form.put("minAge", 18);
        form.put("status", Arrays.asList("active", "trial"));
        assertEquals("(and(name eq bob)(age ge 18)(status in %5B%22active%22%2C%22trial%22%5D))", rules.buildFilter(form));
    }

    @Test
    public void skip_empty_values() {
        FilterRules rules = FilterRules.create().on("name", value -> eq("name", value));
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("name", "");
        form.put("age", null);
        form.put("tags", new ArrayList<>());
        assertEquals("", rules.buildFilter(form));
    }

    @Test
    public void boolean_false_and_zero_are_values() {
        FilterRules rules = FilterRules.create()
                .on("active", value -> eq("active", value))
                .on("age", value -> eq("age", value));
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("active", false);
        form.put("age", 0);
        assertEquals("(and(active eq false)(age eq 0))", rules.buildFilter(form));
    }

    @Test
    public void ignore_form_fields_without_rules() {
        FilterRules rules = FilterRules.create().on("name", value -> eq("name", value));
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("name", "bob");
        form.put("noRule", "x");
        assertEquals("(and(name eq bob))", rules.buildFilter(form));
    }

    @Test
    public void default_rule_applied() {
        FilterRules rules = FilterRules.create()
                .on("name", value -> eq("name", value))
                .withDefault(form -> form.get("tenantId") != null ? eq("tenantId", form.get("tenantId")) : null);
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("name", "bob");
        form.put("tenantId", 42);
        assertEquals("(and(name eq bob)(tenantId eq 42))", rules.buildFilter(form));
    }

    @Test
    public void extra_conditions_appended() {
        FilterRules rules = FilterRules.create().on("name", value -> eq("name", value));
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("name", "bob");
        assertEquals("(and(name eq bob)(phone ct 123))", rules.buildFilter(form, contains("phone", "123")));
    }

    @Test
    public void only_extra_conditions_when_form_empty() {
        FilterRules rules = FilterRules.create().on("name", value -> eq("name", value));
        assertEquals("(and(type eq black))", rules.buildFilter(new LinkedHashMap<>(), eq("type", "black")));
    }

    @Test
    public void rule_returning_null_skipped() {
        FilterRules rules = FilterRules.create().on("name", value -> "bob".equals(value) ? eq("name", value) : null);
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("name", "alice");
        assertEquals("", rules.buildFilter(form));
    }

    @Test
    public void rule_returning_group() {
        FilterRules rules = FilterRules.create().on("range", value -> and(eq("a", "1"), eq("b", "2")));
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("range", "x");
        assertEquals("(and(and(a eq 1)(b eq 2)))", rules.buildFilter(form));
    }

    @Test
    public void empty_form_returns_no_filter() {
        FilterRules rules = FilterRules.create().on("name", value -> eq("name", value));
        assertEquals("", rules.buildFilter(new LinkedHashMap<>()));
        assertEquals("", rules.buildFilter(new LinkedHashMap<>(), (String) null));
    }

    @Test
    public void should_execute_query_with_filter_rules() {
        FilterRules rules = FilterRules.create()
                .on("name", value -> eq("name", value))
                .on("minAge", value -> ge("age", value));
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("name", "bob");
        form.put("minAge", 18);
        form.put("unused", "");
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultBean.class);
        dslQuery.where(rules.buildFilter(form)).query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        expectSqlWithoudEnter("select " + fields + " from view_query where ((view_query.name = :p0) and (view_query.age >= :p1))", sqlQuery.getSql());
        Map<String, Object> params = sqlQuery.getParams();
        assertEquals("bob", params.get("p0"));
        assertEquals(18, params.get("p1"));
    }
}
