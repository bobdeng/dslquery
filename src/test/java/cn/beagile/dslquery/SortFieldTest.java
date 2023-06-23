package cn.beagile.dslquery;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SortFieldTest {
    //当字段名称不合法的时候，抛出异常
    @Test
    public void should_throw_exception_when_field_not_start_with_letter() {
        RuntimeException e = assertThrows(RuntimeException.class, () -> new SortField("1name"));
        assertEquals("invalid field:1name", e.getMessage());
    }
    //当direction不是asc活着desc的时候，抛出异常
    @Test
    public void should_throw_exception_when_direction_not_asc_or_desc() {
        RuntimeException e = assertThrows(RuntimeException.class, () -> new SortField("name asc1"));
        assertEquals("invalid direction:asc1", e.getMessage());
    }
}