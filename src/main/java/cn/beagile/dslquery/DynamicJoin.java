package cn.beagile.dslquery;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DynamicJoin {
    /**
     * 当前表的关联字段名
     */
    String joinKey();

    /**
     * 目标查询结果的关联字段名
     */
    String targetKey();

    /**
     * Join类型，默认LEFT JOIN
     */
    JoinType joinType() default JoinType.LEFT;

    enum JoinType {
        LEFT, INNER, RIGHT
    }
}
