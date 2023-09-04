package cn.beagile.dslquery;

import org.junit.jupiter.api.Test;

import javax.persistence.*;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

public class SelectIgnoresOuterTest {
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
        @JoinColumn(name = "parent_id", referencedColumnName = "id")
        private IgnoreBean2 ignoreBean2;
    }

    @View("t_ignore_bean_2")
    public static class IgnoreBean2 {
        public Long name;
        @Embedded
        @AttributeOverrides({
                @AttributeOverride(name = "name", column = @Column(name = "name"))
        })
        IgnoreBean3 ignoreBean2;
    }

    public static class IgnoreBean3 {
        public Long name;
    }

    @Test
    public void should_has_distinct() {
        SQLBuilder<QueryResult> sqlBuilder = new SQLBuilder<>(new DSLQuery<QueryResult>(null, QueryResult.class)
                .selectIgnores("ignoreBean")
        );
        assertEquals("select distinct v_query_result.name name_ from v_query_result\n" +
                "left join t_ignore_bean ignoreBean_ on ignoreBean_.parent_id = v_query_result.id", sqlBuilder.build().getSql());
    }

    @Test
    public void should_set_params_not_select() {
        DSLQuery<QueryResult> dslQuery = new DSLQuery<>(null, QueryResult.class)
                .deepJoinIncludes("ignoreBean", "ignoreBean.ignoreBean2")
                .selectIgnores("ignoreBean", "ignoreBean.ignoreBean2");
        dslQuery.where("(and(ignoreBean.id equals 1))");
        SQLBuilder<QueryResult> sqlBuilder = new SQLBuilder<>(dslQuery);
        sqlBuilder.addParam("ignoreBean.id", "ignoreBean.id", "1");
        assertEquals(1L, sqlBuilder.getParams().get("ignoreBean.id"));
        assertEquals("select distinct v_query_result.name name_ from v_query_result\n" +
                "left join t_ignore_bean ignoreBean_ on ignoreBean_.parent_id = v_query_result.id\n" +
                "left join t_ignore_bean_2 ignoreBean_ignoreBean2_ on ignoreBean_ignoreBean2_.id = ignoreBean_.parent_id\n" +
                " where ((ignoreBean_.id = :p1))", sqlBuilder.build().getSql());
    }

    @Test
    public void should_not_read_ignored_field() throws SQLException {
        DefaultResultSetReader<QueryResult> reader = new DefaultResultSetReader<>(new DSLQuery<QueryResult>(null, QueryResult.class)
                .selectIgnores("ignoreBean"));
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getLong("ignoreBean_id_")).thenReturn(2L);
        QueryResult result = (QueryResult) reader.apply(resultSet);
        verify(resultSet, times(0)).getLong("ignoreBean_id_");
        assertNull(result.ignoreBean);
    }
}
