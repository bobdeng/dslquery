package cn.beagile.dslquery;

import java.util.ArrayList;
import java.util.List;

public class DSLQuery<T> {
    private final QueryExecutor queryExecutor;
    private final Class<T> queryResultClass;
    private final List<ComplexExpression> whereList;
    private Sort sort;
    private Integer skip;
    private Integer limit;
    private int timezoneOffset;

    public DSLQuery(QueryExecutor queryExecutor, Class<T> queryResultClass) {
        this.queryExecutor = queryExecutor;
        this.queryResultClass = queryResultClass;
        this.whereList = new ArrayList<>();
    }

    public DSLQuery<T> where(String where) {
        this.whereList.add(new WhereParser().parseSubWhere(where));
        return this;
    }

    public DSLQuery<T> sort(String sort) {
        this.sort = new Sort(sort);
        return this;
    }

    public DSLQuery<T> skip(Integer skip) {
        this.skip = skip;
        return this;
    }

    public DSLQuery<T> limit(Integer limit) {
        this.limit = limit;
        return this;
    }

    public DSLQuery<T> timezoneOffset(int timezoneOffset) {
        this.timezoneOffset = timezoneOffset;
        return this;
    }

    public Paged<T> pagedQuery() {
        SQLBuilder sqlQuery = getSqlQuery();
        List<T> result = queryExecutor.list(sqlQuery, new DefaultResultSetReader<>(queryResultClass));
        int count = queryExecutor.count(sqlQuery);
        return new Paged<>(result, count, new Paging(this.skip, this.limit));
    }

    public List<T> query() {
        return queryExecutor.list(getSqlQuery(), new DefaultResultSetReader<>(queryResultClass));
    }

    private SQLBuilder getSqlQuery() {
        return new SQLBuilder(this);
    }

    public Class<T> getQueryResultClass() {
        return queryResultClass;
    }

    public List<ComplexExpression> getWhereList() {
        return whereList;
    }

    public Sort getSort() {
        return sort;
    }

    public Integer getSkip() {
        return skip;
    }

    public Integer getLimit() {
        return limit;
    }

    public int getTimezoneOffset() {
        return timezoneOffset;
    }

}
