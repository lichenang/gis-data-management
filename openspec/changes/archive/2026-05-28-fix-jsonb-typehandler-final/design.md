# Design: fix-jsonb-typehandler-final

## 修复方案

### 1. 创建 PGobjectJsonbTypeHandler

创建使用 PostgreSQL PGobject 的类型处理器：

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

### 2. 修改 RasterMetadata 实体

将 Map<String, Object> 改为 String 类型：

```java
@Data
@TableName("raster_metadata")
@Schema(description = "影像元数据")
public class RasterMetadata {

    // ... 其他字段 ...

    @TableField(typeHandler = PGobjectJsonbTypeHandler.class)
    @Schema(description = "GeoTIFF转换矩阵")
    private String transform;

    @TableField(typeHandler = PGobjectJsonbTypeHandler.class)
    @Schema(description = "金字塔信息")
    private String overviews;

    // ... 其他字段 ...
}
```

### 3. 修改 ImageServiceImpl

手动将 Map 序列化为 JSON 字符串：

**文件**: `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java`

```java
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

// 在方法中序列化
private static final ObjectMapper MAPPER = new ObjectMapper();

RasterMetadata metadata = GeoTiffParser.parse(file);

// 手动序列化 Map 为 JSON 字符串
if (metadata.getTransform() != null) {
    metadata.setTransform(serializeToJson(metadata.getTransform()));
}
if (metadata.getOverviews() != null) {
    metadata.setOverviews(serializeToJson(metadata.getOverviews()));
}

// 保存
rasterMetadataMapper.insert(metadata);

private String serializeToJson(Object obj) {
    if (obj == null) return null;
    try {
        return MAPPER.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
        throw new RuntimeException("序列化失败", e);
    }
}
```

## 数据流（修复后）

```
┌──────────────────┐    serialize    ┌──────────────────┐
│ GeoTiffParser    │ ──────────────▶ │ RasterMetadata   │
│ 返回 Map 对象    │    to JSON      │ transform: String│
└──────────────────┘                 └────────┬─────────┘
                                              │
                                              ▼
                                   ┌─────────────────────┐
                                   │ PGobjectJsonb       │
                                   │ TypeHandler         │
                                   │ setObject(PGobject) │
                                   └──────────┬──────────┘
                                              │
                                              ▼
                                   ┌─────────────────────┐
                                   │ PostgreSQL JSONB    │
                                   │ ✓ 正确写入          │
                                   └─────────────────────┘
```

## 验证步骤

1. Maven 编译: `mvn compile -pl backend`
2. 重启应用
3. 通过 Postman 测试 `POST /api/v1/images/upload`
4. 验证数据库 transform/overviews 字段为 JSONB 类型且数据正确

## 其他实体检查

后续需要检查并修复以下实体（若存在类似问题）：

- Dataset: extent, tags
- MapLayer: style  
- DatasetPermission: row_filter
