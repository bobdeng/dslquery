package cn.beagile.dslquery;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Where implements ToSQL {
    private String condition;
    private List<Where> wheres = new ArrayList<>();
    private List<Predicate> predicates = new ArrayList<>();
    private static Set<String> VALID_CONDITIONS = Stream.of("and", "or").collect(Collectors.toSet());

    public List<Where> getWheres() {
        return wheres;
    }

    public void setCondition(String condition) {
        if (!VALID_CONDITIONS.contains(condition.toLowerCase())) {
            throw new RuntimeException("invalid condition:" + condition);
        }
        this.condition = condition;
    }


    public String getCondition() {
        return condition;
    }

    public void addPredicate(Predicate predicate) {
        predicates.add(predicate);
    }

    public List<Predicate> getPredicates() {
        return predicates;
    }

    public void addWhere(Where where) {
        this.wheres.add(where);
    }

    public String toSQL(SQLQuery sqlParams) {
        return Stream.concat(this.wheres.stream(), this.predicates.stream())
                .map(predicate -> predicate.toSQL(sqlParams))
                .collect(Collectors.joining(" " + this.condition + " ", "(", ")"));
    }

    @Override
    public String toString() {
        return "Where{" +
                "condition='" + condition + '\'' +
                ", wheres=" + wheres +
                ", predicates=" + predicates +
                '}';
    }
}
