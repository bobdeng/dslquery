package cn.beagile.dslquery.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * DSL Query one-to-many annotation.
 * Alternative to javax.persistence.OneToMany / jakarta.persistence.OneToMany
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DslOneToMany {
    /**
     * The field that owns the relationship
     */
    String mappedBy() default "";
}
