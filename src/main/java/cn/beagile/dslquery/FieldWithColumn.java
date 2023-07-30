package cn.beagile.dslquery;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FieldWithColumn {
    private final Field field;
    private final boolean embedded;
    private final Column column;
    private final String prefix;
    private final List<String> prefixes;
    private View view;

    public FieldWithColumn(Field field, Optional<AttributeOverride> attributeOverride, Stack<String> prefix, Stack<Class> classStack, boolean embedded) {
        this.field = field;
        this.embedded = embedded;
        this.column = attributeOverride.map(AttributeOverride::column)
                .orElseGet(() -> field.getAnnotation(Column.class));
        setView(field, classStack);
        this.prefix = setPrefix(prefix);
        this.prefixes = prefix.stream().collect(Collectors.toList());
    }


    private String setPrefix(Stack<String> prefixStack) {
        if (prefixStack.size() > 0) {
            return prefixStack.stream().collect(Collectors.joining("_", "", "_"));
        }
        return "";
    }

    private String getTableAlias() {
        if (prefixes.size() > 0) {
            return prefixes.stream().limit(prefixes.size()).collect(Collectors.joining("_", "", ""));
        }
        return view.value();
    }


    private void setView(Field field, Stack<Class> classStack) {
        if (!embedded) {
            this.view = field.getDeclaringClass().getAnnotation(View.class);
            if (this.view != null) {
                return;
            }
        }
        tryFindLastClassHasViewInStack(classStack);
    }

    private void tryFindLastClassHasViewInStack(Stack<Class> classStack) {
        List<View> views = classStack.stream()
                .limit(1)
                .filter(clz -> clz.isAnnotationPresent(View.class))
                .map(clz -> (View) clz.getAnnotation(View.class))
                .collect(Collectors.toList());
        if (views.size() > 0) {
            this.view = views.get(views.size() - 1);
        }
    }

    public Field getField() {
        return field;
    }

    public String whereName() {
        if (view == null) {
            return column.name();
        }
        return String.format("%s.%s", view.value(), this.column.name());
    }

    public String columnName() {
        return String.format("%s%s", prefix, this.column.name());
    }

    public String selectName() {
        return getTableAlias() + "." + this.column.name() + " " + columnName();
    }

    public String getFieldName() {
        if (prefixes.size() == 0) {
            return Stream.of(view.value(), column.name()).collect(Collectors.joining("."));
        }
        return Stream.of(prefixes.stream().collect(Collectors.joining("_")), column.name()).collect(Collectors.joining("."));
    }
}
