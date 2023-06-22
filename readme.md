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
    public class QueryResultBean {
        @Column("field_a")
        private String fieldA;
        @Column("field_a")
        private String fieldB;
        //getter setter
    }
    public class QueryParams{
        private String query;
        private int limit;
        private int skip;
        private String sort;
        //getter setter
    }
    
   List<QueryResultBean> result = new DSLQuery(jdbcNamedTemplate,QueryResultBean.class)
                .from("view_example")
                .where("or(and(fieldA = value)(fieldB >= value))(or(fieldB <= value)(fieldB != value))")
                .where("field=100")
                .limit(10).skip(0)
                .sort("fieldA desc,fieldB asc")
                .query();

      List<QueryResultBean> result = new DSLQuery(jdbcNamedTemplate,QueryResultBean.class)
              .from("view_example")
              .where("field=100")
              .queryParams(new QueryParams("or(and(fieldA = value)(fieldB >= value))(or(fieldB <= value)(fieldB != value))",10,0,"fieldA desc,fieldB asc"))
              .query();
        
```