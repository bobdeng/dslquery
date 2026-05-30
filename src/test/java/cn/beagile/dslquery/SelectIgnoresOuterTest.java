package cn.beagile.dslquery;

import org.junit.jupiter.api.Test;

import javax.persistence.*;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

public class SelectIgnoresOuterTest {
    private String nullsOrder="";

    @View(value = "v_query_result", distinct = true)
    public static class QueryResult {
        @Column(name = "id",unique = true)
        private Long id;
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
        DSLSQLBuilder<QueryResult> sqlBuilder = new DSLSQLBuilder<>(new DSLQuery<QueryResult>(null, QueryResult.class)
                .selectIgnores("ignoreBean")
        );
        assertEquals("select distinct v_query_result.id id_,v_query_result.name name_ from v_query_result\n" +
                "left join t_ignore_bean ignoreBean_ on ignoreBean_.parent_id = v_query_result.id", sqlBuilder.build(nullsOrder).getSql());
        assertEquals("select count(distinct v_query_result.id) from v_query_result\n" +
                "left join t_ignore_bean ignoreBean_ on ignoreBean_.parent_id = v_query_result.id", sqlBuilder.countSql());
    }

    @Test
    public void should_set_params_not_select() {
        DSLQuery<QueryResult> dslQuery = new DSLQuery<>(null, QueryResult.class)
                .deepJoinIncludes("ignoreBean", "ignoreBean.ignoreBean2")
                .selectIgnores("ignoreBean", "ignoreBean.ignoreBean2");
        dslQuery.where("(and(ignoreBean.id equals 1))");
        DSLSQLBuilder<QueryResult> sqlBuilder = new DSLSQLBuilder<>(dslQuery);
        sqlBuilder.addParam("ignoreBean.id", "ignoreBean.id", "1");
        assertEquals(1L, sqlBuilder.getParams().get("ignoreBean.id"));
        assertEquals("select distinct v_query_result.id id_,v_query_result.name name_ from v_query_result\n" +
                "left join t_ignore_bean ignoreBean_ on ignoreBean_.parent_id = v_query_result.id\n" +
                "left join t_ignore_bean_2 ignoreBean_ignoreBean2_ on ignoreBean_ignoreBean2_.id = ignoreBean_.parent_id\n" +
                " where ((ignoreBean_.id = :p0))", sqlBuilder.build(nullsOrder).getSql());
    }

    @Test
    public void should_not_select_embedded_when_join_ignore() {
        DSLQuery<QueryResult> dslQuery = new DSLQuery<>(null, QueryResult.class)
                .deepJoinIncludes("ignoreBean", "ignoreBean.ignoreBean2")
                .selectIgnores("ignoreBean", "ignoreBean.ignoreBean2");
        DSLSQLBuilder<QueryResult> sqlBuilder = new DSLSQLBuilder<>(dslQuery);
        assertEquals("select distinct v_query_result.id id_,v_query_result.name name_ from v_query_result\n" +
                "left join t_ignore_bean ignoreBean_ on ignoreBean_.parent_id = v_query_result.id\n" +
                "left join t_ignore_bean_2 ignoreBean_ignoreBean2_ on ignoreBean_ignoreBean2_.id = ignoreBean_.parent_id", sqlBuilder.build(nullsOrder).getSql());
    }

    @Test
    public void should_select_embedded() {
        DSLQuery<QueryResult> dslQuery = new DSLQuery<>(null, QueryResult.class)
                .deepJoinIncludes("ignoreBean", "ignoreBean.ignoreBean2")
                .selectIgnores();
        DSLSQLBuilder<QueryResult> sqlBuilder = new DSLSQLBuilder<>(dslQuery);
        assertEquals("select distinct v_query_result.id id_,v_query_result.name name_,ignoreBean_.id ignoreBean_id_,ignoreBean_ignoreBean2_.name ignoreBean_ignoreBean2_ignoreBean2_name_ from v_query_result\n" +
                "left join t_ignore_bean ignoreBean_ on ignoreBean_.parent_id = v_query_result.id\n" +
                "left join t_ignore_bean_2 ignoreBean_ignoreBean2_ on ignoreBean_ignoreBean2_.id = ignoreBean_.parent_id", sqlBuilder.build(nullsOrder).getSql());
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

    @Test
    public void should_ignore_root_column_in_select() {
        DSLQuery<QueryResult> dslQuery = new DSLQuery<>(null, QueryResult.class)
                .selectIgnores("name");
        DSLSQLBuilder<QueryResult> sqlBuilder = new DSLSQLBuilder<>(dslQuery);
        String sql = sqlBuilder.build(nullsOrder).getSql();
        // "name" root column excluded, but "id" root column and join column "ignoreBean_.id" remain
        assertEquals("select distinct v_query_result.id id_,ignoreBean_.id ignoreBean_id_ from v_query_result\n" +
                "left join t_ignore_bean ignoreBean_ on ignoreBean_.parent_id = v_query_result.id", sql);
    }

    @Test
    public void should_ignore_all_root_columns_in_select() {
        DSLQuery<QueryResult> dslQuery = new DSLQuery<>(null, QueryResult.class)
                .selectIgnores("name", "id");
        DSLSQLBuilder<QueryResult> sqlBuilder = new DSLSQLBuilder<>(dslQuery);
        String sql = sqlBuilder.build(nullsOrder).getSql();
        // Both root columns ("id", "name") excluded, but join column "ignoreBean_.id" remains
        assertEquals("select distinct ignoreBean_.id ignoreBean_id_ from v_query_result\n" +
                "left join t_ignore_bean ignoreBean_ on ignoreBean_.parent_id = v_query_result.id", sql);
    }

    @Test
    public void should_not_read_ignored_root_column() throws SQLException {
        DSLQuery<QueryResult> dslQuery = new DSLQuery<>(null, QueryResult.class)
                .selectIgnores("name");
        DefaultResultSetReader<QueryResult> reader = new DefaultResultSetReader<>(dslQuery);
        ResultSet resultSet = mock(ResultSet.class);
        // if reader tries to read name_ column, mock throws — simulating it's excluded from SQL
        when(resultSet.getString("name_")).thenThrow(new SQLException("column not found"));

        // Should not throw because "name" is ignored and should not be read
        reader.apply(resultSet);
        verify(resultSet, times(0)).getString("name_");
    }
}
