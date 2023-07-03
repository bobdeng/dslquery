package cn.beagile.dslquery;

import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

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
        assertEquals("select name,age from view_query", sqlQuery.getSql());
    }

    //带有查询条件，执行查询
    @Test
    public void should_execute_query_with_where() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultBean.class);
        dslQuery.where("(and(name equals bob))").query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        assertEquals("select name,age from view_query where ((name = :name1))", sqlQuery.getSql());
        Map<String, Object> params = sqlQuery.getParams();
        assertEquals("bob", params.get("name1"));
        assertEquals(1, params.size());
    }

    @View("view_query")
    public static class QueryResultForComplexSearch {
        @Column("name")
        private String name;
        @Column("age")
        private Integer age;
    }

    @Test
    public void should_execute_query_with_complex_where() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultForComplexSearch.class);
        dslQuery.where("(and(or(name equals bob)(name equals alice))(age greaterthan 20))").query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        assertEquals("select name,age from view_query where (((name = :name1) or (name = :name2)) and (age > :age3))", sqlQuery.getSql());
        Map<String, Object> params = sqlQuery.getParams();
        assertEquals("bob", params.get("name1"));
        assertEquals("alice", params.get("name2"));
        assertEquals(20, params.get("age3"));
    }

    @View("view_query")
    public static class QueryResultWithAlias {
        @Column("name1")
        private String name;
        @Column("age")
        private Integer age;
    }

    @Test
    public void should_execute_query_with_actual_field_name() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultWithAlias.class);
        dslQuery.where("(and(or(name equals bob)(name equals alice))(age greaterthan 20))").query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        assertEquals("select name1,age from view_query where (((name1 = :name1) or (name1 = :name2)) and (age > :age3))", sqlQuery.getSql());
        Map<String, Object> params = sqlQuery.getParams();
        assertEquals("bob", params.get("name1"));
        assertEquals("alice", params.get("name2"));
    }

    @Test
    public void should_execute_with_2_where() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultWithAlias.class);
        dslQuery.where("(and(age greaterthan 20))")
                .where("(and(name equals bob))").query();
        verify(queryExecutor, times(1)).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        assertEquals("select name1,age from view_query where ((age > :age1)) and ((name1 = :name2))", sqlQuery.getSql());
    }

    //带有排序条件，执行查询
    @Test
    public void should_execute_query_with_sort() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultBean.class);
        dslQuery.sort("name").query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        assertEquals("select name,age from view_query order by name", sqlQuery.getSql());
    }

    @Test
    public void should_execute_query_with_notnull() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultBean.class);
        dslQuery.where("(and(name notnull))").query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        assertEquals("select name,age from view_query where ((name is not null))", sqlQuery.getSql());
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
        assertEquals("select name,age from view_query where ((name " + condition + " :name1))", sqlQuery.getSql());
        assertEquals(1, sqlQuery.getParams().size());
        assertEquals(expected, sqlQuery.getParams().get("name1"));
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
        assertEquals("select name,age from view_query where ((name " + condition + " (:name1)))", sqlQuery.getSql());
        assertEquals(1, sqlQuery.getParams().size());
        assertArrayEquals(expectedArray, ((List) sqlQuery.getParams().get("name1")).toArray());
    }

    @Test
    public void should_execute_query_with_between() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultBean.class);
        dslQuery.where("(and(age between 20,30))").query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        assertEquals("select name,age from view_query where ((age between :age1 and :age2))", sqlQuery.getSql());
        assertEquals(2, sqlQuery.getParams().size());
        assertEquals(20, sqlQuery.getParams().get("age1"));
        assertEquals(30, sqlQuery.getParams().get("age2"));
    }

    @Test
    public void should_execute_query_with_sort_asc() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultBean.class);
        dslQuery.where("").sort("name asc").query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        assertEquals("select name,age from view_query order by name asc", sqlQuery.getSql());
    }

    @Test
    public void should_execute_query_with_field_alias() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultWithAlias.class);
        dslQuery.limit(null).skip(null).sort("name asc").query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        assertEquals("select name1,age from view_query order by name1 asc", sqlQuery.getSql());
    }

    @Test
    public void should_execute_query_with_multiple_fields() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultWithAlias.class);
        dslQuery.sort("name asc,age desc").query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        assertEquals("select name1,age from view_query order by name1 asc,age desc", sqlQuery.getSql());
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
        assertEquals("select count(*) from view_query", sqlQuery.getCountSql());
    }

    @Test
    public void should_get_total_count_with_where() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultWithAlias.class);
        dslQuery.where("(and(name equals bob))").query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        assertEquals("select count(*) from view_query where ((name1 = :name1))", sqlQuery.getCountSql());
        assertEquals(1, sqlQuery.getParams().size());
        assertEquals("bob", sqlQuery.getParams().get("name1"));
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
}
