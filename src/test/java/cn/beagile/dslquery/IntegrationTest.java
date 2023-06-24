package cn.beagile.dslquery;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

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
        @Column("name1")
        private String name;
        @Column("age")
        private Integer age;
        @Column("born_at")
        @DateFormat("yyyy-MM-dd HH:mm:ss")
        private Instant bornAt;
    }

    public static class SpringQueryExecutor implements QueryExecutor {
        private final NamedParameterJdbcTemplate jdbcTemplate;

        public SpringQueryExecutor(NamedParameterJdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        @Override
        public <T> List<T> list(SQLBuilder sqlQuery, Function<ResultSet, T> resultSetReader) {
            String sql = sqlQuery.sql();
            if (sqlQuery.limit() != null) {
                sql += " limit " + sqlQuery.skip() + "," + sqlQuery.limit();
            }
            return jdbcTemplate.query(sql, sqlQuery.getParams(), (rs, rowNum) -> resultSetReader.apply(rs));
        }

        @Override
        public int count(SQLBuilder sqlQuery) {
            return jdbcTemplate.query(sqlQuery.countSql(), sqlQuery.getParams(), (rs, rowNum) -> rs.getInt(1)).get(0);
        }
    }

    @Test
    public void should_query() {
        DSLQuery<Person> query = new DSLQuery<>(new SpringQueryExecutor(jdbcTemplate), Person.class);
        List<Person> result = query.skip(0).limit(10).query();
        assertEquals(2, result.size());
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
        DSLQuery query = new DSLQuery(new SpringQueryExecutor(jdbcTemplate), Person.class);
        List result = query.sort("age desc").skip(0).limit(10).query();
        assertEquals(2, result.size());
    }

    @Test
    public void should_return1_when_add_where() {
        DSLQuery query = new DSLQuery(new SpringQueryExecutor(jdbcTemplate), Person.class);
        List<Person> result = query
                .where("(and(name equal bob))")
                .skip(0).limit(10).query();
        assertEquals(1, result.size());
        Person person = result.get(0);
        assertEquals("bob", person.name);
    }

    @Test
    public void should_return_empty_when_wrong_page() {
        DSLQuery query = new DSLQuery(new SpringQueryExecutor(jdbcTemplate), Person.class);
        List result = query.skip(10).limit(10).query();
        assertEquals(0, result.size());
    }
}
