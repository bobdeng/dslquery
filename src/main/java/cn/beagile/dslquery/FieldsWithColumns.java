package cn.beagile.dslquery;

import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class FieldsWithColumns {
    private List<FieldWithColumn> listFields = new ArrayList<>();
    private HashMap<String, FieldWithColumn> columnHashMapByColumnName = new HashMap<>();
    private HashMap<Field, FieldWithColumn> columnHashMapByField = new HashMap<>();

    FieldsWithColumns(Class clz) {
        findFields(clz, null, "");
    }

    List<FieldWithColumn> getListFields() {
        return new ArrayList<>(listFields);
    }

    private void findFields(Class clz, AttributeOverrides attributeOverrides, String prefix) {
        if ("".equals(prefix)) {
            addColumnsNotOverride(clz, attributeOverrides, prefix);
            addEmbeddedFields(clz);
            return;
        }
        addColumnsOverride(clz, attributeOverrides, prefix);
    }

    private void addEmbeddedFields(Class clz) {
        Arrays.stream(clz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Embedded.class))
                .forEach(field -> findFields(field.getType(), field.getAnnotation(AttributeOverrides.class), field.getName() + "."));
    }

    private void addColumnsOverride(Class clz, AttributeOverrides attributeOverrides, String prefix) {
        Predicate<Field> isOverride = getIsOverridePredicate(attributeOverrides);
        Arrays.stream(clz.getDeclaredFields())
                .filter(isOverride)
                .forEach(field -> addColumnField(attributeOverrides, prefix, field));
    }

    private static Predicate<Field> getIsOverridePredicate(AttributeOverrides attributeOverrides) {
        if (attributeOverrides == null) {
            return field -> false;
        }
        return field -> Stream.of(attributeOverrides.value()).anyMatch(it -> it.name().equals(field.getName()));
    }

    private void addColumnsNotOverride(Class clz, AttributeOverrides attributeOverrides, String prefix) {
        Predicate<Field> isOverride = getIsOverridePredicate(attributeOverrides);
        Arrays.stream(clz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Column.class))
                .filter(field -> !isOverride.test(field))
                .forEach(field -> addColumnField(attributeOverrides, prefix, field));
    }

    private void addColumnField(AttributeOverrides attributeOverrides, String prefix, Field field) {
        FieldWithColumn column = new FieldWithColumn(field, attributeOverrides);
        listFields.add(column);
        columnHashMapByColumnName.put(prefix + field.getName(), column);
        columnHashMapByField.put(field, column);
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
