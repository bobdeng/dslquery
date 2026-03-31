package cn.beagile.dslquery;

import javax.persistence.Column;import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DynamicJoinTest {
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
    }

    public static class OrgStats {
        @Column(name = "id")
        private Integer id;

        @Column(name = "total_employees")
        private Integer totalEmployees;

        @Column(name = "avg_salary")
        private Double avgSalary;
    }

    @Test
    void should_generate_dynamic_join_sql() {
        List<SQLField> fields = List.of(
                new SQLField(
                        new SQLField.ViewName("id"),
                        new SQLField.SQLName("org.id"),
                        Integer.class
                ),
                new SQLField(
                        new SQLField.ViewName("status"),
                        new SQLField.SQLName("org.status"),
                        String.class
                ),
                new SQLField(
                        new SQLField.ViewName("totalEmployees"),
                        new SQLField.SQLName("count(*)"),
                        Integer.class
                ),
                new SQLField(
                        new SQLField.ViewName("avgSalary"),
                        new SQLField.SQLName("avg(salary)"),
                        Double.class
                )
        );

        RawSQLBuilder statsBuilder = new RawSQLBuilder(fields, null, new String[]{"(and(status eq active))"});

        String subQuerySql = """
                select org.id, count(*) as total_employees, avg(salary) as avg_salary
                from employee
                join org on org.id = employee.org_id
                ${where}
                group by org.id
                order by org.id asc
                """;

        DSLQuery<Person> dslQuery = new DSLQuery<>(null, Person.class)
                .dynamicJoin("orgStats", statsBuilder, subQuerySql)
                .where("(and(name contains John))");

        DSLSQLBuilder<Person> sqlBuilder = new DSLSQLBuilder<>(dslQuery);
        SQLQuery sqlQuery = sqlBuilder.build(nullsOrder);

        String expectedSql = """
                select person.id id_,person.name name_,person.org_id orgId_,orgStats_.id orgStats_id_,orgStats_.total_employees orgStats_totalEmployees_,orgStats_.avg_salary orgStats_avgSalary_ from person
                left join (
                select org.id, count(*) as total_employees, avg(salary) as avg_salary
                from employee
                join org on org.id = employee.org_id
                where ((org.status = :dj_orgStats_p0))
                group by org.id
                order by org.id asc
                ) orgStats_ on orgStats_.id = person.org_id
                where ((person.name like :p0))""";

        assertEquals(expectedSql.replaceAll("\\s+", " ").trim(),
                sqlQuery.getSql().replaceAll("\\s+", " ").trim());
        assertEquals("active", sqlQuery.getParams().get("dj_orgStats_p0"));
        assertEquals("%John%", sqlQuery.getParams().get("p0"));
    }

    @Test
    void should_support_multiple_dynamic_joins() {
        List<SQLField> orgFields = List.of(
                new SQLField(new SQLField.ViewName("id"), new SQLField.SQLName("org.id"), Integer.class),
                new SQLField(new SQLField.ViewName("totalEmployees"), new SQLField.SQLName("count(*)"), Integer.class)
        );

        RawSQLBuilder orgBuilder = new RawSQLBuilder(orgFields, null, new String[]{"(and(status eq active))"});

        String orgSql = "select org.id, count(*) as total_employees from employee join org on org.id = employee.org_id ${where} group by org.id";

        DSLQuery<Person> dslQuery = new DSLQuery<>(null, Person.class)
                .dynamicJoin("orgStats", orgBuilder, orgSql);

        DSLSQLBuilder<Person> sqlBuilder = new DSLSQLBuilder<>(dslQuery);
        SQLQuery sqlQuery = sqlBuilder.build(nullsOrder);

        assertTrue(sqlQuery.getSql().contains("left join ("));
        assertTrue(sqlQuery.getSql().contains("orgStats_"));
    }
}
