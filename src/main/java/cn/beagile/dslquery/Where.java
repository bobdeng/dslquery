package cn.beagile.dslquery;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Where implements FilterExpression {
    private String condition;
    private List<FilterExpression> expressions;
    private static Set<String> VALID_CONDITIONS = Stream.of("and", "or").collect(Collectors.toSet());

    public Where(String condition, List<FilterExpression> expressions) {
        this.condition = condition;
        if (!VALID_CONDITIONS.contains(condition.toLowerCase())) {
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

    public String toSQL(SQLQuery sqlParams) {
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
