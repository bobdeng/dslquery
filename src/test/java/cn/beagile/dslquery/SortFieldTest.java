package cn.beagile.dslquery;

import org.junit.jupiter.api.Test;

import javax.persistence.Column;

import static org.junit.jupiter.api.Assertions.*;

public class SortFieldTest {
    //当字段名称不合法的时候，抛出异常
    @View("test_view")
    public static class ClassForSort {
        @Column(name = "person_name")
        public String name;
    }
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
    @Test
    public void default_is_asc() {
        String sort = new SortField("name").toSQL(new DSLSQLBuilder(new DSLQuery<>(null, ClassForSort.class)));
        assertEquals("test_view.person_name",sort);
    }
    @Test
    public void sort_asc() {
        String sort = new SortField("name asc").toSQL(new DSLSQLBuilder(new DSLQuery<>(null, ClassForSort.class)));
        assertEquals("test_view.person_name asc",sort);
    }
    @Test
    public void sort_desc() {
        String sort = new SortField("name desc").toSQL(new DSLSQLBuilder(new DSLQuery<>(null, ClassForSort.class)));
        assertEquals("test_view.person_name desc",sort);
    }
}
