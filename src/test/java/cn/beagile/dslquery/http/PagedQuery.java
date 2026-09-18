package cn.beagile.dslquery.http;

import cn.beagile.dslquery.DSLQuery;
import cn.beagile.dslquery.DynamicJoinConfig;
import cn.beagile.dslquery.NullsOrder;
import cn.beagile.dslquery.Paged;
import cn.beagile.dslquery.Paging;
import cn.beagile.dslquery.QueryExecutor;
import cn.beagile.dslquery.RawSQLBuilder;
import cn.beagile.dslquery.SQLField;
import cn.beagile.dslquery.WhereBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * GET 接口分页查询参数封装，负责把请求参数转换成 {@link DSLQuery} 并执行分页查询。
 *
 * <p>可直接作为 Controller 的入参，由框架按 JavaBean 规则绑定查询串：
 * <ul>
 *     <li>{@code $filter}：DSL 条件，可重复传递；</li>
 *     <li>{@code $sort}：排序，如 {@code name desc,age asc}；</li>
 *     <li>{@code $top}：每页条数，默认 {@value #DEFAULT_TOP}，最大 {@value #MAX_TOP}；</li>
 *     <li>{@code $skip}：跳过条数，默认 0；</li>
 *     <li>{@code $timezoneOffset}：时区偏移，默认 +8。</li>
 * </ul>
 *
 * <p>注意：{@code $filter} 的值最终会被 {@code WhereParser} 做 URL 解码，而绑定阶段
 * 框架会按逗号拆分集合参数，因此值里包含逗号时请先做 URL 编码（例如把 {@code ,} 写成 {@code %2C}）。
 *
 * <p>用法示例：
 * <pre>{@code
 * @GetMapping("/persons")
 * public Paged<Person> list(PagedQuery query) {
 *     return query.pagedQuery(queryExecutor, Person.class);
 * }
 * }</pre>
 *
 * <p>本类只依赖 JDK 与 dslquery 自身的 API，不依赖 Spring 或 Lombok。
 */
public class PagedQuery {
    private static final int DEFAULT_TOP = 1000;
    private static final int MAX_TOP = 10000;
    private static final int DEFAULT_TIMEZONE_OFFSET = 8;

    private List<String> filter = new ArrayList<>();
    private List<String> whereList = new ArrayList<>();
    private int skip;
    private int top = DEFAULT_TOP;
    private String sort;
    private int timezoneOffset = DEFAULT_TIMEZONE_OFFSET;
    private String[] deepJoinIncludes;
    private String[] selectIgnores;

    public PagedQuery() {
    }

    /**
     * 绑定 {@code $top}。非正值将被忽略，保持当前分页大小。
     */
    public void set$top(int top) {
        if (top > 0) {
            this.top = Math.min(top, MAX_TOP);
        }
    }

    /**
     * 绑定 {@code $skip}。负值按 0 处理。
     */
    public void set$skip(int skip) {
        this.skip = Math.max(skip, 0);
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

    public PagedQuery deepJoinIncludes(String... deepJoinIncludes) {
        this.deepJoinIncludes = deepJoinIncludes;
        return this;
    }

    public PagedQuery selectIgnores(String... selectIgnores) {
        this.selectIgnores = selectIgnores;
        return this;
    }

    public PagedQuery where(String where) {
        this.whereList.add(where);
        return this;
    }

    public PagedQuery whereEquals(String field, Serializable identity) {
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
                .limit(this.top)
                .skip(this.skip)
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
                    .filter(PagedQuery::hasText)
                    .forEach(query::where);
        }
        if (this.whereList != null) {
            this.whereList.stream()
                    .filter(PagedQuery::hasText)
                    .forEach(query::where);
        }
        if (this.deepJoinIncludes != null) {
            query.deepJoinIncludes(this.deepJoinIncludes);
        }
        if (this.selectIgnores != null) {
            query.selectIgnores(this.selectIgnores);
        }
    }

    public <T> Paged<T> pagedQuery(QueryExecutor queryExecutor, Class<T> factoryClass) {
        return getDslQuery(queryExecutor, factoryClass).pagedQuery();
    }

    /**
     * 便捷重载：用本次请求的条件构造动态 Join 的 {@link RawSQLBuilder}，
     * 因此 {@code $filter} / {@link #where(String)} 会同时作用于主查询和 Join 子查询。
     */
    public <T> Paged<T> pagedQuery(QueryExecutor queryExecutor, Class<T> factoryClass, String dynamicJoinName, List<SQLField> dynamicJoinFields, String dynamicJoinSql) {
        return pagedQuery(queryExecutor, factoryClass,
                new DynamicJoinConfig(dynamicJoinName, new RawSQLBuilder(dynamicJoinFields, getFilters()), dynamicJoinSql));
    }

    /**
     * 使用调用方自行构造的动态 Join 配置执行分页查询。
     *
     * <p>注意：Join 子查询的条件来自 {@link DynamicJoinConfig#getBuilder()} 自身的 builder，
     * 本方法不会把本次请求的 {@code $filter} 追加进去；若需要下推请求条件，请使用带
     * {@code dynamicJoinName/dynamicJoinFields/dynamicJoinSql} 的重载。
     */
    public <T> Paged<T> pagedQuery(QueryExecutor queryExecutor, Class<T> factoryClass, DynamicJoinConfig... dynamicJoinConfigs) {
        DSLQuery<T> query = new DSLQuery<>(queryExecutor, factoryClass, NullsOrder.NULL_LAST)
                .limit(this.top)
                .skip(this.skip)
                .timezoneOffset(this.timezoneOffset)
                .sort(this.sort);

        if (dynamicJoinConfigs != null) {
            for (DynamicJoinConfig config : dynamicJoinConfigs) {
                if (config == null) {
                    continue;
                }
                query.dynamicJoin(config.getFieldName(), config.getBuilder(), config.getSubQuerySql());
            }
        }

        applyConditions(query);
        return query.pagedQuery();
    }

    public Paging page() {
        return new Paging(skip, top);
    }

    public String[] getFilters() {
        return Stream.concat(
                        filterOrEmpty(this.filter).stream(),
                        filterOrEmpty(this.whereList).stream())
                .filter(PagedQuery::hasText).toList().toArray(new String[0]);
    }

    private static List<String> filterOrEmpty(List<String> values) {
        return values == null ? List.of() : values;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
