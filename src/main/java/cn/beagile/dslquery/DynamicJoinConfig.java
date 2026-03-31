package cn.beagile.dslquery;

import java.util.HashMap;
import java.util.Map;

/**
 * 动态Join配置
 */
public class DynamicJoinConfig {
    private final String fieldName;
    private final RawSQLBuilder builder;
    private final String subQuerySql;
    private final Map<String, Object> params;

    public DynamicJoinConfig(String fieldName, RawSQLBuilder builder, String subQuerySql) {
        this.fieldName = fieldName;
        this.builder = builder;
        this.subQuerySql = subQuerySql;
        this.params = new HashMap<>();
    }

    public String getFieldName() {
        return fieldName;
    }

    public RawSQLBuilder getBuilder() {
        return builder;
    }

    public String getSubQuerySql() {
        return subQuerySql;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void addParams(Map<String, Object> newParams) {
        this.params.putAll(newParams);
    }
}
