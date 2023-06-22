package cn.beagile.dslquery;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SQLQueryTest {
    private int timezoneOffset = -8;

    public static class QueryResultForTest {
        @Column("weight")
        private Float weight;
        @Column("age")
        private Integer age;
        @Column("name")
        private String name;
        @Column("doubleValue")
        private double doubleValue;
        @Column("instant")
        @DateFormat("yyyy-MM-dd HH:mm:ss")
        private Instant instant;
        @Column("longTimestamp")
        @DateFormat("yyyy-MM-dd HH:mm:ss")
        private Long longTimestamp;
        @Column("normalLong")
        private Long normalLong;
    }

    @Test
    public void add_float_param() {
        SQLQuery sqlQuery = new SQLQuery(QueryResultForTest.class, this.timezoneOffset);
        sqlQuery.addParam("weight", "weight", "55.4");
        assertEquals(55.4f, sqlQuery.getParams().get("weight"));
    }

    @Test
    public void add_int_param() {
        SQLQuery sqlQuery = new SQLQuery(QueryResultForTest.class, this.timezoneOffset);
        sqlQuery.addParam("age", "age", "55");
        assertEquals(55, sqlQuery.getParams().get("age"));
    }

    @Test
    public void add_string_param() {
        SQLQuery sqlQuery = new SQLQuery(QueryResultForTest.class, this.timezoneOffset);
        sqlQuery.addParam("name", "name", "bob");
        assertEquals("bob", sqlQuery.getParams().get("name"));
    }

    @Test
    public void add_double_param() {
        SQLQuery sqlQuery = new SQLQuery(QueryResultForTest.class, this.timezoneOffset);
        sqlQuery.addParam("doubleValue", "doubleValue", "5564000.00");
        assertEquals(5564000.00, sqlQuery.getParams().get("doubleValue"));
    }

    @Test
    public void add_instant_param() {
        SQLQuery sqlQuery = new SQLQuery(QueryResultForTest.class, this.timezoneOffset);
        sqlQuery.addParam("instant", "instant", "2020-01-01 00:00:00");
        ZoneId zoneId = ZoneOffset.ofHours(this.timezoneOffset).normalized();
        assertEquals(LocalDateTime.parse("2020-01-01 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).atZone(zoneId).toInstant(), sqlQuery.getParams().get("instant"));
    }

    @Test
    public void add_long_timestamp_param() {
        SQLQuery sqlQuery = new SQLQuery(QueryResultForTest.class, this.timezoneOffset);
        sqlQuery.addParam("longTimestamp", "longTimestamp", "2020-01-01 00:00:00");
        ZoneId zoneId = ZoneOffset.ofHours(this.timezoneOffset).normalized();
        assertEquals(LocalDateTime.parse("2020-01-01 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).atZone(zoneId)
                .toInstant().toEpochMilli(), sqlQuery.getParams().get("longTimestamp"));
    }

    @Test
    public void add_long_param() {
        SQLQuery sqlQuery = new SQLQuery(QueryResultForTest.class, this.timezoneOffset);
        sqlQuery.addParam("normalLong", "normalLong", "18");
        assertEquals(18L, sqlQuery.getParams().get("normalLong"));
    }
}