-- 测试数据：用于 DslAnnotationIntegrationTest

-- 创建表
CREATE TABLE IF NOT EXISTS products (
    product_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_name VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2),
    stock INT DEFAULT 0
);

-- 先清空测试数据
DELETE FROM products WHERE product_name LIKE 'DSL %';

-- 插入测试数据
INSERT INTO products (product_name, price, stock) VALUES
('DSL Test Product', 99.99, 100),
('DSL Another Product', 149.99, 50),
('DSL Third Product', 199.99, 25);
