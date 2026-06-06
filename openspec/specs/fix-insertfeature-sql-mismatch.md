# fix-insertfeature-sql-mismatch (更新诊断)

## 问题描述

**错误信息**: `"栏位索引超过许可范围：10，栏位数：9"`
**发生时间**: 2026-06-05 14:24:24
**堆栈**: `MultiFormatImportServiceImpl.insertFeature(MultiFormatImportServiceImpl.java:323)`

**含义**: JDBC 认为 SQL 中有 10 个 `?` 占位符，但只有 9 列定义。

---

## 代码分析

### 当前 insertFeature 方法 (修改后)

```java
private void insertFeature(Connection conn, String tableName,
                            org.locationtech.jts.geom.Geometry geometry,
                            SimpleFeature feature, Set<String> propertyNames,
                            int sourceSrid, int targetSrid) throws Exception {
    StringBuilder sql = new StringBuilder();
    sql.append("INSERT INTO \"").append(dbSchema).append("\".\"").append(tableName).append("\" (");
    sql.append("geometry");

    // 构建列名列表
    List<String> propList = new ArrayList<>(propertyNames);
    for (String prop : propList) {
        String colName = prop.replaceAll("[^a-zA-Z0-9_]", "_");
        sql.append(", \"").append(colName).append("\"");
    }

    // VALUES 子句构建
    String valuesClause;
    if (sourceSrid == targetSrid) {
        valuesClause = ") VALUES (ST_GeomFromWKB(?, " + sourceSrid + ")";  // Line 299
    } else {
        valuesClause = ") VALUES (ST_Transform(ST_GeomFromWKB(?, " + sourceSrid + "), " + targetSrid + ")";  // Line 301
    }

    StringBuilder values = new StringBuilder(valuesClause);
    for (int i = 0; i < propList.size(); i++) {
        values.append(", ?");
    }
    values.append(")");
    sql.append(values.toString());

    // 参数绑定
    try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
        if (geometry != null) {
            stmt.setBytes(1, writeWkb(geometry));  // 参数 1: WKB
            stmt.setInt(2, sourceSrid);             // 参数 2: SRID
        } else {
            stmt.setNull(1, Types.OTHER);
            stmt.setNull(2, Types.INTEGER);
        }

        int idx = 3;
        for (String prop : propList) {
            Object value = feature.getAttribute(prop);
            stmt.setString(idx++, value != null ? value.toString() : null);
        }

        stmt.execute();
    }
}
```

### SQL 生成分析

假设有 8 个属性列，sourceSrid == targetSrid == 4326：

**列名部分**:
```
INSERT INTO "public"."dataset_xxx" (geometry, "col1", "col2", "col3", "col4", "col5", "col6", "col7", "col8"
```
共 9 列 (geometry + 8 属性)

**VALUES 部分**:
```
) VALUES (ST_GeomFromWKB(?, 4326), ?, ?, ?, ?, ?, ?, ?, ?
```
共 9 个占位符 (1 for ST_GeomFromWKB + 8 for 属性)

**参数绑定**:
- idx=1: `stmt.setBytes(1, writeWkb(geometry))` → WKB 数据
- idx=2: `stmt.setInt(2, sourceSrid)` → SRID (4326)
- idx=3-10: 8 个属性值

**占位符总数: 9 个**
**参数设置: 9 个**

这应该是匹配的...但错误说"10 个占位符，9 列"。

---

## 根本原因分析

### 可能原因 1: propertyNames 数量与 propList.size() 不一致

`propertyNames` 来自 `schema.getAttributeDescriptors()` 的过滤结果，而 `feature.getAttribute(prop)` 可能返回不同的属性。

如果 Shapefile 中的某些属性在 schema 中被过滤掉，但 feature 仍然尝试获取，就会导致占位符数量多于列数。

### 可能原因 2: geometry 列名问题

某些 Shapefile 的 geometry 列名可能不是 "geometry" 而是 "the_geom" 或其他名称。如果 `createTableFromSchema` 和 `insertFeature` 使用不同的列名，就会导致不匹配。

```java
// createTableFromSchema 使用 propertyNames 构建列名
for (String prop : propertyNames) {
    columnDefs.add("\"" + colName + "\" TEXT");
}
columnDefs.add("geometry GEOMETRY");  // geometry 是硬编码的

// insertFeature 也使用硬编码的 "geometry"
sql.append("geometry");
```

