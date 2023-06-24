package cn.beagile.dslquery;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ReflectFieldSetterTest {
    @Test
    public void should_throw_when_final() {
        RuntimeException e = assertThrows(RuntimeException.class, () -> new ReflectFieldSetter(new FinalFieldBean(), FinalFieldBean.class.getDeclaredFields()[0], "bob").set());
        assertEquals("java.lang.IllegalAccessException: Can not set static final java.lang.String field cn.beagile.dslquery.ReflectFieldSetterTest$FinalFieldBean.name to null value", e.getMessage());
    }

    public static class FinalFieldBean {
        private static final String name = "";
    }
}