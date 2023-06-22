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
    private String sql;
    private Map<String, Object> params;
    private Integer skip;
    private int index;
    private Class queryResultBeanClass;
    private Integer limit;
    private int timezoneOffset;
    private static final Map<Class, Function<String, Object>> FIELD_CAST_MAP = new HashMap<>();

    static {
        FIELD_CAST_MAP.put(Integer.class, Integer::parseInt);
        FIELD_CAST_MAP.put(int.class, Integer::parseInt);
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


    public int next() {
        return this.index++;
    }

    public void addParam(String paramName, String field, String value) {
        try {
            Field declaredField = this.queryResultBeanClass.getDeclaredField(field);
            Class<?> type = declaredField.getType();
            if (type.equals(Instant.class)) {
                String dateFormat = declaredField.getAnnotation(DateFormat.class).value();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);
                ZoneId zoneId = ZoneOffset.ofHours(this.timezoneOffset).normalized();
                params.put(paramName, LocalDateTime.parse(value, formatter).atZone(zoneId).toInstant());
                return;
            }
            params.put(paramName, FIELD_CAST_MAP.get(type).apply(value));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public String sql() {
        return sql;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }


    public String aliasOf(String field) {
        try {
            return this.queryResultBeanClass.getDeclaredField(field).getAnnotation(Column.class).value();
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public Integer skip() {
        return this.skip;
    }

    public void setSkip(Integer skip) {
        this.skip = skip;
    }

    public void setLimit(Integer limit) {

        this.limit = limit;
    }

    public Integer limit() {
        return this.limit;
    }
}
