# Spec: Fix Global JSONB TypeHandler

## 问题诊断

### 症状
多个实体的 `Map<String, Object>` 字段在写入 PostgreSQL JSONB 列时，即使添加了 `@TableField(typeHandler = JacksonTypeHandler.class)`，仍然报错：
```
字段 "transform" 的类型为 jsonb, 但表达式的类型为 character varying
```

### 当前配置分析

#### 1. MybatisPlusConfig.java (当前状态)
```java
@Bean
public TypeHandler<?> jacksonTypeHandler() {
    return new JacksonTypeHandler(Object.class);
}
```

#### 2. 实体类 RasterMetadata.java (当前注解)
```java
@TableField(typeHandler = JacksonTypeHandler.class)
private Map<String, Object> transform;
```

#### 3. application.yml (缺失配置)
```yaml
mybatis-plus:
  global-config:
    db-config:
      # ...
    # 缺失: type-handlers 配置
```

### 根因分析

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        问题分析                                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  当前流程（错误）:                                                            │
│  ┌──────────────┐     JacksonTypeHandler      ┌──────────────────────────┐ │
│  │ setTransform │ ──────────────────────────▶ │ MyBatis 尝试使用 Handler │ │
│  │  (Map)       │                             │ 但使用了 toString()      │ │
│  └──────────────┘                             │ 输出: "{...json...}"     │ │
│                                                └────────────┬─────────────┘ │
│                                                             │               │
│                                                             ▼               │
│                                              ┌──────────────────────────┐  │
│                                              │ PreparedStatement       │  │
│                                              │ setString() ──▶ String  │  │
│                                              │ PostgreSQL: JSONB ≠ String│  │
│                                              │ ❌ TYPE_MISMATCH         │  │
│                                              └──────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**关键问题**: `JacksonTypeHandler` Bean 的注册方式缺少关键配置：
- 缺少 `@MappedJdbcTypes(JdbcType.OTHER)` 注解
- 对于 PostgreSQL，需要显式指定处理 jsonb 类型
- ObjectMapper 需要配置为正确处理 JSONB

### 修复方案

#### 方案 A: 自定义 JsonbTypeHandler (推荐)

创建专门处理 PostgreSQL JSONB 的 TypeHandler：

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

#### 方案 B: 修改 MybatisPlusConfig 全局配置

```java
package com.gisplatform.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.gisplatform.common.handler.JsonbTypeHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.type.TypeHandler;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
@MapperScan("com.gisplatform.mapper")
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        return interceptor;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }

    @Bean
    public TypeHandler<?> geometryTypeHandler() {
        return new com.gisplatform.common.handler.GeometryTypeHandler();
    }

    @Bean
    public TypeHandler<?> jsonbTypeHandler() {
        return new JsonbTypeHandler();
    }
}
```

#### 方案 C: 字段级注解（保持现有代码，仅替换 Handler）

修改实体类，改用自定义 JsonbTypeHandler：

```java
@TableField(typeHandler = JsonbTypeHandler.class)
private Map<String, Object> transform;

@TableField(typeHandler = JsonbTypeHandler.class)
private Map<String, Object> overviews;
```

## 实现步骤

1. **创建 JsonbTypeHandler 类**
   - 文件: `backend/src/main/java/com/gisplatform/common/handler/JsonbTypeHandler.java`
   
2. **更新 MybatisPlusConfig**
   - 添加 JsonbTypeHandler Bean

3. **更新实体类** (选择一种方式):
   - 方式 A: 字段级注解 `@TableField(typeHandler = JsonbTypeHandler.class)`
   - 方式 B: 类级注解 `@TypeHandler(JsonbTypeHandler.class)` (推荐，一劳永逸)

4. **验证测试**
   - 重新编译: `mvn compile -pl backend`
   - 重启应用
   - 测试影像上传

## 影响的实体

| 实体类 | JSONB 字段 | 修复方式 |
|--------|-----------|----------|
| RasterMetadata | transform, overviews | @TypeHandler |
| 其他实体... | ... | @TypeHandler |

## 备选方案

如果不想每个字段都添加注解，可以在 application.yml 中配置 global type-handlers（但这只对使用注解的字段生效）：

```yaml
mybatis-plus:
  global-config:
    type-handlers:
      - com.gisplatform.common.handler.JsonbTypeHandler
```
