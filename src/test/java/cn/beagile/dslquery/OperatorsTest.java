package cn.beagile.dslquery;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OperatorsTest {
    @Test
    public void should_return_eq() {
        assertEquals(Operator.Equals, Operators.byName("eq"));
        assertEquals(Operator.Between, Operators.byName("bt"));
        assertEquals(Operator.GreaterThanOrEqual, Operators.byName("ge"));
        assertEquals(Operator.GreaterThan, Operators.byName("gt"));
        assertEquals(Operator.LessThan, Operators.byName("lt"));
        assertEquals(Operator.LessThanOrEqual, Operators.byName("le"));
        assertEquals(Operator.NotEquals, Operators.byName("ne"));
        assertEquals(Operator.NotNull, Operators.byName("inn"));
        assertEquals(Operator.IsNull, Operators.byName("isn"));
        assertEquals(Operator.In, Operators.byName("in"));
        assertEquals(Operator.NotIn, Operators.byName("ni"));
        assertEquals(Operator.Contains, Operators.byName("ct"));
        assertEquals(Operator.StartsWith, Operators.byName("sw"));
        assertEquals(Operator.EndsWith, Operators.byName("ew"));
    }
}
