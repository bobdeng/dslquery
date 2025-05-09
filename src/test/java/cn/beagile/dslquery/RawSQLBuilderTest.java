package cn.beagile.dslquery;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class RawSQLBuilderTest {
    @Test
    void params_no_fields(){
        RawSQLBuilder rawSQLBuilder = new RawSQLBuilder(Collections.emptyList(), "(and(name eq 123))");
        String where = rawSQLBuilder.where();
        assertEquals("where (true)", where);
        assertEquals("123", rawSQLBuilder.param("p0"));
    }
}
