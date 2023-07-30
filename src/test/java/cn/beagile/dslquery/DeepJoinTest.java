package cn.beagile.dslquery;

import org.junit.jupiter.api.Test;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeepJoinTest {
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
        @JoinColumn(name = "area_id", referencedColumnName = "id")
        private Area area;
    }

    @View("t_area")
    public static class Area {
        @Column(name = "name")
        private String name;
    }

    @Test
    public void should_select_join() {
        DSLQuery<User> dslQuery = new DSLQuery<>(null, User.class);
        SQLBuilder<User> sqlBuilder = new SQLBuilder<>(dslQuery);
        assertEquals("select t_user.name name,org.name org_name,org_area.name org_area_name from t_user\n" +
                "left join t_org org on org.id = t_user.org_id\n" +
                "left join t_area org_area on org_area.id = org.area_id", sqlBuilder.sql());
    }

    @Test
    public void should_read_join_fields() throws SQLException {
        DefaultResultSetReader<User> reader = new DefaultResultSetReader<>(new ResultBean(User.class));
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("name")).thenReturn("张三");
        when(resultSet.getString("org_name")).thenReturn("某公司");
        when(resultSet.getString("org_area_name")).thenReturn("某地区");
        User result = reader.apply(resultSet);
        assertEquals("张三", result.name);
        assertNotNull(result.org);
        assertEquals("某公司", result.org.name);
        assertNotNull(result.org.area);
        assertEquals("某地区", result.org.area.name);
    }
    @Test
    public void should_add_where_with_join_column(){
        DSLQuery<User> dslQuery = new DSLQuery<>(null, User.class);
        dslQuery=dslQuery.where("(and(org.area.name equals 123))");
        SQLBuilder sqlBuilder = new SQLBuilder<>(dslQuery);
        String expect = "select t_user.name name,org.name org_name,org_area.name org_area_name from t_user\n" +
                "left join t_org org on org.id = t_user.org_id\n" +
                "left join t_area org_area on org_area.id = org.area_id\n" +
                " where ((org_area.name = :p1))";
        assertEquals(expect, sqlBuilder.sql());
    }

}
