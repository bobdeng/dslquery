package cn.beagile.dslquery;

import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;


public class ResultBean {
    private final FieldsWithColumns fieldsWithColumns;
    private Class clazz;
    private Set<Class> ignoreJoinClasses=new HashSet<>();

    public ResultBean(Class clazz) {
        this.clazz = clazz;
        getIgnoredClass(clazz);
        fieldsWithColumns = new FieldsWithColumns(this);
    }
    public Class getClazz() {
        return clazz;
    }

    public FieldsWithColumns getFieldsWithColumns() {
        return fieldsWithColumns;
    }

    private void getIgnoredClass(Class clazz) {
        Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(JoinColumn.class))
                .filter(field -> field.isAnnotationPresent(Ignores.class))
                .flatMap(field -> Stream.of(field.getAnnotation(Ignores.class).value()))
                .forEach(ignoreJoinClasses::add);
        Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(JoinColumn.class))
                .forEach(field -> getIgnoredClass(field.getType()));
        Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(JoinColumns.class))
                .forEach(field -> getIgnoredClass(field.getType()));

    }

    public boolean ignored(Class clz) {
        return ignoreJoinClasses.contains(clz);
    }

}
