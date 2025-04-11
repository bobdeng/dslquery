package cn.beagile.dslquery;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cn.beagile.dslquery.SQLBuilder.FIELD_CAST_MAP;

public class SQLWhere implements SQLBuild {
    private final List<SQLField> fields;
    private final ComplexExpression expression;
    private int paramIndex = 1;
    private Map<String, Object> params;

    public SQLWhere(List<SQLField> fields, String filter) {
        this.fields = fields;
        expression = new WhereParser().parse(filter);
        this.params = new HashMap<>();
    }

    public String sql() {
        return "where " + expression.toSQL(fields, this);
    }

    public int nextParamId() {
        return paramIndex++;
    }

    @Override
    public void addParamArray(String paramName, String fieldName, String value) {
        SQLField field = fields.stream()
                .filter(f -> f.getName().equals(fieldName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("field not found:" + fieldName));
        params.put(paramName, castValueToList(value,field));
    }

    private List<Object> castValueToList(String value, SQLField field) {
        return Stream.of(new Gson().fromJson(value, String[].class))
                .map(v -> castValueByField(v,field))
                .collect(Collectors.toList());
    }

    private Object castValueByField(String value, SQLField field) {
        return FIELD_CAST_MAP.get(field.getType()).apply(value);
    }

    @Override
    public void addParam(String paramName, String fieldName, String value) {
        params.put(paramName, value);
    }

    public Object param(String name) {
        return this.params.get(name);
    }
}
