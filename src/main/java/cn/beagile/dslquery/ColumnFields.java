package cn.beagile.dslquery;

import javax.persistence.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ColumnFields {
    private final Class clz;
    private List<ColumnField> fields;
    private List<JoinField> joinFields = new ArrayList<>();
    private List<One2ManyField> one2ManyFields = new ArrayList<>();
    private Set<String> includes;
    private Set<String> selectIgnores;

    public <T> ColumnFields(Class<T> clz) {
        this(clz, null);
    }

    public <T> ColumnFields(Class<T> clz, DSLQuery<T> dslQuery) {
        this.clz = clz;
        initSelectIgnores(clz, dslQuery);
        initDeepJoins(clz, dslQuery);
        readFields(clz);
    }

    private <T> void readFields(Class<T> clz) {
        readPrimitiveFields(clz);
        readJoins(clz, new ArrayList<>());
        readEmbeddedFields(clz);
        readOneToManyFields(clz);
    }

    private <T> void initSelectIgnores(Class<T> clz, DSLQuery<T> dslQuery) {
        List<String> innerSelectIgnores = Arrays.asList(Optional.ofNullable(clz.getAnnotation(SelectIgnores.class))
                .map(SelectIgnores::value)
                .orElse(new String[0]));
        List<String> outerSelectIgnores = Optional.ofNullable(dslQuery).map(DSLQuery::getSelectIgnores).orElse(Collections.emptyList());
        this.selectIgnores = Stream.concat(innerSelectIgnores.stream(), outerSelectIgnores.stream()).collect(Collectors.toSet());
    }

    private <T> void initDeepJoins(Class<T> clz, DSLQuery<T> dslQuery) {
        String[] deepJoinIncludes = Optional.ofNullable(clz.getAnnotation(DeepJoinIncludes.class))
                .map(DeepJoinIncludes::value)
                .orElse(new String[]{});
        Stream<String> streamDeepJoins = Arrays.stream(deepJoinIncludes);
        Stream<String> streamDeepJoinsOuter = Optional.ofNullable(dslQuery).map(it -> it.getDeepJoins().stream()).orElse(Stream.empty());
        this.includes = Stream.concat(streamDeepJoins, streamDeepJoinsOuter).collect(Collectors.toSet());
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
        readEmbeddedFields(clz, new ArrayList<>());
    }

    private void readEmbeddedFields(Class clz, List<Field> parents) {
        Arrays.stream(clz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Embedded.class))
                .forEach(field -> {
                    List<Field> newParents = new ArrayList<>(parents);
                    newParents.add(field);
                    AttributeOverrides overrides = field.getAnnotation(AttributeOverrides.class);
                    readEmbedded(clz, field, newParents, overrides);
                });
    }

    private void readJoins(Class clz, List<Field> parents) {
        readJoinFields(clz, parents, JoinColumn.class);
        readJoinFields(clz, parents, JoinColumns.class);
    }

    private boolean isJoinInclude(Field field, List<Field> parents) {
        if (parents.isEmpty()) {
            return true;
        }
        String fieldName = Stream.concat(parents.stream(), Stream.of(field)).map(Field::getName).collect(Collectors.joining("."));
        return includes.contains(fieldName);
    }

    private void readJoinFields(Class clz, List<Field> parents, Class<? extends Annotation> annotation) {
        Arrays.stream(clz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(annotation))
                .filter(field -> !field.isAnnotationPresent(OneToMany.class))
                .filter(field -> isJoinInclude(field, parents))
                .forEach(field -> {
                    readJoinFieldsFromField(parents, field);
                });
    }

    private void readJoinFieldsFromField(List<Field> parents, Field field) {
        List<Field> newParents = newParents(parents, field);
        joinFields.add(new JoinField(field, newParents));
        readJoinColumnFields(field, newParents);
        readEmbeddedFields(field.getType(), newParents);
        readJoins(field.getType(), newParents);
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
                fields.add(new ColumnField(embeddedField, clz, newParents, attributeOverride.column(), !clz.equals(this.clz)));
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
        return fields.stream().filter(field -> {
            if (this.selectIgnores.contains(field.parentNames())) {
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

    public boolean hasField(Field field, List<Field> parents) {
        return this.fields.stream().anyMatch(columnField -> columnField.is(field, parents));
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
        return this.joinFields.stream()
                .filter(joinField -> joinField.is(field))
                .anyMatch(joinField -> this.selectIgnores.contains(joinField.parentNames()));

    }

    public List<One2ManyField> oneToManyFields() {
        return one2ManyFields;
    }

    public void fetchOneToManyFields(Object master, QueryExecutor queryExecutor) {
        one2ManyFields.forEach(one2ManyField -> {
            one2ManyField.fetch(master, queryExecutor);
        });
    }

    public ColumnField findField(Field field, List<Field> parents) {
        return this.fields.stream().filter(columnField -> columnField.is(field, parents)).findFirst().orElseThrow(() -> new RuntimeException("not found"));
    }
}
