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
    public Paging getPage(){
        return new Paging(getSkip(), getLimit());
    }
    public DSLQuery<T> where(String where) {
        if (where == null || where.isEmpty()) {
            return this;
        }
        this.whereList.add(new WhereParser().parse(where));
        return this;
    }

    public DSLQuery<T> sort(String sort) {
        if (sort == null || sort.isEmpty()) {
            return this;
        }
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
        SQLBuilder<T> sqlBuilder = new SQLBuilder<>(this);
        List<T> result = queryExecutor.list(new DefaultResultSetReader<>(queryResultClass), sqlBuilder.build());
        int count = queryExecutor.count(sqlBuilder.build());
        return new Paged<>(result, count, new Paging(this.skip, this.limit));
    }

    public List<T> query() {
        SQLBuilder<T> sqlBuilder = new SQLBuilder<>(this);
        return queryExecutor.list(new DefaultResultSetReader<>(queryResultClass), sqlBuilder.build());
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
