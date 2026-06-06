## Context

`MultiFormatImportServiceImpl.insertFeature()` 方法构建 INSERT SQL 语句时，VALUES 子句的构建逻辑存在问题，导致占位符数量与列数不匹配。

### 问题分析

**当前代码** (Line 297-302):
```java
if (sourceSrid == targetSrid) {
    sql.append(") VALUES (ST_GeomFromWKB(?, ").append(sourceSrid).append(")");
} else {
    sql.append(") VALUES (ST_Transform(ST_GeomFromWKB(?, ").append(sourceSrid).append("), ").append(targetSrid).append(")");
}
sql.append(")").append(",?".repeat(propList.size())).append(")");
```

**生成的 SQL** (2属性):
```sql
INSERT INTO "public"."table" (geometry, "col1", "col2") VALUES (ST_GeomFromWKB(?, 4326), ?, ?
```

VALUES 子句中 `ST_GeomFromWKB(?, 4326)` 后的 `)` 被误解为函数闭包，导致后续占位符计算错误。

## Goals / Non-Goals

**Goals:**
- 修复 VALUES 子句的 SQL 构建逻辑
- 确保占位符数量与实际列数匹配

**Non-Goals:**
- 不修改 ST_GeomFromWKB 的调用方式
- 不修改参数设置逻辑

## Decisions

### Decision: 重构 VALUES 子句构建逻辑

**方案**：将 VALUES 子句的构建统一到一个 StringBuilder 中，避免多次 append 导致的括号不匹配问题。

```java
String valuesClause;
if (sourceSrid == targetSrid) {
    valuesClause = ") VALUES (ST_GeomFromWKB(?, " + sourceSrid + ")";
} else {
    valuesClause = ") VALUES (ST_Transform(ST_GeomFromWKB(?, " + sourceSrid + "), " + targetSrid + ")";
}

StringBuilder values = new StringBuilder(valuesClause);
for (int i = 0; i < propList.size(); i++) {
    values.append(", ?");
}
values.append(")");
sql.append(values.toString());
```

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|---------|
| 修改可能引入新问题 | 只修改 SQL 构建逻辑，不改变其他部分 |
