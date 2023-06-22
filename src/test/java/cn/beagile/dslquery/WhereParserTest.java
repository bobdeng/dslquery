package cn.beagile.dslquery;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class WhereParserTest {
    @Test
    public void should_parse_where() {
        WhereParser whereParser = new WhereParser();
        Where where = whereParser.parse("(and(name equal bob))");
        assertNotNull(where);
        assertEquals(where.getCondition(), "and");
        assertEquals(1, where.getPredicates().size());
        assertEquals(new Predicate("name", "equal", "bob"), where.getPredicates().get(0));
    }

    @Test
    public void should_parse_where_with_2_predicate() {
        WhereParser whereParser = new WhereParser();
        Where where = whereParser.parse("(and(name equal bob)(age greatterthan 18))");
        assertNotNull(where);
        assertEquals(where.getCondition(), "and");
        assertEquals(2, where.getPredicates().size());
        assertEquals(new Predicate("name", "equal", "bob"), where.getPredicates().get(0));
    }
    @Test
    public void should_parse_where_with_inner_predicate() {
        WhereParser whereParser = new WhereParser();
        Where where = whereParser.parse("(and(or(name equal bob)(name equal alice))(age greatterthan 18))");
        assertNotNull(where);
        assertEquals(where.getCondition(), "and");
        assertEquals(1, where.getPredicates().size());
        assertEquals(new Predicate("age", "greatterthan", "18"), where.getPredicates().get(0));
        assertEquals(1, where.getWheres().size());
        Where subWhere = where.getWheres().get(0);
        assertEquals(2,subWhere.getPredicates().size());
        assertEquals("or",subWhere.getCondition());
    }
}
