package cn.beagile.dslquery;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WhereBuilderTest {
    @Test
    public void build_empty() {
        String result = new WhereBuilder().and()
                .build();
        assertEquals("(and)", result);
    }

    @Test
    public void build_equal() {
        String result = new WhereBuilder().and()
                .equal("name", "bob")
                .build();
        assertEquals("(and(name equal bob))", result);
    }

    @Test
    public void build_greaterthan() {
        String result = new WhereBuilder().and()
                .greaterthan("name", "bob")
                .build();
        assertEquals("(and(name greaterthan bob))", result);
    }

    @Test
    public void build_lessthan() {
        String result = new WhereBuilder().and()
                .lessthan("name", "bob")
                .build();
        assertEquals("(and(name lessthan bob))", result);
    }

    @Test
    public void build_notequal() {
        String result = new WhereBuilder().and()
                .notequal("name", "bob")
                .build();
        assertEquals("(and(name notequal bob))", result);
    }

    @Test
    public void build_lessthanorequal() {
        String result = new WhereBuilder().and()
                .lessthanorequal("name", "bob")
                .build();
        assertEquals("(and(name lessthanorequal bob))", result);
    }

    @Test
    public void build_greaterthanorequal() {
        String result = new WhereBuilder().and()
                .greaterthanorequal("name", "bob")
                .build();
        assertEquals("(and(name greaterthanorequal bob))", result);
    }

    @Test
    public void build_startswith() {
        String result = new WhereBuilder().and()
                .startswith("name", "bob")
                .build();
        assertEquals("(and(name startswith bob))", result);
    }

    @Test
    public void build_endswith() {
        String result = new WhereBuilder().and()
                .endswith("name", "bob")
                .build();
        assertEquals("(and(name endswith bob))", result);
    }

    @Test
    public void build_contains() {
        String result = new WhereBuilder().and()
                .contains("name", "bob")
                .build();
        assertEquals("(and(name contains bob))", result);
    }

    @Test
    public void build_isnull() {
        String result = new WhereBuilder().and()
                .isnull("name")
                .build();
        assertEquals("(and(name isnull))", result);
    }

    @Test
    public void build_notnull() {
        String result = new WhereBuilder().and()
                .notnull("name")
                .build();
        assertEquals("(and(name notnull))", result);
    }

    @Test
    public void build_with_multiple() {
        String result = new WhereBuilder().and()
                .equal("name", "bob")
                .greaterthan("age", "18")
                .build();
        assertEquals("(and(name equal bob)(age greaterthan 18))", result);
    }

    @Test
    public void build_with_multiple_and_inner() {
        String result = new WhereBuilder().and()
                .equal("name", "bob")
                .greaterthan("age", "18")
                .and()
                .equal("name", "alice")
                .build();
        assertEquals("(and(name equal bob)(age greaterthan 18)(and(name equal alice)))", result);
    }

    @Test
    public void build_with_or() {
        String result = new WhereBuilder().or()
                .equal("name", "bob")
                .greaterthan("age", "18")
                .and()
                .equal("name", "alice")
                .build();
        assertEquals("(or(name equal bob)(age greaterthan 18)(and(name equal alice)))", result);
    }
}