package cn.beagile.dslquery;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * 规则驱动的表单过滤构建器：给定字段规则与表单值，自动跳过空值、只对定义了规则的字段生成条件，并按 AND 组合。
 * 规则应只构建一次并复用（{@link #buildFilter} 可被多个线程并发调用）。
 *
 * <p>对应 JS 版 {@code buildFilter(rules, form, ...extraConditions)} 的 Java 适配：
 * <pre>{@code
 * import static cn.beagile.dslquery.Conditions.*;
 *
 * FilterRules rules = FilterRules.create()
 *     .on("name",   value -> eq("name", value))
 *     .on("minAge", value -> ge("age", value))
 *     .on("status", value -> in("status", (Collection<?>) value))
 *     .withDefault(form -> form.get("tenantId") != null ? eq("tenantId", form.get("tenantId")) : null);
 *
 * // 每次请求：
 * String where = rules.buildFilter(params, contains("phone", "123"));
 * if (!where.isEmpty()) {
 *     dslQuery.where(where);
 * }
 * }</pre>
 *
 * <p>空值语义：null、空字符串、空集合/数组视为无值跳过；boolean（含 false）与数字（含 0）视为有值。
 * 规则函数返回 {@code null} 表示跳过该条件；需要生成多个条件时返回 {@code Conditions.and(...)} 组合。
 */
public class FilterRules {
    private final Map<String, Function<Object, String>> fieldRules = new LinkedHashMap<>();
    private Function<Map<String, Object>, String> defaultRule;

    private FilterRules() {
    }

    public static FilterRules create() {
        return new FilterRules();
    }

    /**
     * 为字段注册规则函数：入参为该字段的表单值，返回条件片段（或 null 跳过）。
     */
    public FilterRules on(String field, Function<Object, String> rule) {
        fieldRules.put(field, rule);
        return this;
    }

    /**
     * 注册默认规则：入参为整个表单，在字段规则之后追加（或 null 跳过）。
     */
    public FilterRules withDefault(Function<Map<String, Object>, String> rule) {
        this.defaultRule = rule;
        return this;
    }

    /**
     * 根据表单值构建完整 where 子句。无条件时返回空字符串（表示无过滤，DSLQuery.where("") 为 no-op）。
     */
    public String buildFilter(Map<String, Object> form, String... extraConditions) {
        String expression = buildExpression(form, extraConditions);
        return expression == null ? "" : expression;
    }

    private String buildExpression(Map<String, Object> form, String... extraConditions) {
        List<String> conditions = new ArrayList<>();
        if (form != null) {
            form.forEach((field, value) -> {
                if (hasAnyValue(value)) {
                    Function<Object, String> rule = fieldRules.get(field);
                    if (rule != null) {
                        String condition = rule.apply(value);
                        if (condition != null) {
                            conditions.add(condition);
                        }
                    }
                }
            });
        }
        if (defaultRule != null) {
            String condition = defaultRule.apply(form);
            if (condition != null) {
                conditions.add(condition);
            }
        }
        String[] extra = Arrays.stream(extraConditions).filter(Objects::nonNull).toArray(String[]::new);
        if (conditions.isEmpty()) {
            if (extra.length == 0) {
                return null;
            }
            return Conditions.and(extra);
        }
        return Conditions.and(Stream.concat(conditions.stream(), Arrays.stream(extra)).toArray(String[]::new));
    }

    private boolean hasAnyValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String) {
            return !((String) value).isEmpty();
        }
        if (value instanceof Collection<?>) {
            return !((Collection<?>) value).isEmpty();
        }
        if (value.getClass().isArray()) {
            return Array.getLength(value) > 0;
        }
        return true;
    }
}
