package cn.beagile.dslquery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class DSLQueryTest {
    //没有任何查询条件，执行查询
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
        assertEquals(20, params.get("age3"));

    }
    //带有排序条件，执行查询
    //带有分页条件，执行查询
}
