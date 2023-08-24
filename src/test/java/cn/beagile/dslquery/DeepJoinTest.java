package cn.beagile.dslquery;

import org.junit.jupiter.api.Test;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeepJoinTest {
    @View("t_user")
    @DeepJoinIncludes({"org.area","org.city", "org.city.area"})
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
        @JoinColumn(name = "city_id", referencedColumnName = "id")
        private City city;
    }

    @View("t_city")
    public static class City {
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
        assertEquals("select t_user.name name_,org_.name org_name_,org_area_.name org_area_name_,org_city_.name org_city_name_,org_city_area_.name org_city_area_name_ from t_user\n" +
                "left join t_org org_ on org_.id = t_user.org_id\n" +
                "left join t_area org_area_ on org_area_.id = org_.area_id\n" +
                "left join t_city org_city_ on org_city_.id = org_.city_id\n" +
                "left join t_area org_city_area_ on org_city_area_.id = org_city_.area_id", sqlBuilder.sql());
    }

    @Test
    public void should_read_join_fields() throws SQLException {
        DefaultResultSetReader<User> reader = new DefaultResultSetReader<>(User.class, Arrays.asList());
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("name_")).thenReturn("张三");
        when(resultSet.getString("org_name_")).thenReturn("某公司");
        when(resultSet.getString("org_area_name_")).thenReturn("某地区");
        User result = reader.apply(resultSet);
        assertEquals("张三", result.name);
        assertNotNull(result.org);
        assertEquals("某公司", result.org.name);
        assertNotNull(result.org.area);
        assertEquals("某地区", result.org.area.name);
    }

    @Test
    public void should_add_where_with_join_column() {
        DSLQuery<User> dslQuery = new DSLQuery<>(null, User.class);
        dslQuery = dslQuery.where("(and(org.area.name equals 123))");
        SQLBuilder sqlBuilder = new SQLBuilder<>(dslQuery);
        String expect = "select t_user.name name_,org_.name org_name_,org_area_.name org_area_name_,org_city_.name org_city_name_,org_city_area_.name org_city_area_name_ from t_user\n" +
                "left join t_org org_ on org_.id = t_user.org_id\n" +
                "left join t_area org_area_ on org_area_.id = org_.area_id\n" +
                "left join t_city org_city_ on org_city_.id = org_.city_id\n" +
                "left join t_area org_city_area_ on org_city_area_.id = org_city_.area_id\n" +
                " where ((org_area_.name = :p1))";
        assertEquals(expect, sqlBuilder.sql());
    }

}
