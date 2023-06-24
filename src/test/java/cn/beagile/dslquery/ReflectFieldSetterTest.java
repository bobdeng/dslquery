package cn.beagile.dslquery;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ReflectFieldSetterTest {
    @Test
    public void should_throw_when_final() {
        assertThrows(RuntimeException.class, () -> new ReflectFieldSetter(new FinalFieldBean(), FinalFieldBean.class.getDeclaredFields()[0], "bob").set());
    }

    public static class FinalFieldBean {
        private static final String name = "";
    }
}