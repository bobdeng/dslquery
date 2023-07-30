package cn.beagile.dslquery;

import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ColumnFields {
    private final Class clz;
    private List<ColumnField> fields;

    public ColumnFields(Class clz) {
        this.clz = clz;
        this.fields = Arrays.stream(clz.getDeclaredFields()).filter(field -> field.isAnnotationPresent(Column.class))
                .map(field1 -> new ColumnField(field1, clz)).collect(Collectors.toList());
        Arrays.stream(clz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Embedded.class))
                .forEach(field -> {
                    List<Field> parents = new ArrayList<>();
                    parents.add(field);
                    AttributeOverrides overrides = field.getAnnotation(AttributeOverrides.class);
                    readEmbedded(clz, field, parents, overrides);
                });
    }

    private void readEmbedded(Class clz, Field field, List<Field> parents, AttributeOverrides overrides) {
        Arrays.stream(overrides.value()).forEach(attributeOverride -> {
            try {
                String name=attributeOverride.name();
                String[] names = attributeOverride.name().split("\\.");
                Field result = field;
                for (int i = 0; i < names.length; i++) {
                    result = result.getType().getDeclaredField(names[i]);
                }
                Field embeddedField = result;
                fields.add(new ColumnField(embeddedField, clz, parents,attributeOverride.column()));
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        });
    }

    public List<ColumnField> selectFields() {
        return fields;
    }

    public String from() {
        return ((View) clz.getAnnotation(View.class)).value();
    }

    public List<String> joined() {
        return new ArrayList<>();
    }
}
