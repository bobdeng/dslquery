package cn.beagile.dslquery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.persistence.*;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JoinAndEmbeddedTest {

    private DSLQuery<User> dslQuery;
    private SQLBuilder<User> sqlBuilder;

    @View("t_user")
    public static class User {
        @Column(name = "name")
        private String name;
        @JoinColumn(name = "org_id", referencedColumnName = "id")
        private Org org;
        @Embedded
        @AttributeOverrides({
                @AttributeOverride(name = "name", column = @Column(name = "contact_name"))
        })
        private UserContact userContact;
    }

    @View("t_org")
    public static class Org {
        @Column(name = "name")
        private String name;
    }

    @View("user_contact")
    @Embeddable
    public static class UserContact {
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
        assertEquals("select t_user.name name,t_user.contact_name contact_name,t_org.name org_name from t_user\n" +
                "left join t_org on t_org.id = t_user.org_id", sqlBuilder.sql());
    }

    @Test
    public void should_read_join_fields() throws SQLException {
        DefaultResultSetReader<User> reader = new DefaultResultSetReader<>(User.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("name")).thenReturn("张三");
        when(resultSet.getString("org_name")).thenReturn("某公司");
        User result = reader.apply(resultSet);
        assertEquals("张三", result.name);
        assertNotNull(result.org);
        assertEquals("某公司", result.org.name);
    }
}
