package cn.beagile.dslquery;

import java.util.function.Function;

enum Operator {

    Equals("equals", "=", (value) -> value, "eq"),
    NotEquals("notequals", "!=", (value) -> value, "ne"),
    LessThan("lessthan", "<", (value) -> value, "lt"),
    LessThanOrEqual("lessthanorequals", "<=", (value) -> value, "le"),
    GreaterThan("greaterthan", ">", (value) -> value, "gt"),
    GreaterThanOrEqual("greaterthanorequals", ">=", (value) -> value, "ge"),
    StartsWith("startswith", "like", (value) -> value + "%", "sw"),
    EndsWith("endswith", "like", (value) -> "%" + value, "ew"),
    Contains("contains", "like", (value) -> "%" + value + "%", "ct"),
    IsNull("isnull", "is null", null, "isn", false),
    NotNull("notnull", "is not null", null, "inn", false),
    NotIn("notin", "not in", (value) -> value, "ni", true),
    Between("between", "between", (value) -> value, "bt", true),
    In("in", "in", (value) -> value, "in", true);

    final String operator;
    final String keyword;
    final boolean requireValue;
    final String abbr;
    private final Function<String, String> valueTransfer;

    Operator(String keyword, String operator, Function<String, String> valueTransfer, String abbr) {
        this(keyword, operator, valueTransfer, abbr, true);
    }

    Operator(String keyword, String operator, Function<String, String> valueTransfer, String abbr, boolean requireValue) {
        this.keyword = keyword;
        this.operator = operator;
        this.valueTransfer = valueTransfer;
        this.requireValue = requireValue;
        this.abbr = abbr;
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

    public String[] params(SQLBuilder sqlBuilder) {
        if (this == Between) {
            return new String[]{"p" + sqlBuilder.nextParamId(), "p" + sqlBuilder.nextParamId()};
        }
        return new String[]{"p" + sqlBuilder.nextParamId(), ""};
    }
}
