package cn.beagile.dslquery.http;

import cn.beagile.dslquery.NullsOrder;
import cn.beagile.dslquery.QueryExecutor;
import cn.beagile.dslquery.SQLQuery;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 记录执行过的 {@link SQLQuery}、不真正访问数据库的测试用 QueryExecutor。
 */
class RecordingQueryExecutor implements QueryExecutor {
    final List<SQLQuery> executed = new ArrayList<>();
    int countCalls;

    @Override
    public <T> List<T> list(Function<ResultSet, T> resultSetReader, SQLQuery sqlQuery) {
        executed.add(sqlQuery);
        return List.of();
    }

    @Override
    public int count(SQLQuery sqlQuery) {
        countCalls++;
        return 0;
    }

    @Override
    public String nullsOrder(NullsOrder nullsOrder) {
        return "";
    }
}
