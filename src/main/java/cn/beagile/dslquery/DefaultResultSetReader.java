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
import java.util.HashMap;
import java.util.Map;
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
        COLUMN_READER_MAP.put(Instant.class, (rs, columnName) -> rs.getTimestamp(columnName).toInstant());
        COLUMN_READER_MAP.put(Timestamp.class, ResultSet::getTimestamp);
    }

    private final ResultBean resultBean;
    private final ColumnFields columnFields;

    public DefaultResultSetReader(ResultBean resultBean) {
        this.resultBean = resultBean;
        columnFields = new ColumnFields(resultBean.getClazz());
    }

    @Override
    public T apply(ResultSet resultSet) {
        return (T) newInstance(resultSet, resultBean.getClazz());
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
                .filter(field -> !resultBean.ignored(field))
                .forEach(field -> setEmbeddedFieldValue(resultSet, result, field));
    }

    private void setPrimitiveFields(ResultSet resultSet, Class clz, Object result) {
        Stream.of(clz.getDeclaredFields())
                .filter(getColumns()::hasField)
                .forEach(field -> setFieldValue(resultSet, result, field));
    }

    private FieldsWithColumns getColumns() {
        return resultBean.getFieldsWithColumns();
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
        String fieldValue = resultSet.getString(getFieldColumnName(field));
        if (fieldValue != null) {
            new ReflectFieldSetter(result, field, new Gson().fromJson(fieldValue, field.getType())).set();
        }
    }

    private void readPrimitive(ResultSet resultSet, Object result, Field field, ColumnFieldReader columnFieldReader) throws SQLException {
        Object value = columnFieldReader.readValue(resultSet, getFieldColumnName(field));
        if (resultSet.wasNull()) {
            return;
        }
        new ReflectFieldSetter(result, field, value).set();
    }

    private String getFieldColumnName(Field field) {
        ColumnField columnField = columnFields.findField(field);
        return columnField.alias();
    }

    private void setEmbeddedFieldValue(ResultSet resultSet, Object result, Field field) {
        new ReflectFieldSetter(result, field, newInstance(resultSet, field.getType())).set();
    }
}
