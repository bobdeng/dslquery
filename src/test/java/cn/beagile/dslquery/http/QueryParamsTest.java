package cn.beagile.dslquery.http;

import cn.beagile.dslquery.DynamicJoin;
import cn.beagile.dslquery.NullsOrder;
import cn.beagile.dslquery.Paged;
import cn.beagile.dslquery.QueryExecutor;
import cn.beagile.dslquery.SQLField;
import cn.beagile.dslquery.SQLQuery;
import cn.beagile.dslquery.View;
import org.junit.jupiter.api.Test;

import javax.persistence.Column;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class QueryParamsTest {

    private final List<SQLQuery> executed = new ArrayList<>();
    private int countCalls;

    private final QueryExecutor executor = new QueryExecutor() {
        @Override
        public <T> List<T> list(Function<ResultSet, T> resultSetReader, SQLQuery sqlQuery) {
            executed.add(sqlQuery);
            return List.of();
        }

        @Override
        public int count(SQLQuery sqlQuery) {
            countCalls++;
            return 0;
        }

        @Override
        public String nullsOrder(NullsOrder nullsOrder) {
            return "";
        }
    };

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
    }

    @Test
    void should_merge_filter_and_where_and_ignore_blank() {
        QueryParams params = new QueryParams();
        params.set$filter(List.of("(and(name eq bob))", "  ", ""));
        params.where("(and(id gt 18))");

        assertArrayEquals(new String[]{"(and(name eq bob))", "(and(id gt 18))"}, params.getFilters());
    }

    @Test
    void should_use_default_paging_and_clamp_top() {
        QueryParams params = new QueryParams();
        assertEquals(Integer.valueOf(0), params.page().getSkip());
        assertEquals(Integer.valueOf(1000), params.page().getLimit());

        params.set$skip(5);
        params.set$top(20);
        assertEquals(Integer.valueOf(5), params.page().getSkip());
        assertEquals(Integer.valueOf(20), params.page().getLimit());

        params.set$top(999_999);
        assertEquals(Integer.valueOf(10000), params.page().getLimit());

        params.set$top(0);
        params.set$top(-1);
        assertEquals(Integer.valueOf(10000), params.page().getLimit());
    }

    @Test
    void should_build_order_by_only_for_legal_sort() {
        QueryParams params = new QueryParams();
        params.set$sort("name desc");
        assertEquals("order by IF(name IS NULL, 1, 0), name desc", params.order());

        // 未指定方向时按升序，与 DSLQuery.sort 保持一致
        params.set$sort("name");
        assertEquals("order by IF(name IS NULL, 1, 0), name", params.order());

        params.set$sort("name desc extra");
        assertEquals("", params.order());

        params.set$sort("name;drop table person desc");
        assertEquals("", params.order());

        params.set$sort("1name desc");
        assertEquals("", params.order());
    }

    @Test
    void should_build_order_by_only_for_allowed_fields() {
        QueryParams params = new QueryParams();
        params.set$sort("name desc,id asc");
        assertEquals("order by IF(name IS NULL, 1, 0), name desc", params.order(List.of("name")));
        assertEquals("order by IF(name IS NULL, 1, 0), name desc,IF(id IS NULL, 1, 0), id asc", params.order(List.of("name", "id")));
        assertEquals("", params.order(List.of("age")));
    }

    @Test
    void should_execute_paged_query_with_request_conditions() {
        QueryParams params = new QueryParams();
        params.set$filter(List.of("(and(name eq bob))"));
        params.set$top(10);
        params.set$skip(20);

        Paged<Person> result = params.pagedQuery(executor, Person.class);

        assertTrue(result.getResult().isEmpty());
        assertEquals(0, result.total());
        assertEquals(1, executed.size());

        SQLQuery sqlQuery = executed.get(0);
        assertTrue(sqlQuery.getSql().contains("person.name"));
        assertTrue(sqlQuery.getSql().contains(":p0"));
        assertEquals("bob", sqlQuery.getParams().get("p0"));
        assertEquals(Integer.valueOf(10), sqlQuery.getLimit());
        assertEquals(Integer.valueOf(20), sqlQuery.getSkip());
    }

    @Test
    void should_find_optional_without_running_count_query() {
        QueryParams params = new QueryParams();
        params.set$filter(List.of("(and(name eq bob))"));

        Optional<Person> found = params.findOptional(executor, Person.class);

        assertTrue(found.isEmpty());
        assertEquals(0, countCalls);
        assertEquals(1, executed.size());
        assertEquals(Integer.valueOf(1), executed.get(0).getLimit());
    }

    @Test
    void should_apply_request_filters_to_dynamic_join() {
        List<SQLField> dynamicJoinFields = List.of(
                new SQLField(new SQLField.ViewName("name"), new SQLField.SQLName("name"), String.class)
        );
        String subQuerySql = "select org.id, count(*) as total_employees from employee ${where} group by org.id";

        QueryParams params = new QueryParams();
        params.set$filter(List.of("(and(name eq bob))"));

        Paged<Person> result = params.pagedQuery(executor, Person.class, "orgStats", dynamicJoinFields, subQuerySql);

        assertTrue(result.getResult().isEmpty());
        assertEquals(1, executed.size());

        SQLQuery sqlQuery = executed.get(0);
        assertTrue(sqlQuery.getSql().contains("left join ("));
        assertTrue(sqlQuery.getSql().contains("orgStats_"));
        assertEquals("bob", sqlQuery.getParams().get("dj_orgStats_p0"));
    }

    @Test
    void should_where_equals_null() {
        QueryParams params = new QueryParams();
        params.whereEquals("name", null);
        params.whereEquals("id", 1);

        assertArrayEquals(new String[]{"(and(name isn))", "(and(id eq 1))"}, params.getFilters());
    }

    @Test
    void should_query_raw_sql_as_map() {
        List<SQLField> fields = List.of(
                new SQLField(new SQLField.ViewName("name"), new SQLField.SQLName("person.name"), String.class)
        );
        QueryParams params = new QueryParams();
        params.set$filter(List.of("(and(name eq bob))"));
        params.set$top(5);

        Paged<Map<String, Object>> result = params.pagedQueryMap(executor,
                "select person.name from person ${where}", "select count(*) from person", fields);

        assertTrue(result.getResult().isEmpty());
        assertEquals(0, result.total());
        assertEquals(1, executed.size());

        SQLQuery sqlQuery = executed.get(0);
        assertTrue(sqlQuery.getSql().contains("where ("));
        assertEquals("bob", sqlQuery.getParams().get("p0"));
        assertEquals(Integer.valueOf(5), sqlQuery.getLimit());
    }
}
