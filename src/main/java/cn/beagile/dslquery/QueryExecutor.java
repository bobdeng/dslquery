package cn.beagile.dslquery;

import java.sql.ResultSet;
import java.util.List;
import java.util.function.Function;

public interface QueryExecutor {
    <T> List<T> list(SQLQuery sqlQuery, Function<ResultSet, T> resultSetReader);

    int count(SQLQuery sqlQuery);
}
