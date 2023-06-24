package cn.beagile.dslquery;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class SQLBuilder<T> {
    private static final Map<Class, Function<String, Object>> FIELD_CAST_MAP = new HashMap<>();

    private final Map<String, Object> params;
    private final Class<T> queryResultClass;
    private final int timezoneOffset;
    private final List<ComplexExpression> whereList;
    private final Sort sort;
    private final Paging page;

    private int index;
    private String sql;
    private String countSql;
    private String whereCondition;

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

    SQLBuilder(DSLQuery<T> dslQuery) {
        this.whereList = dslQuery.getWhereList();
        this.sort = dslQuery.getSort();
        this.queryResultClass = dslQuery.getQueryResultClass();
        this.params = new HashMap<>();
        this.timezoneOffset = dslQuery.getTimezoneOffset();
        this.page = new Paging(dslQuery.getSkip(), dslQuery.getLimit());
    }


    int next() {
        return ++this.index;
    }

    void addParam(String paramName, String fieldName, String value) {
        try {
            Field field = this.queryResultClass.getDeclaredField(fieldName);
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

    public Map<String, Object> getParams() {
        return params;
    }

    public Integer skip() {
        return this.page.getSkip();
    }

    public Integer limit() {
        return this.page.getLimit();
    }

    String aliasOf(String field) {
        try {
            return this.queryResultClass.getDeclaredField(field).getAnnotation(Column.class).value();
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("No such field: " + field);
        }
    }

    public String countSql() {
        if (this.countSql == null) {
            this.countSql = this.getCountSQL();
        }
        return this.countSql;
    }

    public String sql() {
        if (this.sql == null) {
            this.sql = this.getSQL();
        }
        return this.sql;
    }

    public SQLQuery build() {
        return new SQLQuery(this.sql(), this.countSql(), this.params, this.page);
    }

    private String getSQL() {
        String sql = getSelectSQL();
        if (whereList.size() > 0) {
            sql += getWhereSQL();
        }
        if (this.sort != null) {
            sql += getSortSQL();
        }
        return sql;
    }

    private String getWhereSQL() {
        if (this.whereCondition == null) {
            this.whereCondition = String.format(" where %s", whereList.stream().map(where -> where.toSQL(this)).collect(Collectors.joining(" and ")));
        }
        return this.whereCondition;
    }

    private String getSelectSQL() {
        String fields = Stream.of(queryResultClass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Column.class))
                .map(field -> field.getAnnotation(Column.class))
                .map(Column::value)
                .collect(Collectors.joining(","));
        return String.format("select %s from %s", fields, getViewName());
    }

    private String getSortSQL() {
        return String.format(" order by %s", sort.toSQL(this));
    }

    private String getViewName() {
        View view = queryResultClass.getAnnotation(View.class);
        return view.value();
    }

    private String getCountSQL() {
        String result = String.format("select count(*) from %s", getViewName());
        if (whereList.size() > 0) {
            result += getWhereSQL();
        }
        return result;
    }
}
