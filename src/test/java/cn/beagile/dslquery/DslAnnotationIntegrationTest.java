package cn.beagile.dslquery;

import cn.beagile.dslquery.annotation.DslColumn;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.sql.ResultSet;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 集成测试：验证自定义注解在实际数据库操作中的功能
 */
@SpringBootTest
@Sql(scripts = "/dsl_annotation_test.sql")
public class DslAnnotationIntegrationTest {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    public void should_query_with_dsl_annotations() {
        SpringQueryExecutor queryExecutor = new SpringQueryExecutor(jdbcTemplate);
        DSLQuery<ProductView> query = new DSLQuery<>(queryExecutor, ProductView.class);

        List<ProductView> products = query.query();

        assertNotNull(products);
        assertTrue(products.size() > 0);

        ProductView product = products.get(0);
        assertNotNull(product.productId);
        assertNotNull(product.productName);
    }

    @Test
    public void should_query_with_where_clause() {
        SpringQueryExecutor queryExecutor = new SpringQueryExecutor(jdbcTemplate);
        DSLQuery<ProductView> query = new DSLQuery<>(queryExecutor, ProductView.class);

        query.where(WhereBuilder.where()
                .and()
                .equals("productName", "DSL Test Product")
                .build());

        List<ProductView> products = query.query();

        assertNotNull(products);
        assertEquals(1, products.size());
        assertEquals("DSL Test Product", products.get(0).productName);
    }

    @Test
    public void should_generate_correct_sql() {
        SpringQueryExecutor queryExecutor = new SpringQueryExecutor(jdbcTemplate);
        DSLQuery<ProductView> query = new DSLQuery<>(queryExecutor, ProductView.class);

        DSLSQLBuilder<ProductView> builder = new DSLSQLBuilder<>(query);
        String sql = builder.sql("");

        // 验证 SQL 使用了正确的列名（来自 @DslColumn 注解）
        assertTrue(sql.contains("product_id"));
        assertTrue(sql.contains("product_name"));
        assertTrue(sql.contains("products"));
    }

    @Test
    public void should_count_with_unique_column() {
        SpringQueryExecutor queryExecutor = new SpringQueryExecutor(jdbcTemplate);
        DSLQuery<ProductView> query = new DSLQuery<>(queryExecutor, ProductView.class);

        DSLSQLBuilder<ProductView> builder = new DSLSQLBuilder<>(query);
        SQLQuery sqlQuery = builder.build("");
        int count = queryExecutor.count(sqlQuery);

        assertTrue(count > 0);
        assertEquals(3, count);
    }

    // QueryExecutor 实现
    public static class SpringQueryExecutor implements QueryExecutor {
        private final NamedParameterJdbcTemplate jdbcTemplate;

        public SpringQueryExecutor(NamedParameterJdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        @Override
        public <T> List<T> list(Function<ResultSet, T> resultSetReader, SQLQuery sqlQuery) {
            String sql = sqlQuery.getSql();
            if (sqlQuery.getLimit() != null) {
                sql += " limit " + sqlQuery.getSkip() + "," + sqlQuery.getLimit();
            }
            return jdbcTemplate.query(sql, sqlQuery.getParams(), (rs, rowNum) -> resultSetReader.apply(rs));
        }

        @Override
        public int count(SQLQuery sqlQuery) {
            Long count = jdbcTemplate.queryForObject(sqlQuery.getCountSql(), sqlQuery.getParams(), Long.class);
            return count == null ? 0 : count.intValue();
        }

        @Override
        public String nullsOrder(NullsOrder nullsOrder) {
            return "";
        }
    }

    // 使用自定义注解的测试实体
    @View("products")
    public static class ProductView {
        @DslColumn(name = "product_id", unique = true)
        private Long productId;

        @DslColumn(name = "product_name")
        private String productName;

        @DslColumn(name = "price")
        private Double price;

        @DslColumn(name = "stock")
        private Integer stock;

        public Long getProductId() {
            return productId;
        }

        public String getProductName() {
            return productName;
        }

        public Double getPrice() {
            return price;
        }

        public Integer getStock() {
            return stock;
        }
    }
}
