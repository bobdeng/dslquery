package cn.beagile.dslquery;

import com.google.gson.Gson;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
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

    private final Class<T> queryResultBeanClass;

    public DefaultResultSetReader(Class<T> queryResultBeanClass) {

        this.queryResultBeanClass = queryResultBeanClass;
    }

    @Override
    public T apply(ResultSet resultSet) {
        try {
            return (T) newInstance(resultSet, this.queryResultBeanClass, null);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Can not create instance of " + this.queryResultBeanClass.getName() + "");
        }


    }

    private Object newInstance(ResultSet resultSet, Class clz, AttributeOverrides attributeOverrides) throws InstantiationException, IllegalAccessException {
        final Object result;
        result = clz.newInstance();
        Stream.of(clz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Column.class) || isFieldOverride(attributeOverrides, field))
                .forEach(field -> setFieldValue(resultSet, result, field, getFieldColumn(attributeOverrides, field)));
        Stream.of(clz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Embedded.class))
                .forEach(field -> setEmbeddedFieldValue(resultSet, result, field));
        return result;
    }

    private Column getFieldColumn(AttributeOverrides attributeOverrides, Field field) {
        if (attributeOverrides == null) {
            return field.getAnnotation(Column.class);
        }
        return Stream.of(attributeOverrides.value()).filter(it -> it.name().equals(field.getName()))
                .map(AttributeOverride::column)
                .findFirst()
                .orElseGet(() -> field.getAnnotation(Column.class));
    }

    private boolean isFieldOverride(AttributeOverrides attributeOverrides, Field field) {
        if (attributeOverrides != null) {
            return Stream.of(attributeOverrides.value()).anyMatch(it -> it.name().equals(field.getName()));
        }
        return false;
    }

    private void setFieldValue(ResultSet resultSet, Object result, Field field, Column column) {
        try {
            ColumnFieldReader columnFieldReader = COLUMN_READER_MAP.get(field.getType());
            if (columnFieldReader != null) {
                readPrimitive(resultSet, result, field, column, columnFieldReader);
                return;
            }
            readJson(resultSet, result, field, column);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void readJson(ResultSet resultSet, Object result, Field field, Column column) throws SQLException {
        String fieldValue = resultSet.getString(column.name());
        if (fieldValue != null) {
            new ReflectFieldSetter(result, field, new Gson().fromJson(fieldValue, field.getType())).set();
        }
    }

    private void readPrimitive(ResultSet resultSet, Object result, Field field, Column column, ColumnFieldReader columnFieldReader) throws SQLException {
        Object value = columnFieldReader.readValue(resultSet, column.name());
        new ReflectFieldSetter(result, field, value).set();
    }

    private void setEmbeddedFieldValue(ResultSet resultSet, Object result, Field field) {
        try {
            AttributeOverrides attributeOverrides = field.getAnnotation(AttributeOverrides.class);
            new ReflectFieldSetter(result, field, newInstance(resultSet, field.getType(), attributeOverrides)).set();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
