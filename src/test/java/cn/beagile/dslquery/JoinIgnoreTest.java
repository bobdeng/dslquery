package cn.beagile.dslquery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.persistence.Column;
import javax.persistence.JoinColumn;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JoinIgnoreTest {

    private DSLQuery<User> dslQuery;
    private SQLBuilder<User> sqlBuilder;

    @View("t_user")
    public static class User {
        @Column(name = "name")
        private String name;
        @JoinColumn(name = "org_id", referencedColumnName = "id")
        @Ignores({Area.class})
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

    @BeforeEach
    public void setup() {
        dslQuery = new DSLQuery<>(null, User.class);

    }

    @Test
    public void should_select_join() {
        sqlBuilder = new SQLBuilder<>(dslQuery, new ResultBean(dslQuery.getQueryResultClass()));
        assertEquals("select t_user.name name,t_org.name org_name from t_user\n" +
                "left join t_org on t_org.id = t_user.org_id", sqlBuilder.sql()
        );
    }

}
