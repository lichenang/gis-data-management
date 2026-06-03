# Proposal: fix-raster-tablefield-annotation

## 问题描述

当前 `RasterMetadata` 实体类在类级别错误使用了 `@TableField` 注解。根据 MyBatis-Plus 文档，`@TableField` 只能在字段上使用，不能在类上使用。错误的注解导致 JSONB 字段的序列化/反序列化可能存在问题。

## 当前代码问题

```java
@Data
@TableName("raster_metadata")
@TableField(typeHandler = JacksonTypeHandler.class)  // 错误：类级别不能使用 @TableField
@Schema(description = "影像元数据")
public class RasterMetadata {
    // ...
    private Map<String, Object> transform;
    private Map<String, Object> overviews;
}
```

## 修复目标

1. 删除类级别错误的 `@TableField` 注解
2. 在 `transform` 和 `overviews` 字段上分别添加 `@TableField(typeHandler = JacksonTypeHandler.class)`
3. JacksonTypeHandler 已在 MybatisPlusConfig 中全局注册

## 影响范围

- `RasterMetadata.java` 实体类修改

## 风险评估

- 低风险：仅修改注解位置，不改变业务逻辑
- 修改后需验证编译通过
