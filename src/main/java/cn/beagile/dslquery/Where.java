package cn.beagile.dslquery;

import java.util.ArrayList;
import java.util.List;

public class Where {
    private String condition;
    private List<Where> wheres = new ArrayList<>();
    private List<Predicate> predicates = new ArrayList<>();

    public List<Where> getWheres() {
        return wheres;
    }

    public void setCondition(String condition) {
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
}
