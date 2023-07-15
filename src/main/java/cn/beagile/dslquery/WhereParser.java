package cn.beagile.dslquery;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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
        Matcher matcher = Pattern.compile("[\\w\\.]+").matcher(subWhere);
        String fieldName = nextMatch(subWhere, matcher);
        String operator = nextMatch(subWhere, matcher);
        String value = subWhere.substring(matcher.end() + 1, Math.max(matcher.end() + 1, subWhere.length() - 1));
        if (value.equals("") && Operators.byName(operator).requireValue) {
            throw new RuntimeException("invalid predicate:" + subWhere);
        }
        return new SingleExpression(fieldName, operator, urlDecode(value));
    }

    private String urlDecode(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private String nextMatch(String subWhere, Matcher matcher) {
        if (matcher.find()) {
            return matcher.group();
        }
        throw new RuntimeException("invalid predicate:" + subWhere);
    }

    private boolean isSubWhere(String subWhere) {
        return subWhere.startsWith("(and") || subWhere.startsWith("(or");
    }

    private boolean isSingleExpression(String subWhere) {
        return !isSubWhere(subWhere);
    }

}
