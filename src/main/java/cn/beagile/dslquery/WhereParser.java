package cn.beagile.dslquery;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class WhereParser {
    public ComplexExpression parse(String whereString) {
        return new ComplexExpression(getCondition(whereString), getFilterExpressions(whereString));
    }

    private List<FilterExpression> getFilterExpressions(String whereString) {
        String content = whereString.substring(whereString.indexOf('(', 1), whereString.lastIndexOf(')'));
        return new WhereList(content).stream().map(this::parseSQL).collect(Collectors.toList());
    }

    private String getCondition(String whereString) {
        String condition = whereString.substring(1, whereString.indexOf('(', 1));
        if (condition.equals("")) {
            return "and";
        }
        return condition;
    }

    private FilterExpression parseSQL(String sql) {
        if (this.isSingleExpression(sql)) {
            return parsePredicate(sql);
        }
        return parse(sql);
    }

    private SingleExpression parsePredicate(String subWhere) {
        Pattern wordPattern = Pattern.compile("\\w+");
        Matcher matcher = wordPattern.matcher(subWhere);
        if (!matcher.find()) {
            throw new RuntimeException("invalid predicate:" + subWhere);
        }
        String fieldName = matcher.group();
        if (!matcher.find()) {
            throw new RuntimeException("invalid predicate:" + subWhere);
        }
        String operator = matcher.group();
        String value = subWhere.substring(subWhere.indexOf(operator) + operator.length(), subWhere.length() - 1).trim();
        if (value.equals("") && Operators.of(operator).needValue()) {
            throw new RuntimeException("invalid predicate:" + subWhere);
        }
        return new SingleExpression(fieldName, operator, value);
    }

    private boolean isSubWhere(String subWhere) {
        return subWhere.startsWith("(and") || subWhere.startsWith("(or");
    }

    private boolean isSingleExpression(String subWhere) {
        return !isSubWhere(subWhere);
    }

}
