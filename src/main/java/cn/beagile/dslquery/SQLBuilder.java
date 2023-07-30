package cn.beagile.dslquery;

import com.google.gson.Gson;

import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import java.lang.reflect.Field;
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
    private final ResultBean resultBean;
    private final DSLQuery<T> dslQuery;

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

    SQLBuilder(DSLQuery<T> dslQuery, ResultBean resultBean) {
        this.resultBean = resultBean;
        this.dslQuery = dslQuery;
        this.params = new HashMap<>();
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
        FieldWithColumn fieldColumn = getColumns().getFieldColumn(fieldName);
        Field field = fieldColumn.getField();
        Object paramValue = valueConverter.apply(value, field);
        params.put(paramName, paramValue);
    }

    private FieldsWithColumns getColumns() {
        return resultBean.getFieldsWithColumns();
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
        ZoneId zoneId = ZoneOffset.ofHours(this.dslQuery.getTimezoneOffset()).normalized();
        return LocalDateTime.parse(value, formatter).atZone(zoneId).toInstant();
    }

    public Map<String, Object> getParams() {
        return params;
    }

    String aliasOf(String field) {
        return getColumns().getFieldColumn(field).whereName();
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
        return new SQLQuery(this.sql(), this.countSql(), this.params, dslQuery.getPage());
    }

    private String getSQL() {
        List<String> lines = new ArrayList<>();
        lines.add(getSelectSQL());
        if (getWhereList().size() > 0) {
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
        List<String> lines = new ArrayList<>();
        String fields = getColumns().getListFields().stream().map(FieldWithColumn::selectName).collect(Collectors.joining(","));
        lines.add(String.format("select %s from %s", fields, getViewName()));
        lines.add(getAllJoinTables(resultBean.getClazz()));
        return lines.stream().map(String::trim).collect(Collectors.joining("\n"));
    }

    private String getAllJoinTables(Class queryResultClass) {
        return String.join("\n", getJoinTables(queryResultClass).trim(), getJoinsTables(queryResultClass).trim());
    }

    private String getJoinTables(Class clz) {
        return Arrays.stream(clz.getDeclaredFields())
                .filter(field -> !isIgnored(field))
                .filter(field -> field.isAnnotationPresent(JoinColumn.class))
                .map(field -> getJoin(clz, field, new JoinColumn[]{field.getAnnotation(JoinColumn.class)}))
                .collect(Collectors.joining("\n", "\n", ""));
    }

    private boolean isIgnored(Field field) {
        return resultBean.ignored(field.getType());
    }

    private String getJoinsTables(Class clz) {
        return Arrays.stream(clz.getDeclaredFields())
                .filter(field -> !isIgnored(field))
                .filter(field -> field.isAnnotationPresent(JoinColumns.class))
                .map(field -> getJoin(clz, field, field.getAnnotation(JoinColumns.class).value()))
                .collect(Collectors.joining("", "\n", ""));
    }

    private String getJoin(Class clz, Field field, JoinColumn[] joinColumns) {
        String myTable = getViewName(clz);
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < joinColumns.length; i++) {
            String joinTable = i == joinColumns.length - 1 ? field.getType().getAnnotation(View.class).value() : joinColumns[i].table();
            String onTable = i == 0 ? myTable : joinColumns[i - 1].table();
            String leftJoin = "left join " + joinTable + " on " + joinTable + "." + joinColumns[i].referencedColumnName() + " = " + onTable + "." + joinColumns[i].name();
            lines.add(leftJoin);
            lines.add(getAllJoinTables(field.getType()));
        }
        return lines.stream().map(String::trim).filter(line -> !line.isEmpty()).collect(Collectors.joining("\n"));
    }


    private String getSortSQL() {
        return String.format("order by %s", dslQuery.getSort().toSQL(this));
    }

    private String getViewName() {
        View view = (View) resultBean.getClazz().getAnnotation(View.class);
        return view.value();
    }

    private String getViewName(Class clz) {
        View annotation = (View) clz.getAnnotation(View.class);
        return annotation.value();
    }

    private String getCountSQL() {
        List<String> lines = new ArrayList<>();
        lines.add(String.format("select count(*) from %s", getViewName() + getJoinTables(resultBean.getClazz())));
        if (getWhereList().size() > 0) {
            lines.add(getWhereSQL());
        }
        return lines.stream().map(String::trim).collect(Collectors.joining("\n"));
    }
}
