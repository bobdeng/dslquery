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
    private ResultBean resultBean;

    @View("t_user")
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
        resultBean = new ResultBean(dslQuery.getQueryResultClass());
        sqlBuilder = new SQLBuilder<>(dslQuery, resultBean);
    }

    @Test
    public void should_select_join() {

        assertEquals("select t_user.name name,org.name org_name,org_area.name org_area_name from t_user\n" +
                "left join t_inner org on org.id = t_user.inner_id\n" +
                "left join t_org org on org.id = t_inner.org_id\n" +
                "left join t_org_area org_area on org_area.org_id = org.id\n" +
                "left join t_area org_area on org_area.id = t_org_area.area_id", sqlBuilder.sql()
        );
    }

    @Test
    public void params() throws SQLException {
        DefaultResultSetReader<User> reader = new DefaultResultSetReader<>(resultBean);
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("org_area_name")).thenReturn("area");
        when(resultSet.getString("org_name")).thenReturn("org");
        User user = reader.apply(resultSet);
        assertNotNull(user.org.name);
        assertNotNull(user.org.area.name);
    }

}
