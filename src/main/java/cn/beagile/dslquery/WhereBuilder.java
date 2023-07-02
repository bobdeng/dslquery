package cn.beagile.dslquery;

import com.google.gson.Gson;

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
        appendExpression(name, value, Operator.Equal.keyword);
        return this;
    }

    private void appendExpression(String name, String value, String condition) {
        currentExpression.add(new SingleExpression(name, condition, value));
    }

    public WhereBuilder greaterthan(String name, String value) {
        appendExpression(name, value, Operator.GreaterThan.keyword);
        return this;
    }

    public WhereBuilder lessthan(String name, String value) {
        appendExpression(name, value, Operator.LessThan.keyword);
        return this;
    }

    public WhereBuilder notequal(String name, String value) {
        appendExpression(name, value, Operator.NotEqual.keyword);
        return this;
    }

    public WhereBuilder lessthanorequal(String name, String value) {
        appendExpression(name, value, Operator.LessThanOrEqual.keyword);
        return this;
    }

    public WhereBuilder greaterthanorequal(String name, String value) {
        appendExpression(name, value, Operator.GreaterThanOrEqual.keyword);
        return this;
    }

    public WhereBuilder startswith(String name, String value) {
        appendExpression(name, value, Operator.StartWith.keyword);
        return this;
    }

    public WhereBuilder endswith(String name, String value) {
        appendExpression(name, value, Operator.EndsWith.keyword);
        return this;
    }

    public WhereBuilder contains(String name, String value) {
        appendExpression(name, value, Operator.Contains.keyword);
        return this;
    }

    public WhereBuilder isnull(String name) {
        appendExpression(name, null, Operator.IsNull.keyword);
        return this;
    }

    public WhereBuilder notnull(String name) {
        appendExpression(name, null, Operator.NotNull.keyword);
        return this;
    }


    public WhereBuilder prev() {
        expressionStack.pop();
        currentExpression = expressionStack.lastElement();
        return this;
    }

    public WhereBuilder in(String name, Object[] values) {
        appendExpression(name, new Gson().toJson(values), Operator.In.keyword);
        return this;
    }

    public WhereBuilder notin(String name, Object[] values) {
        appendExpression(name, new Gson().toJson(values), Operator.NotIn.keyword);
        return this;
    }
}
