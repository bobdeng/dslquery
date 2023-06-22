package cn.beagile.dslquery;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

public class DefaultResultSetReader<T> implements Function<ResultSet, T> {
    private Class<T> queryResultBeanClass;
    private static final Map<Class, ColumnFieldReader> COLUMN_FIELD_READER_HASH_MAP = new HashMap<>();

    static {
        COLUMN_FIELD_READER_HASH_MAP.put(Integer.class, ResultSet::getInt);
        COLUMN_FIELD_READER_HASH_MAP.put(int.class, ResultSet::getInt);
        COLUMN_FIELD_READER_HASH_MAP.put(String.class, ResultSet::getString);
        COLUMN_FIELD_READER_HASH_MAP.put(BigDecimal.class, ResultSet::getBigDecimal);
    }

    public DefaultResultSetReader(Class<T> queryResultBeanClass) {

        this.queryResultBeanClass = queryResultBeanClass;
    }

    @Override
    public T apply(ResultSet resultSet) {
        try {
            final T result = this.queryResultBeanClass.newInstance();
            Stream.of(this.queryResultBeanClass.getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(Column.class))
                    .forEach(field -> setFieldValue(resultSet, result, field));
            return result;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setFieldValue(ResultSet resultSet, T result, Field field) {
        try {
            Column column = field.getAnnotation(Column.class);
            field.setAccessible(true);
            field.set(result, COLUMN_FIELD_READER_HASH_MAP.get(field.getType()).readValue(resultSet, column.value()));
            field.setAccessible(false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
