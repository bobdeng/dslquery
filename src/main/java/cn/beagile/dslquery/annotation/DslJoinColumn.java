package cn.beagile.dslquery.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * DSL Query join column annotation.
 * Alternative to javax.persistence.JoinColumn / jakarta.persistence.JoinColumn
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DslJoinColumn {
    /**
     * The name of the foreign key column
     */
    String name();

    /**
     * The name of the column referenced by this foreign key column
     */
    String referencedColumnName() default "";

    /**
     * Whether the foreign key column is nullable
     */
    boolean nullable() default true;
}
