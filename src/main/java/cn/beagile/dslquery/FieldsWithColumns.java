package cn.beagile.dslquery;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FieldsWithColumns {
    private List<FieldWithColumn> listFields = new ArrayList<>();
    private HashMap<String, FieldWithColumn> columnHashMapByColumnName = new HashMap<>();
    private HashMap<Field, FieldWithColumn> columnHashMapByField = new HashMap<>();
    int deep = 0;
    private AttributeOverrides firstAttributeOverrides;
    private Stack<Field> embeddedFields = new Stack<>();

    FieldsWithColumns(Class clz) {
        findFields(clz, null, "");
    }

    List<FieldWithColumn> getListFields() {
        return new ArrayList<>(listFields);
    }

    private void findFields(Class clz, AttributeOverrides attributeOverrides, String prefix) {
        deep++;
        if ("".equals(prefix)) {
            addColumnsNotOverride(clz, attributeOverrides);
        }
        addColumnsOverride(clz, attributeOverrides, prefix);
        addEmbeddedFields(clz);
        deep--;
    }

    private void addEmbeddedFields(Class clz) {
        Arrays.stream(clz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Embedded.class))
                .forEach(field -> {
                    getEmbeddedFields(field);
                });
    }

    private void getEmbeddedFields(Field field) {
        if (deep == 1) {
            this.firstAttributeOverrides = field.getAnnotation(AttributeOverrides.class);
        }
        embeddedFields.push(field);
        findFields(field.getType(), field.getAnnotation(AttributeOverrides.class), field.getName() + ".");
        embeddedFields.pop();
    }

    private void addColumnsOverride(Class clz, AttributeOverrides attributeOverrides, String prefix) {
        Predicate<Field> isOverride = getIsOverridePredicate(attributeOverrides);
        Arrays.stream(clz.getDeclaredFields())
                .filter(isOverride)
                .forEach(field -> addColumnField(attributeOverrides, field));
    }

    private Predicate<Field> getIsOverridePredicate(AttributeOverrides attributeOverrides) {
        if (this.firstAttributeOverrides != null) {
            return getFirstOverride();
        }
        if (attributeOverrides == null) {
            return field -> false;
        }
        return field -> Stream.of(attributeOverrides.value()).anyMatch(it -> it.name().equals(field.getName()));
    }

    private Predicate<Field> getFirstOverride() {
        return field -> {
            String fullPathInFirstOverride = getFullPathInFirstOverride(field);
            return Stream.of(firstAttributeOverrides.value()).anyMatch(it -> it.name().equals(fullPathInFirstOverride));
        };
    }

    private String getFullPathInFirstOverride(Field field) {
        Stream<String> parentNames = embeddedFields.stream().skip(1).map(Field::getName);
        return Stream.concat(parentNames, Stream.of(field.getName())).collect(Collectors.joining("."));
    }

    private void addColumnsNotOverride(Class clz, AttributeOverrides attributeOverrides) {
        Arrays.stream(clz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Column.class))
                .forEach(field -> addColumnField(attributeOverrides, field));
    }

    private void addColumnField(AttributeOverrides attributeOverrides, Field field) {
        FieldWithColumn column = getFieldWithColumn(attributeOverrides, field);
        listFields.add(column);
        columnHashMapByColumnName.put(getFieldFullName(field), column);
        columnHashMapByField.put(field, column);
    }

    private FieldWithColumn getFieldWithColumn(AttributeOverrides attributeOverrides, Field field) {
        if(this.firstAttributeOverrides!=null){
            String fullPathInFirstOverride = getFullPathInFirstOverride(field);
            AttributeOverride ao = Stream.of(firstAttributeOverrides.value()).filter(it -> it.name().equals(fullPathInFirstOverride))
                    .findFirst().get();
            return new FieldWithColumn(field, ao);

        }
        FieldWithColumn column = new FieldWithColumn(field, attributeOverrides);
        return column;
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
