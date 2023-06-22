package cn.beagile.dslquery;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DSLQuery<T> {
    private QueryExecutor queryExecutor;
    private Class<T> queryResultBeanClass;

    public DSLQuery(QueryExecutor queryExecutor, Class<T> queryResultBeanClass) {
        this.queryExecutor = queryExecutor;
        this.queryResultBeanClass = queryResultBeanClass;
    }

    public List<T> query() {
        SQLQuery sqlQuery = new SQLQuery();
        View view = queryResultBeanClass.getAnnotation(View.class);
        String fields = Stream.of(queryResultBeanClass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Column.class))
                .map(field -> field.getAnnotation(Column.class))
                .map(Column::value)
                .collect(Collectors.joining(","));
        sqlQuery.setSql("select " + fields + " from " + view.value());
        return queryExecutor.execute(sqlQuery, new DefaultResultSetReader<>(queryResultBeanClass));
    }
}
