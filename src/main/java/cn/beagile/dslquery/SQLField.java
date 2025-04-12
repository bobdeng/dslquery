package cn.beagile.dslquery;

public class SQLField {
    private String name;
    private String whereName;
    private Class<?> type;

    public SQLField(ViewName viewName, SQLName sqlName, Class<?> type) {
        this.name = viewName.name;
        this.whereName = sqlName.name;
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

    public static class ViewName {
        private String name;

        public ViewName(String name) {
            this.name = name;
        }
    }

    public static class SQLName {
        private String name;

        public SQLName(String name) {
            this.name = name;
        }
    }
}
