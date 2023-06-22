package cn.beagile.dslquery;

import java.sql.ResultSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DSLQuery<T> {
    private QueryExecutor queryExecutor;
    private Function<ResultSet, T> queryResultBeanReader;
    private Class<T> queryResultBeanClass;

    public DSLQuery(QueryExecutor queryExecutor, Function<ResultSet, T> resultSetReader, Class<T> queryResultBeanClass) {
        this.queryExecutor = queryExecutor;
        this.queryResultBeanReader = resultSetReader;
        this.queryResultBeanClass = queryResultBeanClass;
    }

    public void query() {
        SQLQuery sqlQuery = new SQLQuery();
        View view = queryResultBeanClass.getAnnotation(View.class);
        String fields = Stream.of(queryResultBeanClass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Column.class))
                .map(field -> field.getAnnotation(Column.class))
                .map(Column::value)
                .collect(Collectors.joining(","));
        sqlQuery.setSql("select " + fields + " from " + view.value());
        queryExecutor.execute(sqlQuery, queryResultBeanReader);
    }
}
