package cn.beagile.dslquery.http;

import cn.beagile.dslquery.View;
import org.junit.jupiter.api.Test;

import javax.persistence.Column;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class QueryTest {

    private final RecordingQueryExecutor executor = new RecordingQueryExecutor();

    @View("person")
    public static class Person {
        @Column(name = "id")
        private Integer id;
        @Column(name = "name")
        private String name;
    }

    @Test
    void should_find_optional_without_running_count_query() {
        Query query = new Query();
        query.set$filter(List.of("(and(name eq bob))"));

        Optional<Person> found = query.findOptional(executor, Person.class);

        assertTrue(found.isEmpty());
        assertEquals(0, executor.countCalls);
        assertEquals(1, executor.executed.size());
        assertEquals(Integer.valueOf(1), executor.executed.get(0).getLimit());
    }

    @Test
    void should_return_null_when_find_one_has_no_result() {
        assertNull(new Query().findOne(executor, Person.class));
    }

    @Test
    void should_where_equals_null() {
        Query query = new Query();
        query.whereEquals("name", null);
        query.whereEquals("id", 1);

        assertArrayEquals(new String[]{"(and(name isn))", "(and(id eq 1))"}, query.getFilters());
    }
}
