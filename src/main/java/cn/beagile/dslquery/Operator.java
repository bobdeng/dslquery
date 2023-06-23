package cn.beagile.dslquery;

import java.util.function.Function;

enum Operator {

    Equal("equal", "=", (value) -> value),
    NotEqual("notequal", "!=", (value) -> value),
    LessThan("lessthan", "<", (value) -> value),
    LessThanOrEqual("lessthanorequal", "<=", (value) -> value),
    GreaterThan("greaterthan", ">", (value) -> value),
    GreaterThanOrEqual("greaterthanorequal", ">=", (value) -> value),
    StartWith("startswith", " like ", (value) -> value + "%"),
    EndsWith("endswith", " like ", (value) -> "%" + value),
    Contains("contains", " like ", (value) -> "%" + value + "%"),
    IsNull("isnull", " is null", (value) -> null),
    NotNull("notnull", " is not null", (value) -> null);

    private String operator;
    private String keyword;
    private Function<String, Object> value;

    Operator(String keyword, String operator, Function<String, Object> value) {
        this.keyword = keyword;
        this.operator = operator;
        this.value = value;
    }

    public String getOperator() {
        return this.operator;
    }

    public String getKeyword() {
        return keyword;
    }
}
