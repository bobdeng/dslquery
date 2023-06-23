package cn.beagile.dslquery;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WhereParserTest {
    @Test
    public void should_parse_where() {
        WhereParser whereParser = new WhereParser();
        ComplexExpression where = whereParser.parseSubWhere("(and(name equal bob))");
        assertNotNull(where);
        assertEquals(where.getCondition(), "and");
        assertEquals(1, where.getExpressions().size());
        assertEquals(new SingleExpression("name", "equal", "bob"), where.getExpressions().get(0));
    }

    @Test
    public void should_parse_where_with_2_predicate() {
        WhereParser whereParser = new WhereParser();
        ComplexExpression where = whereParser.parseSubWhere("(and(name equal bob)(age greaterthan 18))");
        assertNotNull(where);
        assertEquals(where.getCondition(), "and");
        assertEquals(2, where.getExpressions().size());
        assertEquals(new SingleExpression("name", "equal", "bob"), where.getExpressions().get(0));
    }

    @Test
    public void should_parse_where_with_inner_predicate() {
        WhereParser whereParser = new WhereParser();
        ComplexExpression where = whereParser.parseSubWhere("(and(or(name equal bob)(name equal alice))(age greaterthan 18))");
        assertNotNull(where);
        assertEquals(where.getCondition(), "and");
        assertEquals(2, where.getExpressions().size());
        assertEquals(new SingleExpression("age", "greaterthan", "18"), where.getExpressions().get(1));
    }

    //当不是以and或者or开头的时候，会默认以and开头
    @Test
    public void should_parse_where_with_inner_predicate_without_and_or() {
        WhereParser whereParser = new WhereParser();
        ComplexExpression where = whereParser.parseSubWhere("((name equal bob)(age greaterthan 18))");
        assertNotNull(where);
        assertEquals(where.getCondition(), "and");
        assertEquals(2, where.getExpressions().size());
        assertEquals(new SingleExpression("name", "equal", "bob"), where.getExpressions().get(0));
        assertEquals(new SingleExpression("age", "greaterthan", "18"), where.getExpressions().get(1));
        System.out.println(where);
    }

    //当条件不是and or的时候，抛出异常
    @Test
    public void should_throw_exception_when_wrong_start() {
        WhereParser whereParser = new WhereParser();
        RuntimeException e = assertThrows(RuntimeException.class, () -> whereParser.parseSubWhere("(ab(name equal bob)))"));
        assertEquals("invalid condition:ab", e.getMessage());
    }

    //当条件格式不对的时候，抛出异常
    @Test
    public void should_throw_exception_when_wrong_predicate_operator() {
        WhereParser whereParser = new WhereParser();
        RuntimeException e = assertThrows(RuntimeException.class, () -> {
            System.out.println(whereParser.parseSubWhere("(and(nameequal bob 123)))"));
        });
        assertEquals("invalid operator:bob", e.getMessage());
    }

    @Test
    public void should_throw_exception_when_wrong_predicate() {
        WhereParser whereParser = new WhereParser();
        RuntimeException e = assertThrows(RuntimeException.class, () -> {
            System.out.println(whereParser.parseSubWhere("(and(nameequalbob)))"));
        });
        assertEquals("invalid predicate:(nameequalbob)", e.getMessage());
    }

    @Test
    public void should_throw_exception_when_wrong_predicate_empty() {
        WhereParser whereParser = new WhereParser();
        RuntimeException e = assertThrows(RuntimeException.class, () -> {
            System.out.println(whereParser.parseSubWhere("(and()))"));
        });
        assertEquals("invalid predicate:()", e.getMessage());
    }

    @Test
    public void should_throw_exception_when_wrong_predicate_no_value() {
        WhereParser whereParser = new WhereParser();
        RuntimeException e = assertThrows(RuntimeException.class, () -> {
            System.out.println(whereParser.parseSubWhere("(and(name equal )))"));
        });
        assertEquals("invalid predicate:(name equal )", e.getMessage());
    }
}
