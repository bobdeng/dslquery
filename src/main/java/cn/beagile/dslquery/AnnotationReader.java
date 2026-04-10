package cn.beagile.dslquery;

import cn.beagile.dslquery.annotation.*;

import java.lang.reflect.Field;

/**
 * Annotation reader that supports both custom DSL annotations
 * and javax.persistence annotations for backward compatibility.
 *
 * Priority: DslXxx annotations > javax.persistence annotations
 */
class AnnotationReader {

    /**
     * Get column information from field.
     * Tries @DslColumn first, then falls back to @Column
     */
    static ColumnInfo getColumn(Field field) {
        // Try DslColumn first
        if (field.isAnnotationPresent(DslColumn.class)) {
            DslColumn col = field.getAnnotation(DslColumn.class);
            return new ColumnInfo(col.name(), col.unique(), col.nullable(), col.length());
        }

        // Fallback to javax.persistence.Column
        try {
            Class<?> columnClass = Class.forName("javax.persistence.Column");
            if (field.isAnnotationPresent((Class) columnClass)) {
                Object col = field.getAnnotation((Class) columnClass);
                String name = (String) columnClass.getMethod("name").invoke(col);
                boolean unique = (boolean) columnClass.getMethod("unique").invoke(col);
                boolean nullable = (boolean) columnClass.getMethod("nullable").invoke(col);
                int length = (int) columnClass.getMethod("length").invoke(col);
                return new ColumnInfo(name, unique, nullable, length);
            }
        } catch (Exception e) {
            // javax.persistence not available, ignore
        }

        return null;
    }

    /**
     * Check if field has column annotation
     */
    static boolean hasColumn(Field field) {
        return getColumn(field) != null;
    }

