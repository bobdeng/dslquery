package cn.beagile.dslquery;

import javax.persistence.Column;
import javax.persistence.Embedded;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ColumnField<T> {
    private Field field;
    private Class<T> rootClass;
    private List<Field> parents;
    private Column column;
    private boolean joined;


    public ColumnField(Field field, Class<T> rootClass) {

        this(field, rootClass, new ArrayList<>(), field.getAnnotation(Column.class), false);
    }

    public ColumnField(Field field, Class<T> rootClass, List<Field> parents, Column column, boolean joined) {

        this.field = field;
        this.rootClass = rootClass;
        this.parents = parents;
        this.column = column;
        this.joined = joined;
    }

    public String columnName() {
        return this.column.name();
    }

    public String alias() {
        return Stream.concat(parents.stream(), Stream.of(field))
                .map(Field::getName).collect(Collectors.joining("_", "", "_"));
    }

    public String expression() {
        return selectName() + " " + alias();
    }

    public String selectName() {
        return getTableName() + "." + columnName();
    }

    private Object getTableName() {
        if (joined) {
            Field lastParentField = parents.get(parents.size() - 1);
            if (lastParentField.isAnnotationPresent(Embedded.class)) {
                return parents.stream().map(Field::getName).limit(parents.size() - 1).collect(Collectors.joining("_", "", "_"));
            }
            return parents.stream().map(Field::getName).collect(Collectors.joining("_", "", "_"));
        }
        return getRootTable();
    }

    private String getRootTable() {
        return ((View) this.rootClass.getAnnotation(View.class)).value();
    }

    public String fieldName() {
        return Stream.concat(parents.stream(), Stream.of(field))
                .map(Field::getName).collect(Collectors.joining("."));
    }

    public Field getField() {
        return field;
    }

    public String parentNames() {
        return parents.stream().map(Field::getName).collect(Collectors.joining("."));
    }

    public boolean is(Field field, List<Field> parents) {
        return this.field.equals(field) && this.parents.equals(parents);
    }
}
