package cn.beagile.dslquery;


import java.util.Objects;


class SingleExpression implements FilterExpression {
    private String field;
    private String operator;
    private String value;

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
        Validators.validateField(field);
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

    public String toSQL(SQLQuery sqlQuery) {
        String paramName = field + sqlQuery.next();
        sqlQuery.addParam(paramName, field, this.value);
        return "(" + sqlQuery.aliasOf(field) + " " + Operators.of(operator).getOperator() + " :" + paramName + ")";
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