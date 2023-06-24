package cn.beagile.dslquery;

import java.sql.ResultSet;
import java.util.List;
import java.util.function.Function;

public interface QueryExecutor {
    <T> List<T> list(SQLBuilder sqlBuilder, Function<ResultSet, T> resultSetReader);

    int count(SQLBuilder sqlBuilder);
}
