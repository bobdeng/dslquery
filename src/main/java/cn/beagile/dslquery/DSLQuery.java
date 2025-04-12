package cn.beagile.dslquery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DSLQuery<T> {
    private final QueryExecutor queryExecutor;
    private final Class<T> queryResultClass;
    private final List<ComplexExpression> whereList;
    private Sort sort;
    private Integer skip;
    private Integer limit;
    private int timezoneOffset;
    private List<String> deepJoins = new ArrayList<>();
    private List<String> selectIgnores = new ArrayList<>();
    private NullsOrder nullsOrder;

    public DSLQuery(QueryExecutor queryExecutor, Class<T> queryResultClass) {
        this(queryExecutor, queryResultClass, NullsOrder.NONE);
    }
    public DSLQuery(QueryExecutor queryExecutor, Class<T> queryResultClass,NullsOrder nullsOrder) {
        this.queryExecutor = queryExecutor;
        this.queryResultClass = queryResultClass;
        this.whereList = new ArrayList<>();
        this.nullsOrder = nullsOrder;
    }

    public NullsOrder getNullsOrder() {
        return nullsOrder;
    }

    public List<String> getDeepJoins() {
        return deepJoins;
    }

    public Paging getPage() {
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
        DSLSQLBuilder<T> sqlBuilder = new DSLSQLBuilder<>(this);
        List<T> result = queryExecutor.list(new DefaultResultSetReader<>(this), sqlBuilder.build(queryExecutor.nullsOrder(this.nullsOrder)));
        int count = queryExecutor.count(sqlBuilder.build(queryExecutor.nullsOrder(this.nullsOrder)));
        sqlBuilder.fetchOne2Many(result, queryExecutor);
        return new Paged<>(result, count, new Paging(this.skip, this.limit));
    }

    public List<T> query() {
        DSLSQLBuilder<T> sqlBuilder = new DSLSQLBuilder<>(this);
        List<T> result = queryExecutor.list(new DefaultResultSetReader<>(this), sqlBuilder.build(queryExecutor.nullsOrder(this.nullsOrder)));
        sqlBuilder.fetchOne2Many(result, queryExecutor);
        return result;
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

    public DSLQuery<T> deepJoinIncludes(String... fields) {
        deepJoins.addAll(Arrays.asList(fields));
        return this;
    }

    public DSLQuery<T> selectIgnores(String... selectIgnores) {
        this.selectIgnores.addAll(Arrays.asList(selectIgnores));
        return this;
    }

    public List<String> getSelectIgnores() {
        return selectIgnores;
    }
}
