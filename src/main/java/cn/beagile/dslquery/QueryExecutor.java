package cn.beagile.dslquery;

import java.sql.ResultSet;
import java.util.List;
import java.util.function.Function;

public interface QueryExecutor {
    <T> List<T> list(Function<ResultSet, T> resultSetReader, SQLQuery sqlQuery);

    int count(SQLQuery sqlQuery);
}
