package cn.beagile.dslquery;

import java.util.function.Function;

enum Operator {

    Equal("equal", "=", (value) -> value),
    NotEqual("notequal", "!=", (value) -> value),
    LessThan("lessthan", "<", (value) -> value),
    LessThanOrEqual("lessthanorequal", "<=", (value) -> value),
    GreaterThan("greaterthan", ">", (value) -> value),
    GreaterThanOrEqual("greaterthanorequal", ">=", (value) -> value),
    StartsWith("startswith", "like", (value) -> value + "%"),
    EndsWith("endswith", "like", (value) -> "%" + value),
    Contains("contains", "like", (value) -> "%" + value + "%"),
    IsNull("isnull", "is null", null, false),
    NotNull("notnull", "is not null", null, false),
    NotIn("notin", "notin", (value) -> value, true),
    In("in", "in", (value) -> value, true);

    final String operator;
    final String keyword;
    final boolean needValue;
    private final Function<String, String> valueTransfer;

    Operator(String keyword, String operator, Function<String, String> valueTransfer) {
        this(keyword, operator, valueTransfer, true);
    }

    Operator(String keyword, String operator, Function<String, String> valueTransfer, boolean needValue) {
        this.keyword = keyword;
        this.operator = operator;
        this.valueTransfer = valueTransfer;
        this.needValue = needValue;
    }

    public boolean isArray() {
        return this == In || this == NotIn;
    }

    public String transferValue(String value) {
        return this.valueTransfer.apply(value);
    }

    public String whereFormat() {
        if(!needValue){
            return "(%s %s)";
        }
        if (isArray()) {
            return "(%s %s (:%s))";
        }
        return "(%s %s :%s)";
    }
}
