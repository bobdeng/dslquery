package cn.beagile.dslquery;

import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class FieldsWithColumns {
    private final Class clz;
    private List<FieldWithColumn> listFields;
    private HashMap<String, FieldWithColumn> columnHashMap;
    private HashMap<Field, FieldWithColumn> columnHashMapWithField = new HashMap<>();

    public FieldsWithColumns(Class clz) {
        this.clz = clz;
        this.listFields = new ArrayList<>();
        columnHashMap = new HashMap<>();
        findFields(this.clz, null, "");
    }

    public List<FieldWithColumn> getListFields() {
        return listFields;
    }

    private void findFields(Class clz, AttributeOverrides attributeOverrides, String prefix) {
        if ("".equals(prefix)) {
            addColumnsNotOverride(clz, attributeOverrides, prefix);
        }
        addColumnsOverride(clz, attributeOverrides, prefix);
        addEmbeddedFields(clz);
    }

    private void addEmbeddedFields(Class clz) {
        Arrays.stream(clz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Embedded.class))
                .forEach(field -> findFields(field.getType(), field.getAnnotation(AttributeOverrides.class), field.getName() + "."));
    }

    private void addColumnsOverride(Class clz, AttributeOverrides attributeOverrides, String prefix) {
        Predicate<Field> isOverride = (field -> attributeOverrides != null && Stream.of(attributeOverrides.value()).anyMatch(it -> it.name().equals(field.getName())));
        Arrays.stream(clz.getDeclaredFields())
                .filter(isOverride)
                .forEach(field -> addColumnField(attributeOverrides, prefix, field));
    }

    private void addColumnsNotOverride(Class clz, AttributeOverrides attributeOverrides, String prefix) {
        Predicate<Field> isOverride = (field -> attributeOverrides != null && Stream.of(attributeOverrides.value()).anyMatch(it -> it.name().equals(field.getName())));
        Arrays.stream(clz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Column.class))
                .filter(field -> !isOverride.test(field))
                .forEach(field -> addColumnField(attributeOverrides, prefix, field));
    }

    private void addColumnField(AttributeOverrides attributeOverrides, String prefix, Field field) {
        FieldWithColumn column = new FieldWithColumn(field, attributeOverrides);
        listFields.add(column);
        columnHashMap.put(prefix + field.getName(), column);
        columnHashMapWithField.put(field, column);
    }

    public FieldWithColumn getFieldColumn(String field) {
        return columnHashMap.get(field);
    }
    public FieldWithColumn getFieldColumnByField(Field field) {
        return columnHashMapWithField.get(field);
    }
    boolean hasField(Field field){
        return columnHashMapWithField.containsKey(field);
    }
}
