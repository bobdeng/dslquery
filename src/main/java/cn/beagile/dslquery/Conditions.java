package cn.beagile.dslquery;

import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 功能式构建查询条件。每个工厂方法返回一段 DSL 字符串，可直接组合或传给 DSLQuery.where(String)。
 * 相比 {@link WhereBuilder}，用 {@link #and(String...)}/{@link #or(String...)} 组合表达式，
 * 无栈、无 prev()，嵌套优先级一目了然。
 *
 * <p>条件需要分步、按需追加时（例如根据请求参数是否为空决定加不加条件），使用累加器：
 * <pre>{@code
 * Conditions conditions = Conditions.accumulator();
 * if (name != null) conditions.add(eq("name", name));
 * if (age != null)  conditions.add(gt("age", age));
 * if (!conditions.isEmpty()) {
 *     dslQuery.where(conditions.build());
 * }
 * }</pre>
 *
 * <pre>{@code
 * import static cn.beagile.dslquery.Conditions.*;
 *
 * String dsl = where(
 *     and(
 *         eq("name", "bob"),
 *         gt("age", 18),
 *         or(startsWith("email", "test"), contains("phone", "123"))
 *     )
 * );
 * // (and(name eq bob)(age gt 18)(or(email sw test)(phone ct 123)))
 * }</pre>
 */
public class Conditions {
    private static final Gson GSON = new Gson();

    private final String condition;
    private final List<String> fragments = new ArrayList<>();

    private Conditions(String condition) {
        this.condition = condition;
    }

    /**
     * 创建一个按 AND 组合的累加器，可多次 {@link #add(String...)} 追加条件。
     */
    public static Conditions accumulator() {
        return new Conditions("and");
    }

    /**
     * 创建一个按 OR 组合的累加器，可多次 {@link #add(String...)} 追加条件。
     */
    public static Conditions orAccumulator() {
        return new Conditions("or");
    }

    /**
     * 追加一个或多个条件片段（叶子条件或 {@link #and}/{@link #or} 组合出的分组）。null 片段被忽略。
     */
    public Conditions add(String... expressions) {
        for (String expression : expressions) {
            if (expression != null) {
                fragments.add(expression);
            }
        }
        return this;
    }

    public boolean isEmpty() {
        return fragments.isEmpty();
    }

    public int size() {
        return fragments.size();
    }

    /**
     * 收尾生成完整 DSL 子句。未追加任何条件时返回空字符串（表示无过滤，DSLQuery.where("") 为 no-op）。
     */
    public String build() {
        if (fragments.isEmpty()) {
            return "";
        }
        return combineRaw(condition, fragments.toArray(new String[0]));
    }

    /**
     * 将所有表达式按 AND 组合成一个完整子句。忽略 null 片段；无有效条件时抛异常。
     */
    public static String and(String... expressions) {
        return combine("and", expressions);
    }

    /**
     * 将所有表达式按 OR 组合成一个完整子句。忽略 null 片段；无有效条件时抛异常。
     */
    public static String or(String... expressions) {
        return combine("or", expressions);
    }

    /**
     * 入口：返回一个完整可解析的 where 子句。
     * <ul>
     *     <li>无有效条件：返回空字符串（表示无过滤）</li>
     *     <li>多个片段：按 AND 隐式组合，如 {@code where(eq("a","1"), eq("b","2"))} → {@code (and(a eq 1)(b eq 2))}</li>
     *     <li>单个已包装片段（以 (and( 或 (or( 开头）：原样返回，避免双重包装</li>
     *     <li>单个未包装片段：用 and 包装，如 {@code where(eq("a","1"))} → {@code (and(a eq 1))}</li>
     * </ul>
     */
    public static String where(String... expressions) {
        String[] effective = nonNull(expressions);
        if (effective.length == 0) {
            return "";
        }
        if (effective.length == 1 && isWrapped(effective[0])) {
            return effective[0];
        }
        return combineRaw("and", effective);
    }

    private static String combine(String condition, String... expressions) {
        String[] effective = nonNull(expressions);
        if (effective.length == 0) {
            throw new IllegalArgumentException("no effective conditions for " + condition);
        }
        return combineRaw(condition, effective);
    }

    private static String combineRaw(String condition, String... expressions) {
        return "(" + condition + String.join("", expressions) + ")";
    }

    private static String[] nonNull(String... expressions) {
        return Arrays.stream(expressions).filter(Objects::nonNull).toArray(String[]::new);
    }

    private static boolean isWrapped(String expression) {
        return expression.startsWith("(and(") || expression.startsWith("(or(");
    }

    public static String eq(String field, Object value) {
        return single(field, Operator.Equals, value);
    }

    public static String ne(String field, Object value) {
        return single(field, Operator.NotEquals, value);
    }

    public static String gt(String field, Object value) {
        return single(field, Operator.GreaterThan, value);
    }

    public static String ge(String field, Object value) {
        return single(field, Operator.GreaterThanOrEqual, value);
    }

    public static String lt(String field, Object value) {
        return single(field, Operator.LessThan, value);
    }

    public static String le(String field, Object value) {
        return single(field, Operator.LessThanOrEqual, value);
    }

    public static String startsWith(String field, Object value) {
        return single(field, Operator.StartsWith, value);
    }

    public static String endsWith(String field, Object value) {
        return single(field, Operator.EndsWith, value);
    }

    public static String contains(String field, Object value) {
        return single(field, Operator.Contains, value);
    }

    public static String notStartsWith(String field, Object value) {
        return single(field, Operator.NotStartsWith, value);
    }

    public static String notEndsWith(String field, Object value) {
        return single(field, Operator.NotEndsWith, value);
    }

    public static String notContains(String field, Object value) {
        return single(field, Operator.NotContains, value);
    }

    public static String isNull(String field) {
        return noValue(field, Operator.IsNull);
    }

    public static String notNull(String field) {
        return noValue(field, Operator.NotNull);
    }

    public static String isEmpty(String field) {
        return noValue(field, Operator.Empty);
    }

    public static String isNotEmpty(String field) {
        return noValue(field, Operator.NotEmpty);
    }

    public static String in(String field, Collection<?> values) {
        return single(field, Operator.In, requireJson(field, values));
    }

    public static String notIn(String field, Collection<?> values) {
        return single(field, Operator.NotIn, requireJson(field, values));
    }

    public static String between(String field, Object start, Object end) {
        return single(field, Operator.Between, requireBoth(field, start, end));
    }

    public static String notBetween(String field, Object start, Object end) {
        return single(field, Operator.NotBetween, requireBoth(field, start, end));
    }

    private static String single(String field, Operator operator, Object value) {
        validate(field);
        if (value == null) {
            throw new IllegalArgumentException("value of field '" + field + "' should not be null");
        }
        return String.format("(%s %s %s)", field, operator.abbr, urlEncode(String.valueOf(value)));
    }

    private static String noValue(String field, Operator operator) {
        validate(field);
        return String.format("(%s %s)", field, operator.abbr);
    }

    private static String requireJson(String field, Collection<?> values) {
        if (values == null) {
            throw new IllegalArgumentException("values of field '" + field + "' should not be null");
        }
        return GSON.toJson(values);
    }

    private static String requireBoth(String field, Object start, Object end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("start and end of field '" + field + "' should not be null");
        }
        return start + "," + end;
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static void validate(String field) {
        Validators.validateFieldName(field);
    }
}
