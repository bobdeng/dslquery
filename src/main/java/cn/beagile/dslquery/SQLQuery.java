package cn.beagile.dslquery;

import java.util.Map;

public class SQLQuery {
    private final String sql;
    private final String countSql;
    private final Map<String, Object> params;
    private Paging page;

    public SQLQuery(String sql, String countSql, Map<String, Object> params, Paging page) {
        this.sql = sql;
        this.countSql = countSql;
        this.params = params;
        this.page = page;
    }

    public String getSql() {
        return sql;
    }

    public String getCountSql() {
        return countSql;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public Integer getLimit() {
        return page.getLimit();
    }

    public Integer getSkip() {
        return page.getSkip();
    }

}
