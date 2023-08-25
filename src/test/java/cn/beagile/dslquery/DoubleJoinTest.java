package cn.beagile.dslquery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DoubleJoinTest {

    private DSLQuery<User> dslQuery;
    private SQLBuilder<User> sqlBuilder;

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

    @BeforeEach
    public void setup() {
        dslQuery = new DSLQuery<>(null, User.class);
        sqlBuilder = new SQLBuilder<>(dslQuery);
    }

    @Test
    public void params() throws SQLException {
        DefaultResultSetReader<User> reader = new DefaultResultSetReader<>(User.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("org_area_name_")).thenReturn("area");
        when(resultSet.getString("org_name_")).thenReturn("org");
        User user = reader.apply(resultSet);
        assertNotNull(user.org.name);
        assertNotNull(user.org.area.name);
    }

}
