package cn.beagile.dslquery;


import java.util.Objects;


public class Predicate implements ToSQL {
    private String field;
    private String operator;
    private String value;

    public Predicate(String field, String operator, String value) {
        this.field = field;
        this.operator = operator;
        this.value = value;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Predicate predicate = (Predicate) o;
        return Objects.equals(field, predicate.field) && Objects.equals(operator, predicate.operator) && Objects.equals(value, predicate.value);
    }

    public String toSQL(SQLQuery sqlQuery) {
        String paramName = field + sqlQuery.next();
        sqlQuery.addParam(paramName, field, this.value);
        return "(" + sqlQuery.aliasOf(field) + " " + Operators.of(operator).getOperator() + " :" + paramName + ")";
    }
}
