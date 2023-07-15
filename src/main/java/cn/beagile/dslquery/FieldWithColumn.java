package cn.beagile.dslquery;

import javax.persistence.AttributeOverride;
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
        AttributeOverride[] attributeOverrides = new AttributeOverride[0];
        if (this.attributeOverrides != null) {
            attributeOverrides = this.attributeOverrides.value();
        }
        return Stream.of(attributeOverrides).filter(it -> it.name().equals(field.getName()))
                .map(it -> it.column().name())
                .findFirst()
                .orElseGet(() -> field.getAnnotation(Column.class).name());
    }

    @Override
    public String toString() {
        return "FieldWithColumn{" +
                "field=" + field +
                ", attributeOverrides=" + attributeOverrides +
                '}';
    }
}
