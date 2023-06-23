package cn.beagile.dslquery;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DSLQuery<T> {
    private QueryExecutor queryExecutor;
    private Class<T> queryResultClass;
    private List<ComplexExpression> whereList;
    private Sort sort;
    private Integer skip;
    private Integer limit;
    private int timezoneOffset;

    public DSLQuery(QueryExecutor queryExecutor, Class<T> queryResultClass) {
        this.queryExecutor = queryExecutor;
        this.queryResultClass = queryResultClass;
        this.whereList = new ArrayList<>();
    }

    public List<T> query() {
        SQLQuery sqlQuery = new SQLQuery(queryResultClass, this.timezoneOffset);
        sqlQuery.setSql(getSQL(sqlQuery));
        sqlQuery.setSkip(this.skip);
        sqlQuery.setLimit(this.limit);
        return queryExecutor.execute(sqlQuery, new DefaultResultSetReader<>(queryResultClass));
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
        return " order by " + this.sort.toSQL(sqlQuery);
    }

    private String getWhereSQL(SQLQuery sqlQuery) {
        return " where " + whereList.stream().map(where -> where.toSQL(sqlQuery)).collect(Collectors.joining(" and "));
    }

    private String getSelectSQL() {
        View view = queryResultClass.getAnnotation(View.class);
        String fields = Stream.of(queryResultClass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Column.class))
                .map(field -> field.getAnnotation(Column.class))
                .map(Column::value)
                .collect(Collectors.joining(","));
        return "select " + fields + " from " + view.value();
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
}
