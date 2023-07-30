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
        View view = field.getType().getAnnotation(View.class);
        String alias = parents.stream()
                .map(Field::getName).collect(Collectors.joining("_"));
        JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
        if (joinColumn != null) {
            String joinTable = getJoinTable();
            return "left join " + view.value() + " " + alias + " on " + alias + "." + joinColumn.referencedColumnName()
                    + " = " + joinTable + "." + joinColumn.name();
        } else {
            JoinColumns joinColumns = field.getAnnotation(JoinColumns.class);
            List<String> result = new ArrayList<>();
            for (int i = 0; i < joinColumns.value().length; i++) {
                joinColumn = joinColumns.value()[i];
                if (i == 0) {
                    String joinTable = getJoinTable();
                    result.add("left join " + joinColumn.table() + " on " + joinColumn.table() + "." + joinColumn.referencedColumnName()
                            + " = " + joinTable + "." + joinColumn.name());
                } else {
                    result.add("left join " + view.value() + " " + alias + " on " + alias + "." + joinColumn.referencedColumnName()
                            + " = " + joinColumns.value()[i - 1].table() + "." + joinColumn.name());
                }
            }
            return result.stream().collect(Collectors.joining("\n"));
        }
    }

    private String getJoinTable() {
        String joinTable = "";
        if (parents.size() == 1) {
            joinTable = field.getDeclaringClass().getAnnotation(View.class).value();
        } else {
            joinTable = parents.stream().map(Field::getName).limit(parents.size() - 1).collect(Collectors.joining("_"));
        }
        return joinTable;
    }
}
