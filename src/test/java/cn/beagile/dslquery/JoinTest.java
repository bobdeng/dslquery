package cn.beagile.dslquery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.persistence.Column;
import javax.persistence.JoinColumn;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JoinTest {

    private DSLQuery<User> dslQuery;
    private SQLBuilder<User> sqlBuilder;

    @View("t_user")
    public static class User {
        @Column(name = "name")
        private String name;
        @JoinColumn(name = "org_id", referencedColumnName = "id")
        private Org org;
    }

    @View("t_org")
    public static class Org {
        @Column(name = "name")
        private String name;
    }
    @BeforeEach
    public void setup(){
        dslQuery = new DSLQuery<>(null, User.class);

    }
    @Test
    public void should_select_join() {
        sqlBuilder = new SQLBuilder<>(dslQuery, new ResultBean(dslQuery.getQueryResultClass()));
        assertEquals("select t_user.name name,org.name org_name from t_user\n" +
                "left join t_org org on org.id = t_user.org_id\n".trim(), sqlBuilder.sql().trim());
    }

    @Test
    public void should_read_join_fields() throws SQLException {
        long start=System.currentTimeMillis();
        DefaultResultSetReader<User> reader = new DefaultResultSetReader<>(new ResultBean(User.class));
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("name")).thenReturn("张三");
        when(resultSet.getString("org_name")).thenReturn("某公司");
        User result = reader.apply(resultSet);
        assertEquals("张三", result.name);
        assertNotNull(result.org);
        assertEquals("某公司", result.org.name);
        System.out.println(System.currentTimeMillis()-start);
    }
    @Test
    public void should_add_where(){
        dslQuery=dslQuery.where("(and(name equals 123))");
        sqlBuilder = new SQLBuilder<>(dslQuery, new ResultBean(dslQuery.getQueryResultClass()));
        String expect = "select t_user.name name,org.name org_name from t_user\n" +
                "left join t_org org on org.id = t_user.org_id\n" +
                " where ((t_user.name = :p1))";
        assertEquals(expect, sqlBuilder.sql());
    }
    @Test
    public void should_add_where_with_join_column(){
        dslQuery=dslQuery.where("(and(org.name equals 123))");
        sqlBuilder = new SQLBuilder<>(dslQuery, new ResultBean(dslQuery.getQueryResultClass()));
        String expect = "select t_user.name name,org.name org_name from t_user\n" +
                "left join t_org org on org.id = t_user.org_id\n" +
                " where ((org.name = :p1))";
        assertEquals(expect, sqlBuilder.sql());
    }
    @Test
    public void should_get_count_with_join_column(){
        dslQuery=dslQuery.where("(and(org.name equals 123))");
        sqlBuilder = new SQLBuilder<>(dslQuery, new ResultBean(dslQuery.getQueryResultClass()));
        String expect = "select count(*) from t_user\n" +
                "left join t_org org on org.id = t_user.org_id\n" +
                "where ((org.name = :p1))";
        assertEquals(expect, sqlBuilder.countSql());
    }
}
