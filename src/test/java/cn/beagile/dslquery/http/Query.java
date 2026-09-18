package cn.beagile.dslquery.http;

import cn.beagile.dslquery.DSLQuery;
import cn.beagile.dslquery.NullsOrder;
import cn.beagile.dslquery.QueryExecutor;
import cn.beagile.dslquery.WhereBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * GET 接口单条查询参数封装，按请求条件取出一条实体数据。
 *
 * <p>可直接作为 Controller 的入参，由框架按 JavaBean 规则绑定查询串：
 * <ul>
 *     <li>{@code $filter}：DSL 条件，可重复传递；</li>
 *     <li>{@code $sort}：排序，如 {@code name desc}；</li>
 *     <li>{@code $timezoneOffset}：时区偏移，默认 +8。</li>
 * </ul>
 *
 * <p>与 {@link PagedQuery} 的区别：不接收 {@code $top}/{@code $skip}，也不会执行 count 查询。
 *
 * <p>本类只依赖 JDK 与 dslquery 自身的 API，不依赖 Spring 或 Lombok。
 */
public class Query {
    private static final int DEFAULT_TIMEZONE_OFFSET = 8;

    private List<String> filter = new ArrayList<>();
    private List<String> whereList = new ArrayList<>();
    private String sort;
    private int timezoneOffset = DEFAULT_TIMEZONE_OFFSET;
    private String[] deepJoinIncludes;
    private String[] selectIgnores;

    public Query() {
    }

    public void set$sort(String sort) {
        this.sort = sort;
    }

    public void set$filter(List<String> filter) {
        this.filter = filter == null ? new ArrayList<>() : filter;
    }

    public void set$timezoneOffset(int timezoneOffset) {
        this.timezoneOffset = timezoneOffset;
    }

    public Query deepJoinIncludes(String... deepJoinIncludes) {
        this.deepJoinIncludes = deepJoinIncludes;
        return this;
    }

    public Query selectIgnores(String... selectIgnores) {
        this.selectIgnores = selectIgnores;
        return this;
    }

    public Query where(String where) {
        this.whereList.add(where);
        return this;
    }

    public Query whereEquals(String field, Serializable identity) {
        if (identity == null) {
            this.where(WhereBuilder.where().and().isnull(field).build());
            return this;
        }
        this.where(WhereBuilder.where()
                .and().equals(field, identity.toString())
                .build());
        return this;
    }

    private <T> DSLQuery<T> getDslQuery(QueryExecutor queryExecutor, Class<T> factoryClass) {
        DSLQuery<T> query = new DSLQuery<>(queryExecutor, factoryClass, NullsOrder.NULL_LAST)
                .timezoneOffset(this.timezoneOffset)
                .sort(this.sort);
        applyConditions(query);
        return query;
    }

    /**
     * 把请求条件（filter/where/deepJoinIncludes/selectIgnores）应用到查询上。
     */
    private void applyConditions(DSLQuery<?> query) {
        if (this.filter != null) {
            this.filter.stream()
                    .filter(Query::hasText)
                    .forEach(query::where);
        }
        if (this.whereList != null) {
            this.whereList.stream()
                    .filter(Query::hasText)
                    .forEach(query::where);
        }
        if (this.deepJoinIncludes != null) {
            query.deepJoinIncludes(this.deepJoinIncludes);
        }
        if (this.selectIgnores != null) {
            query.selectIgnores(this.selectIgnores);
        }
    }

    public <T> T findOne(QueryExecutor queryExecutor, Class<T> factoryClass) {
        return findOptional(queryExecutor, factoryClass).orElse(null);
    }

    /**
     * 只取第一条，因此限定 {@code limit 1} 且不执行 count 查询。
     */
    public <T> Optional<T> findOptional(QueryExecutor queryExecutor, Class<T> factoryClass) {
        return getDslQuery(queryExecutor, factoryClass).limit(1).query().stream().findFirst();
    }

    public String[] getFilters() {
        return Stream.concat(
                        filterOrEmpty(this.filter).stream(),
                        filterOrEmpty(this.whereList).stream())
                .filter(Query::hasText).toList().toArray(new String[0]);
    }

    private static List<String> filterOrEmpty(List<String> values) {
        return values == null ? List.of() : values;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
