package cn.beagile.dslquery.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * DSL Query column annotation.
 * Alternative to javax.persistence.Column / jakarta.persistence.Column
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DslColumn {
    /**
     * The name of the column
     */
    String name() default "";

    /**
     * Whether the column is a unique key
     */
    boolean unique() default false;

    /**
     * Whether the database column is nullable
     */
    boolean nullable() default true;

    /**
     * The column length
     */
    int length() default 255;
}
