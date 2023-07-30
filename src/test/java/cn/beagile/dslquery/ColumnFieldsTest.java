package cn.beagile.dslquery;

import org.junit.jupiter.api.Test;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ColumnFieldsTest {
    @View("empty")
    public static class Empty {

    }


    @Test
    public void should_return_empty() {
        ColumnFields columnFields = new ColumnFields(Empty.class);
        List<ColumnField> fields = columnFields.selectFields();
        assertEquals(0, fields.size());
        assertEquals("empty", columnFields.from());
        assertEquals(0, columnFields.joined().size());
    }

    @View("has_one")
    public static class HasOne {
        @Column(name = "f_name")
        private String name;
    }

    @Test
    public void should_read_column() {
        ColumnFields columnFields = new ColumnFields(HasOne.class);
        assertEquals(1, columnFields.selectFields().size());
        assertEquals("f_name", columnFields.selectFields().get(0).columnName());
        assertEquals("name", columnFields.selectFields().get(0).fieldName());
        assertEquals("name", columnFields.selectFields().get(0).alias());
        assertEquals("has_one.f_name name", columnFields.selectFields().get(0).expression());
    }

    @View("has_embedded")
    public static class HasEmbedded {
        @AttributeOverrides({
                @AttributeOverride(name = "name", column = @Column(name = "f_name"))
        })
        @Embedded
        private HasOne hasOne;
    }

    @Test
    public void should_read_embedded_column() {
        ColumnFields columnFields = new ColumnFields(HasEmbedded.class);
        assertEquals(1, columnFields.selectFields().size());
        assertEquals("f_name", columnFields.selectFields().get(0).columnName());
        assertEquals("hasOne.name", columnFields.selectFields().get(0).fieldName());
        assertEquals("hasOne_name", columnFields.selectFields().get(0).alias());
        assertEquals("has_embedded.f_name hasOne_name", columnFields.selectFields().get(0).expression());
    }

    @View("has_embedded")
    public static class HasEmbeddedNotOverride {
        @AttributeOverrides({
        })
        @Embedded
        private HasOne hasOne;
    }

    @Test
    public void should_not_read_if_not_override() {
        ColumnFields columnFields = new ColumnFields(HasEmbeddedNotOverride.class);
        assertEquals(0, columnFields.selectFields().size());
    }

    @View("has_2embedded")
    public static class HasEmbedded2 {
        @AttributeOverrides({
                @AttributeOverride(name = "hasOne.name", column = @Column(name = "name_2"))
        })
        @Embedded
        private HasEmbeddedNotOverride one;
    }

    @Test
    public void should_read_embedded_twice() {
        ColumnFields columnFields = new ColumnFields(HasEmbedded2.class);
        assertEquals(1, columnFields.selectFields().size());
    }

}
