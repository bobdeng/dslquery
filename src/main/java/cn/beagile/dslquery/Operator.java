package cn.beagile.dslquery;

import java.util.function.Function;

enum Operator {

    Equal("equal", "=", (value) -> value),
    NotEqual("notequal", "!=", (value) -> value),
    LessThan("lessthan", "<", (value) -> value),
    LessThanOrEqual("lessthanorequal", "<=", (value) -> value),
    GreaterThan("greaterthan", ">", (value) -> value),
    GreaterThanOrEqual("greaterthanorequal", ">=", (value) -> value),
    StartWith("startswith", "like", (value) -> value + "%"),
    EndsWith("endswith", "like", (value) -> "%" + value),
    Contains("contains", "like", (value) -> "%" + value + "%"),
    IsNull("isnull", "is null", (value) -> null),
    NotNull("notnull", "is not null", (value) -> null);

    private String operator;
    private String keyword;
    private Function<String, String> valueTransfer;

    Operator(String keyword, String operator, Function<String, String> valueTransfer) {
        this.keyword = keyword;
        this.operator = operator;
        this.valueTransfer = valueTransfer;
    }

    public String getOperator() {
        return this.operator;
    }

    public String getKeyword() {
        return keyword;
    }

    public boolean needValue() {
        return !keyword.equals("isnull") && !keyword.equals("notnull");
    }

    public String transferValue(String value) {
        return this.valueTransfer.apply(value);
    }
}
