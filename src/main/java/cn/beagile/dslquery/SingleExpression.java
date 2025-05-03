package cn.beagile.dslquery;


import java.util.List;
import java.util.Objects;
import java.util.Optional;


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
        String[] paramNames = operatorEnum.params(sqlBuilder);
        if (operatorEnum.requireValue) {
            addParams(sqlBuilder, operatorEnum, paramNames);
        }
        return String.format(operatorEnum.whereFormat(), sqlBuilder.aliasOf(field), operatorEnum.operator, paramNames[0], paramNames[1]);
    }

    private void addParams(SQLBuilder sqlBuilder, Operator operatorEnum, String[] paramNames) {
        if (operatorEnum.isArray()) {
            sqlBuilder.addParamArray(paramNames[0], field, operatorEnum.transferValue(this.value));
            return;
        }
        if (operatorEnum == Operator.Between) {
            sqlBuilder.addParam(paramNames[0], field, operatorEnum.transferValue(this.value.split(",")[0]));
            sqlBuilder.addParam(paramNames[1], field, operatorEnum.transferValue(this.value.split(",")[1]));
            return;
        }
        sqlBuilder.addParam(paramNames[0], field, operatorEnum.transferValue(this.value));
    }

    @Override
    public String toDSL() {
        if (Operators.byName(this.operator).requireValue) {
            return String.format("(%s %s %s)", field, operator, value);
        }
        return String.format("(%s %s)", field, operator);
    }

    @Override
    public String toSQL(List<SQLField> fields, RawSQLBuilder sqlWhere) {
        Operator operatorEnum = Operators.byName(this.operator);
        Optional<String> sqlName = fields.stream()
                .filter(field -> field.getName().equals(this.field))
                .map(SQLField::getWhereName)
                .findFirst();
        if (sqlName.isEmpty()) {
            return "true";
        }
        String[] paramNames = operatorEnum.params(sqlWhere);
        if (operatorEnum.requireValue) {
            addParams(sqlWhere, operatorEnum, paramNames);
        }
        return String.format(operatorEnum.whereFormat(), sqlName.get(), operatorEnum.operator, paramNames[0], paramNames[1]);

    }
}
