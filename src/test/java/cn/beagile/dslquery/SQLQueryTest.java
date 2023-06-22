package cn.beagile.dslquery;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SQLQueryTest {
    public static class QueryResultForTest {
        @Column("weight")
        private Float weight;
        @Column("age")
        private Integer age;
        @Column("name")
        private String name;
        @Column("doubleValue")
        private double doubleValue;
    }

    @Test
    public void add_float_param() {
        SQLQuery sqlQuery = new SQLQuery(QueryResultForTest.class);
        sqlQuery.addParam("weight", "weight", "55.4");
        assertEquals(55.4f, sqlQuery.getParams().get("weight"));
    }
    @Test
    public void add_int_param() {
        SQLQuery sqlQuery = new SQLQuery(QueryResultForTest.class);
        sqlQuery.addParam("age", "age", "55");
        assertEquals(55, sqlQuery.getParams().get("age"));
    }
    @Test
    public void add_string_param() {
        SQLQuery sqlQuery = new SQLQuery(QueryResultForTest.class);
        sqlQuery.addParam("name", "name", "bob");
        assertEquals("bob", sqlQuery.getParams().get("name"));
    }
    @Test
    public void add_double_param() {
        SQLQuery sqlQuery = new SQLQuery(QueryResultForTest.class);
        sqlQuery.addParam("doubleValue", "doubleValue", "5564000.00");
        assertEquals(5564000.00, sqlQuery.getParams().get("doubleValue"));
    }
}