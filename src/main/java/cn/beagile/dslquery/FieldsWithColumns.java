package cn.beagile.dslquery;

import javax.persistence.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FieldsWithColumns {
    private final List<FieldWithColumn> listFields = new ArrayList<>();
    private final HashMap<String, FieldWithColumn> columnHashMapByColumnName = new HashMap<>();
    private final HashMap<Field, FieldWithColumn> columnHashMapByField = new HashMap<>();
    private final Stack<Field> embeddedFields = new Stack<>();
    private final Stack<Class> classStack = new Stack<>();
    private final Stack<String> prefix = new Stack<>();
    private AttributeOverrides firstAttributeOverrides;

    FieldsWithColumns(Class rootClass) {
        findFields(rootClass);
    }

    List<FieldWithColumn> getListFields() {
        return new ArrayList<>(listFields);
    }

    private void findFields(Class clz) {
        classStack.push(clz);
        if (isRoot()) {
            addColumnsWithColumn(clz);
        }
        addColumnsOverride(clz);
        addEmbeddedFields(clz);
        addJoinFields(clz);
        classStack.pop();
    }

    private void findJoinedFields(Class clz) {
        classStack.push(clz);
        addColumnsWithColumn(clz);
        addJoinFields(clz);
        classStack.pop();
    }

    private boolean isRoot() {
        return this.classStack.size() == 1;
    }

    private void addEmbeddedFields(Class clz) {
        Arrays.stream(clz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Embedded.class))
                .forEach(this::getEmbeddedFields);
    }

    private void addJoinFields(Class clz) {
        Arrays.stream(clz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(JoinColumn.class))
                .forEach(this::getJoinedFields);
    }

    private void getJoinedFields(Field field) {
        if (isRoot()) {
            this.firstAttributeOverrides = field.getAnnotation(AttributeOverrides.class);
        }
        prefix.push(field.getName());
        embeddedFields.push(field);
        findJoinedFields(field.getType());
        prefix.pop();
        embeddedFields.pop();
    }

    private void getEmbeddedFields(Field field) {
        if (isRoot()) {
            this.firstAttributeOverrides = field.getAnnotation(AttributeOverrides.class);
        }
        embeddedFields.push(field);
        findFields(field.getType());
        embeddedFields.pop();
    }

    private void addColumnsOverride(Class clz) {
        Arrays.stream(clz.getDeclaredFields())
                .filter(field -> this.getFieldAttributeOverride(field).isPresent())
                .forEach(this::addColumnField);
    }

    private String getFullPathInFirstOverride(Field field) {
        Stream<String> parentNames = embeddedFields.stream().skip(1).map(Field::getName);
        return Stream.concat(parentNames, Stream.of(field.getName())).collect(Collectors.joining("."));
    }

    private void addColumnsWithColumn(Class clz) {
        Arrays.stream(clz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Column.class))
                .forEach(this::addColumnField);
    }

    private void addColumnField(Field field) {
        FieldWithColumn column = getFieldWithColumn(field);
        listFields.add(column);
        columnHashMapByColumnName.put(getFieldFullName(field), column);
        columnHashMapByField.put(field, column);
    }

    private FieldWithColumn getFieldWithColumn(Field field) {
        return new FieldWithColumn(field, getFieldAttributeOverride(field),prefix,classStack);
    }

    private Optional<AttributeOverride> getFieldAttributeOverride(Field field) {
        String fullPathInFirstOverride = getFullPathInFirstOverride(field);
        AttributeOverride[] overrides = Optional.ofNullable(firstAttributeOverrides)
                .map(AttributeOverrides::value)
                .orElseGet(() -> new AttributeOverride[0]);
        return Stream.of(overrides)
                .filter(it -> it.name().equals(fullPathInFirstOverride))
                .findFirst();
    }

    private String getFieldFullName(Field field) {
        Stream<String> parentNames = embeddedFields.stream().map(Field::getName);
        return Stream.concat(parentNames, Stream.of(field.getName()))
                .collect(Collectors.joining("."));
    }

    FieldWithColumn getFieldColumn(String field) {
        FieldWithColumn result = columnHashMapByColumnName.get(field);
        if (result == null) {
            throw new RuntimeException("field not found: " + field);
        }
        return result;
    }

    FieldWithColumn getFieldColumnByField(Field field) {
        return columnHashMapByField.get(field);
    }

    boolean hasField(Field field) {
        return columnHashMapByField.containsKey(field);
    }
}
