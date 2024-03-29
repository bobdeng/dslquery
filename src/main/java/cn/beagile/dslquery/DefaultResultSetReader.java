package cn.beagile.dslquery;

import com.google.gson.Gson;

import javax.persistence.Embedded;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

class DefaultResultSetReader<T> implements Function<ResultSet, T> {
    private static final Map<Class, ColumnFieldReader> COLUMN_READER_MAP = new HashMap<>();

    static {
        COLUMN_READER_MAP.put(Integer.class, ResultSet::getInt);
        COLUMN_READER_MAP.put(int.class, ResultSet::getInt);
        COLUMN_READER_MAP.put(Boolean.class, ResultSet::getBoolean);
        COLUMN_READER_MAP.put(boolean.class, ResultSet::getBoolean);
        COLUMN_READER_MAP.put(String.class, ResultSet::getString);
        COLUMN_READER_MAP.put(BigDecimal.class, ResultSet::getBigDecimal);
        COLUMN_READER_MAP.put(Long.class, ResultSet::getLong);
        COLUMN_READER_MAP.put(long.class, ResultSet::getLong);
        COLUMN_READER_MAP.put(Instant.class, (rs, columnName) -> Optional.ofNullable(rs.getTimestamp(columnName)).map(Timestamp::toInstant).orElse(null));
        COLUMN_READER_MAP.put(Timestamp.class, ResultSet::getTimestamp);
    }

    private final ColumnFields columnFields;
    private final Class queryClass;
    private Stack<Field> parents = new Stack<>();

    public <T> DefaultResultSetReader(DSLQuery<T> dslQuery) {
        this.queryClass = dslQuery.getQueryResultClass();
        columnFields = new ColumnFields(dslQuery);
    }

    @Override
    public T apply(ResultSet resultSet) {
        return (T) newInstance(resultSet, queryClass);
    }

    private Object newInstance(ResultSet resultSet, Class clz) {
        try {
            final Object result = clz.newInstance();
            setPrimitiveFields(resultSet, clz, result);
            setEmbeddedFields(resultSet, clz, result);
            setJoinedFields(resultSet, clz, result);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setEmbeddedFields(ResultSet resultSet, Class clz, Object result) {
        Stream.of(clz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Embedded.class))
                .forEach(field -> setEmbeddedFieldValue(resultSet, result, field));
    }

    private void setJoinedFields(ResultSet resultSet, Class clz, Object result) {
        Stream.of(clz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(JoinColumn.class) || field.isAnnotationPresent(JoinColumns.class))
                .filter(columnFields::hasJoinField)
                .filter(field -> !columnFields.isIgnored(field))
                .forEach(field -> setEmbeddedFieldValue(resultSet, result, field));
    }

    private void setPrimitiveFields(ResultSet resultSet, Class clz, Object result) {
        Stream.of(clz.getDeclaredFields())
                .filter(field1 -> columnFields.hasField(field1, new ArrayList<>(parents)))
                .forEach(field -> setFieldValue(resultSet, result, field));
    }

    private void setFieldValue(ResultSet resultSet, Object result, Field field) {
        try {
            ColumnFieldReader columnFieldReader = COLUMN_READER_MAP.get(field.getType());
            if (columnFieldReader != null) {
                readPrimitive(resultSet, result, field, columnFieldReader);
                return;
            }
            readJson(resultSet, result, field);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void readJson(ResultSet resultSet, Object result, Field field) throws SQLException {
        String fieldValue = resultSet.getString(getFieldColumnName(field, new ArrayList<>(parents)));
        if (fieldValue != null) {
            ReflectField reflectField = new ReflectField(result, field, new Gson().fromJson(fieldValue, field.getType()));
            reflectField.set(new Gson().fromJson(fieldValue, field.getType()));
        }
    }

    private String getFieldColumnName(Field field, List<Field> parents) {
        return columnFields.findField(field, parents).alias();
    }


    private void readPrimitive(ResultSet resultSet, Object result, Field field, ColumnFieldReader columnFieldReader) throws SQLException {
        Object value = columnFieldReader.readValue(resultSet, getFieldColumnName(field, new ArrayList<>(parents)));
        if (resultSet.wasNull()) {
            return;
        }
        ReflectField reflectField = new ReflectField(result, field, value);
        reflectField.set(value);
    }

    private void setEmbeddedFieldValue(ResultSet resultSet, Object result, Field field) {
        parents.push(field);
        ReflectField reflectField = new ReflectField(result, field, newInstance(resultSet, field.getType()));
        reflectField.set(newInstance(resultSet, field.getType()));
        parents.pop();
    }
}
