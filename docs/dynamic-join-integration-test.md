# 动态Join集成测试说明

## 测试状态

已创建集成测试：`DynamicJoinIntegrationTest.java`

## 测试内容

### 测试1：基本动态Join查询
- 测试方法：`should_query_with_dynamic_join()`
- 功能：查询Person并通过动态SQL Join关联OrgStats
- 子查询：统计每个组织的人数
- 验证：
  - Person对象正确填充
  - OrgStats对象正确填充
  - 关联关系正确

### 测试2：带WHERE条件的动态Join
- 测试方法：`should_query_with_dynamic_join_and_where()`  
- 功能：在主查询中添加WHERE条件
- 验证：
  - 过滤条件生效
  - 动态Join仍然正常工作

## 运行测试

由于项目中存在其他测试文件的编译问题（RawSQLBuilder构造函数歧义），需要先修复这些问题才能运行集成测试。

### 临时运行方法

可以临时移除有问题的测试文件：

```bash
# 备份有问题的测试
cd src/test/java/cn/beagile/dslquery
mv SQLWhereTest.java SQLWhereTest.java.bak
mv IntegrationTest.java IntegrationTest.java.bak  
mv RawSQLBuilderTest.java RawSQLBuilderTest.java.bak
mv DynamicJoinTest.java DynamicJoinTest.java.bak

# 运行集成测试
./gradlew test --tests DynamicJoinIntegrationTest

# 恢复测试文件
for f in *.bak; do mv "$f" "${f%.bak}"; done
```

## 测试数据要求

集成测试需要以下数据库表和数据：

1. **person表**：包含id, name1, org_id字段
2. **org表**：包含id, name字段
3. **测试数据**：至少一条person记录关联到org

## 预期结果

测试通过后，控制台会输出：
- 生成的SQL语句
- 查询参数
- 查询结果（Person名称、Org名称、人数统计）

## 下一步

建议修复RawSQLBuilder构造函数的歧义问题，使所有测试都能正常编译和运行。
