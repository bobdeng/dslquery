package cn.beagile.dslquery;


import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.beagile.dslquery.Operators.of;


public class Predicate {
    private String field;
    private String operator;
    private String value;

    public Predicate() {
    }

    public Predicate(String field, String operator, String value) {
        this.field = field;
        this.operator = operator;
        this.value = value;
    }

    public void setField(String field) {
        this.field = field;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String buildQuery(Map<String, Object> args, AtomicInteger index, Map<String, String> fieldsMap) {
        String valueParam = field + index.getAndIncrement();
        getValue(args, valueParam);
        String fieldName = fieldsMap.get(field);
        if (fieldName == null) {
            fieldName = fieldsMap.get(CamelToSnake.camelToSnake(field));
        }
        if (of(this.operator).value(this.value) == null) {
            return fieldName + getCondition();
        }
        return fieldName + getCondition() + ":" + valueParam;
    }

    private void getValue(Map<String, Object> args, String valueParam) {
        if (of(this.operator).value(this.value) == null) {
            return;
        }
        args.put(valueParam, of(this.operator).value(this.value));
    }

    private String getCondition() {
        return of(this.operator).getOperator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Predicate predicate = (Predicate) o;
        return Objects.equals(field, predicate.field) && Objects.equals(operator, predicate.operator) && Objects.equals(value, predicate.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, operator, value);
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
