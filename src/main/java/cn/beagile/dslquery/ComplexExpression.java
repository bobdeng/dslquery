package cn.beagile.dslquery;

import java.util.List;
import java.util.stream.Collectors;

class ComplexExpression implements FilterExpression {
    private String condition;
    private List<FilterExpression> expressions;

    public ComplexExpression(String condition, List<FilterExpression> expressions) {
        this.condition = condition;
        if (!"and".equals(condition) && !"or".equals(condition)) {
            throw new RuntimeException("invalid condition:" + condition);
        }
        this.expressions = expressions;
    }

    public List<FilterExpression> getExpressions() {
        return expressions;
    }

    public String getCondition() {
        return condition;
    }

    public String toSQL(SQLBuilder sqlParams) {
        return expressions.stream()
                .map(predicate -> predicate.toSQL(sqlParams))
                .collect(Collectors.joining(" " + this.condition + " ", "(", ")"));
    }

    @Override
    public String toString() {
        return "Where{" +
                "condition='" + condition + '\'' +
                ", toSQLs=" + expressions +
                '}';
    }
}
