package cn.beagile.dslquery;


import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JoinField {
    private Field field;
    private List<Field> parents;
    private List<String> joinOns;
    private int joinIndex;

    public JoinField(Field field, List<Field> parents, List<String> joinOns, int joinIndex) {
        this.field = field;
        this.parents = parents;
        this.joinOns = joinOns;
        this.joinIndex = joinIndex;
    }

    public String joinStatement() {
        return joinStatement(new java.util.HashMap<>(), 0);
    }

    public String joinStatement(Map<String, Object> params, int timezoneOffset) {
        AnnotationReader.JoinColumnInfo[] joinColumns = AnnotationReader.getJoinColumns(field);
        if (joinColumns.length == 1) {
            return singleJoinStatement(params, timezoneOffset, joinColumns[0]);
        }
        return multiJoinStatement(params, timezoneOffset, joinColumns);
    }

    private String multiJoinStatement(Map<String, Object> params, int timezoneOffset, AnnotationReader.JoinColumnInfo[] columns) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < columns.length; i++) {
            AnnotationReader.JoinColumnInfo preColumn = i == 0 ? null : columns[i - 1];
            AnnotationReader.JoinColumnInfo joinColumn = columns[i];
            if (i == 0) {
                result.add(JoinBuilder.joinBuilder().joinTable(getTableFromJoinColumn(joinColumn))
                        .joinField(joinColumn.referencedColumnName)
                        .onTable(getJoinTable())
                        .onField(joinColumn.name).build());
            }
            if (i == columns.length - 1) {
                result.add(JoinBuilder.joinBuilder().joinTable(field.getType().getAnnotation(View.class).value())
                        .joinTableAlias(getTableAlias())
                        .joinField(joinColumn.referencedColumnName)
                        .onTable(getTableFromJoinColumn(preColumn))
                        .onField(joinColumn.name)
                        .joinOnClause(joinOnClause(params, timezoneOffset))
                        .build());
            }
        }
        return result.stream().collect(Collectors.joining("\n"));
    }

    private String getTableFromJoinColumn(AnnotationReader.JoinColumnInfo joinColumn) {
        // Use table attribute if specified
        if (joinColumn.table != null && !joinColumn.table.isEmpty()) {
            return joinColumn.table;
        }
        // Extract table name from column name if it contains a dot
        if (joinColumn.name.contains(".")) {
            return joinColumn.name.substring(0, joinColumn.name.lastIndexOf("."));
        }
        return getJoinTable();
    }

    private String getTableAlias() {
        return parents.stream().map(Field::getName).collect(Collectors.joining("_", "", "_"));
    }

    private String singleJoinStatement(Map<String, Object> params, int timezoneOffset, AnnotationReader.JoinColumnInfo joinColumn) {
        return JoinBuilder.joinBuilder()
                .joinTable(field.getType().getAnnotation(View.class).value())
                .joinTableAlias(getTableAlias())
                .joinField(joinColumn.referencedColumnName)
                .onTable(getJoinTable())
                .onField(joinColumn.name)
                .joinOnClause(joinOnClause(params, timezoneOffset))
                .build();
    }

    private String joinOnClause(Map<String, Object> params, int timezoneOffset) {
        if (joinOns.isEmpty()) {
            return "";
        }
        return " and " + new JoinOnSQLBuilder(field, parents, "j" + joinIndex + "_", params, timezoneOffset).build(joinOns);
    }

    private String getJoinTable() {
        if (parents.size() == 1) {
            return field.getDeclaringClass().getAnnotation(View.class).value();
        }
        return parents.stream().map(Field::getName).limit(parents.size() - 1).collect(Collectors.joining("_", "", "_"));
    }

    public boolean is(Field field) {
        return this.field.equals(field);
    }

    public String parentNames() {
        return parents.stream().map(Field::getName).collect(Collectors.joining("."));
    }

    public static class JoinBuilder {
        private String joinTable;
        private String joinTableAlias;
        private String onTable;
        private String onField;
        private String joinField;
        private String joinOnClause = "";

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

        public JoinBuilder joinOnClause(String joinOnClause) {
            this.joinOnClause = joinOnClause;
            return this;
        }

        public String build() {
            if (joinTableAlias == null) {
                return "left join " + joinTable + " on " + joinTable + "." + joinField
                        + " = " + onTable + "." + onField + joinOnClause;
            }
            return "left join " + joinTable + " " + joinTableAlias + " on " + joinTableAlias + "." + joinField
                    + " = " + onTable + "." + onField + joinOnClause;
        }
    }
}
