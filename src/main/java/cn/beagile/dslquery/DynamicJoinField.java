package cn.beagile.dslquery;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 动态Join字段
 */
public class DynamicJoinField {
    private final Field field;
    private final DynamicJoinConfig config;
    private final List<ColumnField> subFields;
    private final String alias;

    public DynamicJoinField(Field field, DynamicJoinConfig config) {
        this.field = field;
        this.config = config;
        this.alias = field.getName() + "_";
        this.subFields = readSubFields();
    }

    private List<ColumnField> readSubFields() {
        Class<?> targetClass = field.getType();
        List<Field> parents = new ArrayList<>();
        parents.add(field);

        return Arrays.stream(targetClass.getDeclaredFields())
                .filter(AnnotationReader::hasColumn)
                .map(f -> new ColumnField(f, targetClass, parents, AnnotationReader.getColumn(f), true))
                .collect(Collectors.toList());
    }

    public String joinStatement(Map<String, Object> params, int timezoneOffset) {
        DynamicJoin annotation = field.getAnnotation(DynamicJoin.class);
        if (annotation == null) {
            throw new RuntimeException("@DynamicJoin annotation not found on field: " + field.getName());
        }

        // 构建子查询SQL
        String subQuery = buildSubQuery(params, timezoneOffset);

        // 构建JOIN语句
        String joinType = getJoinType(annotation.joinType());
        String onClause = buildOnClause(annotation);

        return String.format("%s join (\n%s\n) %s on %s",
                joinType, subQuery, alias, onClause);
    }

    private String buildSubQuery(Map<String, Object> params, int timezoneOffset) {
        RawSQLBuilder builder = config.getBuilder();
        String sql = config.getSubQuerySql();

        // 先生成WHERE子句，这会触发参数生成
        String whereClause = builder.where();

        // 获取生成的参数
        Map<String, Object> builderParams = builder.getParams();

        // 重命名参数引用
        if (!whereClause.isEmpty() && !builderParams.isEmpty()) {
            for (String paramKey : builderParams.keySet()) {
                String newParamKey = "dj_" + field.getName() + "_" + paramKey;
                whereClause = whereClause.replaceAll(":" + paramKey + "\\b", ":" + newParamKey);
                params.put(newParamKey, builderParams.get(paramKey));
            }
        }

        if (sql.contains("${where}")) {
            sql = sql.replace("${where}", whereClause);
        } else if (!whereClause.isEmpty()) {
            sql = sql + " " + whereClause;
        }

        // 添加排序
        String sortClause = builder.sort();
        if (!sortClause.isEmpty()) {
            sql = sql + " " + sortClause;
        }

        return sql;
    }

    private String buildOnClause(DynamicJoin annotation) {
        String parentTable = field.getDeclaringClass().getAnnotation(View.class).value();
        return String.format("%s.%s = %s.%s",
                alias, annotation.targetKey(),
                parentTable, annotation.joinKey());
    }

    private String getJoinType(DynamicJoin.JoinType joinType) {
        return switch (joinType) {
            case LEFT -> "left";
            case INNER -> "inner";
            case RIGHT -> "right";
        };
    }

    public List<ColumnField> getSubFields() {
        return subFields;
    }

    public String getAlias() {
        return alias;
    }

    public boolean is(Field field) {
        return this.field.equals(field);
    }

    public String parentNames() {
        return field.getName();
    }
}
