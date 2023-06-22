package cn.beagile.dslquery;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WhereBuilder {
    private List<Where> result = new ArrayList<>();
    private Where current;
    private int level = 0;

    public WhereBuilder start() {
        this.level++;
        return this;
    }

    public WhereBuilder end() {
        this.level--;
        if (this.level == 0 && current != null) {
            result.add(current);
        }
        return this;
    }

    public List<Where> build() {
        return result;
    }

    public WhereBuilder word(String word) {
        if (level == 1) {
            parseWhere(word);
        }
        if (level == 2) {
            parsePredicate(word);
        }
        return this;
    }

    private void parseWhere(String word) {
        current = new Where();
        current.setCondition(word);
    }

    private void parsePredicate(String word) {
        Predicate predicate = new Predicate();
        String[] words = word.split("\\s+");
        predicate.setField(words[0]);
        if (words.length > 2) {
            predicate.setValue(words[2]);
        }
        predicate.setOperator(words[1]);
        current.addPredicate(predicate);
    }

    public WhereBuilder parse(String filter) {
        Pattern pattern = Pattern.compile("(\\()|(\\))|([^\\(\\)]+)");
        Matcher matcher = pattern.matcher(filter);
        while (matcher.find()) {
            expression(matcher);
        }
        return this;
    }

    private void expression(Matcher matcher) {
        String word = matcher.group(0);
        if (word.equals("(")) {
            start();
            return;
        }
        if (word.equals(")")) {
            end();
            return;
        }
        this.word(word);
    }
}
