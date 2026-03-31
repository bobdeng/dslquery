package cn.beagile.dslquery;

import org.junit.jupiter.api.Test;

import javax.persistence.Column;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SimpleDynamicJoinTest {
    private final String nullsOrder = "";

    @View("person")
    public static class Person {
        @Column(name = "id")
        private Integer id;

        @Column(name = "name")
        private String name;

        @Column(name = "org_id")
        private Integer orgId;

        @DynamicJoin(joinKey = "org_id", targetKey = "id")
        private OrgStats orgStats;

        public Integer getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public Integer getOrgId() {
            return orgId;
        }

        public OrgStats getOrgStats() {
            return orgStats;
        }
    }

    public static class OrgStats {
        @Column(name = "id")
        private Integer id;

        @Column(name = "total_employees")
        private Integer totalEmployees;

        public Integer getId() {
            return id;
        }

        public Integer getTotalEmployees() {
            return totalEmployees;
        }
    }

    @Test
    void should_generate_dynamic_join_sql_basic() {
        List<SQLField> fields = List.of(
                new SQLField(
                        new SQLField.ViewName("id"),
                        new SQLField.SQLName("org.id"),
                        Integer.class
                ),
                new SQLField(
                        new SQLField.ViewName("totalEmployees"),
                        new SQLField.SQLName("count(*)"),
                        Integer.class
                )
        );

        // 使用单参数构造函数避免歧义
        RawSQLBuilder statsBuilder = new RawSQLBuilder(fields);

        String subQuerySql = "select org.id, count(*) as total_employees from employee group by org.id";

        DSLQuery<Person> dslQuery = new DSLQuery<>(null, Person.class)
                .dynamicJoin("orgStats", statsBuilder, subQuerySql);

        DSLSQLBuilder<Person> sqlBuilder = new DSLSQLBuilder<>(dslQuery);
        SQLQuery sqlQuery = sqlBuilder.build(nullsOrder);

        System.out.println("Generated SQL:");
        System.out.println(sqlQuery.getSql());

        assertTrue(sqlQuery.getSql().contains("left join ("));
        assertTrue(sqlQuery.getSql().contains("orgStats_"));
        assertTrue(sqlQuery.getSql().contains("orgStats_.id = person.org_id"));
    }
}
