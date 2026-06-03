package cn.beagile.dslquery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DoubleJoinTest {

    private DSLQuery<User> dslQuery;
    private DSLSQLBuilder<User> sqlBuilder;
    private String nullsOrder="";

    @View("t_user")
    @DeepJoinIncludes({"org.area"})
    public static class User {
        @Column(name = "name")
        private String name;
        @JoinColumns({
                @JoinColumn(name = "inner_id", referencedColumnName = "id", table = "t_inner"),
                @JoinColumn(name = "org_id", referencedColumnName = "id")
        })
        private Org org;
    }

    @View("t_org")
    public static class Org {
        @Column(name = "name")
        private String name;
        @JoinColumns({
                @JoinColumn(name = "id", referencedColumnName = "org_id", table = "t_org_area"),
                @JoinColumn(name = "area_id", referencedColumnName = "id")
        })
        public Area area;
    }

    @View("t_area")
    public static class Area {
        @Column(name = "name")
        private String name;
    }

    @View("t_user")
    @DeepJoinIncludes({"third"})
    public static class UserWithThreeJoinColumns {
        @Column(name = "name")
        private String name;
        @JoinColumns({
                @JoinColumn(name = "first_id", referencedColumnName = "id", table = "t_first_second"),
                @JoinColumn(name = "second_id", referencedColumnName = "id", table = "t_second_third"),
                @JoinColumn(name = "third_id", referencedColumnName = "id")
        })
        private Third third;
    }

    @View("t_third")
    public static class Third {
        @Column(name = "name")
        private String name;
    }

    @BeforeEach
    public void setup() {
        dslQuery = new DSLQuery<>(null, User.class);
        sqlBuilder = new DSLSQLBuilder<>(dslQuery);
    }

    @Test
    public void params() throws SQLException {
        DefaultResultSetReader<User> reader = new DefaultResultSetReader<>(new DSLQuery<>(null, User.class));
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("org_area_name_")).thenReturn("area");
        when(resultSet.getString("org_name_")).thenReturn("org");
        User user = reader.apply(resultSet);
        assertNotNull(user.org.name);
        assertNotNull(user.org.area.name);
    }

    @Test
    public void should_select_join() {
        dslQuery.selectIgnores("org");
        sqlBuilder = new DSLSQLBuilder<>(dslQuery);
        assertEquals("select t_user.name name_ from t_user\n" +
                "left join t_inner on t_inner.id = t_user.inner_id\n" +
                "left join t_org org_ on org_.id = t_inner.org_id\n" +
                "left join t_org_area on t_org_area.org_id = org_.id\n" +
                "left join t_area org_area_ on org_area_.id = t_org_area.area_id", sqlBuilder.sql(nullsOrder)
        );
    }

    @Test
    public void should_select_join_with_three_join_columns() {
        DSLQuery<UserWithThreeJoinColumns> dslQuery = new DSLQuery<>(null, UserWithThreeJoinColumns.class);
        DSLSQLBuilder<UserWithThreeJoinColumns> sqlBuilder = new DSLSQLBuilder<>(dslQuery);
        assertEquals("select t_user.name name_,third_.name third_name_ from t_user\n" +
                "left join t_first_second on t_first_second.id = t_user.first_id\n" +
                "left join t_second_third on t_second_third.id = t_first_second.second_id\n" +
                "left join t_third third_ on third_.id = t_second_third.third_id", sqlBuilder.sql(nullsOrder)
        );
    }

    @Test
    public void params_with_three_join_columns() throws SQLException {
        DefaultResultSetReader<UserWithThreeJoinColumns> reader = new DefaultResultSetReader<>(
                new DSLQuery<>(null, UserWithThreeJoinColumns.class));
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("name_")).thenReturn("user");
        when(resultSet.getString("third_name_")).thenReturn("third");
        UserWithThreeJoinColumns user = reader.apply(resultSet);
        assertEquals("user", user.name);
        assertNotNull(user.third);
        assertEquals("third", user.third.name);
    }

    @Test
    public void should_select_join1() {
        dslQuery.selectIgnores("org.area");
        dslQuery.where("(and(org.area.name eq area))");
        sqlBuilder = new DSLSQLBuilder<>(dslQuery);
        assertDoesNotThrow(() -> sqlBuilder.sql(nullsOrder));
    }

}
