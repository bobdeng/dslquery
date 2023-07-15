[![](https://jitpack.io/v/bobdeng/dslquery.svg)](https://jitpack.io/#bobdeng/dslquery)
[![codecov](https://codecov.io/gh/bobdeng/dslquery/branch/main/graph/badge.svg?token=OZW1CQIQQ4)](https://codecov.io/gh/bobdeng/dslquery)

# 这是什么

建立一套规则，让前端通过规则传递查询条件给服务端，在服务端进行解析，生成查询条件，然后查询数据库，返回结果。

# 使用方法

## 定义查询条件

- 查询条件 示例：

```
(or(and(field equal value)(fieldb greaterthan value))(or(fieldc lessthanorequal value)(fieldd notequal value)))
```

支持的运算符有 equal notequal greaterthan greaterthanorequal lessthan lessthanorequal startswith endswith contains
isnull
notnull

- 分页条件

  limit 最大返回条数 skip 跳过条数
- 排序条件

  sort fieldA desc,fieldB asc

## 服务端解析

- 实现查询执行的接口

```java
public interface QueryExecutor {
    <T> List<T> list(SQLQuery sqlQuery, Function<ResultSet, T> resultSetReader);

    int count(SQLQuery sqlQuery);
}
```

可查看测试代码里的IntegrationTest里的实现

- 编写查询结果Bean

```java

@View("view_example")
public class QueryResultBean {
    @Column("field_a")
    private String fieldA;
    @Column("long_field")
    private Long longField;
    @Column("long_field_as_timestamp")
    @DateFormat("yyyy-MM-dd HH:mm:ss")
    private Long longFieldAsTimestamp;
    @Column("instant_field")
    @DateFormat("yyyy-MM-dd HH:mm:ss")
    private Instant instantField;
    @Column("float_field")
    private Float floatField;
    @Column("double_field")
    private Double doubleField;
    //getter setter
}
```

```java
//使用方法
public void listA(){
        List<QueryResultBean> result=new DSLQuery(queryExecutor,QueryResultBean.class)
        .timezoneOffset(-8)
        .where("(or(and(fieldA equal value)(fieldB greaterthan value))(or(fieldB equal value)(fieldB notequal value)))")
        .where("(and(fieldA equal 100))")
        .limit(10).skip(0)
        .sort("fieldA desc,fieldB asc")
        .query();
        }
//分页查询
public void listA(){
        Paged<QueryResultBean> result=new DSLQuery(queryExecutor,QueryResultBean.class)
        .timezoneOffset(-8)
        .where("(or(and(fieldA equal value)(fieldB greaterthan value))(or(fieldB equal value)(fieldB notequal value)))")
        .where("(and(fieldA equal 100))")
        .limit(10).skip(0)
        .sort("fieldA desc,fieldB asc")
        .pagedQuery();
        }
```

- 生成查询条件 new WhereBuilder().build();
- 生成排序条件 new SortBuilder().build();

# 内嵌类使用

- Json内嵌类
  如果是Json类型，以字符串读取并转换。如：
    ```java
  @Column(name="json_field")
  private JsonField jsonField;
  ```
- 其他内嵌类
  其他类型需要映射字段到新的类，需要Embedded注解,并加上AttributeOverrides注解，
 没有AttributeOverrides注解的字段将被忽略，内嵌类里面Column将被忽略
 如：
  ```java
    @Embedded
    @AttributeOverrides({
          @AttributeOverride(name = "fieldA", column = @Column(name = "field_a")),
          @AttributeOverride(name = "fieldB", column = @Column(name = "field_b"))})
    private EmbeddedClass embeddedClass;
    
    public class EmbeddedClass{
        private String fieldA;
        @Column("field_b")
        private String fieldB;
    }
    ```
考虑到多层内嵌可能存在问题，目前只支持一层内嵌。内嵌类字段的查询，使用 field.name这样处理。比如上面的embeddedClass.fieldA
