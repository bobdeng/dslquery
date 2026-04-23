package cn.beagile.dslquery;

import java.util.function.Function;

public class SQLField {
    private String name;
    private String whereName;
    private Class<?> type;
    private Function<String,?> parser;

    public SQLField(ViewName viewName, SQLName sqlName, Class<?> type) {
        this.name = viewName.name;
        this.whereName = sqlName.name;
        this.type = type;
    }

    public SQLField(ViewName viewName, SQLName sqlName, Class<?> type, Function<String, ?> parser) {
        this.name = viewName.name;
        this.whereName = sqlName.name;
        this.type = type;
        this.parser = parser;
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

    public Function<String, ?> getParser() {
        return parser;
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
