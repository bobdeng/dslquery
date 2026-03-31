# 动态SQL Join功能实现总结

## 已实现的功能

1. **@DynamicJoin注解**：用于标记动态SQL Join字段
   - `joinKey`: 当前表的关联字段
   - `targetKey`: 目标查询结果的关联字段  
   - `joinType`: Join类型（LEFT/INNER/RIGHT）

2. **DSLQuery.dynamicJoin()方法**：配置动态Join
   - 接收字段名、RawSQLBuilder和子查询SQL
   - 支持链式调用

3. **DynamicJoinField类**：处理动态Join的SQL生成
   - 自动读取目标类的字段
   - 生成子查询SQL
   - 处理参数绑定和重命名

4. **ColumnFields集成**：
   - 识别@DynamicJoin注解的字段
   - 将动态Join字段添加到SELECT列表
   - 生成JOIN语句

## 核心实现文件

- `DynamicJoin.java` - 注解定义
- `DynamicJoinConfig.java` - 配置类
- `DynamicJoinField.java` - 动态Join字段处理
- `DSLQuery.java` - 添加dynamicJoin()方法
- `ColumnFields.java` - 集成动态Join支持

## 使用示例

```java
// 定义实体
@View("person")
public class Person {
    @Column(name = "id")
    private Integer id;
    
    @Column(name = "org_id")
    private Integer orgId;
    
    @DynamicJoin(joinKey = "org_id", targetKey = "id")
    private OrgStats orgStats;
}

public class OrgStats {
    @Column(name = "id")
    private Integer id;
    
    @Column(name = "total_employees")
    private Integer totalEmployees;
}

// 构建查询
List<SQLField> fields = List.of(
    new SQLField(new SQLField.ViewName("id"), 
                 new SQLField.SQLName("org.id"), Integer.class),
    new SQLField(new SQLField.ViewName("totalEmployees"), 
                 new SQLField.SQLName("count(*)"), Integer.class)
);

RawSQLBuilder statsBuilder = new RawSQLBuilder(fields);

String subQuerySql = "select org.id, count(*) as total_employees from employee group by org.id";

List<Person> result = new DSLQuery<>(executor, Person.class)
    .dynamicJoin("orgStats", statsBuilder, subQuerySql)
    .query();
```

## 生成的SQL

```sql
select person.id id_,person.org_id orgId_,orgStats_.id orgStats_id_,orgStats_.total_employees orgStats_totalEmployees_ 
from person
left join (
  select org.id, count(*) as total_employees 
  from employee 
  group by org.id
) orgStats_ on orgStats_.id = person.org_id
```

## 测试

已通过测试：`SimpleDynamicJoinTest.should_generate_dynamic_join_sql_basic()`

## 注意事项

1. 目标类（如OrgStats）不需要@View注解
2. 子查询SQL支持${where}占位符
3. RawSQLBuilder的参数会自动重命名避免冲突（前缀：dj_字段名_）
4. 默认使用LEFT JOIN
5. 动态Join字段会自动添加到SELECT列表
