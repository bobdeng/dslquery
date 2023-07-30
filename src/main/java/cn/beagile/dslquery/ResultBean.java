package cn.beagile.dslquery;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class ResultBean {
    private final FieldsWithColumns fieldsWithColumns;
    private Class clazz;
    private Set<String> ignoreJoinClasses = new HashSet<>();
    private Map<Field, String> fieldAliasMap = new HashMap<>();
    private Stack<String> classStack = new Stack<>();

    public ResultBean(Class clazz) {
        this.clazz = clazz;
        getIgnoredClass();
        addFieldAlias(clazz);
        fieldsWithColumns = new FieldsWithColumns(this);
    }

    private void addFieldAlias(Class clz) {
        Arrays.stream(clz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(JoinColumn.class))
                .forEach(field -> {
                    fieldAliasMap.put(field, Stream.concat(classStack.stream(),Stream.of(field.getName())).collect(Collectors.joining(".")));
                    classStack.push(field.getName());
                    addFieldAlias(field.getType());
                    classStack.pop();
                });
    }

    public Class getClazz() {
        return clazz;
    }

    public FieldsWithColumns getFieldsWithColumns() {
        return fieldsWithColumns;
    }

    private void getIgnoredClass() {
        if (this.clazz.isAnnotationPresent(Ignores.class)) {
            this.ignoreJoinClasses = Arrays.stream(((Ignores) this.clazz.getAnnotation(Ignores.class)).value())
                    .collect(Collectors.toSet());
        }
    }

    public boolean ignored(Class clz) {
        return false;
    }

    public boolean ignored(Field field) {
        String name = fieldAliasMap.get(field);
        if(name==null){
            return false;
        }
        return ignoreJoinClasses.contains(name);
    }


}
