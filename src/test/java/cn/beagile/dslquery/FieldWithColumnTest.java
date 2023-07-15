package cn.beagile.dslquery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;

import static org.junit.jupiter.api.Assertions.*;

class FieldWithColumnTest {

    private FieldsWithColumns fieldsWithColumns;

    public static class Person {
        @Column(name = "name1")
        private String name;
        private Integer age;
        @Embedded
        @AttributeOverrides({
                @AttributeOverride(name = "phone", column = @Column(name = "address_phone"))
        })
        private Address address;
        @Embedded
        private Another another;

        public static class Address {
            @Column(name = "address_name")
            private String name;
            @Column(name = "phone")
            private String phone;
        }

        public static class Another {
            private String name;
        }
    }

    @BeforeEach
    public void setup() {
        fieldsWithColumns = new FieldsWithColumns(Person.class);
    }

    @Test
    public void should_find_column_no_attribute_override() throws NoSuchFieldException {
        FieldWithColumn columnFinder = new FieldWithColumn(Person.class.getDeclaredField("name"), null);
        assertEquals("name1", columnFinder.columnName());
    }

    @Test
    public void should_return_column_field() {
        FieldWithColumn nameField = fieldsWithColumns.getFieldColumn("name");
        assertNotNull(nameField);
        assertEquals("name", nameField.getField().getName());
        assertEquals("name1", nameField.columnName());
    }

    @Test
    public void should_not_return_embedded_and_not_override_column_field() {
        RuntimeException e = assertThrows(RuntimeException.class, () -> fieldsWithColumns.getFieldColumn("address.name"));
        assertEquals("field not found: address.name", e.getMessage());
    }

    @Test
    public void should_return_embedded_column_field() {
        FieldWithColumn nameField = fieldsWithColumns.getFieldColumn("address.phone");
        assertEquals("phone", nameField.getField().getName());
        assertEquals("address_phone", nameField.columnName());
    }
}
