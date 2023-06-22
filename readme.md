# 这是什么

建立一套规则，让前端通过规则传递查询条件给服务端，在服务端进行解析，生成查询条件，然后查询数据库，返回结果。

# 使用方法

## 定义查询条件

- 查询条件 示例：

```
(or(and(field equal value)(fieldb greaterthan value))(or(fieldc lessthanorequal value)(fieldd notequal value)))
```

支持的运算符有 equal notequal greaterthan greaterthanorequal lessthan lessthanorequal startswith endswith contains isnull
notnull

- 分页条件

  limit 最大返回条数 skip 跳过条数
- 排序条件

  sort fieldA desc,fieldB asc

## 服务端解析

- 实现查询执行的接口

```java
public interface QueryExecutor {
    <T> List<T> execute(SQLQuery sqlQuery, Function<ResultSet, T> resultSetReader);
}
```

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
```