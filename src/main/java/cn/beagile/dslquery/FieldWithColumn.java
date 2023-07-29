package cn.beagile.dslquery;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;

public class FieldWithColumn {
    private final Field field;
    private final Stack<Class> classStack;
    private final Column column;
    private final String prefix;
    private View view;

    public FieldWithColumn(Field field, Optional<AttributeOverride> attributeOverride, Stack<String> prefix, Stack<Class> classStack) {
        this.field = field;
        this.classStack = classStack;
        this.column = attributeOverride.map(AttributeOverride::column)
                .orElseGet(() -> field.getAnnotation(Column.class));
        this.view = field.getDeclaringClass().getAnnotation(View.class);
        if (this.view == null) {
            List<View> views = classStack.stream().filter(clz -> clz.isAnnotationPresent(View.class))
                    .map(clz -> (View) clz.getAnnotation(View.class)).collect(Collectors.toList());
            if (views.size() > 0) {
                this.view = views.get(views.size() - 1);
            }
        }
        if (prefix.size() > 0) {
            this.prefix = prefix.stream().collect(Collectors.joining("_", "", "_"));
        } else {
            this.prefix = "";
        }
    }

    public Field getField() {
        return field;
    }

    public String columnName() {
        return prefix + this.column.name();
    }

    public String selectName() {
        return view.value() + "." + this.column.name() + " " + columnName();
    }

}
