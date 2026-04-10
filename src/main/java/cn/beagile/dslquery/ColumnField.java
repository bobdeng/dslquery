package cn.beagile.dslquery;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ColumnField {
    private final Field field;
    private final Class rootClass;
    private final List<Field> parents;
    private final AnnotationReader.ColumnInfo column;
    private final boolean joined;


    public ColumnField(Field field, Class rootClass) {
        this(field, rootClass, new ArrayList<>(), AnnotationReader.getColumn(field), false);
    }

    public ColumnField(Field field, Class rootClass, List<Field> parents, AnnotationReader.ColumnInfo column, boolean joined) {
        this.field = field;
        this.rootClass = rootClass;
        this.parents = parents;
        this.column = column;
        this.joined = joined;
    }

    public String columnName() {
        return this.column.name;
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
            if (AnnotationReader.hasEmbedded(lastParentField)) {
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
