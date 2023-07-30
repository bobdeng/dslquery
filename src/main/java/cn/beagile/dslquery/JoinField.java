package cn.beagile.dslquery;


import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class JoinField {
    private Field field;
    private List<Field> parents;

    public JoinField(Field field, List<Field> parents) {
        this.field = field;
        this.parents = parents;
    }

    public String joinStatement() {
        if (field.isAnnotationPresent(JoinColumn.class)) {
            return singleJoinStatement();
        }
        return multiJoinStatement();
    }

    private String multiJoinStatement() {
        List<String> result = new ArrayList<>();
        JoinColumn[] columns = field.getAnnotation(JoinColumns.class).value();
        for (int i = 0; i < columns.length; i++) {
            JoinColumn preColumn = i == 0 ? null : columns[i - 1];
            JoinColumn joinColumn = columns[i];
            if (i == 0) {
                result.add(JoinBuilder.joinBuilder().joinTable(joinColumn.table())
                        .joinField(joinColumn.referencedColumnName())
                        .onTable(getJoinTable())
                        .onField(joinColumn.name()).build());
            }
            if (i == columns.length - 1) {
                result.add(JoinBuilder.joinBuilder().joinTable(field.getType().getAnnotation(View.class).value())
                        .joinTableAlias(getTableAlias())
                        .joinField(joinColumn.referencedColumnName())
                        .onTable(preColumn.table())
                        .onField(joinColumn.name()).build());
            }
        }
        return result.stream().collect(Collectors.joining("\n"));
    }

    private String getTableAlias() {
        return parents.stream().map(Field::getName).collect(Collectors.joining("_"));
    }

    private String singleJoinStatement() {
        JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
        return JoinBuilder.joinBuilder().joinTable(field.getType().getAnnotation(View.class).value())
                .joinTableAlias(getTableAlias())
                .joinField(joinColumn.referencedColumnName())
                .onTable(getJoinTable())
                .onField(joinColumn.name()).build();
    }

    private String getJoinTable() {
        if (parents.size() == 1) {
            return field.getDeclaringClass().getAnnotation(View.class).value();
        }
        return parents.stream().map(Field::getName).limit(parents.size() - 1).collect(Collectors.joining("_"));
    }

    public static class JoinBuilder {
        private String joinTable;
        private String joinTableAlias;
        private String onTable;
        private String onField;
        private String joinField;

        public static JoinBuilder joinBuilder() {
            return new JoinBuilder();
        }

        public JoinBuilder joinTable(String joinTable) {
            this.joinTable = joinTable;
            return this;
        }

        public JoinBuilder joinTableAlias(String joinTableAlias) {
            this.joinTableAlias = joinTableAlias;
            return this;
        }

        public JoinBuilder onTable(String onTable) {
            this.onTable = onTable;
            return this;
        }

        public JoinBuilder onField(String onField) {
            this.onField = onField;
            return this;
        }

        public JoinBuilder joinField(String joinField) {
            this.joinField = joinField;
            return this;
        }

        public String build() {
            if (joinTableAlias == null) {
                return "left join " + joinTable + " on " + joinTable + "." + joinField
                        + " = " + onTable + "." + onField;
            }
            return "left join " + joinTable + " " + joinTableAlias + " on " + joinTableAlias + "." + joinField
                    + " = " + onTable + "." + onField;
        }
    }
}
