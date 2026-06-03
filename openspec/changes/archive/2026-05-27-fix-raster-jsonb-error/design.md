# Design: fix-raster-jsonb-error

## 修复方案

### 1. 修改 RasterMetadata 实体

将 transform 和 overviews 字段从 String 改为 Map，并添加 typeHandler：

```java
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;

@Data
@TableName("raster_metadata")
@TypeHandler(JacksonTypeHandler.class)  // 类级别注解
public class RasterMetadata {

    // 改为 Map 类型
    @TableField(typeHandler = JacksonTypeHandler.class)
    @Schema(description = "GeoTIFF转换矩阵")
    private Map<String, Object> transform;

    @TableField(typeHandler = JacksonTypeHandler.class)
    @Schema(description = "金字塔信息")
    private Map<String, Object> overviews;
}
```

### 2. 全局注册 JacksonTypeHandler

在 MybatisPlusConfig 中添加全局 typeHandler 注册：

```java
@Bean
public TypeHandler<?> jacksonTypeHandler() {
    return new JacksonTypeHandler();
}
```

### 3. 简化 GeoTiffParser

删除手动 JSON 字符串转换，直接设置 Map：

```java
// 之前
metadata.setTransform(JSONUtil.toJsonStr(transformMap));

// 之后（直接设置 Map）
metadata.setTransform(transformMap);
```

## 验证步骤

1. 修改代码后执行 `mvn compile`
2. 启动应用并测试影像上传
3. 查询数据库验证 JSONB 字段正确存储

## 备选方案（方案B）

如果全局注册不生效，可以在 ImageServiceImpl 中使用 LambdaUpdateWrapper 显式转换：

```java
LambdaUpdateWrapper<RasterMetadata> wrapper = new LambdaUpdateWrapper<>();
wrapper.set(RasterMetadata::getTransform, JSONUtil.toJsonStr(transformMap));
wrapper.eq(RasterMetadata::getId, id);
rasterMetadataMapper.update(null, wrapper);
```

推荐方案A（一劳永逸）。
