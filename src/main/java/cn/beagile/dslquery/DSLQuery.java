package cn.beagile.dslquery;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DSLQuery<T> {
    private QueryExecutor queryExecutor;
    private Class<T> queryResultBeanClass;
    private List<Where> whereList;
    private SQLParams sqlParams;

    public DSLQuery(QueryExecutor queryExecutor, Class<T> queryResultBeanClass) {
        this.queryExecutor = queryExecutor;
        this.queryResultBeanClass = queryResultBeanClass;
        this.whereList = new ArrayList<>();
        this.sqlParams = new SQLParams();
    }

    public List<T> query() {
        SQLQuery sqlQuery = new SQLQuery(queryResultBeanClass);
        View view = queryResultBeanClass.getAnnotation(View.class);
        String fields = Stream.of(queryResultBeanClass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Column.class))
                .map(field -> field.getAnnotation(Column.class))
                .map(Column::value)
                .collect(Collectors.joining(","));
        String sql = "select " + fields + " from " + view.value();
        if(whereList.size()>0){
            sql+=" where "+whereList.stream().map(where -> where.toSQL(sqlQuery)).collect(Collectors.joining(" and "));
        }
        sqlQuery.setSql(sql);
        return queryExecutor.execute(sqlQuery, new DefaultResultSetReader<>(queryResultBeanClass));
    }

    public DSLQuery<T> where(String where) {
        this.whereList.add(new WhereParser().parse(where));
        return this;
    }
}
