package cn.beagile.dslquery;

import java.util.ArrayList;
import java.util.List;

public class WhereList {
    private final ArrayList<String> list;
    private int level;
    private String listStr;

    public WhereList(String listStr) {
        this.list = new ArrayList<>();
        this.listStr = listStr;
        split();
    }

    private void split() {
        this.level = 0;
        for (int i = 0; i < this.listStr.length(); i++) {
            if (this.listStr.charAt(i) == '(') {
                this.level++;
            }
            if (this.listStr.charAt(i) == ')') {
                this.level--;
                if (this.level == 0) {
                    this.list.add(this.listStr.substring(0, i + 1));
                    this.listStr = this.listStr.substring(i + 1);
                    split();
                    break;
                }
            }
        }
    }

    public List<String> list() {
        return list;
    }
}
