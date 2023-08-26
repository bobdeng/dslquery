package cn.beagile.dslquery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Primary;

import javax.persistence.Column;
import javax.persistence.JoinColumn;

import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class IgnoreDeepJoinTest {

    private DSLQuery<User> dslQuery;
    private SQLBuilder<User> sqlBuilder;

    @View("t_user")
    public static class User {
        @Column(name = "name",unique = true)
        private String name;
        @JoinColumn(name = "org_id", referencedColumnName = "id")
        private Org org;
        @JoinColumn(name = "area_id", referencedColumnName = "id")
        private Area area;
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

    @BeforeEach
    public void setup() {
        dslQuery = new DSLQuery<>(null, User.class);

    }

    @Test
    public void should_select_join() {
        sqlBuilder = new SQLBuilder<>(dslQuery);
        assertEquals("select t_user.name name_,org_.name org_name_,area_.name area_name_ from t_user\n" +
                "left join t_org org_ on org_.id = t_user.org_id\n" +
                "left join t_area area_ on area_.id = t_user.area_id", sqlBuilder.sql()
        );
    }

    @Test
    public void should_select_count_join() {
        dslQuery.selectIgnores("org", "area");
        sqlBuilder = new SQLBuilder<>(dslQuery);
        assertEquals("select count(distinct t_user.name) from t_user\n" +
                "left join t_org org_ on org_.id = t_user.org_id\n" +
                "left join t_area area_ on area_.id = t_user.area_id", sqlBuilder.countSql()
        );
    }

    @Test
    public void should_select_count_join_withignore() {
        sqlBuilder = new SQLBuilder<>(dslQuery);
        assertEquals("select count(distinct t_user.name) from t_user\n" +
                "left join t_org org_ on org_.id = t_user.org_id\n" +
                "left join t_area area_ on area_.id = t_user.area_id", sqlBuilder.countSql()
        );
    }

    @Test
    public void should_not_read() {
        DefaultResultSetReader<User> reader = new DefaultResultSetReader<>(User.class);
        ResultSet resultSet = mock(ResultSet.class);
        User user = reader.apply(resultSet);
        assertNull(user.org.area);
    }


}
