# Proposal: fix-global-jsonb-handler

## 问题描述

多个实体的 `Map<String, Object>` 字段在写入 PostgreSQL JSONB 列时失败，报错：
```
字段 "transform" 的类型为 jsonb, 但表达式的类型为 character varying
```

当前已尝试使用 `@TableField(typeHandler = JacksonTypeHandler.class)` 注解，但未生效。

## 根因分析

MyBatis-Plus 的 `JacksonTypeHandler` 对于 PostgreSQL JSONB 类型处理存在以下问题：
1. 缺少 `@MappedJdbcTypes(JdbcType.OTHER)` 注解，MyBatis 不知道该 Handler 处理 jsonb 类型
2. PostgreSQL 驱动对 JSONB 类型有特殊处理要求
3. 当前 Bean 注册方式无法让 Handler 正确拦截 JSONB 字段

## 修复目标

1. 创建自定义 `JsonbTypeHandler`，继承 `BaseTypeHandler` 并正确标注注解
2. 在 `MybatisPlusConfig` 中注册该 Handler
3. 更新 `RasterMetadata` 实体使用新的 Handler
4. 验证影像上传功能正常工作

## 影响范围

- 新增文件：`JsonbTypeHandler.java`
- 修改文件：`MybatisPlusConfig.java`、`RasterMetadata.java`

## 风险评估

- 低风险：仅修复类型转换问题，不改变业务逻辑
