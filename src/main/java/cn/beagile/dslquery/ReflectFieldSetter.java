package cn.beagile.dslquery;

import java.lang.reflect.Field;

public class ReflectFieldSetter {
    private Object target;
    private Field field;
    private Object value;

    public ReflectFieldSetter(Object target, Field field, Object value) {
        this.target = target;
        this.field = field;
        this.value = value;
    }

    public void set() {
        field.setAccessible(true);
        try {
            field.set(this.target, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } finally {
            field.setAccessible(false);
        }
    }
}
