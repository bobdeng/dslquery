package cn.beagile.dslquery;

import org.junit.jupiter.api.Test;

import static cn.beagile.dslquery.WhereBuilder.where;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class WhereBuilderTest {
    @Test
    public void build_empty() {
        String result = new WhereBuilder().and()
                .build();
        assertEquals("(and)", result);
    }

    @Test
    public void build_equals() {
        String result = new WhereBuilder().and()
                .equals("name", "bob")
                .build();
        assertEquals("(and(name equals bob))", result);
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
                .notequals("name", "bob")
                .build();
        assertEquals("(and(name notequals bob))", result);
    }

    @Test
    public void build_lessthanorequal() {
        String result = new WhereBuilder().and()
                .lessthanorequal("name", "bob")
                .build();
        assertEquals("(and(name lessthanorequals bob))", result);
    }

    @Test
    public void build_greaterthanorequal() {
        String result = new WhereBuilder().and()
                .greaterthanorequal("name", "bob")
                .build();
        assertEquals("(and(name greaterthanorequals bob))", result);
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
        String result = where().and()
                .notnull("name")
                .build();
        assertEquals("(and(name notnull))", result);
    }


    @Test
    public void build_with_in() {
        String result = new WhereBuilder().and()
                .in("name", new String[]{"bob", "alice"})
                .build();
        assertEquals("(and(name in [\"bob\",\"alice\"]))", result);
    }

    @Test
    public void build_with_not_in() {
        String result = new WhereBuilder().and()
                .notin("name", new String[]{"bob", "alice"})
                .build();
        assertEquals("(and(name notin [\"bob\",\"alice\"]))", result);
    }

    @Test
    public void build_with_between() {
        String result = new WhereBuilder().and()
                .between("age", 1, 32)
                .build();
        assertEquals("(and(age between 1,32))", result);
    }

    @Test
    public void build_with_multiple() {
        String result = new WhereBuilder().and()
                .equals("name", "bob")
                .greaterthan("age", "18")
                .build();
        assertEquals("(and(name equals bob)(age greaterthan 18))", result);
    }

    @Test
    public void build_with_multiple_and_inner() {
        String result = new WhereBuilder().and()
                .equals("name", "bob")
                .greaterthan("age", "18")
                .and()
                .equals("name", "alice")
                .build();
        assertEquals("(and(name equals bob)(age greaterthan 18)(and(name equals alice)))", result);
    }

    @Test
    public void build_with_or() {
        String result = new WhereBuilder().or()
                .equals("name", "bob")
                .greaterthan("age", "18")
                .and()
                .equals("name", "alice")
                .contains("name", "bob")
                .build();
        assertEquals("(or(name equals bob)(age greaterthan 18)(and(name equals alice)(name contains bob)))", result);
    }

    @Test
    public void build_with_prev() {
        String result = new WhereBuilder().or()
                .equals("name", "bob")
                .greaterthan("age", "18")
                .and()
                .equals("name", "alice")
                .prev()
                .contains("name", "bob")
                .build();
        assertEquals("(or(name equals bob)(age greaterthan 18)(and(name equals alice))(name contains bob))", result);
    }
}
