package cn.beagile.dslquery;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
}
