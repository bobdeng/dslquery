package cn.beagile.dslquery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
    //查询结果映射到QueryResultBean
    //带有查询条件，执行查询
    //带有排序条件，执行查询
    //带有分页条件，执行查询
}
