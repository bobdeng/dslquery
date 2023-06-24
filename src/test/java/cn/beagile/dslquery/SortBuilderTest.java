package cn.beagile.dslquery;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SortBuilderTest {
    @Test
    public void should_empty() {
        assertEquals("", new SortBuilder().build());
    }
    @Test
    public void should_asc() {
        assertEquals("name asc", new SortBuilder().asc("name").build());
    }
    @Test
    public void should_asc_multi() {
        assertEquals("name asc,age asc", new SortBuilder().asc("name").asc("age").build());
    }
    @Test
    public void should_desc() {
        assertEquals("name desc", new SortBuilder().desc("name").build());
    }
}