如果 Shapefile 的实际 geometry 列名不是 "geometry"，这两处代码就会不一致。

### 可能原因 3: 重复的属性列

`propertyNames` 是一个 `HashSet`，如果 Shapefile 的 schema 中有重复的属性名，只会被添加一次。但如果 `propList` 或 feature 访问时出现了重复，就会导致占位符数量多于列数。

### 可能原因 4: ID 列问题

`createTableFromSchema` 创建表时包含了 `id SERIAL PRIMARY KEY` 列，但 `insertFeature` 的 INSERT 语句不包含这个列。这是正确的（SERIAL 列自动生成）。

但如果 SQL 其他地方的逻辑出错了，可能会影响占位符计数。

---

## 精确修复方案

### 修复策略: 添加调试日志并验证 SQL

在 `insertFeature` 方法开头添加 SQL 生成日志，并在构建完 SQL 后验证占位符数量：

```java
private void insertFeature(Connection conn, String tableName,
                            org.locationtech.jts.geom.Geometry geometry,
                            SimpleFeature feature, Set<String> propertyNames,
                            int sourceSrid, int targetSrid) throws Exception {
    StringBuilder sql = new StringBuilder();
    sql.append("INSERT INTO \"").append(dbSchema).append("\".\"").append(tableName).append("\" (");

    List<String> propList = new ArrayList<>(propertyNames);

    // 构建列名
    List<String> columnNames = new ArrayList<>();
    columnNames.add("geometry");
    for (String prop : propList) {
        String colName = prop.replaceAll("[^a-zA-Z0-9_]", "_");
        columnNames.add(colName);
        sql.append(", \"").append(colName).append("\"");
    }

    // VALUES 子句
    String geometryFunc;
    if (sourceSrid == targetSrid) {
        geometryFunc = "ST_GeomFromWKB(?, " + sourceSrid + ")";
    } else {
        geometryFunc = "ST_Transform(ST_GeomFromWKB(?, " + sourceSrid + "), " + targetSrid + ")";
    }

    // 完整构建 VALUES 子句
    StringBuilder valuesSql = new StringBuilder(") VALUES (");
    valuesSql.append(geometryFunc);
    for (int i = 0; i < propList.size(); i++) {
        valuesSql.append(", ?");
    }
    valuesSql.append(")");
    sql.append(valuesSql.toString());

    // 调试日志
    String finalSql = sql.toString();
    long placeholderCount = finalSql.chars().filter(ch -> ch == '?').count();
    logger.info("Generated SQL has {} columns and {} placeholders. SQL: {}",
            columnNames.size(), placeholderCount, finalSql);

    // 验证
    if (placeholderCount != columnNames.size()) {
        throw new IllegalStateException("SQL placeholder mismatch: expected " +
                columnNames.size() + " but found " + placeholderCount + " in SQL: " + finalSql);
    }

    // 执行插入
    try (PreparedStatement stmt = conn.prepareStatement(finalSql)) {
        if (geometry != null) {
            stmt.setBytes(1, writeWkb(geometry));
            stmt.setInt(2, sourceSrid);
        } else {
            stmt.setNull(1, Types.OTHER);
            stmt.setNull(2, Types.INTEGER);
        }

        int idx = 3;
        for (String prop : propList) {
            Object value = feature.getAttribute(prop);
            stmt.setString(idx++, value != null ? value.toString() : null);
        }

        stmt.execute();
    }
}
```

---

## 验证检查清单

1. **检查 Shapefile 的 geometry 列名**
   - 是否是 "geometry" 还是 "the_geom"？
   - `createTableFromSchema` 和 `insertFeature` 是否使用相同的列名？

2. **检查 propertyNames 的来源**
   - 是否只包含非 geometry 属性？
   - 是否与 feature.getAttribute() 返回的属性一致？

3. **添加日志验证**
   - 打印生成的 SQL
   - 打印列名数量和占位符数量
   - 打印实际的 feature.getAttribute() 返回的属性数量

4. **测试不同的 Shapefile**
   - 是否有多个不同的 Shapefile 表现不同？
   - 问题是否与特定的 Shapefile 相关？

---

## 修改位置

| 文件 | 行号 | 修改内容 |
|------|------|---------|
| `MultiFormatImportServiceImpl.java` | 286-328 | 添加调试日志和占位符验证逻辑 |
