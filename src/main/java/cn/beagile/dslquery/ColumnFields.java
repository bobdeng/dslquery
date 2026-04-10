package cn.beagile.dslquery;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ColumnFields {
    private final Class clz;
    private final DSLQuery dslQuery;
    private List<ColumnField> fields;
    private List<JoinField> joinFields = new ArrayList<>();
    private List<DynamicJoinField> dynamicJoinFields = new ArrayList<>();
    private List<One2ManyField> one2ManyFields = new ArrayList<>();
    private Set<String> includes;
    private Set<String> selectIgnores;

    public <T> ColumnFields(DSLQuery dslQuery) {
        this.dslQuery = dslQuery;
        this.clz = dslQuery.getQueryResultClass();
        initSelectIgnores(this.clz);
        initDeepJoins(this.clz);
        readFields(this.clz);
    }

    private <T> void readFields(Class<T> clz) {
        readPrimitiveFields(clz);
        readJoins(clz, new ArrayList<>());
        readDynamicJoins(clz);
        readEmbeddedFields(clz);
        readOneToManyFields(clz);
    }

    private <T> void initSelectIgnores(Class<T> clz) {
        List<String> innerSelectIgnores = Arrays.asList(Optional.ofNullable(clz.getAnnotation(SelectIgnores.class))
                .map(SelectIgnores::value)
                .orElse(new String[0]));
        List<String> outerSelectIgnores = Optional.ofNullable(this.dslQuery).map(DSLQuery::getSelectIgnores).orElse(Collections.emptyList());
        this.selectIgnores = Stream.concat(innerSelectIgnores.stream(), outerSelectIgnores.stream()).collect(Collectors.toSet());
    }

    private <T> void initDeepJoins(Class<T> clz) {
        String[] deepJoinIncludes = Optional.ofNullable(clz.getAnnotation(DeepJoinIncludes.class))
                .map(DeepJoinIncludes::value)
                .orElse(new String[]{});
        Stream<String> streamDeepJoins = Arrays.stream(deepJoinIncludes);
        Stream<String> streamDeepJoinsOuter = Optional.ofNullable(this.dslQuery).map(it -> it.getDeepJoins().stream()).orElse(Stream.empty());
        this.includes = Stream.concat(streamDeepJoins, streamDeepJoinsOuter).collect(Collectors.toSet());
    }

    private void readOneToManyFields(Class clz) {
        Arrays.stream(clz.getDeclaredFields()).filter(
                AnnotationReader::hasOneToMany
        ).forEach(field -> {
            one2ManyFields.add(new One2ManyField(field));
        });

    }

    private void readPrimitiveFields(Class clz) {
        this.fields = Arrays.stream(clz.getDeclaredFields())
                .filter(AnnotationReader::hasColumn)
                .map(field1 -> new ColumnField(field1, clz)).collect(Collectors.toList());
    }

    private void readEmbeddedFields(Class clz) {
        readEmbeddedFields(clz, new ArrayList<>());
    }

    private void readEmbeddedFields(Class clz, List<Field> parents) {
        Arrays.stream(clz.getDeclaredFields())
                .filter(AnnotationReader::hasEmbedded)
                .filter(field -> isEmbeddedInclude(parents))
                .forEach(field -> {
                    List<Field> newParents = new ArrayList<>(parents);
                    newParents.add(field);
                    // Check if has AttributeOverrides annotation
                    if (AnnotationReader.hasAttributeOverrides(field)) {
                        // Has AttributeOverrides, use it (even if empty)
                        AnnotationReader.AttributeOverrideInfo[] overrides = AnnotationReader.getAttributeOverrides(field);
                        readEmbedded(clz, field, newParents, overrides);
                    } else {
                        // No AttributeOverrides, read all fields
                        readEmbedded(clz, field, newParents);
                    }
                });
    }

    private void readJoins(Class clz, List<Field> parents) {
        readJoinFields(clz, parents);
    }

    private void readDynamicJoins(Class clz) {
        Map<String, DynamicJoinConfig> dynamicJoins = dslQuery.getDynamicJoins();
        if (dynamicJoins == null || dynamicJoins.isEmpty()) {
            return;
        }

        Arrays.stream(clz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(DynamicJoin.class))
                .filter(field -> dynamicJoins.containsKey(field.getName()))
                .forEach(field -> {
                    DynamicJoinConfig config = dynamicJoins.get(field.getName());
                    DynamicJoinField dynamicJoinField = new DynamicJoinField(field, config);
                    dynamicJoinFields.add(dynamicJoinField);
                    fields.addAll(dynamicJoinField.getSubFields());
                });
    }

    private boolean isJoinInclude(Field field, List<Field> parents) {
        if (parents.isEmpty()) {
            return true;
        }
        String fieldName = Stream.concat(parents.stream(), Stream.of(field)).map(Field::getName).collect(Collectors.joining("."));
        return isContains(fieldName);
    }

    private boolean isContains(String fieldName) {
        if (includes.contains(fieldName)) {
            return true;
        }
        return includes.stream().anyMatch(includes -> includes.startsWith(fieldName));
    }

    private boolean isEmbeddedInclude(List<Field> parents) {
        if (parents.size() <= 1) {
            return true;
        }
        String fieldName = parents.stream().map(Field::getName).collect(Collectors.joining("."));
        return isContains(fieldName) && !this.selectIgnores.contains(fieldName);
    }

    private void readJoinFields(Class clz, List<Field> parents) {
        Arrays.stream(clz.getDeclaredFields())
                .filter(AnnotationReader::hasJoinColumn)
                .filter(field -> !AnnotationReader.hasOneToMany(field))
                .filter(field -> isJoinInclude(field, parents))
                .forEach(field -> {
                    readJoinFieldsFromField(parents, field);
                });
    }

    private void readJoinFieldsFromField(List<Field> parents, Field field) {
        List<Field> newParents = newParents(parents, field);
        joinFields.add(new JoinField(field, newParents, joinOnConditions(field, newParents), joinFields.size()));
        readJoinColumnFields(field, newParents);
        readEmbeddedFields(field.getType(), newParents);
        readJoins(field.getType(), newParents);
    }

    private List<String> joinOnConditions(Field field, List<Field> parents) {
        List<String> result = new ArrayList<>();
        if (field.isAnnotationPresent(JoinOn.class)) {
            result.addAll(Arrays.asList(field.getAnnotation(JoinOn.class).value()));
        }
        @SuppressWarnings("unchecked")
        List<String> outerJoinOns = (List<String>) dslQuery.getJoinOns().get(pathOf(parents));
        if (outerJoinOns != null) {
            result.addAll(outerJoinOns);
        }
        return result;
    }

    private String pathOf(List<Field> parents) {
        return parents.stream().map(Field::getName).collect(Collectors.joining("."));
    }

    private void readJoinColumnFields(Field field, List<Field> newParents) {
        Arrays.stream(field.getType().getDeclaredFields())
                .filter(AnnotationReader::hasColumn)
                .forEach(it -> {
                    fields.add(new ColumnField(it, field.getType(), newParents, AnnotationReader.getColumn(it), true));
                });
    }

    private static List<Field> newParents(List<Field> parents, Field field) {
        List<Field> newParents = new ArrayList<>();
        newParents.addAll(parents);
        newParents.add(field);
        return newParents;
    }

    private void readEmbedded(Class clz, Field field, List<Field> parents) {
        Arrays.stream(field.getType().getDeclaredFields())
                .filter(AnnotationReader::hasColumn)
                .forEach(embeddedField -> {
                    List<Field> newParents = new ArrayList<>(parents);
                    AnnotationReader.ColumnInfo columnInfo = AnnotationReader.getColumn(embeddedField);
                    fields.add(new ColumnField(embeddedField, clz, newParents, columnInfo, !clz.equals(this.clz)));
                });
    }

    private void readEmbedded(Class clz, Field field, List<Field> parents, AnnotationReader.AttributeOverrideInfo[] overrides) {
        Arrays.stream(overrides).forEach(attributeOverride -> {
            try {
                String[] names = attributeOverride.name.split("\\.");
                List<Field> newParents = new ArrayList<>(parents);
                Field embeddedField = getFieldByName(field, names, newParents);
                fields.add(new ColumnField(embeddedField, clz, newParents, attributeOverride.column, !clz.equals(this.clz)));
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
            if (this.selectIgnores.stream().anyMatch(ignore -> field.parentNames().startsWith(ignore))) {
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
        return joins(new HashMap<>(), 0);
    }

    public String joins(Map<String, Object> params, int timezoneOffset) {
        String regularJoins = joinFields.stream()
                .map(joinField -> joinField.joinStatement(params, timezoneOffset))
                .collect(Collectors.joining("\n"));

        String dynamicJoinsStr = dynamicJoinFields.stream()
                .map(dynamicJoinField -> dynamicJoinField.joinStatement(params, timezoneOffset))
                .collect(Collectors.joining("\n"));

        if (regularJoins.isEmpty()) {
            return dynamicJoinsStr;
        }
        if (dynamicJoinsStr.isEmpty()) {
            return regularJoins;
        }
        return regularJoins + "\n" + dynamicJoinsStr;
    }

    public boolean hasField(Field field, List<Field> parents) {
        return this.fields.stream().anyMatch(columnField -> columnField.is(field, parents));
    }

    public ColumnField findFieldByName(String field) {
        return this.fields.stream().filter(columnField -> columnField.fieldName().equals(field))
                .findFirst().orElseThrow(() -> new RuntimeException("field not found: " + field));
    }

    public boolean hasJoinField(Field field) {
        boolean hasRegularJoin = this.joinFields.stream().anyMatch(joinField -> joinField.is(field));
        boolean hasDynamicJoin = this.dynamicJoinFields.stream().anyMatch(dynamicJoinField -> dynamicJoinField.is(field));
        return hasRegularJoin || hasDynamicJoin;
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
