# 注解兼容性说明

## 概述

从 1.0 版本开始，DSLQuery 支持自定义注解和 javax.persistence 注解的双重兼容。这意味着：

1. **旧代码无需修改** - 继续使用 `javax.persistence` 注解
2. **新代码推荐使用自定义注解** - 使用 `cn.beagile.dslquery.annotation` 包下的注解
3. **运行时自动检测** - 优先使用自定义注解，自动 fallback 到 javax.persistence

## 支持的注解

### 自定义注解 (推荐)

| 自定义注解 | 对应的 javax.persistence 注解 | 说明 |
|-----------|------------------------------|------|
| `@DslColumn` | `@Column` | 标记数据库列 |
| `@DslEmbedded` | `@Embedded` | 标记嵌入对象 |
| `@DslJoinColumn` | `@JoinColumn` | 标记关联列 |
| `@DslJoinColumns` | `@JoinColumns` | 标记多个关联列 |
| `@DslOneToMany` | `@OneToMany` | 标记一对多关系 |

### 使用示例

#### 使用自定义注解（推荐）

```java
import cn.beagile.dslquery.annotation.*;

@View("users")
public class UserView {
    @DslColumn(name = "id", unique = true)
    private Long id;

    @DslColumn(name = "name")
    private String name;

    @DslEmbedded
    private Address address;

    @DslJoinColumn(name = "department_id", referencedColumnName = "id")
    private Department department;

    @DslOneToMany(mappedBy = "userId")
    private List<Order> orders;
}
```

#### 使用 javax.persistence 注解（向后兼容）

```java
import javax.persistence.*;

@View("users")
public class UserView {
    @Column(name = "id", unique = true)
    private Long id;

    @Column(name = "name")
    private String name;

    @Embedded
    private Address address;

    @JoinColumn(name = "department_id", referencedColumnName = "id")
    private Department department;

    @OneToMany(mappedBy = "userId")
    private List<Order> orders;
}
```

## 迁移指南

### 渐进式迁移

1. **保持现有代码不变** - 所有使用 `javax.persistence` 的代码继续正常工作
2. **新功能使用自定义注解** - 新增的实体类使用 `@DslXxx` 注解
3. **逐步迁移旧代码** - 根据需要逐步将旧代码迁移到自定义注解

### 迁移脚本示例

```bash
# 批量替换注解导入
find src -name "*.java" -exec sed -i '' \
  -e 's/import javax.persistence.Column;/import cn.beagile.dslquery.annotation.DslColumn;/g' \
  -e 's/import javax.persistence.Embedded;/import cn.beagile.dslquery.annotation.DslEmbedded;/g' \
  -e 's/@Column/@DslColumn/g' \
  -e 's/@Embedded/@DslEmbedded/g' \
  {} \;
```

## 依赖配置

### Gradle

```gradle
dependencies {
    // DSLQuery 库（已包含自定义注解）
    implementation 'cn.beagile.lib:dslquery:1.0'

    // 可选：如果需要使用 javax.persistence 注解
    compileOnly 'javax.persistence:javax.persistence-api:2.2'

    // 或者：如果需要使用 jakarta.persistence 注解（未来支持）
    // compileOnly 'jakarta.persistence:jakarta.persistence-api:3.1.0'
}
```

### Maven

```xml
<dependencies>
    <!-- DSLQuery 库 -->
    <dependency>
        <groupId>cn.beagile.lib</groupId>
        <artifactId>dslquery</artifactId>
        <version>1.0</version>
    </dependency>

    <!-- 可选：javax.persistence 支持 -->
    <dependency>
        <groupId>javax.persistence</groupId>
        <artifactId>javax.persistence-api</artifactId>
        <version>2.2</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

## 注解优先级

当同一个字段同时存在自定义注解和 javax.persistence 注解时，优先级如下：

1. **自定义注解优先** - `@DslColumn` > `@Column`
2. **Fallback 机制** - 如果没有自定义注解，自动使用 javax.persistence 注解

示例：

```java
// 优先使用 @DslColumn
@DslColumn(name = "user_name")
@Column(name = "name")  // 被忽略
private String name;
```

## 常见问题

### Q: 必须迁移到自定义注解吗？

A: 不必须。旧代码可以继续使用 `javax.persistence` 注解，完全向后兼容。

### Q: 自定义注解有什么优势？

A: 
- 不依赖外部 JPA 实现
- 避免 javax/jakarta 迁移问题
- 可以根据 DSLQuery 需求定制属性
- 更轻量，无额外依赖

### Q: 可以混用两种注解吗？

A: 可以。同一个项目中可以混用，甚至同一个类中也可以混用（但不推荐）。

### Q: 未来会移除 javax.persistence 支持吗？

A: 短期内不会。我们会长期保持向后兼容，但推荐新项目使用自定义注解。

## 技术细节

### 实现原理

DSLQuery 使用 `AnnotationReader` 类在运行时检测注解：

1. 首先尝试读取自定义注解（`@DslXxx`）
2. 如果不存在，通过反射尝试读取 `javax.persistence` 注解
3. 如果 `javax.persistence` 不在 classpath 中，自动跳过

### 性能影响

- 反射调用有轻微性能开销，但可以忽略不计
- 注解信息在首次读取后会被缓存
- 对查询执行性能无影响

## 更新日志

### v1.0 (2026-03-31)
- 新增自定义注解体系
- 实现 javax.persistence 兼容层
- 所有测试通过（169 个测试用例）
