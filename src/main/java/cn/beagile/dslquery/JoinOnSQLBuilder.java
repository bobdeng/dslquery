package cn.beagile.dslquery;

import com.google.gson.Gson;import jakarta.persistence.Column;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class JoinOnSQLBuilder implements SQLBuilder {
    private final Map<String, ColumnField> fields = new LinkedHashMap<>();
    private final Map<String, Object> params;
    private final String paramPrefix;
    private final int timezoneOffset;

    JoinOnSQLBuilder(Field joinField, List<Field> parents, String paramPrefix, Map<String, Object> params, int timezoneOffset) {
        this.params = params;
        this.paramPrefix = paramPrefix;
        this.timezoneOffset = timezoneOffset;
        initFields(joinField, parents);
    }

    String build(List<String> joinOns) {
        if (joinOns.isEmpty()) {
            return "";
        }
        WhereParser parser = new WhereParser();
        return joinOns.stream()
                .map(parser::parse)
                .map(expression -> expression.toSQL(this))
                .collect(Collectors.joining(" and "));
    }

    private void initFields(Field joinField, List<Field> parents) {
        Class<?> rootClass = parents.get(0).getDeclaringClass();
        addScope("", readColumns(joinField.getType(), joinField.getType(), parents, true));
        addScope("self.", readColumns(joinField.getType(), joinField.getType(), parents, true));
        if (parents.size() == 1) {
            addScope("parent.", readColumns(rootClass, rootClass, new ArrayList<>(), false));
        } else {
            addScope("parent.", readColumns(joinField.getDeclaringClass(), joinField.getDeclaringClass(), parents.subList(0, parents.size() - 1), true));
        }
        addScope("root.", readColumns(rootClass, rootClass, new ArrayList<>(), false));
    }

    private void addScope(String prefix, List<ColumnField> scopedFields) {
        scopedFields.forEach(field -> fields.put(prefix + field.getField().getName(), field));
    }

    private List<ColumnField> readColumns(Class<?> ownerClass, Class<?> rootClass, List<Field> parents, boolean joined) {
        return Arrays.stream(ownerClass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Column.class))
                .map(field -> new ColumnField(field, rootClass, new ArrayList<>(parents), field.getAnnotation(Column.class), joined))
                .collect(Collectors.toList());
    }

    @Override
    public void addParamArray(String paramName, String fieldName, String value) {
        params.put(paramName, Stream.of(new Gson().fromJson(value, String[].class))
                .map(it -> DSLSQLBuilder.castValueByField(it, findField(fieldName).getField(), timezoneOffset))
                .collect(Collectors.toList()));
    }

    @Override
    public void addParam(String paramName, String fieldName, String value) {
        params.put(paramName, DSLSQLBuilder.castValueByField(value, findField(fieldName).getField(), timezoneOffset));
    }

    @Override
    public String aliasOf(String field) {
        return findField(field).selectName();
    }

    @Override
    public String paramName(String rawParamName) {
        return paramPrefix + rawParamName;
    }

    @Override
    public boolean supportsFieldReferenceValues() {
        return true;
    }

    private ColumnField findField(String field) {
        ColumnField columnField = fields.get(field);
        if (columnField == null) {
            throw new RuntimeException("field not found: " + field);
        }
        return columnField;
    }
}