    /**
     * Check if field has embedded annotation
     */
    static boolean hasEmbedded(Field field) {
        if (field.isAnnotationPresent(DslEmbedded.class)) {
            return true;
        }

        try {
            Class<?> embeddedClass = Class.forName("javax.persistence.Embedded");
            return field.isAnnotationPresent((Class) embeddedClass);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get join column information from field
     */
    static JoinColumnInfo[] getJoinColumns(Field field) {
        // Try DslJoinColumns first
        if (field.isAnnotationPresent(DslJoinColumns.class)) {
            DslJoinColumn[] cols = field.getAnnotation(DslJoinColumns.class).value();
            JoinColumnInfo[] result = new JoinColumnInfo[cols.length];
            for (int i = 0; i < cols.length; i++) {
                result[i] = new JoinColumnInfo(cols[i].name(), cols[i].referencedColumnName());
            }
            return result;
        }

        // Try single DslJoinColumn
        if (field.isAnnotationPresent(DslJoinColumn.class)) {
            DslJoinColumn col = field.getAnnotation(DslJoinColumn.class);
            return new JoinColumnInfo[]{new JoinColumnInfo(col.name(), col.referencedColumnName())};
        }

        // Fallback to javax.persistence
        try {
            Class<?> joinColumnsClass = Class.forName("javax.persistence.JoinColumns");
            if (field.isAnnotationPresent((Class) joinColumnsClass)) {
                Object joinCols = field.getAnnotation((Class) joinColumnsClass);
                Object[] cols = (Object[]) joinColumnsClass.getMethod("value").invoke(joinCols);
                JoinColumnInfo[] result = new JoinColumnInfo[cols.length];
                Class<?> joinColumnClass = Class.forName("javax.persistence.JoinColumn");
                for (int i = 0; i < cols.length; i++) {
                    String name = (String) joinColumnClass.getMethod("name").invoke(cols[i]);
                    String refName = (String) joinColumnClass.getMethod("referencedColumnName").invoke(cols[i]);
                    String table = (String) joinColumnClass.getMethod("table").invoke(cols[i]);
                    result[i] = new JoinColumnInfo(name, refName, table);
                }
                return result;
            }

            Class<?> joinColumnClass = Class.forName("javax.persistence.JoinColumn");
            if (field.isAnnotationPresent((Class) joinColumnClass)) {
                // Check if there are multiple @JoinColumn annotations (repeatable)
                try {
                    Object[] cols = (Object[]) field.getClass().getMethod("getAnnotationsByType", Class.class)
                            .invoke(field, joinColumnClass);
                    if (cols.length > 1) {
                        JoinColumnInfo[] result = new JoinColumnInfo[cols.length];
                        for (int i = 0; i < cols.length; i++) {
                            String name = (String) joinColumnClass.getMethod("name").invoke(cols[i]);
                            String refName = (String) joinColumnClass.getMethod("referencedColumnName").invoke(cols[i]);
                            String table = (String) joinColumnClass.getMethod("table").invoke(cols[i]);
                            result[i] = new JoinColumnInfo(name, refName, table);
                        }
                        return result;
                    }
                } catch (Exception e) {
                    // Fall through to single annotation handling
                }

                // Single @JoinColumn
                Object col = field.getAnnotation((Class) joinColumnClass);
                String name = (String) joinColumnClass.getMethod("name").invoke(col);
                String refName = (String) joinColumnClass.getMethod("referencedColumnName").invoke(col);
                String table = (String) joinColumnClass.getMethod("table").invoke(col);
                return new JoinColumnInfo[]{new JoinColumnInfo(name, refName, table)};
            }
        } catch (Exception e) {
            // javax.persistence not available, ignore
        }

        return new JoinColumnInfo[0];
    }

    /**
     * Check if field has join column annotation
     */
    static boolean hasJoinColumn(Field field) {
        return getJoinColumns(field).length > 0;
    }

    /**
     * Get OneToMany mappedBy value
     */
    static String getOneToManyMappedBy(Field field) {
        // Try DslOneToMany first
        if (field.isAnnotationPresent(DslOneToMany.class)) {
            return field.getAnnotation(DslOneToMany.class).mappedBy();
        }

        // Fallback to javax.persistence.OneToMany
        try {
            Class<?> oneToManyClass = Class.forName("javax.persistence.OneToMany");
            if (field.isAnnotationPresent((Class) oneToManyClass)) {
                Object oneToMany = field.getAnnotation((Class) oneToManyClass);
                return (String) oneToManyClass.getMethod("mappedBy").invoke(oneToMany);
            }
        } catch (Exception e) {
            // javax.persistence not available, ignore
        }

        return "";
    }

    /**
     * Check if field has OneToMany annotation
     */
    static boolean hasOneToMany(Field field) {
        if (field.isAnnotationPresent(DslOneToMany.class)) {
            return true;
        }

        try {
            Class<?> oneToManyClass = Class.forName("javax.persistence.OneToMany");
            return field.isAnnotationPresent((Class) oneToManyClass);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get AttributeOverrides from field (only supports javax.persistence for now)
     */
    static AttributeOverrideInfo[] getAttributeOverrides(Field field) {
        try {
            Class<?> attributeOverridesClass = Class.forName("javax.persistence.AttributeOverrides");
            if (field.isAnnotationPresent((Class) attributeOverridesClass)) {
                Object attributeOverrides = field.getAnnotation((Class) attributeOverridesClass);
                Object[] overrides = (Object[]) attributeOverridesClass.getMethod("value").invoke(attributeOverrides);

                AttributeOverrideInfo[] result = new AttributeOverrideInfo[overrides.length];
                Class<?> attributeOverrideClass = Class.forName("javax.persistence.AttributeOverride");
                Class<?> columnClass = Class.forName("javax.persistence.Column");

                for (int i = 0; i < overrides.length; i++) {
                    String name = (String) attributeOverrideClass.getMethod("name").invoke(overrides[i]);
                    Object column = attributeOverrideClass.getMethod("column").invoke(overrides[i]);
                    String columnName = (String) columnClass.getMethod("name").invoke(column);
                    boolean unique = (boolean) columnClass.getMethod("unique").invoke(column);
                    boolean nullable = (boolean) columnClass.getMethod("nullable").invoke(column);
                    int length = (int) columnClass.getMethod("length").invoke(column);

                    result[i] = new AttributeOverrideInfo(name, new ColumnInfo(columnName, unique, nullable, length));
                }
                return result;
            }
        } catch (Exception e) {
            // javax.persistence not available or error, ignore
        }

        return new AttributeOverrideInfo[0];
    }

    /**
     * Check if field has AttributeOverrides annotation
     */
    static boolean hasAttributeOverrides(Field field) {
        try {
            Class<?> attributeOverridesClass = Class.forName("javax.persistence.AttributeOverrides");
            return field.isAnnotationPresent((Class) attributeOverridesClass);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Column information holder
     */
    static class ColumnInfo {
        final String name;
        final boolean unique;
        final boolean nullable;
        final int length;

        ColumnInfo(String name, boolean unique, boolean nullable, int length) {
            this.name = name;
            this.unique = unique;
            this.nullable = nullable;
            this.length = length;
        }
    }

    /**
     * Join column information holder
     */
    static class JoinColumnInfo {
        final String name;
        final String referencedColumnName;
        final String table;

        JoinColumnInfo(String name, String referencedColumnName) {
            this(name, referencedColumnName, "");
        }

        JoinColumnInfo(String name, String referencedColumnName, String table) {
            this.name = name;
            this.referencedColumnName = referencedColumnName;
            this.table = table;
        }
    }

    /**
     * AttributeOverride information holder
     */
    static class AttributeOverrideInfo {
        final String name;
        final ColumnInfo column;

        AttributeOverrideInfo(String name, ColumnInfo column) {
            this.name = name;
            this.column = column;
        }
    }
}

