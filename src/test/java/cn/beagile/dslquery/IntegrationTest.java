package cn.beagile.dslquery;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class)
//@Disabled
public class IntegrationTest {
    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;

    @View("person")
    @DeepJoinIncludes("org.area")
    public static class Person {
        @Column(name = "id")
        private Integer id;
        @Column(name = "name1")
        private String name;
        @Column(name = "age")
        private Integer age;
        @Column(name = "born_at")
        @DateFormat("yyyy-MM-dd HH:mm:ss")
        private Instant bornAt;
        @Embedded
        @AttributeOverrides({
                @AttributeOverride(name = "address", column = @Column(name = "address")),
                @AttributeOverride(name = "phone", column = @Column(name = "phone")),
                @AttributeOverride(name = "zipCode.code", column = @Column(name = "zip_code"))
        })
        private Contact contact;

        @Embeddable
        public static class Contact {
            private String address;
            @Column(name = "phone_number")
            private String phone;
            @Embedded
            private ZipCode zipCode;
        }

        @Embeddable
        public static class ZipCode {
            @Column(name = "zip_code1")
            private String code;
        }

        @JoinColumn(name = "org_id", referencedColumnName = "id")
        private Org org;
        @OneToMany(targetEntity = Child.class)
        @JoinColumn(name = "id", referencedColumnName = "personId")
        private List<Child> children;
    }

    @View("org")
    public static class Org {
        @Column(name = "name")
        private String name;
        @JoinColumn(name = "area_id", referencedColumnName = "id")
        private Area area;
    }

    @View("area")
    public static class Area {
        @Column(name = "name")
        private String name;
    }

    @View("children")
    public static class Child {
        @Column(name = "name")
        private String name;
        @Column(name = "person_id")
        private Integer personId;
        @Column(name = "id")
        private Integer id;
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
            System.out.println(sql);
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
    public void should_query_by_area() {
        DSLQuery<Person> query = new DSLQuery<>(new SpringQueryExecutor(jdbcTemplate), Person.class);
        List<Person> result = query.timezoneOffset(-8).where("(and(org.area.name equals cn))").skip(0).limit(10).query();
        assertEquals(1, result.size());
    }

    @Test
    public void sql_query() {
        List<SQLField> fields = List.of(new SQLField(new SQLField.ViewName("name"), new SQLField.SQLName("person.name1"), String.class));
        RawSQLBuilder where = new RawSQLBuilder(fields, "(and(name equals John smith))");
        String sql = "select person.name1 from person";
        String countSql = "select count(*) from person";
        Paging page = new Paging(0, 10);
        SQLQuery sqlQuery = where.toSQLQuery(sql, countSql, page);
        SpringQueryExecutor springQueryExecutor = new SpringQueryExecutor(jdbcTemplate);
        List<Map<String, Object>> list = springQueryExecutor.list(resultSet -> {
            try {
                return new ColumnMapRowMapper().mapRow(resultSet, 0);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, sqlQuery);
        assertEquals(1, list.size());

    }

    @Test
    public void sql_query_no_where_with_sort() {
        List<SQLField> fields = List.of(new SQLField(new SQLField.ViewName("name"), new SQLField.SQLName("person.name1"), String.class));
        RawSQLBuilder where = new RawSQLBuilder(fields, null, "name asc");
        String sql = "select person.name1 from person";
        String countSql = "select count(*) from person";
        Paging page = new Paging(0, 10);
        SQLQuery sqlQuery = where.toSQLQuery(sql, countSql, page);
        SpringQueryExecutor springQueryExecutor = new SpringQueryExecutor(jdbcTemplate);
        List<Map<String, Object>> list = springQueryExecutor.list(resultSet -> {
            try {
                return new ColumnMapRowMapper().mapRow(resultSet, 0);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, sqlQuery);
        assertEquals("bob robert", list.get(0).get("name1"));
        assertEquals(2, list.size());
        assertEquals(2,springQueryExecutor.count(sqlQuery));

    }

    @Test
    public void should_query() {

        DSLQuery<Person> query = new DSLQuery<>(new SpringQueryExecutor(jdbcTemplate), Person.class);
        List<Person> result = query.skip(0).limit(10).query();
        assertEquals(2, result.size());
        Person person = result.get(0);
        assertEquals("123 main st", person.contact.address);
        assertEquals("123456789", person.contact.phone);
        assertEquals("ms", person.org.name);
        assertEquals("cn", person.org.area.name);
    }

    @Test
    public void should_query_with_embedded() {

        DSLQuery<Person> query = new DSLQuery<>(new SpringQueryExecutor(jdbcTemplate), Person.class);
        List<Person> result = query.where("(and(contact.address contains 123))").skip(0).limit(10).query();
        assertEquals(1, result.size());
        assertEquals("123 main st", result.get(0).contact.address);
        assertEquals("123456789", result.get(0).contact.phone);
        assertEquals("12345", result.get(0).contact.zipCode.code);
    }

    @Test
    public void should_query_with_one2many() {

        DSLQuery<Person> query = new DSLQuery<>(new SpringQueryExecutor(jdbcTemplate), Person.class);
        List<Person> result = query.skip(0).limit(10).query();
        assertEquals(2, result.size());
        assertEquals(2, result.get(1).children.size());
        assertEquals(0, result.get(0).children.size());
    }

    @Test
    public void should_query_paged() {
        DSLQuery<Person> query = new DSLQuery<>(new SpringQueryExecutor(jdbcTemplate), Person.class);
        Paged<Person> result = query.skip(0).limit(10).pagedQuery();
        assertEquals(2, result.getResult().size());
        assertEquals(2, result.total());
    }

    @Test
    public void should_query_by_born_at() {
        DSLQuery<Person> query = new DSLQuery<>(new SpringQueryExecutor(jdbcTemplate), Person.class);
        List<Person> result = query.timezoneOffset(-8).where("(and(bornAt greaterthan 1980-01-01 12:00:00))").skip(0).limit(10).query();
        assertEquals(1, result.size());
    }

    @Test
    public void should_query_with_order() {
        DSLQuery<Person> query = new DSLQuery<>(new SpringQueryExecutor(jdbcTemplate), Person.class);
        List<Person> result = query.sort("age desc").skip(0).limit(10).query();
        assertEquals(2, result.size());
    }

    @Test
    public void should_query_with_int_in() {
        DSLQuery<Person> query = new DSLQuery<>(new SpringQueryExecutor(jdbcTemplate), Person.class);
        List<Person> result = query.where("(and(age in [42,55]))").skip(0).limit(10).query();
        assertEquals(1, result.size());
    }

    @Test
    public void should_query_with_string_in() {
        DSLQuery<Person> query = new DSLQuery<>(new SpringQueryExecutor(jdbcTemplate), Person.class);
        List<Person> result = query.where("(and(name in ['bob robert']))").skip(0).limit(10).query();
        assertEquals(1, result.size());
    }

    @Test
    public void should_return1_when_add_where() {
        DSLQuery<Person> query = new DSLQuery<>(new SpringQueryExecutor(jdbcTemplate), Person.class);
        List<Person> result = query
                .where("(and(name equals bob robert))")
                .skip(0).limit(10).query();
        assertEquals(1, result.size());
        Person person = result.get(0);
        assertEquals("bob robert", person.name);
    }

    @Test
    public void should_return_empty_when_wrong_page() {
        DSLQuery<Person> query = new DSLQuery<>(new SpringQueryExecutor(jdbcTemplate), Person.class);
        List<Person> result = query.skip(10).limit(10).query();
        assertEquals(0, result.size());
    }
}
