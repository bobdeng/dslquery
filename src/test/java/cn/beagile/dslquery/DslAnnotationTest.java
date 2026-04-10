package cn.beagile.dslquery;

import cn.beagile.dslquery.annotation.*;
import org.junit.jupiter.api.Test;

import javax.persistence.Column;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试自定义 DSL 注解的功能
 */
public class DslAnnotationTest {

    @Test
    public void should_read_dsl_column() throws NoSuchFieldException {
        Field field = UserWithDslAnnotation.class.getDeclaredField("name");
        AnnotationReader.ColumnInfo columnInfo = AnnotationReader.getColumn(field);

        assertNotNull(columnInfo);
        assertEquals("user_name", columnInfo.name);
        assertFalse(columnInfo.unique);
        assertTrue(columnInfo.nullable);
    }

    @Test
    public void should_read_dsl_column_with_unique() throws NoSuchFieldException {
        Field field = UserWithDslAnnotation.class.getDeclaredField("id");
        AnnotationReader.ColumnInfo columnInfo = AnnotationReader.getColumn(field);

        assertNotNull(columnInfo);
        assertEquals("id", columnInfo.name);
        assertTrue(columnInfo.unique);
    }

    @Test
    public void should_detect_dsl_embedded() throws NoSuchFieldException {
        Field field = UserWithDslAnnotation.class.getDeclaredField("address");
        assertTrue(AnnotationReader.hasEmbedded(field));
    }

    @Test
    public void should_read_dsl_join_column() throws NoSuchFieldException {
        Field field = UserWithDslAnnotation.class.getDeclaredField("department");
        AnnotationReader.JoinColumnInfo[] joinColumns = AnnotationReader.getJoinColumns(field);

        assertEquals(1, joinColumns.length);
        assertEquals("dept_id", joinColumns[0].name);
        assertEquals("id", joinColumns[0].referencedColumnName);
    }

    @Test
    public void should_read_dsl_one_to_many() throws NoSuchFieldException {
        Field field = UserWithDslAnnotation.class.getDeclaredField("orders");
        assertTrue(AnnotationReader.hasOneToMany(field));
        assertEquals("userId", AnnotationReader.getOneToManyMappedBy(field));
    }

    @Test
    public void should_prefer_dsl_annotation_over_javax() throws NoSuchFieldException {
        // 测试优先级：当同时存在 @DslColumn 和 @Column 时，优先使用 @DslColumn
        Field field = MixedAnnotationEntity.class.getDeclaredField("name");
        AnnotationReader.ColumnInfo columnInfo = AnnotationReader.getColumn(field);

        assertNotNull(columnInfo);
        // 应该读取 @DslColumn 的值，而不是 @Column 的值
        assertEquals("dsl_name", columnInfo.name);
    }

    @Test
    public void should_fallback_to_javax_when_no_dsl_annotation() throws NoSuchFieldException {
        // 测试 fallback：当只有 @Column 时，应该能读取
        Field field = MixedAnnotationEntity.class.getDeclaredField("email");
        AnnotationReader.ColumnInfo columnInfo = AnnotationReader.getColumn(field);

        assertNotNull(columnInfo);
        assertEquals("email_address", columnInfo.name);
    }

    @Test
    public void should_work_with_dsl_query() {
        // 集成测试：验证 DSLQuery 能正确处理自定义注解
        DSLQuery<UserWithDslAnnotation> query = new DSLQuery<>(null, UserWithDslAnnotation.class);
        ColumnFields columnFields = new ColumnFields(query);

        // 应该能读取到所有标记了 @DslColumn 的字段
        assertTrue(columnFields.selectFields().size() >= 2);

        // 验证字段名称映射正确
        boolean hasIdField = columnFields.selectFields().stream()
                .anyMatch(f -> "id".equals(f.columnName()));
        boolean hasNameField = columnFields.selectFields().stream()
                .anyMatch(f -> "user_name".equals(f.columnName()));

        assertTrue(hasIdField);
        assertTrue(hasNameField);
    }

    // 测试实体类 - 使用自定义注解
    @View("users")
    public static class UserWithDslAnnotation {
        @DslColumn(name = "id", unique = true)
        private Long id;

        @DslColumn(name = "user_name")
        private String name;

        @DslEmbedded
        private Address address;

        @DslJoinColumn(name = "dept_id", referencedColumnName = "id")
        private Department department;

        @DslOneToMany(mappedBy = "userId")
        private java.util.List<Order> orders;
    }

    // 测试实体类 - 混用注解
    @View("mixed")
    public static class MixedAnnotationEntity {
        // 同时存在两种注解，应该优先使用 @DslColumn
        @DslColumn(name = "dsl_name")
        @Column(name = "javax_name")
        private String name;

        // 只有 @Column，应该能正常读取
        @Column(name = "email_address")
        private String email;
    }

    public static class Address {
        @DslColumn(name = "street")
        private String street;
    }

    @View("departments")
    public static class Department {
        @DslColumn(name = "id")
        private Long id;
    }

    @View("orders")
    public static class Order {
        @DslColumn(name = "id")
        private Long id;
    }
}
