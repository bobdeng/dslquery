package cn.beagile.dslquery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class DSLQueryTest {
    QueryExecutor queryExecutor;
    ArgumentCaptor<SQLQuery> sqlQueryArgumentCaptor;

    @BeforeEach
    public void setup() {
        queryExecutor = mock(QueryExecutor.class);
        sqlQueryArgumentCaptor = ArgumentCaptor.forClass(SQLQuery.class);
        when(queryExecutor.execute(any(), any())).thenReturn(null);
    }

    @Test
    public void should_execute_query_without_filter_sort_page() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultBean.class);
        dslQuery.query();
        verify(queryExecutor).execute(sqlQueryArgumentCaptor.capture(), any());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        assertEquals("select name from view_query", sqlQuery.sql());
    }

    //带有查询条件，执行查询
    @Test
    public void should_execute_query_with_where() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultBean.class);
        dslQuery.where("(and(name equal bob))").query();
        verify(queryExecutor).execute(sqlQueryArgumentCaptor.capture(), any());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        assertEquals("select name from view_query where ((name = :name1))", sqlQuery.sql());
        Map<String, Object> params = sqlQuery.getParams();
        assertEquals("bob", params.get("name1"));
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
        dslQuery.where("(and(or(name equal bob)(name equal alice))(age greaterthan 20))").query();
        verify(queryExecutor).execute(sqlQueryArgumentCaptor.capture(), any());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        assertEquals("select name,age from view_query where (((name = :name1) or (name = :name2)) and (age > :age3))", sqlQuery.sql());
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
        dslQuery.where("(and(or(name equal bob)(name equal alice))(age greaterthan 20))").query();
        verify(queryExecutor).execute(sqlQueryArgumentCaptor.capture(), any());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        assertEquals("select name1,age from view_query where (((name1 = :name1) or (name1 = :name2)) and (age > :age3))", sqlQuery.sql());
        Map<String, Object> params = sqlQuery.getParams();
        assertEquals("bob", params.get("name1"));
        assertEquals("alice", params.get("name2"));
    }

    @Test
    public void should_execute_with_2_where() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultWithAlias.class);
        dslQuery.where("(and(age greaterthan 20))")
                .where("(and(name equal bob))").query();
        verify(queryExecutor, times(1)).execute(sqlQueryArgumentCaptor.capture(), any());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        assertEquals("select name1,age from view_query where ((age > :age1)) and ((name1 = :name2))", sqlQuery.sql());

    }

    //带有排序条件，执行查询
    @Test
    public void should_execute_query_with_sort() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultBean.class);
        dslQuery.sort("name").query();
        verify(queryExecutor).execute(sqlQueryArgumentCaptor.capture(), any());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        assertEquals("select name from view_query order by name", sqlQuery.sql());
    }

    @Test
    public void should_execute_query_with_sort_asc() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultBean.class);
        dslQuery.sort("name asc").query();
        verify(queryExecutor).execute(sqlQueryArgumentCaptor.capture(), any());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        assertEquals("select name from view_query order by name asc", sqlQuery.sql());
    }

    @Test
    public void should_execute_query_with_field_alias() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultWithAlias.class);
        dslQuery.sort("name asc").query();
        verify(queryExecutor).execute(sqlQueryArgumentCaptor.capture(), any());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        assertEquals("select name1,age from view_query order by name1 asc", sqlQuery.sql());
    }

    @Test
    public void should_execute_query_with_multiple_fields() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultWithAlias.class);
        dslQuery.sort("name asc,age desc").query();
        verify(queryExecutor).execute(sqlQueryArgumentCaptor.capture(), any());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        assertEquals("select name1,age from view_query order by name1 asc,age desc", sqlQuery.sql());
    }

    //带有分页条件，执行查询
    @Test
    public void should_execute_query_with_limit() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultWithAlias.class);
        dslQuery.skip(10).limit(10).query();
        verify(queryExecutor).execute(sqlQueryArgumentCaptor.capture(), any());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        assertEquals(10, sqlQuery.limit());
    }

    @Test
    public void should_execute_query_without_limit() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultWithAlias.class);
        dslQuery.query();
        verify(queryExecutor).execute(sqlQueryArgumentCaptor.capture(), any());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        assertNull(sqlQuery.limit());
        assertNull(sqlQuery.skip());
    }
}
