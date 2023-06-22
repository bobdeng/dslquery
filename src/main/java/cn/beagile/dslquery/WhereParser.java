package cn.beagile.dslquery;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WhereParser {
    public Where parse(String whereString) {
        Where where = new Where();
        String condition = whereString.substring(1, whereString.indexOf('(', 1));
        where.setCondition(condition);
        String content = whereString.substring(whereString.indexOf('(', 1), whereString.lastIndexOf(')'));
        List<String> subWhereStrings = new WhereList(content).list();
        subWhereStrings.stream()
                .filter(this::isPredicate)
                .map(this::parsePredicate)
                .forEach(where::addPredicate);
        subWhereStrings.stream()
                .filter(this::isSubWhere)
                .map(this::parse)
                .forEach(where::addWhere);
        return where;
    }

    private Predicate parsePredicate(String subWhere) {
        Pattern wordPattern = Pattern.compile("\\w+");
        Matcher matcher = wordPattern.matcher(subWhere);
        matcher.find();
        String fieldName = matcher.group();
        matcher.find();
        String operator = matcher.group();
        String value = subWhere.substring(subWhere.indexOf(operator) + operator.length(), subWhere.length() - 1).trim();
        return new Predicate(fieldName, operator, value);
    }

    private boolean isSubWhere(String subWhere) {
        return subWhere.startsWith("(and") || subWhere.startsWith("(or");
    }

    private boolean isPredicate(String subWhere) {
        return !isSubWhere(subWhere);
    }

}
