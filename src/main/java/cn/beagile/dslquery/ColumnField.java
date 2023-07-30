package cn.beagile.dslquery;

import javax.persistence.Column;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ColumnField {
    private final Field field;
    private final Class rootClass;
    private final List<Field> parents;
    private final Column column;


    public ColumnField(Field field, Class rootClass) {

        this(field, rootClass, new ArrayList<>(), field.getAnnotation(Column.class));
    }

    public ColumnField(Field field, Class rootClass, List<Field> parents) {
        this(field, rootClass, parents, field.getAnnotation(Column.class));
    }

    public ColumnField(Field field, Class rootClass, List<Field> parents, Column column) {

        this.field = field;
        this.rootClass = rootClass;
        this.parents = parents;
        this.column = column;
    }

    public String columnName() {
        return this.column.name();
    }

    public String alias() {
        return Stream.concat(parents.stream(), Stream.of(field))
                .map(Field::getName).collect(Collectors.joining("_"));
    }

    public String expression() {
        return getTableName() + "." + columnName() + " " + alias();
    }

    private Object getTableName() {
        return ((View) this.rootClass.getAnnotation(View.class)).value();
    }

    public String fieldName() {
        return Stream.concat(parents.stream(), Stream.of(field))
                .map(Field::getName).collect(Collectors.joining("."));
    }
}
