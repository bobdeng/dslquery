package cn.beagile.dslquery;

import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class DSLQueryTest {
    QueryExecutor queryExecutor;
    ArgumentCaptor<SQLQuery> sqlQueryArgumentCaptor;
    private String fields = "view_query.name name,view_query.age age,view_query.json json,view_query.another_name embeddingField_name";

    @BeforeEach
    public void setup() {
        queryExecutor = mock(QueryExecutor.class);
        sqlQueryArgumentCaptor = ArgumentCaptor.forClass(SQLQuery.class);
        ArrayList<QueryResultBean> list = new ArrayList<>();
        list.add(new QueryResultBean("bob"));
        when(queryExecutor.list(any(), any())).thenReturn(Collections.singletonList(list));
    }

    @Test
    public void should_execute_query_without_filter_sort_page() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultBean.class);
        dslQuery.query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        expectSqlWithoudEnter("select " + fields + " from view_query", sqlQuery.getSql());
    }

    //带有查询条件，执行查询
    @Test
    public void should_execute_query_with_where() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultBean.class);
        dslQuery.where("(and(name equals bob))").query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        expectSqlWithoudEnter("select " + fields + " from view_query where ((view_query.name = :p1))", sqlQuery.getSql());
        Map<String, Object> params = sqlQuery.getParams();
        assertEquals("bob", params.get("p1"));
        assertEquals(1, params.size());
    }

    @View("view_query")
    public static class QueryResultForComplexSearch {
        @Column(name = "name")
        private String name;
        @Column(name = "age")
        private Integer age;
    }

    @Test
    public void should_execute_query_with_complex_where() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultForComplexSearch.class);
        dslQuery.where("(and(or(name equals bob)(name equals alice))(age greaterthan 20))").query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        expectSqlWithoudEnter("select view_query.name name,view_query.age age from view_query where (((view_query.name = :p1) or (view_query.name = :p2)) and (view_query.age > :p3))", sqlQuery.getSql());
        Map<String, Object> params = sqlQuery.getParams();
        assertEquals("bob", params.get("p1"));
        assertEquals("alice", params.get("p2"));
        assertEquals(20, params.get("p3"));
    }

    @View("view_query")
    public static class QueryResultWithAlias {
        @Column(name = "name1")
        private String name;
        @Column(name = "age")
        private Integer age;
    }

    @Test
    public void should_execute_query_with_actual_field_name() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultWithAlias.class);
        dslQuery.where("(and(or(name equals bob)(name equals alice))(age greaterthan 20))").query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        String sql = sqlQuery.getSql();
        expectSqlWithoudEnter("select view_query.name1 name,view_query.age age from view_query where (((view_query.name1 = :p1) or (view_query.name1 = :p2)) and (view_query.age > :p3))", sql);
        Map<String, Object> params = sqlQuery.getParams();
        assertEquals("bob", params.get("p1"));
        assertEquals("alice", params.get("p2"));
    }

    private static void expectSqlWithoudEnter(String expected, String sql) {
        assertEquals(expected, sql.replace("\n", ""));
    }

    @Test
    public void should_execute_with_2_where() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultWithAlias.class);
        dslQuery.where("(and(age greaterthan 20))")
                .where("(and(name equals bob))").query();
        verify(queryExecutor, times(1)).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        expectSqlWithoudEnter("select view_query.name1 name,view_query.age age from view_query where ((view_query.age > :p1)) and ((view_query.name1 = :p2))", sqlQuery.getSql());
    }

    //带有排序条件，执行查询
    @Test
    public void should_execute_query_with_sort() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultBean.class);
        dslQuery.sort("name").query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        expectSqlWithoudEnter("select " + fields + " from view_queryorder by view_query.name", sqlQuery.getSql());
    }

    @Test
    public void should_execute_query_with_notnull() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultBean.class);
        dslQuery.where("(and(name notnull))").query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        expectSqlWithoudEnter("select " + fields + " from view_query where ((view_query.name is not null))", sqlQuery.getSql());
        assertEquals(0, sqlQuery.getParams().size());
    }

    @ParameterizedTest
    @CsvSource({
            "startswith,like,bob%",
            "contains,like,%bob%",
            "endswith,like,%bob",
            "greaterthanorequals,>=,bob",
            "greaterthan,>,bob",
            "lessthanorequals,<=,bob",
            "lessthan,<,bob",
            "notequals,!=,bob",
            "equals,=,bob",
    })
    public void should_execute_query_with_condition(String operator, String condition, String expected) {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultBean.class);
        dslQuery.where("(and(name " + operator + " bob))").query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        expectSqlWithoudEnter("select " + fields + " from view_query where ((view_query.name " + condition + " :p1))", sqlQuery.getSql());
        assertEquals(1, sqlQuery.getParams().size());
        assertEquals(expected, sqlQuery.getParams().get("p1"));
    }

    @ParameterizedTest
    @CsvSource({
            "in,in",
            "notin,not in",
    })
    public void should_execute_query_with_condition_in_string_array(String operator, String condition) {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultBean.class);
        String[] expectedArray = new Gson().fromJson("['bob','alice']", String[].class);
        dslQuery.where("(and(name " + operator + " ['bob','alice']))").query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        expectSqlWithoudEnter("select " + fields + " from view_query where ((view_query.name " + condition + " (:p1)))", sqlQuery.getSql());
        assertEquals(1, sqlQuery.getParams().size());
        assertArrayEquals(expectedArray, ((List) sqlQuery.getParams().get("p1")).toArray());
    }

    @Test
    public void should_execute_query_with_between() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultBean.class);
        dslQuery.where("(and(age between 20,30))").query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        expectSqlWithoudEnter("select " + fields + " from view_query where ((view_query.age between :p1 and :p2))", sqlQuery.getSql());
        assertEquals(2, sqlQuery.getParams().size());
        assertEquals(20, sqlQuery.getParams().get("p1"));
        assertEquals(30, sqlQuery.getParams().get("p2"));
    }

    @Test
    public void should_execute_query_with_sort_asc() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultBean.class);
        dslQuery.where("").sort("name asc").query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        expectSqlWithoudEnter("select " + fields + " from view_queryorder by view_query.name asc", sqlQuery.getSql());
    }

    @Test
    public void should_execute_query_with_field_alias() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultWithAlias.class);
        dslQuery.limit(null).skip(null).sort("name asc").query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        expectSqlWithoudEnter("select view_query.name1 name,view_query.age age from view_queryorder by view_query.name1 asc", sqlQuery.getSql());
    }

    @Test
    public void should_execute_query_with_multiple_fields() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultWithAlias.class);
        dslQuery.sort("name asc,age desc").query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        expectSqlWithoudEnter("select view_query.name1 name,view_query.age age from view_queryorder by view_query.name1 asc,view_query.age desc", sqlQuery.getSql());
    }

    //带有分页条件，执行查询
    @Test
    public void should_execute_query_with_limit() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultWithAlias.class);
        dslQuery.skip(10).limit(10).sort(null).query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        assertEquals(10, sqlQuery.getLimit());
    }

    @Test
    public void should_execute_query_without_limit() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultWithAlias.class);
        dslQuery.where(null).sort("").query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        assertNull(sqlQuery.getLimit());
        assertNull(sqlQuery.getSkip());
    }

    @Test
    public void should_get_total_count() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultWithAlias.class);
        dslQuery.query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        expectSqlWithoudEnter("select count(*) from view_query", sqlQuery.getCountSql());
    }

    @Test
    public void should_get_total_count_with_where() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultWithAlias.class);
        dslQuery.where("(and(name equals bob))").query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        expectSqlWithoudEnter("select count(*) from view_querywhere ((view_query.name1 = :p1))", sqlQuery.getCountSql());
        assertEquals(1, sqlQuery.getParams().size());
        assertEquals("bob", sqlQuery.getParams().get("p1"));
    }

    @Test
    public void should_get_paged_result() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultWithAlias.class);
        when(queryExecutor.count(any())).thenReturn(100);
        Paged<QueryResultBean> pagedResult = dslQuery.skip(10).limit(20).where("(and(name equals bob))").pagedQuery();
        assertNotNull(pagedResult);
        assertEquals(1, pagedResult.getResult().size());
        assertEquals(100, pagedResult.total());
        assertEquals(20, pagedResult.limit());
        assertEquals(10, pagedResult.skip());
    }

    @View("view_query")
    public static class QueryBeanWithEmbedded {
        @Embedded
        @AttributeOverrides({
                @AttributeOverride(name = "code", column = @Column(name = "code2")),
        })
        private EmbeddedField field;
        @Embedded
        @AttributeOverrides({
                @AttributeOverride(name = "name", column = @Column(name = "name1")),
        })
        EmbeddedField2 field2;
        @View("view_query2")
        public static class EmbeddedField {
            @Column(name = "name2")
            private String name;
            @Column(name = "code")
            public String code;

        }
        @View("view_query3")
        public static class EmbeddedField2 {
            @Column(name = "name2")
            private String name;
            @Column(name = "code")
            public String code;
        }
        @View("t_join")
        public static class EmbeddedField3 {
            @Column(name = "name2")
            private String name;
            @Column(name = "code")
            public String code;
        }
    }

    @Test
    public void should_read_attribute_override_column() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryBeanWithEmbedded.class);
        dslQuery.query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        expectSqlWithoudEnter("select view_query.code2 field_code,view_query.name1 field2_name from view_query", sqlQuery.getSql());
    }

    @Test
    public void field_not_found() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryBeanWithEmbedded.class);
        RuntimeException e = assertThrows(RuntimeException.class, () -> dslQuery.where("(and(fieldNotExist equals 123))").query());
        assertEquals("field not found: fieldNotExist", e.getMessage());
    }
}
