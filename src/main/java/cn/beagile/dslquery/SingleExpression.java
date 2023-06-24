package cn.beagile.dslquery;


import java.util.Objects;


class SingleExpression implements FilterExpression {
    private final String field;
    private final String operator;
    private final String value;

    public SingleExpression(String field, String operator, String value) {
        this.field = field;
        this.operator = operator;
        this.value = value;
        checkFields();
    }

    private void checkFields() {
        checkFieldName();
        checkOperator();
    }

    private void checkFieldName() {
        Validators.validateFieldName(field);
    }

    private void checkOperator() {
        if (Operators.of(this.operator) == null) {
            throw new RuntimeException("invalid operator:" + this.operator);
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SingleExpression predicate = (SingleExpression) o;
        return Objects.equals(field, predicate.field) && Objects.equals(operator, predicate.operator) && Objects.equals(value, predicate.value);
    }

    public String toSQL(SQLBuilder sqlBuilder) {
        Operator operatorEnum = Operators.of(this.operator);
        if (operatorEnum.needValue()) {
            String paramName = field + sqlBuilder.nextParamId();
            sqlBuilder.addParam(paramName, field, operatorEnum.transferValue(value));
            return String.format("(%s %s :%s)", sqlBuilder.aliasOf(field), operatorEnum.getOperator(), paramName);
        }
        return String.format("(%s %s)", sqlBuilder.aliasOf(field), operatorEnum.getOperator());
    }

    @Override
    public String toString() {
        return "Predicate{" +
                "field='" + field + '\'' +
                ", operator='" + operator + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
