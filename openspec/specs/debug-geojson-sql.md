# GeoJSON SQL 字段名不一致问题 - 诊断报告

## 问题描述

GeoJSON 接口返回 500 错误：
```
SELECT ST_AsGeoJSON(geom) as gj FROM dataset______1019
                      ↑
                   字段名错误
```

## 根因分析

### 数据库表结构

根据用户提供的信息：

```sql
-- 表字段列表（部分）
id  | Id1 | TBBH | sheng | shi | xian | ... | geometry
----|-----|------|-------|-----|------|-----|---------
1   | 0   | ...  | 山西  | ... | ...  | ... | (binary)
```

**几何列字段名是 `geometry`，不是 `geom`**

### 建表代码分析

**文件**: `GisDataParserServiceImpl.java` 第 259-284 行

```java
private void createGeoJSONTable(Connection conn, String tableName, Set<String> propertyNames) throws SQLException {
    // ...
    columnDefs.add("geometry GEOMETRY");  // ← 字段名是 "geometry"
    // ...
}

// 建表 SQL 示例：
// CREATE TABLE "public"."dataset______1019" (
//   id SERIAL PRIMARY KEY,
//   ...其他属性...
//   geometry GEOMETRY  ← 正确
// );
```

### 插入数据代码

**文件**: `GisDataParserServiceImpl.java` 第 290-291 行

```java
sql.append("INSERT INTO \"").append(dbSchema).append("\".\"").append(tableName).append("\" (");
sql.append("geometry");  // ← 字段名是 "geometry"
```

### GeoJSON 查询代码（错误）

**文件**: `DatasetServiceImpl.java` 第 142 行

```java
// ❌ 错误代码
String sql = "SELECT ST_AsGeoJSON(geom) as gj FROM " + tableName;
//                                    ↑
//                            应该是 "geometry"
```

---

## 问题总结

```
数据导入时:
  CREATE TABLE ... ( geometry GEOMETRY )  ✅ 字段名: geometry
  INSERT INTO ... (geometry, ...)         ✅ 字段名: geometry

GeoJSON 查询时:
  SELECT ST_AsGeoJSON(geom) ...           ❌ 字段名: geom (错误!)
```

**根因**: 查询 SQL 中使用了错误的字段名 `geom`，但实际表中的几何字段是 `geometry`。

---

## 修复方案

将 `DatasetServiceImpl.getDatasetAsGeoJSON()` 中的字段名从 `geom` 改为 `geometry`:

```java
// 修改前
String sql = "SELECT ST_AsGeoJSON(geom) as gj FROM " + tableName;

// 修改后
String sql = "SELECT ST_AsGeoJSON(geometry) as gj FROM \"" + dbSchema + "\".\"" + tableName + "\"";
```

**同时需要修复**:
1. 字段名：`geom` → `geometry`
2. Schema 前缀：添加 `"public".` 或使用配置的 dbSchema

---

## 修改的文件

| 文件 | 行号 | 修改内容 |
|------|------|---------|
| `DatasetServiceImpl.java` | ~142 | `ST_AsGeoJSON(geom)` → `ST_AsGeoJSON(geometry)` |
| `DatasetServiceImpl.java` | ~142 | 添加 schema 前缀 |
