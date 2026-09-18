package cn.beagile.dslquery.http;

import cn.beagile.dslquery.Paged;
import cn.beagile.dslquery.SQLField;
import cn.beagile.dslquery.SQLQuery;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RawQueryTest {

    private final RecordingQueryExecutor executor = new RecordingQueryExecutor();

    @Test
    void should_build_order_by_only_for_legal_sort() {
        RawQuery query = new RawQuery();
        query.set$sort("name desc");
        assertEquals("order by IF(name IS NULL, 1, 0), name desc", query.order());

        // 未指定方向时按升序，与 DSLQuery.sort 保持一致
        query.set$sort("name");
        assertEquals("order by IF(name IS NULL, 1, 0), name", query.order());

        query.set$sort("name desc extra");
        assertEquals("", query.order());

        query.set$sort("name;drop table person desc");
        assertEquals("", query.order());

        query.set$sort("1name desc");
        assertEquals("", query.order());
    }

    @Test
    void should_build_order_by_only_for_allowed_fields() {
        RawQuery query = new RawQuery();
        query.set$sort("name desc,id asc");
        assertEquals("order by IF(name IS NULL, 1, 0), name desc", query.order(List.of("name")));
        assertEquals("order by IF(name IS NULL, 1, 0), name desc,IF(id IS NULL, 1, 0), id asc", query.order(List.of("name", "id")));
        assertEquals("", query.order(List.of("age")));
    }

    @Test
    void should_merge_filter_and_where_and_ignore_blank() {
        RawQuery query = new RawQuery();
        query.set$filter(List.of("(and(name eq bob))", "  "));
        query.where("(and(id gt 18))");

        assertArrayEquals(new String[]{"(and(name eq bob))", "(and(id gt 18))"}, query.getFilters());
    }

    @Test
    void should_query_raw_sql_as_map() {
        List<SQLField> fields = List.of(
                new SQLField(new SQLField.ViewName("name"), new SQLField.SQLName("person.name"), String.class)
        );
        RawQuery query = new RawQuery();
        query.set$filter(List.of("(and(name eq bob))"));
        query.set$top(5);

        Paged<Map<String, Object>> result = query.pagedQueryMap(executor,
                "select person.name from person ${where}", "select count(*) from person", fields);

        assertTrue(result.getResult().isEmpty());
        assertEquals(0, result.total());
        assertEquals(1, executor.executed.size());
        assertEquals(1, executor.countCalls);

        SQLQuery sqlQuery = executor.executed.get(0);
        assertTrue(sqlQuery.getSql().contains("where ("));
        assertEquals("bob", sqlQuery.getParams().get("p0"));
        assertEquals(Integer.valueOf(5), sqlQuery.getLimit());
    }
}
