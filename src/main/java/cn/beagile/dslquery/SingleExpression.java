package cn.beagile.dslquery;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


class SingleExpression implements FilterExpression {
    private final String field;
    private final String operator;
    private final String value;
    private String paramName;

    public SingleExpression(String field, String operator, String value, String paramName) {
        this.field = field;
        this.operator = operator;
        this.value = value;
        this.paramName = paramName;
        checkFields();
    }

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
                ", paramName='" + paramName + '\'' +
                '}';
    }

    public String toSQL(SQLBuilder sqlBuilder) {
        Operator operatorEnum = Operators.byName(this.operator);
        String[] paramNames = Arrays.stream(operatorEnum.params(paramName))
                .map(sqlBuilder::paramName)
                .toArray(String[]::new);
        if (operatorEnum.requireValue && sqlBuilder.supportsFieldReferenceValues() && isFieldReferenceValue()) {
            return toFieldReferenceSQL(sqlBuilder, operatorEnum);
        }
        if (operatorEnum.requireValue) {
            addParams(sqlBuilder, operatorEnum, paramNames);
        }
        return String.format(operatorEnum.whereFormat(), sqlBuilder.aliasOf(field), operatorEnum.operator, paramNames[0], paramNames[1]);
    }

    private boolean isFieldReferenceValue() {
        return value != null && value.startsWith("@");
    }

    private String toFieldReferenceSQL(SQLBuilder sqlBuilder, Operator operatorEnum) {
        if (operatorEnum.isArray() || operatorEnum == Operator.Between) {
            throw new RuntimeException("field reference value not supported for operator:" + operatorEnum.keyword);
        }
        return String.format("(%s %s %s)", sqlBuilder.aliasOf(field), operatorEnum.operator, sqlBuilder.aliasOf(value.substring(1)));
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
        Operator operator = Operators.byName(this.operator);
        if (operator.requireValue) {
            try {
                return String.format("(%s %s %s)", field, this.operator, URLEncoder.encode(value, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        return String.format("(%s %s)", field, this.operator);
    }

    @Override
    public String toSQL(List<SQLField> fields, RawSQLBuilder sqlWhere) {
        Operator operatorEnum = Operators.byName(this.operator);
        String[] paramNames = operatorEnum.params(paramName);
        if (operatorEnum.requireValue) {
            addParams(sqlWhere, operatorEnum, paramNames);
        }
        return fields.stream()
                .filter(field -> field.getName().equals(this.field))
                .map(SQLField::getWhereName)
                .findFirst()
                .map(sqlName -> String.format(operatorEnum.whereFormat(), sqlName, operatorEnum.operator, paramNames[0], paramNames[1]))
                .orElse("true");

    }
}
