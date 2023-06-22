package cn.beagile.dslquery;

import java.util.HashMap;
import java.util.Map;

public class SQLParams {
    private int index;
    private Map<String, Object> params;

    public SQLParams() {
        this.index = 1;
        this.params = new HashMap<>();
    }

    public int next() {
        return this.index++;
    }

    public void addParam(String paramName, String value) {
        params.put(paramName, value);
    }

}
