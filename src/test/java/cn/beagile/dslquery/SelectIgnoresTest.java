package cn.beagile.dslquery;

import org.junit.jupiter.api.Test;

import javax.persistence.Column;
import javax.persistence.JoinColumn;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

public class SelectIgnoresTest {
    @SelectIgnores({"ignoreBean"})
    @View(value = "v_query_result", distinct = true)
    public static class QueryResult {
        @JoinColumn(name = "id", referencedColumnName = "parent_id")
        private IgnoreBean ignoreBean;
        @Column(name = "name")
        private String name;
    }

    @View("t_ignore_bean")
    public static class IgnoreBean {
        @Column(name = "id")
        public Long id;
    }

    @Test
    public void should_has_distinct() {
        SQLBuilder<QueryResult> sqlBuilder = new SQLBuilder<>(new DSLQuery<QueryResult>(null, QueryResult.class));
        assertEquals("select distinct v_query_result.name name_ from v_query_result\n" +
                "left join t_ignore_bean ignoreBean_ on ignoreBean_.parent_id = v_query_result.id", sqlBuilder.build().getSql());
    }

    @Test
    public void should_set_params_not_select() {
        DSLQuery<QueryResult> dslQuery = new DSLQuery<>(null, QueryResult.class);
        dslQuery.where("(and(ignoreBean.id equals 1))");
        SQLBuilder<QueryResult> sqlBuilder = new SQLBuilder<>(dslQuery);
        sqlBuilder.addParam("ignoreBean.id", "ignoreBean.id", "1");
        assertEquals(1L, sqlBuilder.getParams().get("ignoreBean.id"));
        assertEquals("select distinct v_query_result.name name_ from v_query_result\n" +
                "left join t_ignore_bean ignoreBean_ on ignoreBean_.parent_id = v_query_result.id\n" +
                " where ((ignoreBean_.id = :p1))", sqlBuilder.build().getSql());
    }

    @Test
    public void should_not_read_ignored_field() throws SQLException {
        DefaultResultSetReader<QueryResult> reader = new DefaultResultSetReader<>(new DSLQuery<>(null, QueryResult.class));
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getLong("ignoreBean_id_")).thenReturn(2L);
        QueryResult result = (QueryResult) reader.apply(resultSet);
        verify(resultSet, times(0)).getLong("ignoreBean_id_");
        assertNull(result.ignoreBean);
    }
}
