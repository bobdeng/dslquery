package cn.beagile.dslquery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.persistence.Column;
import javax.persistence.JoinColumn;

import static org.junit.jupiter.api.Assertions.*;

public class JoinIgnoreTest {

    private DSLQuery<User> dslQuery;
    private SQLBuilder<User> sqlBuilder;

    @View("t_user")
    @Ignores({"org.area"})
    public static class User {
        @Column(name = "name")
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
    public void should_has_no_ignored_column(){
        FieldsWithColumns fieldsWithColumns = new FieldsWithColumns(new ResultBean(dslQuery.getQueryResultClass()));
        assertFalse(fieldsWithColumns.findFieldColumn("org.area").isPresent());
        assertTrue(fieldsWithColumns.findFieldColumn("org.name").isPresent());
        assertTrue(fieldsWithColumns.findFieldColumn("area.name").isPresent());
    }
    @Test
    public void should_select_join() {
        sqlBuilder = new SQLBuilder<>(dslQuery);
        assertEquals("select t_user.name name,org.name org_name,area.name area_name from t_user\n" +
                "left join t_org org on org.id = t_user.org_id\n" +
                "left join t_area area on area.id = t_user.area_id", sqlBuilder.sql()
        );
    }

}
