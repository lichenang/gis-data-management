# Tasks: fix-global-jsonb-handler

- [x] Task 1: 创建 JsonbTypeHandler 类
- [x] Task 2: 更新 MybatisPlusConfig
- [x] Task 3: 更新 RasterMetadata 实体
- [x] Task 4: 验证编译

## Task 5: 测试影像上传

1. 重启应用
2. 通过 Postman 测试 `POST /api/v1/images/upload`
3. 确认成功保存到数据库

**文件**: `backend/src/main/java/com/gisplatform/common/handler/JsonbTypeHandler.java`

创建内容：
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

## Task 2: 更新 MybatisPlusConfig

**文件**: `backend/src/main/java/com/gisplatform/config/MybatisPlusConfig.java`

1. 添加 import：
```java
import com.gisplatform.common.handler.JsonbTypeHandler;
```

2. 添加 Bean 方法（在文件末尾）：
```java
@Bean
public TypeHandler<?> jsonbTypeHandler() {
    return new JsonbTypeHandler();
}
```

## Task 3: 更新 RasterMetadata 实体

**文件**: `backend/src/main/java/com/gisplatform/entity/RasterMetadata.java`

1. 添加 import：
```java
import com.gisplatform.common.handler.JsonbTypeHandler;
```

2. 删除字段上的 `@TableField(typeHandler = JacksonTypeHandler.class)` 注解（原第53、57行）

3. 在类级别添加 `@TypeHandler` 注解：
```java
@Data
@TableName("raster_metadata")
@TypeHandler(JsonbTypeHandler.class)
@Schema(description = "影像元数据")
public class RasterMetadata {
```

## Task 4: 验证编译

执行 Maven 编译：
```bash
cd backend
mvn compile
```

## Task 5: 测试影像上传

1. 重启应用
2. 通过 Postman 测试 `POST /api/v1/images/upload`
3. 确认成功保存到数据库
