# 动态SQL Join特性

## 概述

除了传统的实体对象Join，DSLQuery现在支持通过`RawSQLBuilder`构建动态SQL作为Join源，将查询结果映射到指定对象。这在需要复杂子查询、聚合计算或临时视图时特别有用。

## 使用场景

- 需要Join一个带有聚合函数的子查询（如统计、求和）
- 需要Join一个复杂的UNION查询结果
- 需要Join一个带有动态过滤条件的临时结果集
- 需要Join一个跨多表的复杂查询结果

## 基本用法

### 1. 定义实体类

```java
@View("person")
public class Person {
    @Column(name = "id")
    private Integer id;
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "org_id")
    private Integer orgId;
    
    // 使用@DynamicJoin注解标记这是一个动态SQL Join
    @DynamicJoin(
        joinKey = "org_id",           // 当前表的关联字段
        targetKey = "id"               // 目标查询结果的关联字段
    )
    private OrgStats orgStats;         // 映射目标对象
}

// 映射目标对象（不需要@View注解）
public class OrgStats {
    @Column(name = "id")
    private Integer id;
    
    @Column(name = "total_employees")
    private Integer totalEmployees;
    
    @Column(name = "avg_salary")
    private Double avgSalary;
}
```

### 2. 构建动态SQL Join

```java
// 定义子查询的字段映射
List<SQLField> fields = List.of(
    new SQLField(
        new SQLField.ViewName("id"),
        new SQLField.SQLName("org.id"),
        Integer.class
    ),
    new SQLField(
        new SQLField.ViewName("totalEmployees"),
        new SQLField.SQLName("count(*)"),
        Integer.class
    ),
    new SQLField(
        new SQLField.ViewName("avgSalary"),
        new SQLField.SQLName("avg(salary)"),
        Double.class
    )
);

// 创建RawSQLBuilder（可以带DSL过滤条件）
RawSQLBuilder statsBuilder = new RawSQLBuilder(
    fields,
    "sort id asc",                    // 排序（可选）
    "(and(status eq active))"         // DSL过滤条件（可选）
);

// 定义子查询SQL
String subQuerySql = """
    select org.id, count(*) as total_employees, avg(salary) as avg_salary
    from employee
    join org on org.id = employee.org_id
    ${where}
    group by org.id
    """;

// 执行查询
List<Person> result = new DSLQuery<>(executor, Person.class)
    .dynamicJoin("orgStats", statsBuilder, subQuerySql)
    .where("(and(name contains John))")
    .query();

// 访问Join结果
result.forEach(person -> {
    System.out.println(person.getName() + " - " + 
        person.getOrgStats().getTotalEmployees() + " employees");
});
```

## 高级用法

### 1. 带参数的动态Join

```java
// 创建带过滤条件的RawSQLBuilder
RawSQLBuilder statsBuilder = new RawSQLBuilder(
    fields,
    "(and(status eq active)(year eq 2024))"
);

String subQuerySql = """
    select org.id, count(*) as total_employees, sum(salary) as total_salary
    from employee
    join org on org.id = employee.org_id
    ${where}
    group by org.id
    """;

List<Person> result = new DSLQuery<>(executor, Person.class)
    .dynamicJoin("orgStats", statsBuilder, subQuerySql)
    .query();
```

### 2. 多个动态Join

```java
@View("person")
public class Person {
    @Column(name = "id")
    private Integer id;
    
    @DynamicJoin(joinKey = "org_id", targetKey = "id")
    private OrgStats orgStats;
    
    @DynamicJoin(joinKey = "id", targetKey = "person_id")
    private PersonScore score;
}

// 可以同时使用多个动态Join
List<Person> result = new DSLQuery<>(executor, Person.class)
    .dynamicJoin("orgStats", orgStatsBuilder, orgStatsSql)
    .dynamicJoin("score", scoreBuilder, scoreSql)
    .query();
```

### 3. 混合使用实体Join和动态Join

```java
@View("person")
@DeepJoinIncludes({"department"})  // 传统实体Join
public class Person {
    @Column(name = "id")
    private Integer id;
    
    // 传统实体Join
    @JoinColumn(name = "dept_id", referencedColumnName = "id")
    private Department department;
    
    // 动态SQL Join
    @DynamicJoin(joinKey = "org_id", targetKey = "id")
    private OrgStats orgStats;
}

List<Person> result = new DSLQuery<>(executor, Person.class)
    .dynamicJoin("orgStats", statsBuilder, statsSql)
    .where("(and(department.name eq Sales))")  // 可以查询实体Join字段
    .query();
```

### 4. 使用JoinOn追加条件

