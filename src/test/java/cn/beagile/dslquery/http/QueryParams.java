package cn.beagile.dslquery.http;

import cn.beagile.dslquery.DSLQuery;
import cn.beagile.dslquery.DynamicJoinConfig;
import cn.beagile.dslquery.NullsOrder;
import cn.beagile.dslquery.Paged;
import cn.beagile.dslquery.Paging;
import cn.beagile.dslquery.QueryExecutor;
import cn.beagile.dslquery.RawSQLBuilder;
import cn.beagile.dslquery.SQLField;
import cn.beagile.dslquery.SQLQuery;
import cn.beagile.dslquery.WhereBuilder;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * GET 接口查询参数封装，负责把请求参数转换成 {@link DSLQuery} 并执行。
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
 * public Paged<Person> list(QueryParams params) {
 *     return params.pagedQuery(queryExecutor, Person.class);
 * }
 * }</pre>
 *
 * <p>本类只依赖 JDK 与 dslquery 自身的 API，不依赖 Spring 或 Lombok。
 */
public class QueryParams {
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

    public QueryParams() {
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

    public QueryParams deepJoinIncludes(String... deepJoinIncludes) {
        this.deepJoinIncludes = deepJoinIncludes;
        return this;
    }

    public QueryParams selectIgnores(String... selectIgnores) {
        this.selectIgnores = selectIgnores;
        return this;
    }

    public QueryParams where(String where) {
        this.whereList.add(where);
        return this;
    }

    public QueryParams whereEquals(String field, Serializable identity) {
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
                    .filter(QueryParams::hasText)
                    .forEach(query::where);
        }
        if (this.whereList != null) {
            this.whereList.stream()
                    .filter(QueryParams::hasText)
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

    public <T> T findOne(QueryExecutor queryExecutor, Class<T> tClass) {
        return findOptional(queryExecutor, tClass).orElse(null);
    }

    public <T> Optional<T> findOptional(QueryExecutor queryExecutor, Class<T> tClass) {
        return getDslQuery(queryExecutor, tClass).limit(1).query().stream().findFirst();
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

    /**
     * 生成 {@code order by} 片段，只保留 {@code allowedFields} 中声明的字段。
     * 供拼接原生 SQL 使用（{@link #pagedQuery} 走 DSLQuery 自带排序，不需要该方法）。
     * 无方向的字段按升序处理。
     */
    public String order(List<String> fields) {
        return buildOrder(sort1 -> fields.contains(sort1.field));
    }

    /**
     * 生成 {@code order by} 片段，并使用字段名白名单过滤。
     * 供拼接原生 SQL 使用（{@link #pagedQuery} 走 DSLQuery 自带排序，不需要该方法）。
     * 无方向的字段按升序处理。
     */
    public String order() {
        return buildOrder(Sort::isFieldLegal);
    }

    private String buildOrder(Predicate<Sort> fieldFilter) {
        if (!hasText(this.sort)) {
            return "";
        }
        List<String> sorts = Arrays.stream(this.sort.split(",")).map(Sort::new)
                .filter(fieldFilter)
                .filter(Sort::isDirectionLegal)
                .map(Sort::toSQL).toList();
        if (sorts.isEmpty()) {
            return "";
        }
        return sorts.stream().collect(Collectors.joining(",", "order by ", ""));
    }

    /**
     * 执行原生 SQL 查询并返回 Map 列表。
     * 注意：本方法不会应用 {@code $sort}，需要排序时请把 {@link #order()} 的结果拼进 SQL。
     */
    public List<Map<String, Object>> queryMap(QueryExecutor queryExecutor, String sql, List<SQLField> fields) {
        SQLQuery sqlQuery = getRawSQLBuilder(fields).toSQLQuery(sql, "", page());
        return queryExecutor.list(RESULT_SET_MAP_READER, sqlQuery);
    }

    /**
     * 执行原生 SQL 分页查询并返回 Map 列表。
     * 注意：本方法不会应用 {@code $sort}，需要排序时请把 {@link #order()} 的结果拼进 SQL。
     */
    public Paged<Map<String, Object>> pagedQueryMap(QueryExecutor queryExecutor, String sql, String countSql, List<SQLField> fields) {
        SQLQuery sqlQuery = getRawSQLBuilder(fields).toSQLQuery(sql, countSql, page());
        List<Map<String, Object>> result = queryExecutor.list(RESULT_SET_MAP_READER, sqlQuery);
        int count = queryExecutor.count(sqlQuery);
        return new Paged<>(result, count, page());
    }

    private RawSQLBuilder getRawSQLBuilder(List<SQLField> fields) {
        return new RawSQLBuilder(fields, getFilters());
    }

    /**
     * 把结果集读取成 {@code columnLabel -> value} 的有序 Map。
     */
    private static final Function<ResultSet, Map<String, Object>> RESULT_SET_MAP_READER = resultSet -> {
        try {
            ResultSetMetaData metaData = resultSet.getMetaData();
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                row.put(metaData.getColumnLabel(i), resultSet.getObject(i));
            }
            return row;
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    };

    public String[] getFilters() {
        return Stream.concat(
                        Optional.ofNullable(this.filter).orElseGet(List::of).stream(),
                        Optional.ofNullable(this.whereList).orElseGet(List::of).stream())
                .filter(QueryParams::hasText).toList().toArray(new String[0]);
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static class Sort {
        /**
         * 与主代码 {@code Validators.validateFieldName} 保持一致的白名单。
         */
        private static final Pattern LEGAL_FIELD = Pattern.compile("[a-zA-Z][a-zA-Z0-9_.-]*");

        private final String field;
        private String direction;
        private boolean legalFormat = true;

        public Sort(String sort) {
            String[] tokens = sort.trim().split("\\s+");
            this.field = tokens[0];
            if (tokens.length == 2) {
                this.direction = tokens[1];
            } else if (tokens.length > 2) {
                this.legalFormat = false;
            }
        }

        /**
         * 未指定方向时按升序处理，与 DSLQuery 的排序行为保持一致。
         */
        public boolean isDirectionLegal() {
            return legalFormat
                    && (!hasText(direction)
                    || "asc".equalsIgnoreCase(direction)
                    || "desc".equalsIgnoreCase(direction));
        }

        public String toSQL() {
            return "IF(" + field + " IS NULL, 1, 0), " + field + (hasText(direction) ? " " + direction : "");
        }

        public boolean isFieldLegal() {
            return LEGAL_FIELD.matcher(field).matches();
        }
    }
}
