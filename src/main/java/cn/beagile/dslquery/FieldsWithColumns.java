package cn.beagile.dslquery;

import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import java.util.*;
import java.util.stream.Stream;

public class FieldsWithColumns {
    private final Class clz;
    private List<FieldWithColumn> listFields;
    private HashMap<String, FieldWithColumn> columnHashMap;

    public FieldsWithColumns(Class clz) {
        this.clz = clz;
        this.listFields = new ArrayList<>();
        all();
    }

    public List<FieldWithColumn> getListFields() {
        return listFields;
    }

    public void all() {
        columnHashMap = new HashMap<>();
        findFields(clz, null, "");
        Arrays.stream(clz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Embedded.class))
                .forEach(field -> {
                    findFields(field.getType(), field.getAnnotation(AttributeOverrides.class), field.getName() + ".");
                });
    }

    private void findFields(Class clz, AttributeOverrides attributeOverrides, String prefix) {
        if (attributeOverrides != null) {
            Arrays.stream(clz.getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(Column.class))
                    .filter(field -> Stream.of(attributeOverrides.value()).noneMatch(it -> it.name().equals(field.getName())))
                    .forEach(field -> {
                        FieldWithColumn column = new FieldWithColumn(field, null);
                        listFields.add(column);
                        columnHashMap.put(prefix + field.getName(), column);
                    });
        } else {
            Arrays.stream(clz.getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(Column.class))
                    .forEach(field -> {
                        FieldWithColumn column = new FieldWithColumn(field, null);
                        listFields.add(column);
                        columnHashMap.put(prefix + field.getName(), column);
                    });
        }
        if (attributeOverrides != null) {
            Arrays.stream(clz.getDeclaredFields())
                    .filter(field -> Stream.of(attributeOverrides.value()).anyMatch(it -> it.name().equals(field.getName())))
                    .forEach(field -> {
                        FieldWithColumn column = new FieldWithColumn(field, attributeOverrides);
                        listFields.add(column);
                        columnHashMap.put(prefix + field.getName(), column);
                    });
        }
    }

    public FieldWithColumn getFieldColumn(String field) {
        return columnHashMap.get(field);
    }
}
