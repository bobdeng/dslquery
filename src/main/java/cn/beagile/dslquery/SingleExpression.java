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
        if (Operators.byName(this.operator) == null) {
            throw new RuntimeException("invalid operator:" + this.operator);
        }
    }


    @Override
    public boolean equals(Object o) {
        SingleExpression predicate = (SingleExpression) o;
        return Objects.equals(toString(), predicate.toString());
    }

    @Override
    public String toString() {
        return "SingleExpression{" +
                "field='" + field + '\'' +
                ", operator='" + operator + '\'' +
                ", value='" + value + '\'' +
                '}';
    }

    public String toSQL(SQLBuilder sqlBuilder) {
        Operator operatorEnum = Operators.byName(this.operator);
        if (operatorEnum.needValue) {
            String paramName = field + sqlBuilder.nextParamId();
            String value = operatorEnum.transferValue(this.value);
            addParams(sqlBuilder, operatorEnum, paramName, value);
            return String.format(operatorEnum.whereFormat(), sqlBuilder.aliasOf(field), operatorEnum.operator, paramName);
        }
        return String.format("(%s %s)", sqlBuilder.aliasOf(field), operatorEnum.operator);
    }

    private void addParams(SQLBuilder sqlBuilder, Operator operatorEnum, String paramName, String value) {
        if (operatorEnum.isArray()) {
            sqlBuilder.addParamArray(paramName, field, value);
            return;
        }
        sqlBuilder.addParam(paramName, field, value);
    }

    @Override
    public String toDSL() {
        if (Operators.byName(this.operator).needValue) {
            return String.format("(%s %s %s)", field, operator, value);
        }
        return String.format("(%s %s)", field, operator);
    }
}
