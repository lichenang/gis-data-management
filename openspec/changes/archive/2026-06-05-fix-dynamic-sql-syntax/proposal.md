## Why

`insertFeature` 方法中动态 SQL 拼接存在语法错误。VALUES 子句中的几何函数调用后缺少闭合括号，导致生成的 SQL 语句无效，数据导入失败。

## What Changes

- 修复 `MultiFormatImportServiceImpl.insertFeature()` 方法中 VALUES 子句的 SQL 语法错误

## Capabilities

### Modified Capabilities

- `multi-format-vector-import`: 修复 Shapefile 导入时 SQL 拼接 bug

## Impact

### 受影响的文件

- `backend/src/main/java/com/gisplatform/service/impl/MultiFormatImportServiceImpl.java`

### 非目标

- 不修改其他方法
- 不修改业务逻辑
- 不添加新功能

## 问题分析

当前代码（第 297-302 行）：

```java
if (sourceSrid == targetSrid) {
    sql.append(") VALUES (ST_GeomFromWKB(?, ").append(sourceSrid).append(")");
} else {
    sql.append(") VALUES (ST_Transform(ST_GeomFromWKB(?, ").append(sourceSrid).append("), ").append(targetSrid).append(")");
}
sql.append(")").append(",?".repeat(propList.size())).append(")");
```

**问题**：VALUES 子句闭合方式不正确

- 第 298/300 行添加 `) VALUES (ST_GeomFromWKB(?, 4326)` 或 `ST_Transform(...)`
- 此时 VALUES 后的括号已经打开但只闭合了函数，未闭合 VALUES 子句
- 第 302 行添加 `)` + `,?...?` + `)` 导致多出一个括号

**正确的结构应该是**：
```sql
INSERT INTO table (geometry, col1, col2) VALUES (ST_GeomFromWKB(?, 4326), ?, ?)
```
注意 VALUES 后只有一个开括号，参数用逗号分隔。
