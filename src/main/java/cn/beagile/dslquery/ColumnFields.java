package cn.beagile.dslquery;

import javax.persistence.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ColumnFields {
    private final Class clz;
    private List<String> ignores = new ArrayList<>();
    private List<ColumnField> fields;
    private List<JoinField> joinFields = new ArrayList<>();
    private List<One2ManyField> one2ManyFields = new ArrayList<>();

    public ColumnFields(Class clz) {
        this(clz, new ArrayList<>());
    }

    public <T> ColumnFields(Class<T> clz, List<String> otherIgnores) {
        Ignores ignores = clz.getAnnotation(Ignores.class);
        if (ignores != null) {
            this.ignores = Arrays.asList(ignores.value());
        }
        this.ignores = Stream.concat(this.ignores.stream(), otherIgnores.stream()).collect(Collectors.toList());
        this.clz = clz;
        readPrimitiveFields(clz);
        readJoins(clz, new ArrayList<>());
        readEmbeddedFields(clz);
        readOneToManyFields(clz);
    }

    private void readOneToManyFields(Class clz) {
        Arrays.stream(clz.getDeclaredFields()).filter(
                field -> field.isAnnotationPresent(OneToMany.class)
        ).forEach(field -> {
            one2ManyFields.add(new One2ManyField(field));
        });

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
                .filter(field -> !field.isAnnotationPresent(OneToMany.class))
                .forEach(field -> {
                    readJoinFieldsFromField(parents, annotation, field);
                });
    }

    private void readJoinFieldsFromField(List<Field> parents, Class<? extends Annotation> annotation, Field field) {
        List<Field> newParents = newParents(parents, field);
        if (isFieldIgnored(newParents)) return;
        joinFields.add(new JoinField(field, newParents));
        readJoinColumnFields(field, newParents);
        readJoins(field.getType(), newParents);
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
                throw new RuntimeException(e);
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
        List<String> selectIgnores = getSelectIgnores();
        return fields.stream().filter(field -> {
            if (selectIgnores.contains(field.parentNames())) {
                return false;
            }
            return true;
        }).collect(Collectors.toList());
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

    public boolean hasField(Field field) {
        return this.fields.stream().anyMatch(columnField -> columnField.is(field));
    }

    public ColumnField findFieldByName(String field) {
        return this.fields.stream().filter(columnField -> columnField.fieldName().equals(field))
                .findFirst().orElseThrow(() -> new RuntimeException("field not found: " + field));
    }

    public boolean hasJoinField(Field field) {
        return this.joinFields.stream().anyMatch(joinField -> joinField.is(field));
    }

    public String distinct() {
        if (((View) this.clz.getAnnotation(View.class)).distinct()) {
            return " distinct ";
        }
        return " ";
    }

    public boolean isIgnored(Field field) {
        List<String> selectIgnores = getSelectIgnores();
        boolean result = this.joinFields.stream()
                .filter(joinField -> joinField.is(field))
                .anyMatch(joinField -> selectIgnores.contains(joinField.parentNames()));
        return result;

    }

    private List<String> getSelectIgnores() {
        List<String> selectIgnores = Arrays.asList(Optional.ofNullable(((SelectIgnores) clz.getAnnotation(SelectIgnores.class)))
                .map(SelectIgnores::value)
                .orElse(new String[0]));
        return selectIgnores;
    }

    public List<One2ManyField> oneToManyFields() {
        return one2ManyFields;
    }

    public void fetchOneToManyFields(Object master, QueryExecutor queryExecutor) {
        one2ManyFields.forEach(one2ManyField -> {
            one2ManyField.fetch(master, queryExecutor);
        });
    }
}
