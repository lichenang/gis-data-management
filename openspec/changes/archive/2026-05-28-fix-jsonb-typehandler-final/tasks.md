# Tasks: fix-jsonb-typehandler-final

## Task 1: 创建 PGobjectJsonbTypeHandler ✓

**文件**: `backend/src/main/java/com/gisplatform/common/handler/PGobjectJsonbTypeHandler.java`

```java
package com.gisplatform.common.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedTypes({String.class})
@MappedJdbcTypes(JdbcType.OTHER)
public class PGobjectJsonbTypeHandler extends BaseTypeHandler<String> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i,
            String parameter, JdbcType jdbcType) throws SQLException {
        PGobject pgObject = new PGobject();
        pgObject.setType("jsonb");
        pgObject.setValue(parameter);
        ps.setObject(i, pgObject);
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Object obj = rs.getObject(columnName);
        return obj != null ? obj.toString() : null;
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Object obj = rs.getObject(columnIndex);
        return obj != null ? obj.toString() : null;
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Object obj = cs.getObject(columnIndex);
        return obj != null ? obj.toString() : null;
    }
}
```

## Task 2: 修改 RasterMetadata 实体 ✓

**文件**: `backend/src/main/java/com/gisplatform/entity/RasterMetadata.java`

1. 修改 import - 添加 PGobjectJsonbTypeHandler，删除 JacksonTypeHandler
2. 删除 `@TableField(typeHandler = JsonbTypeHandler.class)`（之前的修复）
3. 添加新的字段注解：`@TableField(typeHandler = PGobjectJsonbTypeHandler.class)`
4. 将字段类型改为 String

修改后 transform 和 overviews 字段：
```java
@TableField(typeHandler = PGobjectJsonbTypeHandler.class)
@Schema(description = "GeoTIFF转换矩阵")
private String transform;

@TableField(typeHandler = PGobjectJsonbTypeHandler.class)
@Schema(description = "金字塔信息")
private String overviews;
```

## Task 3: 删除旧的 JsonbTypeHandler（如存在） ✓

**文件**: `backend/src/main/java/com/gisplatform/common/handler/JsonbTypeHandler.java`

删除之前的 Map 类型 JsonbTypeHandler（不再需要）。

## Task 4: 修改 ImageServiceImpl ✓

**文件**: `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java`

1. 添加 import：
```java
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
```

2. 在 ImageServiceImpl 类中添加：
```java
private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
```

3. 在调用 rasterMetadataMapper.insert(metadata) 之前，序列化 Map 为 String：
```java
// 序列化 transform
if (metadata.getTransform() != null && metadata.getTransform() instanceof Map) {
    try {
        metadata.setTransform(OBJECT_MAPPER.writeValueAsString(metadata.getTransform()));
    } catch (JsonProcessingException e) {
        throw new RuntimeException("序列化 transform 失败", e);
    }
}

// 序列化 overviews
if (metadata.getOverviews() != null && metadata.getOverviews() instanceof Map) {
    try {
        metadata.setOverviews(OBJECT_MAPPER.writeValueAsString(metadata.getOverviews()));
    } catch (JsonProcessingException e) {
        throw new RuntimeException("序列化 overviews 失败", e);
    }
}

// 保存到数据库
rasterMetadataMapper.insert(metadata);
```

## Task 5: 验证编译 ✓

执行 Maven 编译：
```bash
cd backend
mvn compile
```

## Task 6: 测试影像上传 🚧（需要手动测试）

1. 重启应用
2. 通过 Postman 测试 `POST /api/v1/images/upload`
3. 确认成功保存到数据库
4. 查询数据库验证 transform 和 overviews 字段正确存储为 JSONB
