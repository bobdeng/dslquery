package cn.beagile.dslquery;

import java.sql.ResultSet;
import java.sql.SQLException;

interface ColumnFieldReader {
    Object readValue(ResultSet resultSet, String columnName) throws SQLException;
}