```java
@View("person")
public class Person {
    @Column(name = "tenant_id")
    private Long tenantId;
    
    @DynamicJoin(
        joinKey = "org_id",
        targetKey = "id"
    )
    @JoinOn("(and(tenantId eq @parent.tenantId))")  // 追加Join条件
    private OrgStats orgStats;
}

// 生成的SQL类似：
// left join (
//   select org.id, count(*) as total_employees
//   from employee
//   join org on org.id = employee.org_id
//   group by org.id
// ) orgStats_ on orgStats_.id = person.org_id
//   and orgStats_.tenant_id = person.tenant_id
```

## 注解说明

### @DynamicJoin

标记字段为动态SQL Join。

**属性：**
- `joinKey`: 当前表的关联字段名（必填）
- `targetKey`: 目标查询结果的关联字段名（必填）
- `joinType`: Join类型，默认为`LEFT`，可选`INNER`、`RIGHT`

```java
@DynamicJoin(
    joinKey = "org_id",
    targetKey = "id",
    joinType = JoinType.LEFT
)
private OrgStats orgStats;
```

## API说明

### DSLQuery.dynamicJoin()

```java
public DSLQuery<T> dynamicJoin(
    String fieldName,           // 实体类中的字段名
    RawSQLBuilder builder,      // 动态SQL构建器
    String subQuerySql          // 子查询SQL（支持${where}占位符）
)
```

**参数说明：**
- `fieldName`: 实体类中标记了`@DynamicJoin`的字段名
- `builder`: RawSQLBuilder实例，用于构建子查询的WHERE条件和参数
- `subQuerySql`: 子查询SQL模板，使用`${where}`占位符表示WHERE子句插入位置

**返回值：**
- 返回DSLQuery实例，支持链式调用

## 完整示例

```java
// 1. 定义实体
@View("person")
public class Person {
    @Column(name = "id")
    private Integer id;
    
    @Column(name = "name")
    private String name;
    
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
    
    @Column(name = "avg_salary")
    private Double avgSalary;
}

// 2. 构建查询
List<SQLField> fields = List.of(
    new SQLField(new SQLField.ViewName("id"), 
                 new SQLField.SQLName("org.id"), Integer.class),
    new SQLField(new SQLField.ViewName("totalEmployees"), 
                 new SQLField.SQLName("count(*)"), Integer.class),
    new SQLField(new SQLField.ViewName("avgSalary"), 
                 new SQLField.SQLName("avg(salary)"), Double.class)
);

RawSQLBuilder statsBuilder = new RawSQLBuilder(
    fields,
    "sort id asc",
    "(and(status eq active))"
);

String subQuerySql = """
    select org.id, count(*) as total_employees, avg(salary) as avg_salary
    from employee
    join org on org.id = employee.org_id
    ${where}
    group by org.id
    """;

// 3. 执行查询
List<Person> result = new DSLQuery<>(executor, Person.class)
    .dynamicJoin("orgStats", statsBuilder, subQuerySql)
    .where("(and(name contains John))")
    .sort("name asc")
    .limit(10)
    .query();

// 4. 使用结果
result.forEach(person -> {
    System.out.printf("%s works in org with %d employees (avg salary: %.2f)%n",
        person.getName(),
        person.getOrgStats().getTotalEmployees(),
        person.getOrgStats().getAvgSalary()
    );
});
```

## 生成的SQL示例

上述查询会生成类似以下的SQL：

```sql
select 
    person.id id_,
    person.name name_,
    person.org_id orgId_,
    orgStats_.id orgStats_id_,
    orgStats_.total_employees orgStats_totalEmployees_,
    orgStats_.avg_salary orgStats_avgSalary_
from person
left join (
    select org.id, count(*) as total_employees, avg(salary) as avg_salary
    from employee
    join org on org.id = employee.org_id
    where org.status = :j0_p0
    group by org.id
    order by org.id asc
) orgStats_ on orgStats_.id = person.org_id
where person.name like :p0
order by person.name asc
limit 10
```

## 注意事项

1. **性能考虑**：动态Join会生成子查询，注意子查询的性能优化
2. **字段映射**：确保SQLField的定义与子查询的SELECT字段一致
3. **参数绑定**：RawSQLBuilder的DSL条件会自动生成参数绑定，避免SQL注入
4. **别名冲突**：系统会自动为子查询生成别名（如`orgStats_`），避免与主表冲突
5. **NULL处理**：动态Join默认使用LEFT JOIN，关联不到的记录字段值为null

## 限制

1. 动态Join不支持OneToMany关系
2. 动态Join不支持深度嵌套（即动态Join的结果对象不能再包含Join字段）
3. 子查询SQL必须包含targetKey指定的字段
