# Proposal: fix-jsonb-typehandler-final

## 问题描述

之前的 JSONB 修复尝试（使用 JacksonTypeHandler + Map<String, Object>）未生效。PostgreSQL 报错：
```
字段 "transform" 的类型为 jsonb, 但表达式的类型为 character varying
```

## 根因分析

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        问题根因                                          │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  MyBatis-Plus 默认使用 PreparedStatement.setString() 设置 JSONB 参数    │
│  但 PostgreSQL 对 JSONB 类型有特殊要求：                                 │
│                                                                         │
│  ┌─────────────┐    setString()    ┌─────────────────────────────┐    │
│  │ PostgreSQL  │ ◀──────────────── │ MyBatis/PreparedStatement   │    │
│  │ JSONB 列    │    纯字符串       │ 发送: "{...json...}"        │    │
│  │             │                   │ ❌ 类型不匹配!               │    │
│  └─────────────┘                   └─────────────────────────────┘    │
│                                                                         │
│  正确做法: 使用 org.postgresql.util.PGobject 包装                       │
│                                                                         │
│  ┌─────────────┐    PGobject      ┌─────────────────────────────┐    │
│  │ PostgreSQL  │ ◀──────────────── │ MyBatis/PreparedStatement   │    │
│  │ JSONB 列    │   type="json"    │ 发送: PGobject(json)         │    │
│  │             │                   │ ✓ 类型匹配!                  │    │
│  └─────────────┘                   └─────────────────────────────┘    │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

## 修复方案

采用更简洁的方案：使用 String 类型 + 自定义 JsonbTypeHandler

1. **JsonbTypeHandler** - 使用 PGobject 包装 JSON 字符串
2. **RasterMetadata 实体** - transform/overviews 改为 String 类型
3. **ImageServiceImpl** - 手动将 Map 序列化为 JSON 字符串

### 其他待检查实体

| 表名 | JSONB 字段 | 实体类 | 状态 |
|------|-----------|--------|------|
| dataset | extent, tags | Dataset | 待检查 |
| dataset_permission | row_filter | DatasetPermission | 待检查 |
| vector_features_001 | properties | - | 动态表 |
| map_layer | style | MapLayer | 待检查 |

## 影响范围

- 新增: JsonbTypeHandler.java
- 修改: RasterMetadata.java, ImageServiceImpl.java

## 风险评估

- 低风险：仅修改类型处理方式，不改变业务逻辑
