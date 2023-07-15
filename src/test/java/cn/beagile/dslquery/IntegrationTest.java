package cn.beagile.dslquery;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.*;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class)
public class IntegrationTest {
    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;

    @View("person")
    public static class Person {
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
                @AttributeOverride(name = "phone", column = @Column(name = "phone"))
        })
        private Contact contact;

        @Embeddable
        public static class Contact {
            private String address;
            @Column(name = "phone_number")
            private String phone;
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
            return jdbcTemplate.query(sql, sqlQuery.getParams(), (rs, rowNum) -> resultSetReader.apply(rs));
        }

        @Override
        public int count(SQLQuery sqlQuery) {
            return jdbcTemplate.query(sqlQuery.getCountSql(), sqlQuery.getParams(), (rs, rowNum) -> rs.getInt(1)).get(0);
        }
    }

    @Test
    public void should_query() {

        DSLQuery<Person> query = new DSLQuery<>(new SpringQueryExecutor(jdbcTemplate), Person.class);
        List<Person> result = query.skip(0).limit(10).query();
        assertEquals(2, result.size());
        assertEquals("123 main st", result.get(0).contact.address);
        assertEquals("123456789", result.get(0).contact.phone);
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
