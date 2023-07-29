package cn.beagile.dslquery;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class WhereListTest {
    @Test
    public void should_return_1() {
        WhereList whereList = new WhereList("(a.name equal bob)");
        List<String> list = whereList.list();
        assertEquals(1, list.size());
    }


    @Test
    public void should_return_2() {
        WhereList whereList = new WhereList("(name equal bob)(name equal alice)");
        List<String> list = whereList.list();
        assertEquals(2, list.size());
        assertEquals("(name equal bob)", list.get(0));
        assertEquals("(name equal alice)", list.get(1));
    }

    @Test
    public void should_return_2_has_inner() {
        WhereList whereList = new WhereList("(and(name equal bob))(name equal alice)");
        List<String> list = whereList.list();
        assertEquals(2, list.size());
        assertEquals("(and(name equal bob))", list.get(0));
        assertEquals("(name equal alice)", list.get(1));
    }
}
