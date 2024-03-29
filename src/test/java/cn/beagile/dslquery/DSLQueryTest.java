package cn.beagile.dslquery;

import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import javax.persistence.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class DSLQueryTest {
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
    }

    private static void expectSqlWithoudEnter(String expected, String sql) {
        assertEquals(expected, sql.replace("\n", ""));
    }

    @Test
    public void should_execute_query_without_filter_sort_page() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultBean.class);
        dslQuery.query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        expectSqlWithoudEnter("select " + fields + " from view_query", sqlQuery.getSql());
    }

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

    @Test
    public void should_execute_query_with_where_between() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultBean.class);
        dslQuery.where("(and(age between 18,26))").query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        expectSqlWithoudEnter("select " + fields + " from view_query where ((view_query.age between :p1 and :p2))", sqlQuery.getSql());
        Map<String, Object> params = sqlQuery.getParams();
        assertEquals(18, params.get("p1"));
        assertEquals(26, params.get("p2"));
    }

    @Test
    public void should_execute_query_with_where_in() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultBean.class);
        dslQuery.where("(and(name in ['bob','alice']))").query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        expectSqlWithoudEnter("select " + fields + " from view_query where ((view_query.name in (:p1)))", sqlQuery.getSql());
        Map<String, Object> params = sqlQuery.getParams();
        assertEquals(Arrays.asList("bob", "alice"), params.get("p1"));
    }

    @Test
    public void should_execute_query_with_where_notnull() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultBean.class);
        dslQuery.where("(and(name notnull))").sort("name desc").query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        expectSqlWithoudEnter("select " + fields + " from view_query where ((view_query.name is not null))order by view_query.name desc", sqlQuery.getSql());
        Map<String, Object> params = sqlQuery.getParams();
        assertNull(params.get("p1"));
    }

    @Test
    public void when_where_null() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultBean.class);
        dslQuery.where(null).sort(null).query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        expectSqlWithoudEnter("select " + fields + " from view_query", sqlQuery.getSql());
    }
    @Test
    public void when_where_empty() {
        DSLQuery dslQuery = new DSLQuery(queryExecutor, QueryResultBean.class);
        dslQuery.where("").sort("").query();
        verify(queryExecutor).list(any(), sqlQueryArgumentCaptor.capture());
        SQLQuery sqlQuery = sqlQueryArgumentCaptor.getValue();
        expectSqlWithoudEnter("select " + fields + " from view_query", sqlQuery.getSql());
    }
}
