package cn.beagile.dslquery;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class SQLQuery {
    private final Map<String, Object> params;
    private final Class queryResultBeanClass;
    private final int timezoneOffset;
    private static final Map<Class, Function<String, Object>> FIELD_CAST_MAP = new HashMap<>();
    private int index;
    private String sql;
    private Paging page;
    private String countSql;

    static {
        FIELD_CAST_MAP.put(Integer.class, Integer::parseInt);
        FIELD_CAST_MAP.put(int.class, Integer::parseInt);
        FIELD_CAST_MAP.put(Long.class, Long::parseLong);
        FIELD_CAST_MAP.put(long.class, Long::parseLong);
        FIELD_CAST_MAP.put(Float.class, Float::parseFloat);
        FIELD_CAST_MAP.put(float.class, Float::parseFloat);
        FIELD_CAST_MAP.put(Double.class, Double::parseDouble);
        FIELD_CAST_MAP.put(double.class, Double::parseDouble);
        FIELD_CAST_MAP.put(String.class, s -> s);
    }

    public SQLQuery(Class queryResultBeanClass, int timezoneOffset) {
        this.index = 1;
        this.queryResultBeanClass = queryResultBeanClass;
        this.params = new HashMap<>();
        this.timezoneOffset = timezoneOffset;
    }


    int next() {
        return this.index++;
    }

    void addParam(String paramName, String fieldName, String value) {
        try {
            Field field = this.queryResultBeanClass.getDeclaredField(fieldName);
            Object paramValue = castValueByField(value, field);
            params.put(paramName, paramValue);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("No such field: " + paramName);
        }
    }

    private Object castValueByField(String value, Field field) {
        if (isInstant(field.getType())) {
            return getInstantValue(value, field);
        }
        if (isTimestampAsDate(field)) {
            return getInstantValue(value, field).toEpochMilli();
        }
        return FIELD_CAST_MAP.get(field.getType()).apply(value);
    }

    private boolean isInstant(Class<?> type) {
        return type.equals(Instant.class);
    }

    private boolean isTimestampAsDate(Field field) {
        if (field.getType().equals(Long.class)) {
            return field.isAnnotationPresent(DateFormat.class);
        }
        if (field.getType().equals(long.class)) {
            return field.isAnnotationPresent(DateFormat.class);
        }
        return false;
    }

    private Instant getInstantValue(String value, Field field) {
        String dateFormat = field.getAnnotation(DateFormat.class).value();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);
        ZoneId zoneId = ZoneOffset.ofHours(this.timezoneOffset).normalized();
        return LocalDateTime.parse(value, formatter).atZone(zoneId).toInstant();
    }

    public String sql() {
        return sql;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public Integer skip() {
        return this.page.getSkip();
    }

    public Integer limit() {
        return this.page.getLimit();
    }

    void setSql(String sql) {
        this.sql = sql;
    }


    String aliasOf(String field) {
        try {
            return this.queryResultBeanClass.getDeclaredField(field).getAnnotation(Column.class).value();
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("No such field: " + field);
        }
    }

    public void setPaging(Paging page) {
        this.page = page;
    }

    public void setCountSql(String countSql) {
        this.countSql = countSql;
    }

    public String countSql() {
        return this.countSql;
    }
}
