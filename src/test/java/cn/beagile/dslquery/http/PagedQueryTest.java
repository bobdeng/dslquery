package cn.beagile.dslquery.http;

import cn.beagile.dslquery.DynamicJoin;
import cn.beagile.dslquery.Paged;
import cn.beagile.dslquery.SQLField;
import cn.beagile.dslquery.SQLQuery;
import cn.beagile.dslquery.View;
import org.junit.jupiter.api.Test;

import javax.persistence.Column;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PagedQueryTest {

    private final RecordingQueryExecutor executor = new RecordingQueryExecutor();

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
        PagedQuery query = new PagedQuery();
        query.set$filter(List.of("(and(name eq bob))", "  ", ""));
        query.where("(and(id gt 18))");

        assertArrayEquals(new String[]{"(and(name eq bob))", "(and(id gt 18))"}, query.getFilters());
    }

    @Test
    void should_use_default_paging_and_clamp_top() {
        PagedQuery query = new PagedQuery();
        assertEquals(Integer.valueOf(0), query.page().getSkip());
        assertEquals(Integer.valueOf(1000), query.page().getLimit());

        query.set$skip(5);
        query.set$top(20);
        assertEquals(Integer.valueOf(5), query.page().getSkip());
        assertEquals(Integer.valueOf(20), query.page().getLimit());

        query.set$top(999_999);
        assertEquals(Integer.valueOf(10000), query.page().getLimit());

        query.set$top(0);
        query.set$top(-1);
        assertEquals(Integer.valueOf(10000), query.page().getLimit());
    }

    @Test
    void should_execute_paged_query_with_request_conditions() {
        PagedQuery query = new PagedQuery();
        query.set$filter(List.of("(and(name eq bob))"));
        query.set$top(10);
        query.set$skip(20);

        Paged<Person> result = query.pagedQuery(executor, Person.class);

        assertTrue(result.getResult().isEmpty());
        assertEquals(0, result.total());
        assertEquals(1, executor.executed.size());
        assertEquals(1, executor.countCalls);

        SQLQuery sqlQuery = executor.executed.get(0);
        assertTrue(sqlQuery.getSql().contains("person.name"));
        assertTrue(sqlQuery.getSql().contains(":p0"));
        assertEquals("bob", sqlQuery.getParams().get("p0"));
        assertEquals(Integer.valueOf(10), sqlQuery.getLimit());
        assertEquals(Integer.valueOf(20), sqlQuery.getSkip());
    }

    @Test
    void should_apply_request_filters_to_dynamic_join() {
        List<SQLField> dynamicJoinFields = List.of(
                new SQLField(new SQLField.ViewName("name"), new SQLField.SQLName("name"), String.class)
        );
        String subQuerySql = "select org.id, count(*) as total_employees from employee ${where} group by org.id";

        PagedQuery query = new PagedQuery();
        query.set$filter(List.of("(and(name eq bob))"));

        Paged<Person> result = query.pagedQuery(executor, Person.class, "orgStats", dynamicJoinFields, subQuerySql);

        assertTrue(result.getResult().isEmpty());
        assertEquals(1, executor.executed.size());

        SQLQuery sqlQuery = executor.executed.get(0);
        assertTrue(sqlQuery.getSql().contains("left join ("));
        assertTrue(sqlQuery.getSql().contains("orgStats_"));
        assertEquals("bob", sqlQuery.getParams().get("dj_orgStats_p0"));
    }
}
