package cn.beagile.dslquery;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SingleExpressionTest {
    //当field不是以字母开头的时候，抛出异常
    @Test
    public void should_throw_exception_when_field_not_start_with_letter() {
        RuntimeException e = assertThrows(RuntimeException.class, () -> new SingleExpression("1name", "equal", "bob"));
        assertEquals("invalid field:1name", e.getMessage());
    }

    //当field包含非字母数字下划线的时候，抛出异常
    @Test
    public void should_throw_exception_when_field_contains_not_letter_number_underline() {
        RuntimeException e = assertThrows(RuntimeException.class, () -> new SingleExpression("na%me", "equal", "bob"));
        assertEquals("invalid field:na%me", e.getMessage());
    }
}