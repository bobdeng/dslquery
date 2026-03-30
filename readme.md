[![](https://jitpack.io/v/bobdeng/dslquery.svg)](https://jitpack.io/#bobdeng/dslquery)
[![codecov](https://codecov.io/gh/bobdeng/dslquery/branch/main/graph/badge.svg?token=OZW1CQIQQ4)](https://codecov.io/gh/bobdeng/dslquery)

# DSLQuery

一个轻量级的Java DSL查询类库，提供类Lisp语法的DSL来构建数据库查询，自动生成SQL并映射结果到Java对象。

## 特性

- **简洁的DSL语法**：类Lisp的括号表达式语法，支持复杂嵌套条件
- **类型安全**：基于JPA注解的强类型映射
- **自动SQL生成**：自动将DSL转换为优化的SQL查询
- **深度关联查询**：支持多级JOIN和OneToMany关系
- **Join On扩展**：支持在关联ON子句中追加DSL条件
- **灵活配置**：支持字段忽略、深度关联控制、时区转换
- **分页支持**：内置分页查询功能
- **数据库无关**：通过QueryExecutor接口适配不同数据库

## 快速开始

### 添加依赖

Gradle:
```gradle
dependencies {
    implementation 'cn.beagile.lib:dslquery:1.0'
    implementation 'com.google.code.gson:gson:2.10.1'
    implementation 'jakarta.persistence:jakarta.persistence-api:2.2.1'
}
```

Maven:
```xml
<dependency>
    <groupId>cn.beagile.lib</groupId>
    <artifactId>dslquery</artifactId>
    <version>1.0</version>
</dependency>
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
</dependency>
<dependency>
    <groupId>jakarta.persistence</groupId>
    <artifactId>jakarta.persistence-api</artifactId>
    <version>2.2.1</version>
</dependency>
```

### 基本使用

#### 1. 定义实体类

```java
@View("person")
public class Person {
    @Column(name = "id")
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "age")
    private Integer age;
}
```

#### 2. 实现QueryExecutor

```java
public class SpringQueryExecutor implements QueryExecutor {
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
        return jdbcTemplate.query(sql, sqlQuery.getParams(),
            (rs, rowNum) -> resultSetReader.apply(rs));
    }

    @Override
    public int count(SQLQuery sqlQuery) {
        return jdbcTemplate.query(sqlQuery.getCountSql(), sqlQuery.getParams(),
            (rs, rowNum) -> rs.getInt(1)).get(0);
    }

    @Override
    public String nullsOrder(NullsOrder nullsOrder) {
        return switch (nullsOrder) {
            case NULL_FIRST -> "nulls first";
            case NULL_LAST -> "nulls last";
            default -> "";
        };
    }
}
```

#### 3. 执行查询

```java
QueryExecutor executor = new SpringQueryExecutor(jdbcTemplate);

// 简单查询
List<Person> result = new DSLQuery<>(executor, Person.class)
    .where("(and(name eq bob)(age gt 18))")
    .sort("age desc")
    .limit(10)
    .skip(0)
    .query();

// 分页查询
Paged<Person> paged = new DSLQuery<>(executor, Person.class)
    .where("(and(name contains John))")
    .limit(10)
    .skip(0)
    .pagedQuery();

int total = paged.total();
List<Person> data = paged.getResult();
```

## DSL语法

### 查询条件语法

基本格式：`(逻辑运算符(字段 操作符 值)(字段 操作符 值)...)`

```
(and(name eq bob)(age gt 18))
(or(name eq alice)(age lt 60))
```

### 支持的操作符

| 操作符 | 缩写 | SQL | 说明 | 示例 |
|--------|------|-----|------|------|
| equals | eq | = | 等于 | `(name eq bob)` |
| notequals | ne | != | 不等于 | `(age ne 18)` |
| greaterthan | gt | > | 大于 | `(age gt 18)` |
| greaterthanorequals | ge | >= | 大于等于 | `(age ge 18)` |
| lessthan | lt | < | 小于 | `(age lt 60)` |
| lessthanorequals | le | <= | 小于等于 | `(age le 60)` |
| startswith | sw | like 'value%' | 以...开头 | `(name sw John)` |
| endswith | ew | like '%value' | 以...结尾 | `(name ew smith)` |
| contains | ct | like '%value%' | 包含 | `(name ct John)` |
| isnull | isn | is null | 为空 | `(name isn)` |
| notnull | inn | is not null | 不为空 | `(name inn)` |
| in | in | in (...) | 在集合中 | `(age in [18,20,22])` |
| notin | ni | not in (...) | 不在集合中 | `(status ni [1,2])` |
| between | bt | between ... and ... | 在范围内 | `(age bt 18,60)` |

### 排序语法

```
sort fieldA desc,fieldB asc
```

### 嵌套条件

```
(and
  (name eq bob)
  (or
    (age gt 18)
    (age lt 60)
  )
)
```

## 注解说明

### 类级别注解

#### @View
指定数据库表或视图名称

```java
@View(value = "table_name", distinct = false)
public class Entity {
    // ...
}
```

#### @DeepJoinIncludes
指定深度关联路径，支持多级关联

```java
@View("person")
@DeepJoinIncludes({"org.area", "org.city.area"})
public class Person {
    @JoinColumn(name = "org_id", referencedColumnName = "id")
    private Org org;
}
```

#### @SelectIgnores
排除不需要查询的字段

```java
@View("person")
@SelectIgnores({"password", "org.secret"})
public class Person {
    // ...
}
```

### 字段级别注解

#### @Column
指定数据库列名（JPA标准注解）

```java
@Column(name = "db_column_name")
private String fieldName;
```

#### @DateFormat
用于Long/Instant字段的日期格式化

```java
@Column(name = "born_at")
@DateFormat("yyyy-MM-dd HH:mm:ss")
private Instant bornAt;
```

#### @Embedded
嵌入式对象（JPA标准注解）

```java
@Embedded
@AttributeOverrides({
    @AttributeOverride(name = "address", column = @Column(name = "address")),
    @AttributeOverride(name = "phone", column = @Column(name = "phone"))
})
private Contact contact;
```

#### @JoinColumn
关联字段（JPA标准注解）

```java
@JoinColumn(name = "org_id", referencedColumnName = "id")
private Org org;
```

#### @JoinOn
为关联的 `join on` 子句追加DSL条件

```java
@JoinColumn(name = "org_id", referencedColumnName = "id")
@JoinOn("(and(enabled eq true)(tenantId eq @parent.tenantId))")
private Org org;
```

支持以下作用域：

- 裸字段名或 `self.xxx`：当前join目标对象
- `parent.xxx`：当前join的上一级对象
- `root.xxx`：根查询对象
- `@fieldPath`：把值解释为字段引用，而不是绑定参数

#### @OneToMany
一对多关系（JPA标准注解）

```java
@OneToMany(targetEntity = Child.class)
@JoinColumn(name = "id", referencedColumnName = "parent_id")
private List<Child> children;
```

## 使用示例

### 基本查询

```java
// 简单条件查询
List<Person> result = new DSLQuery<>(executor, Person.class)
    .where("(and(name eq bob)(age gt 18))")
    .query();

// 多条件查询
List<Person> result = new DSLQuery<>(executor, Person.class)
    .where("(and(name eq bob))")
    .where("(and(age ge 18))")  // 多个where会用AND连接
    .query();

// 排序查询
List<Person> result = new DSLQuery<>(executor, Person.class)
    .sort("age desc,name asc")
    .query();

// 分页查询
List<Person> result = new DSLQuery<>(executor, Person.class)
    .limit(10)
    .skip(20)
    .query();
```

### 编程式构建条件

使用WhereBuilder可以通过编程方式构建查询条件：

```java
String where = new WhereBuilder()
    .and()
        .equals("name", "bob")
        .greaterthan("age", "18")
        .or()
            .startswith("email", "test")
            .contains("phone", "123")
        .prev()  // 返回上一级
    .build();

// 结果: (and(name eq bob)(age gt 18)(or(email sw test)(phone ct 123)))

List<Person> result = new DSLQuery<>(executor, Person.class)
    .where(where)
    .query();
```

### 嵌套对象查询

```java
@View("person")
public class Person {
    @Column(name = "name")
    private String name;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "address", column = @Column(name = "address")),
        @AttributeOverride(name = "phone", column = @Column(name = "phone"))
    })
    private Contact contact;
}

// 查询嵌套字段
List<Person> result = new DSLQuery<>(executor, Person.class)
    .where("(and(contact.address contains street))")
    .query();
```

### 关联查询

```java
@View("person")
@DeepJoinIncludes({"org.area"})  // 启用深度关联
public class Person {
    @Column(name = "name")
    private String name;

    @JoinColumn(name = "org_id", referencedColumnName = "id")
    private Org org;
}

@View("org")
public class Org {
    @Column(name = "name")
    private String name;

    @JoinColumn(name = "area_id", referencedColumnName = "id")
    private Area area;
}

// 查询关联字段
List<Person> result = new DSLQuery<>(executor, Person.class)
    .where("(and(org.area.name eq Beijing))")
    .query();

// 生成的SQL类似：
// select person.name, org.name, area.name
// from person
// left join org on org.id = person.org_id
// left join area on area.id = org.area_id
// where area.name = 'Beijing'
```

### 关联Join On扩展条件

```java
@View("person")
public class Person {
    @Column(name = "tenant_id")
    private Long tenantId;

    @JoinColumn(name = "org_id", referencedColumnName = "id")
    @JoinOn("(and(enabled eq true)(tenantId eq @parent.tenantId))")
    private Org org;
}

@View("org")
public class Org {
    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "enabled")
    private Boolean enabled;

    @Column(name = "type")
    private String type;
}

// 注解条件 + 运行时条件一起追加到ON
List<Person> result = new DSLQuery<>(executor, Person.class)
    .joinOn("org", "(and(type eq SALES))")
    .query();

// 生成的SQL类似：
// select person.tenant_id, org.tenant_id, org.enabled, org.type
// from person
// left join org on org.id = person.org_id
//   and org.enabled = true
//   and org.tenant_id = person.tenant_id
//   and org.type = 'SALES'
```

深层关联同样支持作用域字段引用：

```java
@View("org")
public class Org {
    @Column(name = "tenant_id")
    private Long tenantId;

    @JoinColumn(name = "area_id", referencedColumnName = "id")
    @JoinOn("(and(code eq @root.areaCode)(tenantId eq @parent.tenantId))")
    private Area area;
}
```

### OneToMany关系

```java
@View("person")
public class Person {
    @Column(name = "id")
    private Integer id;

    @Column(name = "name")
    private String name;

    @OneToMany(targetEntity = Child.class)
    @JoinColumn(name = "id", referencedColumnName = "person_id")
    private List<Child> children;
}

@View("children")
public class Child {
    @Column(name = "id")
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "person_id")
    private Integer personId;
}

// 自动加载OneToMany关系
List<Person> result = new DSLQuery<>(executor, Person.class)
    .query();
// children字段会被自动填充
```

### 日期时间查询

```java
@View("person")
public class Person {
    @Column(name = "born_at")
    @DateFormat("yyyy-MM-dd HH:mm:ss")
    private Instant bornAt;
}

// 查询时指定时区偏移（东8区为-8）
List<Person> result = new DSLQuery<>(executor, Person.class)
    .timezoneOffset(-8)
    .where("(and(bornAt gt 1980-01-01 00:00:00))")
    .query();
```

### 高级操作符使用

```java
// IN操作符
List<Person> result = new DSLQuery<>(executor, Person.class)
    .where("(and(age in [18,20,22,25]))")
    .query();

// BETWEEN操作符
List<Person> result = new DSLQuery<>(executor, Person.class)
    .where("(and(age bt 18,60))")
    .query();

// IS NULL操作符
List<Person> result = new DSLQuery<>(executor, Person.class)
    .where("(and(address isn))")
    .query();

// NOT NULL操作符
List<Person> result = new DSLQuery<>(executor, Person.class)
    .where("(and(email inn))")
    .query();

// 字符串模糊匹配
List<Person> result = new DSLQuery<>(executor, Person.class)
    .where("(or(name sw John)(name ew smith)(email ct test))")
    .query();
```

### 原始SQL查询

对于复杂的SQL查询，可以使用RawSQLBuilder：

```java
// 定义字段映射
List<SQLField> fields = List.of(
    new SQLField(
        new SQLField.ViewName("name"),
        new SQLField.SQLName("person.name"),
        String.class
    )
);

// 创建RawSQLBuilder
RawSQLBuilder builder = new RawSQLBuilder(fields, "(and(name eq John))");

// 定义SQL模板（使用${where}占位符）
String sql = "select person.name from person ${where} group by person.name";
String countSql = "select count(*) from person";

// 生成SQLQuery
Paging page = new Paging(0, 10);
SQLQuery sqlQuery = builder.toSQLQuery(sql, countSql, page);

// 执行查询
List<Map<String, Object>> result = executor.list(resultSetReader, sqlQuery);
```

### 字段忽略

```java
// 方式1：使用注解
@View("person")
@SelectIgnores({"password", "org.secret"})
public class Person {
    @Column(name = "password")
    private String password;  // 不会被查询

    @JoinColumn(name = "org_id", referencedColumnName = "id")
    private Org org;
}

// 方式2：动态指定
List<Person> result = new DSLQuery<>(executor, Person.class)
    .selectIgnores("password", "org.secret")
    .query();
```

### 深度关联控制

```java
// 方式1：使用注解
@View("person")
@DeepJoinIncludes({"org.area", "org.city"})
public class Person {
    @JoinColumn(name = "org_id", referencedColumnName = "id")
    private Org org;
}

// 方式2：动态指定
List<Person> result = new DSLQuery<>(executor, Person.class)
    .deepJoinIncludes("org.area", "org.city")
    .query();
```

## 高级特性

### NULL值排序

```java
// 指定NULL值排序方式
List<Person> result = new DSLQuery<>(executor, Person.class, NullsOrder.NULL_FIRST)
    .sort("age desc")
    .query();
```

### 类型转换

DSLQuery自动处理以下类型转换：
- 基本类型：Integer, Long, Float, Double, Boolean, String
- 日期时间：Instant, Timestamp
- JSON字段：自动使用Gson反序列化
- BigDecimal

### URL解码

DSL字符串中的值会自动进行URL解码，方便从HTTP请求参数中直接使用：

```java
// URL参数: where=(and(name%20eq%20bob))
String where = request.getParameter("where");
List<Person> result = new DSLQuery<>(executor, Person.class)
    .where(where)  // 自动解码为: (and(name eq bob))
    .query();
```

## 最佳实践

### 1. 前后端协作

前端通过URL参数传递DSL查询条件：

```javascript
// 前端代码
const params = {
    where: '(and(name eq bob)(age gt 18))',
    sort: 'age desc',
    limit: 10,
    skip: 0
};

fetch(`/api/persons?${new URLSearchParams(params)}`);
```

```java
// 后端代码
@GetMapping("/api/persons")
public Paged<Person> getPersons(
    @RequestParam(required = false) String where,
    @RequestParam(required = false) String sort,
    @RequestParam(defaultValue = "10") Integer limit,
    @RequestParam(defaultValue = "0") Integer skip
) {
    return new DSLQuery<>(executor, Person.class)
        .where(where)
        .sort(sort)
        .limit(limit)
        .skip(skip)
        .pagedQuery();
}
```

### 2. 安全性考虑

- DSLQuery使用参数化查询，自动防止SQL注入
- 建议在服务端验证字段名，防止查询敏感字段
- 使用@SelectIgnores排除敏感字段

### 3. 性能优化

- 使用@DeepJoinIncludes仅加载需要的关联数据
- 使用@SelectIgnores排除不需要的字段
- 合理使用分页，避免一次性加载大量数据
- OneToMany关系会执行额外查询，按需使用

### 4. 错误处理

```java
try {
    List<Person> result = new DSLQuery<>(executor, Person.class)
        .where(where)
        .query();
} catch (IllegalArgumentException e) {
    // DSL语法错误
    log.error("Invalid DSL syntax: {}", where, e);
} catch (Exception e) {
    // 其他错误
    log.error("Query failed", e);
}
```

## 技术要求

- Java 8+
- Gson 2.10.1+
- Jakarta Persistence API 2.2.1+

## 贡献

欢迎提交Issue和Pull Request。

## 相关链接

- 项目主页：https://github.com/bobdeng/dslquery
- 问题反馈：https://github.com/bobdeng/dslquery/issues
