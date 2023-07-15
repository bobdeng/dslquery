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
                @AttributeOverride(name = "phone", column = @Column(name = "address_phone")),
                @AttributeOverride(name = "another2.name2", column = @Column(name = "another_name2")),
                @AttributeOverride(name = "another2.name3", column = @Column(name = "another_name3"))
        })
        private Address address;
        @Embedded
        private Another another;

        public static class Address {
            @Column(name = "address_name")
            private String name;
            @Column(name = "phone")
            private String phone;
            @Embedded
            private Another2 another2;
        }

        public static class Another2 {
            @Column(name = "name2")
            private String name2;
            private String name3;
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
    @Test
    public void should_return_embedded2_column_field_with_column(){
        FieldWithColumn nameField = fieldsWithColumns.getFieldColumn("address.another2.name2");
        assertEquals("name2", nameField.getField().getName());
        assertEquals("another_name2", nameField.columnName());
    }
    @Test
    public void should_return_embedded2_column_field_without_column(){
        FieldWithColumn nameField = fieldsWithColumns.getFieldColumn("address.another2.name3");
        assertEquals("name3", nameField.getField().getName());
        assertEquals("another_name3", nameField.columnName());
    }
}
