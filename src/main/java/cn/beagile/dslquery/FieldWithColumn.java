package cn.beagile.dslquery;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;

public class FieldWithColumn {
    private final Field field;
    private final Column column;
    private final String prefix;

    public FieldWithColumn(Field field, Optional<AttributeOverride> attributeOverride, Stack<String> prefix) {
        this.field = field;
        this.column = attributeOverride.map(AttributeOverride::column)
                .orElseGet(() -> field.getAnnotation(Column.class));
        if (prefix.size() > 0) {
            this.prefix = prefix.stream().collect(Collectors.joining("_", "", "_"));
        } else {
            this.prefix = "";
        }
    }

    public Field getField() {
        return field;
    }

    public String columnName() {
        return prefix + this.column.name();
    }

    public String selectName() {
        View view = field.getDeclaringClass().getAnnotation(View.class);
        if (view == null) {
            return this.column.name() + " " + columnName();
        }
        return view.value() + "." + this.column.name() + " " + columnName();
    }

}
