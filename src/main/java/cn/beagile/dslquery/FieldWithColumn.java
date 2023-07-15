package cn.beagile.dslquery;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import java.lang.reflect.Field;
import java.util.Optional;

public class FieldWithColumn {
    private final Field field;
    private final Column column;

    public FieldWithColumn(Field field, Optional<AttributeOverride> attributeOverride) {
        this.field = field;
        this.column = attributeOverride.map(AttributeOverride::column)
                .orElseGet(() -> field.getAnnotation(Column.class));
    }

    public Field getField() {
        return field;
    }

    public String columnName() {
        return this.column.name();
    }

}
