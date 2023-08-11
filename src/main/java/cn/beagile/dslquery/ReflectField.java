package cn.beagile.dslquery;

import java.lang.reflect.Field;

class ReflectField {
    private final Object target;
    private final Field field;
    private final Object value;

    public ReflectField(Object target, Field field, Object value) {
        this.target = target;
        this.field = field;
        this.value = value;
    }

    public ReflectField(Object master, Field field) {
        this(master, field, null);
    }

    public void set(Object value) {
        field.setAccessible(true);
        try {
            field.set(this.target, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } finally {
            field.setAccessible(false);
        }
    }

    public Object get() {
        field.setAccessible(true);
        try {
            return field.get(this.target);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } finally {
            field.setAccessible(false);
        }
    }
}
