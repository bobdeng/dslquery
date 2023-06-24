package cn.beagile.dslquery;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DSLQuery<T> {
    private final QueryExecutor queryExecutor;
    private final Class<T> queryResultClass;
    private final List<ComplexExpression> whereList;
    private Sort sort;
    private Integer skip;
    private Integer limit;
    private int timezoneOffset;
    private String whereCondition = null;

    public DSLQuery(QueryExecutor queryExecutor, Class<T> queryResultClass) {
        this.queryExecutor = queryExecutor;
        this.queryResultClass = queryResultClass;
        this.whereList = new ArrayList<>();
    }

    private String getCountSQL(SQLQuery sqlQuery) {
        String result = String.format("select count(*) from %s", getViewName());
        if (whereList.size() > 0) {
            result += getWhereSQL(sqlQuery);
        }
        return result;
    }

    private String getSQL(SQLQuery sqlQuery) {
        String sql = getSelectSQL();
        if (whereList.size() > 0) {
            sql += getWhereSQL(sqlQuery);
        }
        if (this.sort != null) {
            sql += getSortSQL(sqlQuery);
        }
        return sql;
    }

    private String getSortSQL(SQLQuery sqlQuery) {
        return String.format(" order by %s", sort.toSQL(sqlQuery));
    }

    private String getWhereSQL(SQLQuery sqlQuery) {
        if (this.whereCondition == null) {
            this.whereCondition = String.format(" where %s", whereList.stream().map(where -> where.toSQL(sqlQuery)).collect(Collectors.joining(" and ")));
        }
        return this.whereCondition;
    }

    private String getSelectSQL() {
        String fields = Stream.of(queryResultClass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Column.class))
                .map(field -> field.getAnnotation(Column.class))
                .map(Column::value)
                .collect(Collectors.joining(","));
        return String.format("select %s from %s", fields, getViewName());
    }

    private String getViewName() {
        View view = queryResultClass.getAnnotation(View.class);
        return view.value();
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
        SQLQuery sqlQuery = getSqlQuery();
        List<T> result = queryExecutor.list(sqlQuery, new DefaultResultSetReader<>(queryResultClass));
        int count = queryExecutor.count(sqlQuery);
        return new Paged<>(result, count, new Paging(this.skip, this.limit));
    }

    public List<T> query() {
        return queryExecutor.list(getSqlQuery(), new DefaultResultSetReader<>(queryResultClass));
    }

    private SQLQuery getSqlQuery() {
        SQLQuery sqlQuery = new SQLQuery(queryResultClass, this.timezoneOffset);
        sqlQuery.setSql(getSQL(sqlQuery));
        sqlQuery.setCountSql(getCountSQL(sqlQuery));
        sqlQuery.setPaging(new Paging(this.skip, this.limit));
        return sqlQuery;
    }
}
