package cn.beagile.dslquery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.persistence.*;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
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
        @Embedded
        @AttributeOverrides({
                @AttributeOverride(name = "name", column = @Column(name = "contact_name"))
        })
        private UserContact userContact;
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
    public void should_read_join_fields() throws SQLException {
        DefaultResultSetReader<User> reader = new DefaultResultSetReader<>(dslQuery.getQueryResultClass());
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("name_")).thenReturn("张三");
        when(resultSet.getString("org_name_")).thenReturn("某公司");
        User result = reader.apply(resultSet);
        assertEquals("张三", result.name);
        assertNotNull(result.org);
        assertEquals("某公司", result.org.name);
    }

    @Test
    public void should_select_join_fields_with_embedded() {
        ColumnFields columnFields = new ColumnFields(User.class);
        columnFields.selectFields().stream().map(ColumnField::expression).forEach(System.out::println);
        assertTrue(columnFields.selectFields().stream().map(ColumnField::expression).anyMatch(select -> select.contains("org_.contact_name org_userContact_name_")));

    }

    @Test
    public void should_read_join_fields_with_embedded() throws SQLException {
        DefaultResultSetReader<User> reader = new DefaultResultSetReader<>(dslQuery.getQueryResultClass());
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("org_userContact_name_")).thenReturn("张三");
        User result = reader.apply(resultSet);
        assertEquals("张三", result.org.userContact.name);
    }
}
