package cn.beagile.dslquery;

import java.util.HashMap;
import java.util.Map;

public class SQLQuery {
    private String sql;
    private Map<String, Object> params;
    private Integer skip;
    private int index;
    private Class queryResultBeanClass;
    private Integer limit;

    public SQLQuery(Class queryResultBeanClass) {
        this.index = 1;
        this.queryResultBeanClass = queryResultBeanClass;
        this.params = new HashMap<>();
    }


    public int next() {
        return this.index++;
    }

    public void addParam(String paramName, String field, String value) {
        try {
            if (this.queryResultBeanClass.getDeclaredField(field).getType().equals(Integer.class)) {
                params.put(paramName, 20);
                return;
            }
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        params.put(paramName, value);
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
