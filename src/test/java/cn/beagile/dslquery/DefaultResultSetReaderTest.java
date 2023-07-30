package cn.beagile.dslquery;

import org.junit.jupiter.api.Test;

import javax.persistence.*;
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
        DefaultResultSetReader<QueryResultBean> defaultResultSetReader = new DefaultResultSetReader(new ResultBean(QueryResultBean.class));
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("name")).thenReturn("bob");
        QueryResultBean result = defaultResultSetReader.apply(rs);
        assertNotNull(result);
        assertEquals("bob", result.getName());
    }


    @Test
    public void should_read_json_field() throws SQLException {
        DefaultResultSetReader<QueryResultBean> defaultResultSetReader = new DefaultResultSetReader(new ResultBean(QueryResultBean.class));
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("json")).thenReturn("[{\"name\":\"bob\"}]");
        QueryResultBean result = defaultResultSetReader.apply(rs);
        assertNotNull(result.getJson());
        assertEquals("bob", result.getJson()[0].getName());
        assertNull(result.getJson()[0].getCode());
    }

    @Test
    public void should_read_embedding_field() throws SQLException {
        DefaultResultSetReader<QueryResultBean> defaultResultSetReader = new DefaultResultSetReader(new ResultBean(QueryResultBean.class));
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("embeddingField_name")).thenReturn("alice");
        QueryResultBean result = defaultResultSetReader.apply(rs);
        assertNotNull(result.getEmbeddingField());
        assertEquals("alice", result.getEmbeddingField().getName());
    }

    @Test
    public void should_read_null_json_field() throws SQLException {
        DefaultResultSetReader<QueryResultBean> defaultResultSetReader = new DefaultResultSetReader(new ResultBean(QueryResultBean.class));
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("json")).thenReturn(null);
        QueryResultBean result = defaultResultSetReader.apply(rs);
        assertNull(result.getJson());
    }

    @Test
    public void should_throw_when_sql_fail() throws SQLException {
        DefaultResultSetReader<QueryResultBean> defaultResultSetReader = new DefaultResultSetReader(new ResultBean(QueryResultBean.class));
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("name")).thenThrow(new SQLException("sql error"));
        RuntimeException e = assertThrows(RuntimeException.class, () -> defaultResultSetReader.apply(rs));
    }
    public static class QueryWithBoolean{
        @Column(name = "flag")
        private Boolean flag;
    }

    @Test
    public void should_read_boolean_json_field() throws SQLException {
        DefaultResultSetReader<QueryWithBoolean> defaultResultSetReader = new DefaultResultSetReader(new ResultBean(QueryWithBoolean.class));
        ResultSet rs = mock(ResultSet.class);
        when(rs.getBoolean("flag")).thenReturn(true);
        QueryWithBoolean result = defaultResultSetReader.apply(rs);
        assertTrue(result.flag);
    }
    @Test
    public void should_read_boolean_null_field() throws SQLException {
        DefaultResultSetReader<QueryWithBoolean> defaultResultSetReader = new DefaultResultSetReader(new ResultBean(QueryWithBoolean.class));
        ResultSet rs = mock(ResultSet.class);
        when(rs.wasNull()).thenReturn(true);
        when(rs.getBoolean("flag")).thenReturn(true);
        QueryWithBoolean result = defaultResultSetReader.apply(rs);
        assertNull(result.flag);
    }

    public static class QueryResultWithoutDefaultConstructorBean {
        @Column(name = "name1")
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
        DefaultResultSetReader<QueryResultWithoutDefaultConstructorBean> defaultResultSetReader = new DefaultResultSetReader(new ResultBean(QueryResultWithoutDefaultConstructorBean.class));
        ResultSet rs = mock(ResultSet.class);
        RuntimeException e = assertThrows(RuntimeException.class, () -> defaultResultSetReader.apply(rs));
    }

    public static class QueryResultWithAliasBean {
        @Column(name = "name1")
        private String name;

        public String getName() {
            return name;
        }
    }

    @Test
    public void should_read_string_field_with_alias() throws SQLException {
        DefaultResultSetReader<QueryResultWithAliasBean> defaultResultSetReader = new DefaultResultSetReader(new ResultBean(QueryResultWithAliasBean.class));
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("name")).thenReturn("bob");
        QueryResultWithAliasBean result = defaultResultSetReader.apply(rs);
        assertNotNull(result);
        assertEquals("bob", result.getName());
    }

    @Test
    public void should_read_int_field() throws SQLException {
        DefaultResultSetReader<QueryResultWithIntFieldBean> defaultResultSetReader = new DefaultResultSetReader(new ResultBean(QueryResultWithIntFieldBean.class));
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt("age")).thenReturn(18);
        QueryResultWithIntFieldBean result = defaultResultSetReader.apply(rs);
        assertNotNull(result);
        assertEquals(18, result.getAge());
    }

    public static class QueryResultWithDecimalFieldBean {
        @Column(name = "weight")
        private BigDecimal weight;

        public BigDecimal getWeight() {
            return weight;
        }
    }

    @Test
    public void should_read_decimal_field() throws SQLException {
        DefaultResultSetReader<QueryResultWithDecimalFieldBean> defaultResultSetReader = new DefaultResultSetReader(new ResultBean(QueryResultWithDecimalFieldBean.class));
        ResultSet rs = mock(ResultSet.class);
        when(rs.getBigDecimal("weight")).thenReturn(new BigDecimal("18.001"));
        QueryResultWithDecimalFieldBean result = defaultResultSetReader.apply(rs);
        assertNotNull(result);
        assertEquals(new BigDecimal("18.001"), result.getWeight());
    }

    public static class QueryResultWithLongFieldBean {
        @Column(name = "weight")
        private Long weight;

        public Long getWeight() {
            return weight;
        }
    }

    @Test
    public void should_read_long_field() throws SQLException {
        DefaultResultSetReader<QueryResultWithLongFieldBean> defaultResultSetReader = new DefaultResultSetReader(new ResultBean(QueryResultWithLongFieldBean.class));
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("weight")).thenReturn(18L);
        QueryResultWithLongFieldBean result = defaultResultSetReader.apply(rs);
        assertNotNull(result);
        assertEquals(18L, result.getWeight());
    }

    public static class QueryResultWithInstantFieldBean {
        @Column(name = "created_at")
        private Instant createdAt;

    }

    @Test
    public void should_read_instant_field() throws SQLException {
        DefaultResultSetReader<QueryResultWithInstantFieldBean> defaultResultSetReader = new DefaultResultSetReader(new ResultBean(QueryResultWithInstantFieldBean.class));
        ResultSet rs = mock(ResultSet.class);
        Timestamp time = new Timestamp(18L);
        when(rs.getTimestamp("createdAt")).thenReturn(time);
        QueryResultWithInstantFieldBean result = defaultResultSetReader.apply(rs);
        assertNotNull(result);
        assertEquals(18L, result.createdAt.toEpochMilli());
    }

    public static class QueryResultWithTimestampFieldBean {
        @Column(name = "created_at")
        private Timestamp createdAt;

    }

    @Test
    public void should_read_timestamp_field() throws SQLException {
        DefaultResultSetReader<QueryResultWithTimestampFieldBean> defaultResultSetReader = new DefaultResultSetReader(new ResultBean(QueryResultWithTimestampFieldBean.class));
        ResultSet rs = mock(ResultSet.class);
        Timestamp time = new Timestamp(18L);
        when(rs.getTimestamp("createdAt")).thenReturn(time);
        QueryResultWithTimestampFieldBean result = defaultResultSetReader.apply(rs);
        assertNotNull(result);
        assertEquals(18L, result.createdAt.getTime());
    }

    public static class QueryBeanWithEmbeddedFieldAndOverride {

        @Embedded
        @AttributeOverrides({
                @AttributeOverride(name = "name", column = @Column(name = "embedded_field_name"))
        })
        private EmbeddingField embeddingField;

        public EmbeddingField getEmbeddingField() {
            return embeddingField;
        }

        public void setEmbeddingField(EmbeddingField embeddingField) {
            this.embeddingField = embeddingField;
        }

        @Embeddable
        public static class EmbeddingField {
            private String name;
            @Column(name = "code")
            private String code;
        }
    }

    @Test
    public void should_read_embedding_field_override_field() throws SQLException {
        DefaultResultSetReader<QueryBeanWithEmbeddedFieldAndOverride> defaultResultSetReader = new DefaultResultSetReader(new ResultBean(QueryBeanWithEmbeddedFieldAndOverride.class));
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("embeddingField_name")).thenReturn("alice");
        when(rs.getString("code")).thenReturn("123456");
        QueryBeanWithEmbeddedFieldAndOverride result = defaultResultSetReader.apply(rs);
        assertNotNull(result.getEmbeddingField());
        assertEquals("alice", result.getEmbeddingField().name);
        assertNull(result.getEmbeddingField().code);
    }
    @Ignores({"fieldWithJoin.joined"})
    public static class QueryBeanWithIgnoredField {

        @JoinColumn(name = "org_id", referencedColumnName = "id")
        private FieldWithJoin fieldWithJoin;

    }
    public static class FieldWithJoin{
        @JoinColumn(name = "org_id", referencedColumnName = "id")
        Joined joined;
    }
    public static class Joined{
       private String name;
    }
    @Test
    public void should_not_read_ignored_field() throws SQLException {
        DefaultResultSetReader<QueryBeanWithIgnoredField> defaultResultSetReader = new DefaultResultSetReader(new ResultBean(QueryBeanWithIgnoredField.class));
        ResultSet rs = mock(ResultSet.class);
        QueryBeanWithIgnoredField result = defaultResultSetReader.apply(rs);
        assertNull(result.fieldWithJoin.joined);
    }
}
