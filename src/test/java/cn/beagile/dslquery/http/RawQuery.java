package cn.beagile.dslquery.http;

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
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * GET 接口原生 SQL 查询参数封装，把请求条件转换成 {@link RawSQLBuilder} 后执行。
 *
 * <p>可直接作为 Controller 的入参，由框架按 JavaBean 规则绑定查询串：
 * <ul>
 *     <li>{@code $filter}：DSL 条件，可重复传递；</li>
 *     <li>{@code $sort}：排序，如 {@code name desc}；</li>
 *     <li>{@code $top}：每页条数，默认 {@value #DEFAULT_TOP}，最大 {@value #MAX_TOP}；</li>
 *     <li>{@code $skip}：跳过条数，默认 0。</li>
 * </ul>
 *
 * <p>与 {@link PagedQuery} 的区别：查询语句由调用方提供，返回 {@code Map} 而不是实体；
 * {@code $sort} 不会自动应用到 SQL 上，需要排序时把 {@link #order()} 的结果拼进 SQL 模板。
 *
 * <p>本类只依赖 JDK 与 dslquery 自身的 API，不依赖 Spring 或 Lombok。
 */
public class RawQuery {
    private static final int DEFAULT_TOP = 1000;
    private static final int MAX_TOP = 10000;

    private List<String> filter = new ArrayList<>();
    private List<String> whereList = new ArrayList<>();
    private int skip;
    private int top = DEFAULT_TOP;
    private String sort;

    public RawQuery() {
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

    public RawQuery where(String where) {
        this.whereList.add(where);
        return this;
    }

    public RawQuery whereEquals(String field, Serializable identity) {
        if (identity == null) {
            this.where(WhereBuilder.where().and().isnull(field).build());
            return this;
        }
        this.where(WhereBuilder.where()
                .and().equals(field, identity.toString())
                .build());
        return this;
    }

    /**
     * 生成 {@code order by} 片段，只保留 {@code allowedFields} 中声明的字段。
     * 无方向的字段按升序处理。
     */
    public String order(List<String> allowedFields) {
        return buildOrder(sort1 -> allowedFields.contains(sort1.field));
    }

    /**
     * 生成 {@code order by} 片段，并使用字段名白名单过滤。
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
     */
    public List<Map<String, Object>> queryMap(QueryExecutor queryExecutor, String sql, List<SQLField> fields) {
        SQLQuery sqlQuery = getRawSQLBuilder(fields).toSQLQuery(sql, "", page());
        return queryExecutor.list(RESULT_SET_MAP_READER, sqlQuery);
    }

    /**
     * 执行原生 SQL 分页查询并返回 Map 列表。
     */
    public Paged<Map<String, Object>> pagedQueryMap(QueryExecutor queryExecutor, String sql, String countSql, List<SQLField> fields) {
        SQLQuery sqlQuery = getRawSQLBuilder(fields).toSQLQuery(sql, countSql, page());
        List<Map<String, Object>> result = queryExecutor.list(RESULT_SET_MAP_READER, sqlQuery);
        int count = queryExecutor.count(sqlQuery);
        return new Paged<>(result, count, page());
    }

    public Paging page() {
        return new Paging(skip, top);
    }

    private RawSQLBuilder getRawSQLBuilder(List<SQLField> fields) {
        return new RawSQLBuilder(fields, getFilters());
    }

    public String[] getFilters() {
        return Stream.concat(
                        filterOrEmpty(this.filter).stream(),
                        filterOrEmpty(this.whereList).stream())
                .filter(RawQuery::hasText).toList().toArray(new String[0]);
    }

    private static List<String> filterOrEmpty(List<String> values) {
        return values == null ? List.of() : values;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
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
