package cn.beagile.dslquery;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class RawSQLBuilderTest {
    @Test
    void params_no_fields() {
        RawSQLBuilder rawSQLBuilder = new RawSQLBuilder(Collections.emptyList(), "(and(name eq 123))");
        String where = rawSQLBuilder.where();
        assertEquals("where (true)", where);
        assertEquals("123", rawSQLBuilder.param("p0"));
    }

    @Test
    void params_no_fields_array() {
        RawSQLBuilder rawSQLBuilder = new RawSQLBuilder(Collections.emptyList(), "(and(name in ['123']))");
        String where = rawSQLBuilder.where();
        assertEquals("where (true)", where);
        assertArrayEquals(new String[]{"123"}, (String[]) rawSQLBuilder.param("p0"));
    }
    @Test
    void params_no_fields_bt() {
        RawSQLBuilder rawSQLBuilder = new RawSQLBuilder(Collections.emptyList(), "(and(name bt 1,2000))");
        String where = rawSQLBuilder.where();
        assertEquals("where (true)", where);
        assertEquals("1", rawSQLBuilder.param("p0_0"));
        assertEquals("2000", rawSQLBuilder.param("p0_1"));
    }
}
