package cn.beagile.dslquery;

import java.util.function.Function;

enum Operator {

    Equals("equals", "=", (value) -> value),
    NotEquals("notequals", "!=", (value) -> value),
    LessThan("lessthan", "<", (value) -> value),
    LessThanOrEqual("lessthanorequals", "<=", (value) -> value),
    GreaterThan("greaterthan", ">", (value) -> value),
    GreaterThanOrEqual("greaterthanorequals", ">=", (value) -> value),
    StartsWith("startswith", "like", (value) -> value + "%"),
    EndsWith("endswith", "like", (value) -> "%" + value),
    Contains("contains", "like", (value) -> "%" + value + "%"),
    IsNull("isnull", "is null", null, false),
    NotNull("notnull", "is not null", null, false),
    NotIn("notin", "not in", (value) -> value, true),
    Between("between", "between", (value) -> value, true),
    In("in", "in", (value) -> value, true);

    final String operator;
    final String keyword;
    final boolean requireValue;
    private final Function<String, String> valueTransfer;

    Operator(String keyword, String operator, Function<String, String> valueTransfer) {
        this(keyword, operator, valueTransfer, true);
    }

    Operator(String keyword, String operator, Function<String, String> valueTransfer, boolean requireValue) {
        this.keyword = keyword;
        this.operator = operator;
        this.valueTransfer = valueTransfer;
        this.requireValue = requireValue;
    }

    public boolean isArray() {
        return this == In || this == NotIn;
    }

    public String transferValue(String value) {
        return this.valueTransfer.apply(value);
    }

    public String whereFormat() {
        if (!requireValue) {
            return "(%s %s)";
        }
        if (isArray()) {
            return "(%s %s (:%s))";
        }
        if (this == Between) {
            return "(%s %s :%s and :%s)";
        }
        return "(%s %s :%s)";
    }

    public String[] params(String field, SQLBuilder sqlBuilder) {
        if (this == Between) {
            return new String[]{field + sqlBuilder.nextParamId(), field + sqlBuilder.nextParamId()};
        }
        return new String[]{field + sqlBuilder.nextParamId(), ""};
    }
}
