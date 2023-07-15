package cn.beagile.dslquery;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.stream.Stream;

public class FieldWithColumn {
    private final Field field;
    private final Column column;

    public FieldWithColumn(Field field, AttributeOverride ao) {
        this.field = field;
        if (ao == null) {
            this.column = field.getAnnotation(Column.class);
        } else {
            this.column = ao.column();
        }
    }

    public Field getField() {
        return field;
    }

    public FieldWithColumn(Field field, AttributeOverrides attributeOverrides) {
        this.field = field;
        this.column = column(attributeOverrides);
    }

    public String columnName() {
        return this.column.name();
    }

    public Column column(AttributeOverrides parentOverrides) {
        AttributeOverride[] attributeOverrides = Optional.ofNullable(parentOverrides)
                .map(AttributeOverrides::value)
                .orElseGet(() -> new AttributeOverride[0]);
        return Stream.of(attributeOverrides).filter(it -> it.name().equals(field.getName()))
                .map(AttributeOverride::column)
                .findFirst()
                .orElseGet(() -> field.getAnnotation(Column.class));
    }

}
