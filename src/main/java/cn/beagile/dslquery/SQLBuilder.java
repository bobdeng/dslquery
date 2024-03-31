package cn.beagile.dslquery;

import com.google.gson.Gson;

import javax.persistence.Column;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class SQLBuilder<T> {
    private static final Map<Class, Function<String, Object>> FIELD_CAST_MAP = new HashMap<>();

    private final Map<String, Object> params;
    private final DSLQuery<T> dslQuery;
    private final ColumnFields columnFields;

    private int paramIndex;
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
        FIELD_CAST_MAP.put(Boolean.class, v -> "true".equalsIgnoreCase(v) || "1".equals(v));
        FIELD_CAST_MAP.put(boolean.class, v -> "true".equalsIgnoreCase(v) || "1".equals(v));
        FIELD_CAST_MAP.put(String.class, s -> s);
    }

    SQLBuilder(DSLQuery<T> dslQuery) {
        this.dslQuery = dslQuery;
        this.params = new HashMap<>();
        columnFields = new ColumnFields(dslQuery);
    }


    int nextParamId() {
        return ++this.paramIndex;
    }

    void addParam(String paramName, String fieldName, String value) {
        setParam(paramName, fieldName, value, this::castValueByField);
    }

    void addParamArray(String paramName, String fieldName, String value) {
        setParam(paramName, fieldName, value, this::castValueToList);
    }

    private List<Object> castValueToList(String value, Field field) {
        return Stream.of(new Gson().fromJson(value, String[].class))
                .map(v -> castValueByField(v, field))
                .collect(Collectors.toList());
    }

    private void setParam(String paramName, String fieldName, String value, BiFunction<String, Field, Object> valueConverter) {
        Field field = columnFields.findFieldByName(fieldName).getField();
        Object paramValue = valueConverter.apply(value, field);
        params.put(paramName, paramValue);
    }

    private Object castValueByField(String value, Field field) {
        if (isInstant(field.getType())) {
            return getInstantValue(value, field);
        }
        if (isTimestampAsDate(field)) {
            if (field.getType().equals(Timestamp.class)) {
                return Timestamp.from(getInstantValue(value, field));
            }
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
        if (field.getType().equals(Timestamp.class)) {
            return field.isAnnotationPresent(DateFormat.class);
        }
        return false;
    }

    private Instant getInstantValue(String value, Field field) {
        String dateFormat = field.getAnnotation(DateFormat.class).value();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);
        ZoneId zoneId = ZoneOffset.ofHours(this.dslQuery.getTimezoneOffset()).normalized();
        return LocalDateTime.parse(value, formatter).atZone(zoneId).toInstant();
    }

    public Map<String, Object> getParams() {
        return params;
    }

    String aliasOf(String field) {
        return columnFields.findFieldByName(field).selectName();
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
        return new SQLQuery(this.sql(), this.countSql(), this.params, dslQuery.getPage(), dslQuery.getNullsOrder());
    }

    private String getSQL() {
        List<String> lines = new ArrayList<>();
        lines.add(getSelectSQL());
        if (!getWhereList().isEmpty()) {
            lines.add(getWhereSQL());
        }
        if (dslQuery.getSort() != null) {
            lines.add(getSortSQL());
        }
        return lines.stream().collect(Collectors.joining("\n"));
    }

    private String getWhereSQL() {
        if (this.whereCondition == null) {
            this.whereCondition = String.format(" where %s", getWhereList().stream().map(where -> where.toSQL(this)).collect(Collectors.joining(" and ")));
        }
        return this.whereCondition;
    }

    private List<ComplexExpression> getWhereList() {
        return this.dslQuery.getWhereList();
    }

    private String getSelectSQL() {
        String select = "select" + columnFields.distinct() + columnFields.selectFields().stream().map(ColumnField::expression).collect(Collectors.joining(",")) + " from " + columnFields.from();
        String join = columnFields.joins();
        return String.join("\n", select, join);
    }

    private String getSortSQL() {
        return String.format("order by %s", dslQuery.getSort().toSQL(this));
    }

    private String getCountSQL() {
        List<String> lines = new ArrayList<>();
        String countField = getCountField();
        lines.add(String.format("select count(" + countField + ") from %s\n%s", columnFields.from(), columnFields.joins()));
        if (!getWhereList().isEmpty()) {
            lines.add(getWhereSQL());
        }
        return lines.stream().map(String::trim).collect(Collectors.joining("\n"));
    }

    private String getCountField() {
        String tableName = this.dslQuery.getQueryResultClass().getAnnotation(View.class).value();
        return Arrays.stream(this.dslQuery.getQueryResultClass().getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Column.class))
                .filter(field -> field.getAnnotation(Column.class).unique())
                .findFirst()
                .map(field -> "distinct " + tableName + "." + field.getAnnotation(Column.class).name())
                .orElse("*");
    }

    public void fetchOne2Many(List<T> result, QueryExecutor queryExecutor) {
        result.forEach(t -> fetchOne2Many(t, queryExecutor));
    }

    private void fetchOne2Many(T t, QueryExecutor queryExecutor) {
        columnFields.fetchOneToManyFields(t, queryExecutor);
    }
}
