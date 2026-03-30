package cn.beagile.dslquery;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JoinOnApiTest {
    @Test
    void should_expose_join_on_annotation() {
        assertDoesNotThrow(() -> Class.forName("cn.beagile.dslquery.JoinOn"));
    }

    @Test
    void should_expose_runtime_join_on_api() {
        Method method = assertDoesNotThrow(() -> DSLQuery.class.getMethod("joinOn", String.class, String.class));
        assertNotNull(method);
    }
}
