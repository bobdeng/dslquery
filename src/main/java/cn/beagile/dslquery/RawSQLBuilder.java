package cn.beagile.dslquery;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cn.beagile.dslquery.DSLSQLBuilder.FIELD_CAST_MAP;

public class RawSQLBuilder implements SQLBuilder {
    private final List<SQLField> fields;
    private String sort;
    private ComplexExpression expression;
    private int paramIndex = 1;
    private Map<String, Object> params;

    public RawSQLBuilder(List<SQLField> fields, String filter) {
        this(fields, filter, null);
    }

    public RawSQLBuilder(List<SQLField> fields, String filter, String sort) {
        this.fields = fields;
        if (filter != null) {
            expression = new WhereParser().parse(filter);
        }
        this.sort = sort;
        this.params = new HashMap<>();
    }

    public String where() {
        if (expression == null) {
            return "";
        }
        return "where " + expression.toSQL(fields, this);
    }

    public String sort() {
        if (sort == null) {
            return "";
        }
        return "order by " + new Sort(sort).toSQL(this);
    }

    public int nextParamId() {
        return paramIndex++;
    }

    @Override
    public void addParamArray(String paramName, String fieldName, String value) {
        SQLField field = getSqlField(fieldName);
        params.put(paramName, castValueToList(value, field));
    }

    private SQLField getSqlField(String fieldName) {
        return fields.stream()
                .filter(f -> f.getName().equals(fieldName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("field not found:" + fieldName));
    }

    private List<Object> castValueToList(String value, SQLField field) {
        return Stream.of(new Gson().fromJson(value, String[].class))
                .map(v -> castValueByField(v, field))
                .collect(Collectors.toList());
    }

    private Object castValueByField(String value, SQLField field) {
        return FIELD_CAST_MAP.get(field.getType()).apply(value);
    }

    @Override
    public void addParam(String paramName, String fieldName, String value) {
        params.put(paramName, value);
    }

    @Override
    public String aliasOf(String field) {
        return getSqlField(field).getWhereName();
    }

    public Object param(String name) {
        return this.params.get(name);
    }

    public SQLQuery toSQLQuery(String sql, String countSql, Paging page) {
        return new SQLQuery(getSqlAndWhere(sql) + " " + sort(), getSqlAndWhere(countSql), this.params, page);
    }

    private String getSqlAndWhere(String sql) {
        if (sql.contains("${where}")) {
            return sql.replace("${where}", where());
        }
        return sql + " " + where();
    }
}
