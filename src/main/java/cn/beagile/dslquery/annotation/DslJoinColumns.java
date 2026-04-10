package cn.beagile.dslquery.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * DSL Query join columns annotation.
 * Alternative to javax.persistence.JoinColumns / jakarta.persistence.JoinColumns
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DslJoinColumns {
    /**
     * The join columns that map the relationship
     */
    DslJoinColumn[] value();
}
