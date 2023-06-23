package cn.beagile.dslquery;

import java.util.ArrayList;
import java.util.List;

public class WhereList {
    private final ArrayList<String> list;
    private int level = 0;
    private String listStr;

    public WhereList(String listStr) {
        this.list = new ArrayList<>();
        this.listStr = listStr;
        split();
    }

    private void split() {
        for (int i = 0; i < this.listStr.length(); i++) {
            scanCharAt(i);
        }
    }

    private void scanCharAt(int charIndex) {
        char c = this.listStr.charAt(charIndex);
        if (c == '(') {
            this.level++;
            return;
        }
        if (c == ')') {
            this.level--;
            splitAtCurrentWhenEnd(charIndex);
        }
    }

    private void splitAtCurrentWhenEnd(int charIndex) {
        if (this.level == 0) {
            this.list.add(this.listStr.substring(0, charIndex + 1));
            this.listStr = this.listStr.substring(charIndex + 1);
            split();
        }
    }

    public List<String> list() {
        return list;
    }
}
