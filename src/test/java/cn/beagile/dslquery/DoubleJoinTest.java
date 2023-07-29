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

public class DoubleJoinTest {

    private DSLQuery<User> dslQuery;
    private SQLBuilder<User> sqlBuilder;

    @View("t_user")
    public static class User {
        @Column(name = "name")
        private String name;
        @JoinColumn(name = "inner_id", referencedColumnName = "id", table = "t_inner")
        @JoinColumn(name = "org_id", referencedColumnName = "id")
        private Org org;
    }

    @View("t_org")
    public static class Org {
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
        assertEquals("select t_user.name name,t_org.name org_name from t_user\n" +
                "left join t_inner on t_inner.id = t_user.inner_id\n" +
                "left join t_org on t_org.id = t_inner.org_id", sqlBuilder.sql()
                );
    }

}
