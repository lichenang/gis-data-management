## Why

`insertFeature` 方法在构建 INSERT SQL 时，VALUES 子句的占位符数量与实际列数不匹配，导致 `"栏位索引超过许可范围：10，栏位数：9"` 错误。

## What Changes

- 修复 `MultiFormatImportServiceImpl.insertFeature()` 方法中 VALUES 子句的 SQL 构建逻辑
- 确保几何列的占位符(?) + 属性列的占位符(?) 总数与表实际列数完全一致

## Capabilities

### Modified Capabilities

- `multi-format-vector-import`: 修复 Shapefile 导入时 SQL 占位符数量不匹配问题

## Impact

### 受影响的文件

- `backend/src/main/java/com/gisplatform/service/impl/MultiFormatImportServiceImpl.java`

### 非目标

- 不修改其他方法
- 不修改业务逻辑
- 不添加新功能

## 问题根因

当前代码 (Line 297-302):

```java
if (sourceSrid == targetSrid) {
    sql.append(") VALUES (ST_GeomFromWKB(?, ").append(sourceSrid).append(")");
} else {
    sql.append(") VALUES (ST_Transform(ST_GeomFromWKB(?, ").append(sourceSrid).append("), ").append(targetSrid).append(")");
}
sql.append(")").append(",?".repeat(propList.size())).append(")");
```

Line 298 添加的 `)` 破坏了 VALUES 子句结构，导致括号不匹配。
