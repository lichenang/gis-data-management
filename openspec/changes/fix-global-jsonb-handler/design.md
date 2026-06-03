# Design: fix-global-jsonb-handler

## 修复方案

### 1. 创建 JsonbTypeHandler

创建专门处理 PostgreSQL JSONB 的自定义 TypeHandler：

**文件**: `backend/src/main/java/com/gisplatform/common/handler/JsonbTypeHandler.java`

```java
package com.gisplatform.common.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.base.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

@MappedTypes({Map.class})
@MappedJdbcTypes(JdbcType.OTHER)
public class JsonbTypeHandler extends BaseTypeHandler<Map<String, Object>> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, 
            Map<String, Object> parameter, JdbcType jdbcType) throws SQLException {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(parameter);
            ps.setString(i, json);
        } catch (JsonProcessingException e) {
            throw new SQLException("Error converting Map to JSON", e);
        }
    }

    @Override
    public Map<String, Object> getNullableResult(ResultSet rs, String columnName) 
            throws SQLException {
        return parseJson(rs.getString(columnName));
    }

    @Override
    public Map<String, Object> getNullableResult(ResultSet rs, int columnIndex) 
            throws SQLException {
        return parseJson(rs.getString(columnIndex));
    }

    @Override
    public Map<String, Object> getNullableResult(CallableStatement cs, int columnIndex) 
            throws SQLException {
        return parseJson(cs.getString(columnIndex));
    }

    private Map<String, Object> parseJson(String json) throws SQLException {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            throw new SQLException("Error parsing JSON to Map", e);
        }
    }
}
```

### 2. 更新 MybatisPlusConfig

添加 JsonbTypeHandler Bean：

```java
@Bean
public TypeHandler<?> jsonbTypeHandler() {
    return new JsonbTypeHandler();
}
```

### 3. 更新 RasterMetadata 实体

在类级别添加 `@TypeHandler` 注解，一劳永逸：

```java
@Data
@TableName("raster_metadata")
@TypeHandler(JsonbTypeHandler.class)
@Schema(description = "影像元数据")
public class RasterMetadata {
    // ... fields with Map<String, Object> will now use the handler
}
```

### 数据流（修复后）

```
┌─────────────────┐     JsonbTypeHandler      ┌──────────────────┐
│ RasterMetadata  │ ──────────────────────▶ │ PreparedStatement│
│ transform:      │   Map<String,Object>     │                  │
│ Map<String,     │                          │ PostgreSQL 接受  │
│    Object>      │                          │ setString() for  │
│                 │                          │ JSONB columns    │
└─────────────────┘                          └──────────────────┘
```

## 验证步骤

1. 执行 Maven 编译：`mvn compile -pl backend`
2. 重启应用
3. 通过 Postman 上传 GeoTIFF 文件
4. 验证数据库中 transform 和 overviews 字段正确存储为 JSONB
