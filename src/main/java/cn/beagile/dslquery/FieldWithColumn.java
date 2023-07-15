package cn.beagile.dslquery;

import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.stream.Stream;

public class FieldWithColumn {
    private final Field field;
    private final AttributeOverrides attributeOverrides;

    public Field getField() {
        return field;
    }

    public FieldWithColumn(Field field, AttributeOverrides attributeOverrides) {

        this.field = field;
        this.attributeOverrides = attributeOverrides;
    }

    public String columnName() {
        if (attributeOverrides != null) {
            return Stream.of(attributeOverrides.value()).filter(it -> it.name().equals(field.getName()))
                    .map(it -> it.column().name())
                    .findFirst()
                    .orElseGet(() -> field.getAnnotation(Column.class).name());
        }
        return field.getAnnotation(Column.class).name();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldWithColumn that = (FieldWithColumn) o;
        return Objects.equals(field, that.field);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field);
    }
}
