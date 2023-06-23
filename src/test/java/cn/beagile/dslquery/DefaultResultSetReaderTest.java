package cn.beagile.dslquery;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultResultSetReaderTest {
    @Test
    public void should_read_string_field() throws SQLException {
        DefaultResultSetReader<QueryResultBean> defaultResultSetReader = new DefaultResultSetReader(QueryResultBean.class);
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("name")).thenReturn("bob");
        QueryResultBean result = defaultResultSetReader.apply(rs);
        assertNotNull(result);
        assertEquals("bob", result.getName());
    }

    @Test
    public void should_throw_when_sql_fail() throws SQLException {
        DefaultResultSetReader<QueryResultBean> defaultResultSetReader = new DefaultResultSetReader(QueryResultBean.class);
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("name")).thenThrow(new SQLException("sql error"));
        RuntimeException e = assertThrows(RuntimeException.class, () -> defaultResultSetReader.apply(rs));
        assertEquals("java.sql.SQLException: sql error", e.getMessage());
    }

    public static class QueryResultWithoutDefaultConstructorBean {
        @Column("name1")
        private String name;

        public String getName() {
            return name;
        }

        public QueryResultWithoutDefaultConstructorBean(String name) {
            this.name = name;
        }
    }

    @Test
    public void should_throw_when_class_init_fail() {
        DefaultResultSetReader<QueryResultWithoutDefaultConstructorBean> defaultResultSetReader = new DefaultResultSetReader(QueryResultWithoutDefaultConstructorBean.class);
        ResultSet rs = mock(ResultSet.class);
        RuntimeException e = assertThrows(RuntimeException.class, () -> defaultResultSetReader.apply(rs));
        assertEquals("Can not create instance of cn.beagile.dslquery.DefaultResultSetReaderTest$QueryResultWithoutDefaultConstructorBean", e.getMessage());
    }

    public static class QueryResultWithAliasBean {
        @Column("name1")
        private String name;

        public String getName() {
            return name;
        }
    }

    @Test
    public void should_read_string_field_with_alias() throws SQLException {
        DefaultResultSetReader<QueryResultWithAliasBean> defaultResultSetReader = new DefaultResultSetReader(QueryResultWithAliasBean.class);
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("name1")).thenReturn("bob");
        QueryResultWithAliasBean result = defaultResultSetReader.apply(rs);
        assertNotNull(result);
        assertEquals("bob", result.getName());
    }

    @Test
    public void should_read_int_field() throws SQLException {
        DefaultResultSetReader<QueryResultWithIntFieldBean> defaultResultSetReader = new DefaultResultSetReader(QueryResultWithIntFieldBean.class);
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt("age")).thenReturn(18);
        QueryResultWithIntFieldBean result = defaultResultSetReader.apply(rs);
        assertNotNull(result);
        assertEquals(18, result.getAge());
    }

    public static class QueryResultWithDecimalFieldBean {
        @Column("weight")
        private BigDecimal weight;

        public BigDecimal getWeight() {
            return weight;
        }
    }

    @Test
    public void should_read_decimal_field() throws SQLException {
        DefaultResultSetReader<QueryResultWithDecimalFieldBean> defaultResultSetReader = new DefaultResultSetReader(QueryResultWithDecimalFieldBean.class);
        ResultSet rs = mock(ResultSet.class);
        when(rs.getBigDecimal("weight")).thenReturn(new BigDecimal("18.001"));
        QueryResultWithDecimalFieldBean result = defaultResultSetReader.apply(rs);
        assertNotNull(result);
        assertEquals(new BigDecimal("18.001"), result.getWeight());
    }

    public static class QueryResultWithLongFieldBean {
        @Column("weight")
        private Long weight;

        public Long getWeight() {
            return weight;
        }
    }

    @Test
    public void should_read_long_field() throws SQLException {
        DefaultResultSetReader<QueryResultWithLongFieldBean> defaultResultSetReader = new DefaultResultSetReader(QueryResultWithLongFieldBean.class);
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("weight")).thenReturn(18L);
        QueryResultWithLongFieldBean result = defaultResultSetReader.apply(rs);
        assertNotNull(result);
        assertEquals(18L, result.getWeight());
    }

    public static class QueryResultWithInstantFieldBean {
        @Column("created_at")
        private Instant createdAt;

    }

    @Test
    public void should_read_instant_field() throws SQLException {
        DefaultResultSetReader<QueryResultWithInstantFieldBean> defaultResultSetReader = new DefaultResultSetReader(QueryResultWithInstantFieldBean.class);
        ResultSet rs = mock(ResultSet.class);
        Timestamp time = new Timestamp(18L);
        when(rs.getTimestamp("created_at")).thenReturn(time);
        QueryResultWithInstantFieldBean result = defaultResultSetReader.apply(rs);
        assertNotNull(result);
        assertEquals(18L, result.createdAt.toEpochMilli());
    }

    public static class QueryResultWithTimestampFieldBean {
        @Column("created_at")
        private Timestamp createdAt;

    }

    @Test
    public void should_read_timestamp_field() throws SQLException {
        DefaultResultSetReader<QueryResultWithTimestampFieldBean> defaultResultSetReader = new DefaultResultSetReader(QueryResultWithTimestampFieldBean.class);
        ResultSet rs = mock(ResultSet.class);
        Timestamp time = new Timestamp(18L);
        when(rs.getTimestamp("created_at")).thenReturn(time);
        QueryResultWithTimestampFieldBean result = defaultResultSetReader.apply(rs);
        assertNotNull(result);
        assertEquals(18L, result.createdAt.getTime());
    }
}
