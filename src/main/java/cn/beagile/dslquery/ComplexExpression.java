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

    public String toSQL(SQLBuild sqlBuilder) {
        return expressions.stream()
                .map(predicate -> predicate.toSQL(sqlBuilder))
                .collect(Collectors.joining(" " + this.condition + " ", "(", ")"));
    }

    public void add(FilterExpression expression) {
        this.expressions.add(expression);
    }

    public String toDSL() {
        return expressions.stream().map(FilterExpression::toDSL).collect(Collectors.joining("", "(" + this.condition, ")"));
    }

    @Override
    public String toSQL(List<SQLField> fields, SQLWhere sqlWhere) {
          return expressions.stream()
                .map(predicate -> predicate.toSQL(fields, sqlWhere))
                .collect(Collectors.joining(" " + this.condition + " ", "(", ")"));
    }
}
