package cn.beagile.dslquery;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

class DefaultResultSetReader<T> implements Function<ResultSet, T> {
    private Class<T> queryResultBeanClass;
    private static final Map<Class, ColumnFieldReader> COLUMN_READER_MAP = new HashMap<>();

    static {
        COLUMN_READER_MAP.put(Integer.class, ResultSet::getInt);
        COLUMN_READER_MAP.put(int.class, ResultSet::getInt);
        COLUMN_READER_MAP.put(String.class, ResultSet::getString);
        COLUMN_READER_MAP.put(BigDecimal.class, ResultSet::getBigDecimal);
        COLUMN_READER_MAP.put(Long.class, ResultSet::getLong);
        COLUMN_READER_MAP.put(long.class, ResultSet::getLong);
        COLUMN_READER_MAP.put(Instant.class, (rs, columnName) -> rs.getTimestamp(columnName).toInstant());
        COLUMN_READER_MAP.put(Timestamp.class, ResultSet::getTimestamp);
    }

    public DefaultResultSetReader(Class<T> queryResultBeanClass) {

        this.queryResultBeanClass = queryResultBeanClass;
    }

    @Override
    public T apply(ResultSet resultSet) {
        final T result;
        try {
            result = this.queryResultBeanClass.newInstance();
            Stream.of(this.queryResultBeanClass.getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(Column.class))
                    .forEach(field -> setFieldValue(resultSet, result, field));
            return result;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Can not create instance of " + this.queryResultBeanClass.getName() + "");
        }


    }

    private void setFieldValue(ResultSet resultSet, T result, Field field) {
        Column column = field.getAnnotation(Column.class);
        try {
            Object value = COLUMN_READER_MAP.get(field.getType()).readValue(resultSet, column.value());
            new ReflectFieldSetter(result, field, value).set();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
