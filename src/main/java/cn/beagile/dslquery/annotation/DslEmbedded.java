package cn.beagile.dslquery.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * DSL Query embedded annotation.
 * Alternative to javax.persistence.Embedded / jakarta.persistence.Embedded
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DslEmbedded {
}
