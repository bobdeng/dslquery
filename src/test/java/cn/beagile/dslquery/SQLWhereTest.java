package cn.beagile.dslquery;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SQLWhereTest {
    @Test
    void test_single() {
        List<SQLField> fields = List.of(new SQLField("name", "a.name", String.class));
        RawSQLBuilder sqlWhere = new RawSQLBuilder(fields, "(and(name eq 123))");
        assertEquals("where ((a.name = :p1))", sqlWhere.where());
        assertEquals(sqlWhere.param("p1"), "123");
    }

    @Test
    void test_array() {
        List<SQLField> fields = List.of(new SQLField("name", "a.name", String.class));
        RawSQLBuilder sqlWhere = new RawSQLBuilder(fields, "(and(name bt 1,2))");
        assertEquals("where ((a.name between :p1 and :p2))", sqlWhere.where());
        assertEquals(sqlWhere.param("p1"), "1");
        assertEquals(sqlWhere.param("p2"), "2");
    }

    @Test
    void test_in() {
        List<SQLField> fields = List.of(new SQLField("name", "a.name", String.class));
        RawSQLBuilder sqlWhere = new RawSQLBuilder(fields, "(and(name in [1,2]))");
        assertEquals("where ((a.name in (:p1)))", sqlWhere.where());
        assertEquals(sqlWhere.param("p1"), List.of("1", "2"));
    }
    @Test
    void test_in_int() {
        List<SQLField> fields = List.of(new SQLField("name", "a.name", Integer.class));
        RawSQLBuilder sqlWhere = new RawSQLBuilder(fields, "(and(name in [1,2]))");
        assertEquals("where ((a.name in (:p1)))", sqlWhere.where());
        assertEquals(sqlWhere.param("p1"), List.of(1, 2));
    }
}
