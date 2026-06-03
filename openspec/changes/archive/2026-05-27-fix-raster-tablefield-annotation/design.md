# Design: fix-raster-tablefield-annotation

## 修复方案

### 问题根因

MyBatis-Plus 的 `@TableField` 注解只能用于字段上，不能用于类级别。当前代码错误地将 `typeHandler` 配置放在类级别，导致 JSONB 字段的类型转换可能不生效。

### 修改内容

#### 1. 修改 RasterMetadata.java

删除类级别的 `@TableField(typeHandler = JacksonTypeHandler.class)`（第13行），改为在字段上添加：

```java
@Data
@TableName("raster_metadata")
@Schema(description = "影像元数据")
public class RasterMetadata {

    // ... 其他字段 ...

    @TableField(typeHandler = JacksonTypeHandler.class)
    @Schema(description = "GeoTIFF转换矩阵")
    private Map<String, Object> transform;

    @TableField(typeHandler = JacksonTypeHandler.class)
    @Schema(description = "金字塔信息")
    private Map<String, Object> overviews;

    // ...
}
```

#### 2. JacksonTypeHandler 全局注册

MybatisPlusConfig 中已存在 `jacksonTypeHandler()` Bean，无需额外修改：

```java
@Bean
public TypeHandler<?> jacksonTypeHandler() {
    return new JacksonTypeHandler();
}
```

### 验证步骤

1. 执行 Maven 编译：`mvn compile -pl backend`
2. 确认编译无错误
3. 启动应用测试影像上传功能
4. 验证数据库中 transform 和 overviews 字段正确存储为 JSONB
