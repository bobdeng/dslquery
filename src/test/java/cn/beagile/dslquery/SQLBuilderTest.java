package cn.beagile.dslquery;

import org.junit.jupiter.api.Test;

import javax.persistence.Column;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SQLBuilderTest {
    private int timezoneOffset = -8;

    @View("test_view")
    public static class QueryResultForTest {
        @Column(name = "weight")
        private Float weight;
        @Column(name = "age")
        private Integer age;
        @Column(name = "name")
        private String name;
        @Column(name = "double_value")
        private double doubleValue;
        @Column(name = "instant")
        @DateFormat("yyyy-MM-dd HH:mm:ss")
        private Instant instant;
        @Column(name = "longTimestamp")
        @DateFormat("yyyy-MM-dd HH:mm:ss")
        private Long longTimestamp;
        @Column(name = "normalLong")
        private Long normalLong;
        @Column(name = "normalLongPrimitive")
        private long normalLongPrimitive;
        @Column(name = "passed")
        private Boolean passed;
        @Column(name = "passed")
        private boolean anotherBoolean;
        @Column(name = "big_decimal")
        private BigDecimal bigDecimal;
    }

    @Test
    public void add_float_param() {
        DSLSQLBuilder sqlQuery = getSqlBuilder();
        sqlQuery.addParam("weight", "weight", "55.4");
        assertEquals(55.4f, sqlQuery.getParams().get("weight"));
    }
    @Test
    public void add_big_decimal_param() {
        DSLSQLBuilder sqlQuery = getSqlBuilder();
        sqlQuery.addParam("bigDecimal", "bigDecimal", "55.4");
        assertEquals(new BigDecimal("55.4"), sqlQuery.getParams().get("bigDecimal"));
    }

    @Test
    public void add_param_not_exist() {
        DSLSQLBuilder<QueryResultForTest> sqlQuery = getSqlBuilder();
        RuntimeException e = assertThrows(RuntimeException.class, () -> {
            sqlQuery.addParam("weight1", "weight1", "55.4");
        });
        assertEquals("field not found: weight1", e.getMessage());
    }

    private DSLSQLBuilder<QueryResultForTest> getSqlBuilder() {
        DSLQuery<QueryResultForTest> dslQuery = new DSLQuery<>(null, QueryResultForTest.class)
                .timezoneOffset(this.timezoneOffset);
        return new DSLSQLBuilder(dslQuery);
    }

    @Test
    public void add_int_param() {
        DSLSQLBuilder<QueryResultForTest> sqlQuery = getSqlBuilder();
        sqlQuery.addParam("age", "age", "55");
        assertEquals(55, sqlQuery.getParams().get("age"));
    }

    @Test
    public void add_string_param() {
        DSLSQLBuilder<QueryResultForTest> sqlQuery = getSqlBuilder();
        sqlQuery.addParam("name", "name", "bob");
        assertEquals("bob", sqlQuery.getParams().get("name"));
    }

    @Test
    public void add_double_param() {
        DSLSQLBuilder<QueryResultForTest> sqlQuery = getSqlBuilder();
        sqlQuery.addParam("doubleValue", "doubleValue", "5564000.00");
        assertEquals(5564000.00, sqlQuery.getParams().get("doubleValue"));
    }

    @Test
    public void add_Boolean_param() {
        DSLSQLBuilder<QueryResultForTest> sqlQuery = getSqlBuilder();
        sqlQuery.addParam("passed", "passed", "true");
        assertEquals(true, sqlQuery.getParams().get("passed"));
    }
    @Test
    public void add_boolean_param() {
        DSLSQLBuilder<QueryResultForTest> sqlQuery = getSqlBuilder();
        sqlQuery.addParam("anotherBoolean", "anotherBoolean", "true");
        assertEquals(true, sqlQuery.getParams().get("anotherBoolean"));
    }
    @Test
    public void add_boolean_param_with1() {
        DSLSQLBuilder<QueryResultForTest> sqlQuery = getSqlBuilder();
        sqlQuery.addParam("anotherBoolean", "anotherBoolean", "1");
        assertEquals(true, sqlQuery.getParams().get("anotherBoolean"));
    }
    @Test
    public void add_boolean_param_with_0() {
        DSLSQLBuilder<QueryResultForTest> sqlQuery = getSqlBuilder();
        sqlQuery.addParam("anotherBoolean", "anotherBoolean", "0");
        assertEquals(false, sqlQuery.getParams().get("anotherBoolean"));
    }
    @Test
    public void add_Boolean_param_with_1() {
        DSLSQLBuilder<QueryResultForTest> sqlQuery = getSqlBuilder();
        sqlQuery.addParam("passed", "passed", "1");
        assertEquals(true, sqlQuery.getParams().get("passed"));
    }
    @Test
    public void add_Boolean_param_with_0() {
        DSLSQLBuilder<QueryResultForTest> sqlQuery = getSqlBuilder();
        sqlQuery.addParam("passed", "passed", "0");
        assertEquals(false, sqlQuery.getParams().get("passed"));
    }

    @Test
    public void add_instant_param() {
        DSLSQLBuilder<QueryResultForTest> sqlQuery = getSqlBuilder();
        sqlQuery.addParam("instant", "instant", "2020-01-01 00:00:00");
        ZoneId zoneId = ZoneOffset.ofHours(this.timezoneOffset).normalized();
        assertEquals(LocalDateTime.parse("2020-01-01 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).atZone(zoneId).toInstant(), sqlQuery.getParams().get("instant"));
    }

    @Test
    public void add_long_timestamp_param() {
        DSLSQLBuilder<QueryResultForTest> sqlQuery = getSqlBuilder();
        sqlQuery.addParam("longTimestamp", "longTimestamp", "2020-01-01 00:00:00");
        ZoneId zoneId = ZoneOffset.ofHours(this.timezoneOffset).normalized();
        assertEquals(LocalDateTime.parse("2020-01-01 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).atZone(zoneId)
                .toInstant().toEpochMilli(), sqlQuery.getParams().get("longTimestamp"));
    }

    @Test
    public void add_Long_param() {
        DSLSQLBuilder<QueryResultForTest> sqlQuery = getSqlBuilder();
        sqlQuery.addParam("normalLong", "normalLong", "18");
        assertEquals(18L, sqlQuery.getParams().get("normalLong"));
    }

    @Test
    public void add_long_param() {
        DSLSQLBuilder<QueryResultForTest> sqlQuery = getSqlBuilder();
        sqlQuery.addParam("normalLongPrimitive", "normalLongPrimitive", "18");
        assertEquals(18L, sqlQuery.getParams().get("normalLongPrimitive"));
    }

    @Test
    public void should_return_alias() {
        DSLSQLBuilder<QueryResultForTest> sqlQuery = getSqlBuilder();
        assertEquals("test_view.double_value", sqlQuery.aliasOf("doubleValue"));
    }

    @Test
    public void should_throw_when_alias_not_exist() {
        DSLSQLBuilder<QueryResultForTest> sqlQuery = getSqlBuilder();
        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> {
            assertEquals("double_value", sqlQuery.aliasOf("doubleValue1"));
        });
        assertEquals("field not found: doubleValue1", runtimeException.getMessage());
    }

    @Test
    public void should_add_int_array_params() {
        DSLSQLBuilder<QueryResultForTest> sqlQuery = getSqlBuilder();
        sqlQuery.addParamArray("age", "age", "[55,56,57]");
        List ages = (List) sqlQuery.getParams().get("age");
        assertEquals(55, ages.get(0));
        assertEquals(56, ages.get(1));
        assertEquals(57, ages.get(2));
    }
}
