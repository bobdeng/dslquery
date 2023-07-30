package cn.beagile.dslquery;

import javax.persistence.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ColumnFields {
    private final Class clz;
    private List<String> ignores = new ArrayList<>();
    private List<ColumnField> fields;
    private List<JoinField> joinFields = new ArrayList<>();

    public ColumnFields(Class clz) {
        Ignores ignores = (Ignores) clz.getAnnotation(Ignores.class);
        if (ignores != null) {
            this.ignores = Arrays.asList(ignores.value());
        }
        this.clz = clz;
        readPrimitiveFields(clz);
        readJoins(clz, new ArrayList<>());
        readEmbeddedFields(clz);
    }

    private void readPrimitiveFields(Class clz) {
        this.fields = Arrays.stream(clz.getDeclaredFields()).filter(field -> field.isAnnotationPresent(Column.class))
                .map(field1 -> new ColumnField(field1, clz)).collect(Collectors.toList());
    }

    private void readEmbeddedFields(Class clz) {
        Arrays.stream(clz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Embedded.class))
                .forEach(field -> {
                    List<Field> parents = new ArrayList<>();
                    parents.add(field);
                    AttributeOverrides overrides = field.getAnnotation(AttributeOverrides.class);
                    readEmbedded(clz, field, parents, overrides);
                });
    }

    private void readJoins(Class clz, List<Field> parents) {
        readJoinFields(clz, parents, JoinColumn.class);
        readJoinFields(clz, parents, JoinColumns.class);
    }

    private void readJoinFields(Class clz, List<Field> parents, Class<? extends Annotation> annotation) {
        Arrays.stream(clz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(annotation))
                .forEach(field -> {
                    readJoinFieldsFromField(parents, annotation, field);
                });
    }

    private void readJoinFieldsFromField(List<Field> parents, Class<? extends Annotation> annotation, Field field) {
        List<Field> newParents = newParents(parents, field);
        if (isFieldIgnored(newParents)) return;
        joinFields.add(new JoinField(field, newParents));
        readJoinColumnFields(field, newParents);
        readJoinTables(annotation, field, newParents);
    }

    private boolean isFieldIgnored(List<Field> newParents) {
        String fieldName = newParents.stream().map(Field::getName).collect(Collectors.joining("."));
        return ignores.contains(fieldName);
    }

    private void readJoinColumnFields(Field field, List<Field> newParents) {
        Arrays.stream(field.getType().getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Column.class))
                .forEach(it -> {
                    fields.add(new ColumnField(it, field.getType(), newParents, it.getAnnotation(Column.class), true));
                });
    }

    private void readJoinTables(Class<? extends Annotation> annotation, Field field, List<Field> newParents) {
        Arrays.stream(field.getType().getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(annotation))
                .forEach(it -> {
                    readJoins(field.getType(), newParents);
                });
    }

    private static List<Field> newParents(List<Field> parents, Field field) {
        List<Field> newParents = new ArrayList<>();
        newParents.addAll(parents);
        newParents.add(field);
        return newParents;
    }

    private void readEmbedded(Class clz, Field field, List<Field> parents, AttributeOverrides overrides) {
        Arrays.stream(overrides.value()).forEach(attributeOverride -> {
            try {
                String[] names = attributeOverride.name().split("\\.");
                List<Field> newParents = new ArrayList<>();
                newParents.addAll(parents);
                Field embeddedField = getFieldByName(field, names, newParents);
                fields.add(new ColumnField(embeddedField, clz, newParents, attributeOverride.column(), false));
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        });
    }

    private static Field getFieldByName(Field field, String[] names, List<Field> newParents) throws NoSuchFieldException {
        Field result = field;
        for (int i = 0; i < names.length; i++) {
            result = result.getType().getDeclaredField(names[i]);
            if (i < names.length - 1) {
                newParents.add(result);
            }
        }
        return result;
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

    public String joins() {
        return joinFields.stream().map(JoinField::joinStatement).collect(Collectors.joining("\n"));
    }

    public ColumnField findField(Field field) {
        return this.fields.stream().filter(columnField -> columnField.is(field)).findFirst().orElseThrow(() -> new RuntimeException("not found"));
    }
}
