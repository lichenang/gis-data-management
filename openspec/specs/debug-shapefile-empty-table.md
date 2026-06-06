# Shapefile 导入空表问题诊断报告

## 问题描述

Shapefile 导入后 PostGIS 表为空 (`feature_count=0`)，日志显示解压和建表成功，但几何数据未写入。

## 代码流程分析

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     Shapefile 导入流程                                   │
└─────────────────────────────────────────────────────────────────────────┘

1. importShapefile()                                                        │
   ├── extractShapefileFromZip() → 返回 .shp 文件 InputStream              │
   ├── parseShpFile() → 解析二进制，返回 ShapefileData                      │
   ├── createSimpleTable() → 创建 PostGIS 表                               │
   └── insertShapefileRecords() → 批量写入几何数据                         │
                                                                            ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  关键数据流:                                                              │
│  parseShpFile() 返回 ShapefileData {                                     │
│    geometries: List<Geometry> ← 这里是关键！                            │
│  }                                                                       │
│                                                                            │
│  insertShapefileRecords() 遍历 data.geometries 并写入                    │
└─────────────────────────────────────────────────────────────────────────┘
```

## 诊断结果

### ✅ 问题 1: SimpleFeatureCollection 是否为空？

**状态**: 不适用 - 代码未使用 GeoTools SimpleFeatureCollection

代码使用自定义二进制解析（直接读取 .shp 文件头和记录），未使用 GeoTools 的 SimpleFeatureCollection。

---

### ❌ 问题 2: 记录头未跳过 (关键 BUG)

**位置**: `MultiFormatImportService.java:470-480`

```java
buf.position(100);  // 跳过主文件头

while (buf.hasRemaining()) {
    if (buf.remaining() < 8) break;
    buf.getInt();              // ← 读取记录号（第1条记录 = 1）
    int contentLength = buf.getInt() * 2;

    if (buf.remaining() < contentLength) break;
    int remainingBefore = buf.position();

    int recordShapeType = buf.getInt();  // ← 错误！读取的是内容长度，不是 shape type
```

**问题分析**:

Shapefile 文件结构:
```
主文件头 (100 bytes):
  [0-3]   File Code = 9994
  [4-7]   Unused
  [8-11]  File Length
  [12-15] Version = 1000
  [16-19] Shape Type (如 1=Point, 5=Polygon)
  [20-99] Bounding Box

记录 1 (变长):
  [100-103] Record Number = 1
  [104-107] Content Length (以 16-bit words 为单位)
  [108+]    Geometry Data (实际 shape type 在这里!)
```

**当前代码问题**:
- 位置 100 开始是记录 1 的头：记录号(4字节) + 内容长度(4字节)
- 代码直接 `buf.getInt()` 读取 shape type → 读到的是"记录号(值为1)"，不是 shape type
- 正确的 shape type 在位置 108，但代码读错了

**结果**: recordShapeType 始终为 1 (Point)，即使实际是 Polygon！

---

### ⚠️ 问题 3: 静默异常的 try-catch

**位置**: `MultiFormatImportService.java:482-489`

```java
try {
    Geometry geom = parseShpRecord(buf, recordShapeType, gf);
    if (geom != null) {
        data.geometries.add(geom);
    }
} catch (Exception e) {
    logger.warn("解析 Shapefile 记录失败: {}", e.getMessage());
}
```

**问题**: 异常被捕获后只打印 warn 日志，geometries 列表不会增加元素。如果解析失败，数据就静默丢失了。

---

### ✅ 问题 4: 事务管理

**位置**: `MultiFormatImportService.java:629-651`

```java
conn.setAutoCommit(false);
try (PreparedStatement stmt = conn.prepareStatement(sql)) {
    for (Geometry geom : data.geometries) {
        if (geom != null) {
            // ... 写入
            stmt.addBatch();
            count++;
        }
    }
    stmt.executeBatch();
    conn.commit();  // ✓ 正确提交
} catch (Exception e) {
    conn.rollback();  // ✓ 正确回滚
    throw new SQLException(e);
}
```

**结论**: 事务管理正确。

---

### ✅ 问题 5: 几何列名

**建表** (`MultiFormatImportService.java:612-614`):
```java
String sql = "CREATE TABLE IF NOT EXISTS \"" + dbSchema + "\".\"" + tableName + "\" (" +
        "id SERIAL PRIMARY KEY, geometry GEOMETRY)";
```

**插入** (`MultiFormatImportService.java:625`):
```java
String sql = "INSERT INTO \"" + dbSchema + "\".\"" + tableName + "\" (geometry) VALUES (ST_GeomFromWKB(?))";
```

**结论**: 列名 "geometry" 匹配，正确。

---

### ⚠️ Feature Count 估算问题

**位置**: `MultiFormatImportService.java:292-296`

```java
int fileLength = buffer.getInt(20) * 2;
int headerSize = 100;
int recordHeaderSize = 8;
int avgRecordSize = estimateAvgRecordSize(header.shapeType);
header.featureCount = (fileLength - headerSize) / (recordHeaderSize + avgRecordSize);
```

**问题**: featureCount 是估算值，不准确。这是表层问题，不是数据写入失败的原因。

## 问题总结

| # | 检查项 | 状态 | 说明 |
|---|--------|------|------|
| 1 | SimpleFeatureCollection | ✅ 不适用 | 未使用 GeoTools API |
| 2 | **记录头未跳过** | ❌ **关键 BUG** | 读错数据位置，shape type 读取错误 |
| 3 | 静默异常 | ⚠️ 风险 | 异常被捕获但不中断流程 |
| 4 | 事务管理 | ✅ 正确 | 正确提交/回滚 |
| 5 | 列名匹配 | ✅ 正确 | geometry 列名匹配 |

## 根本原因

**parseShpFile() 方法中记录头处理逻辑错误**：
- 当前从位置 100 直接读取 shape type，但位置 100 是记录号，不是 shape type
- 正确的 shape type 在位置 108（跳过 8 字节的记录头后）
- 导致所有记录的 shape type 读取错误，parseShpRecord() 返回 null

## 修复建议

1. **修复记录头跳过**:
   ```java
   buf.position(100);  // 跳过主文件头
   
   while (buf.hasRemaining()) {
       if (buf.remaining() < 8) break;
       buf.getInt();              // 跳过记录号
       int contentLength = buf.getInt() * 2;  // 读取内容长度用于跳过
       
       if (buf.remaining() < 4) break;  // 至少需要 4 字节读 shape type
       
       int recordShapeType = buf.getInt();  // 现在才是正确的 shape type
       // ... 处理几何数据 ...
       
       // 跳过剩余的记录内容
       int consumed = 8 + (contentLength - 4);  // 头(8) + 剩余内容
       if (buf.position() - 100 < consumed) {
           buf.position(100 + consumed);
       }
   }
   ```

2. **添加调试日志**:
   - 打印实际解析到的几何数量
   - 打印 shape type 值以便排查

3. **改进错误处理**:
   - 统计失败记录数
   - 如果失败率过高，抛出异常而不是静默继续

## 验证方法

1. 在 parseShpFile 末尾添加日志:
   ```java
   logger.info("解析 Shapefile 完成，共 {} 条几何记录，原始估算 {}", 
       data.geometries.size(), data.featureCount);
   ```

2. 检查 shapefile 二进制结构:
   - 使用十六进制编辑器打开 .shp 文件
   - 确认位置 100-107 是记录 1 的头（记录号=1，内容长度）
   - 位置 108 应该是实际的 shape type
