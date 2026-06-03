# Proposal: fix-dataset-extent-jsonb

## 问题描述

调用 `POST /api/v1/images/27/publish` 接口时，PostgreSQL 报错：

```
字段 "extent" 的类型为 jsonb, 但表达式的类型为 character varying
```

## 根因分析

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        问题根因                                          │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Dataset.extent 字段是 String 类型                                       │
│  PostgreSQL extent 列是 jsonb 类型                                      │
│                                                                         │
│  MyBatis-Plus UPDATE 时:                                                │
│  ┌─────────────┐    setString()    ┌─────────────────────────────┐    │
│  │ PostgreSQL  │ ◀──────────────── │ PreparedStatement           │    │
│  │ JSONB 列    │    纯字符串       │ 发送: "{\"minX\":...}"       │    │
│  │             │                   │ ❌ 类型不匹配!               │    │
│  └─────────────┘                   └─────────────────────────────┘    │
│                                                                         │
│  正确做法: 使用 PGobjectJsonbTypeHandler 包装                           │
│                                                                         │
│  ┌─────────────┐    PGobject      ┌─────────────────────────────┐    │
│  │ PostgreSQL  │ ◀──────────────── │ PreparedStatement           │    │
│  │ JSONB 列    │   type="jsonb"   │ 发送: PGobject(jsonb)        │    │
│  │             │                   │ ✓ 类型匹配!                  │    │
│  └─────────────┘                   └─────────────────────────────┘    │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

## 修复方案

复用已有的 `PGobjectJsonbTypeHandler`，在 `Dataset.extent` 字段添加 `@TableField(typeHandler = PGobjectJsonbTypeHandler.class)` 注解。

## 影响范围

- 修改: `Dataset.java` - 添加 typeHandler 注解

## 风险评估

- 低风险：仅添加一个注解，不改变业务逻辑
