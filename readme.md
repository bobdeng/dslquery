# 这是什么

建立一套规则，让前端通过规则传递查询条件给服务端，在服务端进行解析，生成查询条件，然后查询数据库，返回结果。

# 使用方法

## 定义查询条件

- 查询条件 示例：

```
(or(and(field = value)(fieldb >= value))(or(fieldc <= value)(fieldd != value)))
```
- 分页条件
  limit 最大返回条数
  skip  跳过条数
- 排序条件
  sort fieldA desc,fieldB asc

## 服务端解析
```java
   List<QueryResultBean> result = new DSLQuery(jdbcNamedTemplate,QueryResultBean.class)
                .select("fieldA,fieldB")
                .from("table")
                .where("or(and(field = value)(fieldb >= value))(or(fieldc <= value)(fieldd != value))")
                .where("field=100")
                .limit(10).skip(0)
                .sort("fieldA desc,fieldB asc")
                .query();
        
```