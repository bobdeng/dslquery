package cn.beagile.dslquery;

import java.sql.ResultSet;
import java.util.List;
import java.util.function.Function;

public interface QueryExecutor {
    <T> List<T> list(SQLBuilder sqlQuery, Function<ResultSet, T> resultSetReader);

    int count(SQLBuilder sqlQuery);
}
