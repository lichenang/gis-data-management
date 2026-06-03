# Tasks: fix-raster-jsonb-error

## Task 1: 注册全局 JacksonTypeHandler

**File**: `backend/src/main/java/com/gisplatform/config/MybatisPlusConfig.java`

添加 JacksonTypeHandler Bean：

```java
@Bean
public TypeHandler<?> jacksonTypeHandler() {
    return new JacksonTypeHandler();
}
```

## Task 2: 修改 RasterMetadata 实体

**File**: `backend/src/main/java/com/gisplatform/entity/RasterMetadata.java`

1. 添加 import：
   ```java
   import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
   ```

2. 在类上添加 @TypeHandler 注解：
   ```java
   @TypeHandler(JacksonTypeHandler.class)
   ```

3. 将字段改为 Map 类型并添加 @TableField 注解：
   ```java
   @TableField(typeHandler = JacksonTypeHandler.class)
   private Map<String, Object> transform;

   @TableField(typeHandler = JacksonTypeHandler.class)
   private Map<String, Object> overviews;
   ```

## Task 3: 简化 GeoTiffParser 代码

**File**: `backend/src/main/java/com/gisplatform/util/GeoTiffParser.java`

删除手动 JSON 转换代码，改为直接设置 Map：

```java
// 删除 JSONUtil.toJsonStr() 包装
metadata.setTransform(transformMap);  // 直接设置 Map
metadata.setOverviews(res);            // 直接设置 Map
```

## Task 4: 验证编译

执行 Maven 编译：
```bash
cd backend
mvn compile
```
