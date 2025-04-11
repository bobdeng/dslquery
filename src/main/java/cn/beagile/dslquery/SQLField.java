package cn.beagile.dslquery;

public class SQLField {
    private String name;
    private String whereName;
    private Class<?> type;

    public SQLField(String name, String whereName, Class<?> type) {
        this.name = name;
        this.whereName = whereName;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getWhereName() {
        return whereName;
    }

    public Class<?> getType() {
        return type;
    }
}
