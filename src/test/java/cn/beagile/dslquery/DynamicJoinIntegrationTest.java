package cn.beagile.dslquery;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.Column;
import java.sql.ResultSet;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class)
public class DynamicJoinIntegrationTest {
    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;

    @View("person")
    public static class Person {
        @Column(name = "id")
        private Integer id;

        @Column(name = "name1")
        private String name;

        @Column(name = "org_id")
        private Integer orgId;

        @DynamicJoin(joinKey = "org_id", targetKey = "org_id")
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
        @Column(name = "org_id")
        private Integer orgId;

        @Column(name = "person_count")
        private Integer personCount;

        @Column(name = "org_name")
        private String orgName;

        public Integer getOrgId() {
            return orgId;
        }

        public Integer getPersonCount() {
            return personCount;
        }

        public String getOrgName() {
            return orgName;
        }
    }

    public static class SpringQueryExecutor implements QueryExecutor {
        private final NamedParameterJdbcTemplate jdbcTemplate;

        public SpringQueryExecutor(NamedParameterJdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        @Override
        public <T> List<T> list(Function<ResultSet, T> resultSetReader, SQLQuery sqlQuery) {
            String sql = sqlQuery.getSql();
            if (sqlQuery.getLimit() != null) {
                sql += " limit " + sqlQuery.getSkip() + "," + sqlQuery.getLimit();
            }
            System.out.println("Executing SQL:");
            System.out.println(sql);
            System.out.println("Parameters: " + sqlQuery.getParams());
            return jdbcTemplate.query(sql, sqlQuery.getParams(), (rs, rowNum) -> resultSetReader.apply(rs));
        }

        @Override
        public int count(SQLQuery sqlQuery) {
            return jdbcTemplate.query(sqlQuery.getCountSql(), sqlQuery.getParams(), (rs, rowNum) -> rs.getInt(1)).get(0);
        }

        @Override
        public String nullsOrder(NullsOrder nullsOrder) {
            return switch (nullsOrder) {
                case NULL_FIRST -> "nulls first";
                case NULL_LAST -> "nulls last";
                default -> "";
            };
        }
    }

    @Test
    public void should_query_with_dynamic_join() {
        // 定义动态Join的字段映射
        List<SQLField> fields = List.of(

        );

        // 创建RawSQLBuilder
        RawSQLBuilder statsBuilder = new RawSQLBuilder(fields);

        // 定义子查询SQL
        String subQuerySql = """
                select org.id as org_id, count(*) as person_count, org.name as org_name
                from person
                join org on org.id = person.org_id
                group by org.id, org.name
                """;

        // 执行查询
        DSLQuery<Person> query = new DSLQuery<>(new SpringQueryExecutor(jdbcTemplate), Person.class);
        List<Person> result = query
                .dynamicJoin("orgStats", statsBuilder, subQuerySql)
                .query();

        // 验证结果
        assertNotNull(result);
        assertFalse(result.isEmpty());

        // 验证第一个结果
        Person person = result.get(0);
        assertNotNull(person.getId());
        assertNotNull(person.getName());
        assertNotNull(person.getOrgId());

        // 验证动态Join的结果
        assertNotNull(person.getOrgStats());
        assertNotNull(person.getOrgStats().getOrgId());
        assertNotNull(person.getOrgStats().getPersonCount());
        assertNotNull(person.getOrgStats().getOrgName());

        System.out.println("Person: " + person.getName() +
                          ", Org: " + person.getOrgStats().getOrgName() +
                          ", Person Count: " + person.getOrgStats().getPersonCount());
    }

    @Test
    public void should_query_with_dynamic_join_and_where() {
        // 定义动态Join的字段映射
        List<SQLField> fields = List.of(
                new SQLField(
                        new SQLField.ViewName("orgId"),
                        new SQLField.SQLName("org.id"),
                        Integer.class
                ),
                new SQLField(
                        new SQLField.ViewName("personCount"),
                        new SQLField.SQLName("count(*)"),
                        Integer.class
                ),
                new SQLField(
                        new SQLField.ViewName("orgName"),
                        new SQLField.SQLName("org.name"),
                        String.class
                )
        );

        // 创建带过滤条件的RawSQLBuilder
        RawSQLBuilder statsBuilder = new RawSQLBuilder(fields);

        // 定义子查询SQL
        String subQuerySql = """
                select org.id as org_id, count(*) as person_count, org.name as org_name
                from person
                join org on org.id = person.org_id
                ${where}
                group by org.id, org.name
                """;

        // 执行查询，同时在主查询和子查询中都有条件
        DSLQuery<Person> query = new DSLQuery<>(new SpringQueryExecutor(jdbcTemplate), Person.class);
        List<Person> result = query
                .dynamicJoin("orgStats", statsBuilder, subQuerySql)
                .where("(and(name contains bob))")
                .query();

        // 验证结果
        assertNotNull(result);

        if (!result.isEmpty()) {
            Person person = result.get(0);
            assertTrue(person.getName().toLowerCase().contains("bob"));
            assertNotNull(person.getOrgStats());

            System.out.println("Filtered Person: " + person.getName() +
                              ", Org: " + person.getOrgStats().getOrgName() +
                              ", Person Count: " + person.getOrgStats().getPersonCount());
        }
    }
}
