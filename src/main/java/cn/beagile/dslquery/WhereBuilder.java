package cn.beagile.dslquery;

import java.util.ArrayList;
import java.util.Stack;

public class WhereBuilder {
    ComplexExpression currentExpression;
    Stack<ComplexExpression> expressionStack = new Stack<>();

    public WhereBuilder and() {
        return getWhereBuilderByCondition("and");
    }

    public WhereBuilder or() {
        return getWhereBuilderByCondition("or");
    }

    private WhereBuilder getWhereBuilderByCondition(String condition) {
        if (currentExpression != null) {
            ComplexExpression nextExpression = new ComplexExpression(condition, new ArrayList<>());
            currentExpression.add(nextExpression);
            this.currentExpression = nextExpression;
            expressionStack.push(currentExpression);
            return this;
        }
        currentExpression = new ComplexExpression(condition, new ArrayList<>());
        expressionStack.push(currentExpression);
        return this;
    }

    public String build() {
        return expressionStack.firstElement().toDSL();
    }

    public WhereBuilder equal(String name, String value) {
        appendExpression(name, value, "equal");
        return this;
    }

    private void appendExpression(String name, String value, String condition) {
        currentExpression.add(new SingleExpression(name, condition, value));
    }

    public WhereBuilder greaterthan(String name, String value) {
        appendExpression(name, value, Operator.GreaterThan.getKeyword());
        return this;
    }

    public WhereBuilder lessthan(String name, String value) {
        appendExpression(name, value, Operator.LessThan.getKeyword());
        return this;
    }

    public WhereBuilder notequal(String name, String value) {
        appendExpression(name, value, Operator.NotEqual.getKeyword());
        return this;
    }

    public WhereBuilder lessthanorequal(String name, String value) {
        appendExpression(name, value, Operator.LessThanOrEqual.getKeyword());
        return this;
    }

    public WhereBuilder greaterthanorequal(String name, String value) {
        appendExpression(name, value, Operator.GreaterThanOrEqual.getKeyword());
        return this;
    }

    public WhereBuilder startswith(String name, String value) {
        appendExpression(name, value, Operator.StartWith.getKeyword());
        return this;
    }

    public WhereBuilder endswith(String name, String value) {
        appendExpression(name, value, Operator.EndsWith.getKeyword());
        return this;
    }

    public WhereBuilder contains(String name, String value) {
        appendExpression(name, value, Operator.Contains.getKeyword());
        return this;
    }

    public WhereBuilder isnull(String name) {
        appendExpression(name, null, Operator.IsNull.getKeyword());
        return this;
    }

    public WhereBuilder notnull(String name) {
        appendExpression(name, null, Operator.NotNull.getKeyword());
        return this;
    }


}
