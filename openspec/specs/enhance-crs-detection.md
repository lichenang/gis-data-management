# enhance-crs-detection

## Context

当前 Shapefile 导入流程存在坐标系识别问题，导致中国常用坐标系（CGCS2000/EPSG:4490）被错误处理，数据定位出现巨大偏差。

### 问题场景

用户上传使用 CGCS2000 坐标系的中国山西 Shapefile 数据后：
- 地图上显示位置跑到国外（伊朗/波斯湾地区）
- 实际坐标：山西 (110°E~115°E, 34°N~40°N)
- 被误读为：(113.5°N, 36.2°E) → 位于伊朗

### 根本原因

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        当前 CrsTransformUtil.getEpsgCode() 逻辑              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  public static int getEpsgCode(CoordinateReferenceSystem crs) {             │
│      if (crs == null) return 0;                                             │
│                                                                             │
│      // 方法1: 从 identifiers 获取 (大多数 CRS 都有)                         │
│      if (crs.getIdentifiers() != null && !crs.getIdentifiers().isEmpty()) { │
│          return Integer.parseInt(code);                                     │
│      }                                                                      │
│                                                                             │
│      // 方法2: 从 name 字符串中查找 "EPSG:"                                  │
│      String name = crs.getName().toString();                                │
│      if (name.contains("EPSG:")) {                                          │
│          return Integer.parseInt(...);                                      │
│      }                                                                      │
│                                                                             │
│      // 找不到就返回 0 ← 问题所在!                                           │
│      return 0;                                                              │
│  }                                                                          │
│                                                                             │
│  CGCS2000 的 prj 文件通常写成:                                              │
│  "GCS_China_Geodetic_Coordinate_System_2000"                                │
│  不包含 "EPSG:" 字符串，identifiers 可能为空或使用非 EPSG 标识               │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 问题链

```
MultiFormatImportServiceImpl.importUsingDataStore()
        │
        ▼
nativeCrs = schema.getGeometryDescriptor().getCRS()
        │
        ▼
nativeSrid = CrsTransformUtil.getEpsgCode(nativeCrs)  // 返回 0!
        │
        ▼
insertFeature(conn, tableName, geom, ..., nativeSrid)
        │
        ▼
ST_Transform(ST_GeomFromWKB(?, 0), 4326)  ← ST_Transform 忽略 SRID=0
        │
        ▼
或者:
ST_Transform(ST_GeomFromWKB(?, 4326), 4326)  ← nativeSrid > 0 ? nativeSrid : 4326
        │                                         ↑
        │                                    当 nativeSrid=0 时
        ▼                                    使用 4326 作为源!
数据直接以原始坐标存入，但被当作 4326 解释
        │
        ▼
如果原始坐标是投影坐标系(XY)但被当作经纬度(LonLat)解释...
```

## Goals / Non-Goals

**Goals:**
- 建立中国常用坐标系名称到 EPSG 代码的映射表
- 增强 `CrsTransformUtil.getEpsgCode()` 方法的识别能力
- 改进 `MultiFormatImportServiceImpl` 的错误处理和回退逻辑
- 添加明确的日志记录和异常信息

**Non-Goals:**
- 不修改现有的 extent 转换逻辑
- 不修改 GeoJSON 导入逻辑（已有正确的坐标系处理）
- 不修改影像数据的坐标系处理
- 不添加新的 API 接口

## 设计决策

### Decision 1: 建立静态映射表而非运行时查询

**方案**：在 `CrsTransformUtil` 中添加静态 `Map<String, Integer>` 映射表

**理由**：
- 中国坐标系种类有限，映射表可以穷举
- 静态表查询 O(1)，性能最优
- 不引入外部依赖，保持简单

```java
// 关键映射项
"GCS_China_Geodetic_Coordinate_System_2000" → 4490
"CGCS2000" → 4490
"China_2000" → 4490
"GCS_WGS_1984" → 4326
"Beijing_1954" → 4214
"Xian_1980" → 4610
```

### Decision 2: 多级识别策略

**方案**：按优先级尝试以下方法获取 EPSG 代码：

1. **Identifier 提取**（最高优先级）：从 `crs.getIdentifiers()` 提取
2. **名称匹配**（中等优先级）：在映射表中查找完整名称或关键词
3. **EPSG 字符串提取**（次高优先级）：从 `crs.getName()` 提取 "EPSG:" 模式

### Decision 3: 识别失败时的处理策略

**方案**：在 `MultiFormatImportServiceImpl` 中实现分层处理：

| 情况 | 处理策略 |
|------|---------|
| nativeSrid > 0 | 正常用于 ST_Transform |
| nativeSrid == 0 && 用户指定了 targetSrs | 使用 targetSrs，记录警告日志 |
| nativeSrid == 0 && 未指定 targetSrs | 抛出明确异常，明确说明坐标系无法识别 |

### Decision 4: 使用 Slf4J 日志框架

**理由**：
- 项目已在 `CrsTransformUtil` 上使用 `@Slf4j` (Lombok)
- `MultiFormatImportServiceImpl` 也使用 Slf4J
- 保持一致性

## 实现设计

### 1. CrsTransformUtil 增强

**文件**: `backend/src/main/java/com/gisplatform/util/CrsTransformUtil.java`

#### 1.1 添加静态映射表

```java
// 中国常用坐标系映射表
private static final Map<String, Integer> CHINA_CRS_MAPPINGS = new HashMap<>();
static {
    // CGCS2000 (China Geodetic Coordinate System 2000)
    CHINA_CRS_MAPPINGS.put("GCS_China_Geodetic_Coordinate_System_2000", 4490);
    CHINA_CRS_MAPPINGS.put("CGCS2000", 4490);
    CHINA_CRS_MAPPINGS.put("China_2000", 4490);
    CHINA_CRS_MAPPINGS.put("GCS_China_2000", 4490);

    // Beijing 1954
    CHINA_CRS_MAPPINGS.put("Beijing_1954", 4214);
    CHINA_CRS_MAPPINGS.put("GCS_Beijing_1954", 4214);

    // Xian 1980
    CHINA_CRS_MAPPINGS.put("Xian_1980", 4610);
    CHINA_CRS_MAPPINGS.put("GCS_Xian_1980", 4610);

    // WGS 84 (显式列出以便统一处理)
    CHINA_CRS_MAPPINGS.put("WGS_1984", 4326);
    CHINA_CRS_MAPPINGS.put("GCS_WGS_1984", 4326);
}
```

#### 1.2 改进 getEpsgCode 方法

```java
/**
 * 获取 CoordinateReferenceSystem 对应的 EPSG 代码
 *
 * @param crs 坐标系对象
 * @return EPSG 代码，如果无法识别返回 0
 */
public static int getEpsgCode(CoordinateReferenceSystem crs) {
    if (crs == null) {
        LOGGER.warn("CRS is null, cannot determine EPSG code");
        return 0;
    }

    try {
        // 优先级 1: 从 identifiers 获取
        if (crs.getIdentifiers() != null && !crs.getIdentifiers().isEmpty()) {
            for (Identifier id : crs.getIdentifiers()) {
                String code = id.getCode();
                // 跳过空代码和非数字代码
                if (code != null && code.matches("\\d+")) {
                    int epsg = Integer.parseInt(code);
                    LOGGER.debug("Found EPSG code from identifier: {}", epsg);
                    return epsg;
                }
            }
        }

        // 优先级 2: 从名称匹配映射表
        String name = crs.getName().toString();
        Integer mappedCode = findInChinaMapping(name);
        if (mappedCode != null) {
            LOGGER.info("Matched CRS '{}' to EPSG:{} via China CRS mapping", name, mappedCode);
            return mappedCode;
        }

        // 优先级 3: 从名称中提取 "EPSG:XXX" 模式
        if (name.contains("EPSG:")) {
            int idx = name.indexOf("EPSG:") + 5;
            String code = name.substring(idx);
            // EPSG 代码可能是 "4490" 或 "EPSG:4490" 格式
            if (code.matches("\\d+")) {
                int epsg = Integer.parseInt(code);
                LOGGER.debug("Extracted EPSG code from name: {}", epsg);
                return epsg;
            }
        }

        // 无法识别
        LOGGER.warn("Cannot determine EPSG code for CRS: '{}'. Consider adding it to CHINA_CRS_MAPPINGS.", name);
        return 0;

    } catch (Exception e) {
        LOGGER.error("Failed to get EPSG code: {}", e.getMessage(), e);
        return 0;
    }
}

/**
 * 在中国坐标系映射表中查找
 */
private static Integer findInChinaMapping(String crsName) {
    if (crsName == null || crsName.isEmpty()) {
        return null;
    }

    // 完整匹配
    if (CHINA_CRS_MAPPINGS.containsKey(crsName)) {
        return CHINA_CRS_MAPPINGS.get(crsName);
    }

    // 包含匹配 (检查关键词)
    for (Map.Entry<String, Integer> entry : CHINA_CRS_MAPPINGS.entrySet()) {
        if (crsName.contains(entry.getKey())) {
            return entry.getValue();
        }
    }

    return null;
}
```

### 2. MultiFormatImportServiceImpl 优化

**文件**: `backend/src/main/java/com/gisplatform/service/impl/MultiFormatImportServiceImpl.java`

#### 2.1 修改 importUsingDataStore 方法签名

需要将 targetSrs 参数传入，以便在识别失败时使用：

```java
// 当前方法签名
private int importUsingDataStore(Connection conn, MultipartFile file, String fileName,
                                  VectorFileFormat format, String tableName, String datasetName)

// 修改后：增加 targetSrs 参数
private int importUsingDataStore(Connection conn, MultipartFile file, String fileName,
                                  VectorFileFormat format, String tableName, String datasetName,
                                  String targetSrs)
```

#### 2.2 修改调用点

```java
// importToPostGIS 方法中
importedCount = importUsingDataStore(conn, file, fileName, format, tableName, datasetName, targetSrs);
```

#### 2.3 改进坐标系处理逻辑

```java
private int importUsingDataStore(Connection conn, MultipartFile file, String fileName,
                                  VectorFileFormat format, String tableName, String datasetName,
                                  String targetSrs) throws Exception {
    DataStore dataStore = vectorDataStoreFactory.createDataStore(format, file, fileName);

    String[] typeNames = dataStore.getTypeNames();
    if (typeNames == null || typeNames.length == 0) {
        throw new IOException("无法获取要素类型");
    }

    FeatureSource<SimpleFeatureType, SimpleFeature> featureSource =
            dataStore.getFeatureSource(typeNames[0]);

    SimpleFeatureType schema = featureSource.getSchema();
    Set<String> propertyNames = new HashSet<>();
    for (var attr : schema.getAttributeDescriptors()) {
        String localName = attr.getName().getLocalPart();
        if (!localName.equalsIgnoreCase("geometry") && !localName.equalsIgnoreCase("the_geom")) {
            propertyNames.add(localName);
        }
    }

    CoordinateReferenceSystem nativeCrs = schema.getGeometryDescriptor().getCoordinateReferenceSystem();
    int nativeSrid = CrsTransformUtil.getEpsgCode(nativeCrs);
    String nativeCrsName = nativeCrs != null ? nativeCrs.getName().toString() : "unknown";

    // 解析 targetSrs 获取目标 EPSG 代码
    int targetEpsg = parseTargetSrs(targetSrs);

    // 确定用于转换的源 SRS
    int sourceSridForTransform;
    if (nativeSrid > 0) {
        // 成功识别，使用原生 SRS
        sourceSridForTransform = nativeSrid;
        logger.info("Identified native CRS: {} (EPSG:{}), will transform to EPSG:{}",
                nativeCrsName, nativeSrid, targetEpsg);
    } else {
        // 无法识别原生 SRS
        if (targetEpsg > 0) {
            // 有用户指定的目标 SRS，使用它作为源（直接存储，不转换）
            logger.warn("Cannot identify native CRS for '{}'. User specified target SRS: {}. " +
                    "Data will be stored in target SRS without transformation. " +
                    "This may cause incorrect positioning if the source data uses a different CRS.",
                    nativeCrsName, targetSrs);
            sourceSridForTransform = targetEpsg;
        } else {
            // 既无法识别原生 SRS，也没有有效的目标 SRS
            throw new IllegalStateException(
                    "Cannot determine source CRS for coordinate transformation. " +
                    "Native CRS: '" + nativeCrsName + "' (EPSG code not found). " +
                    "User-specified target SRS: '" + targetSrs + "' is invalid or not provided. " +
                    "Please ensure your Shapefile has a valid .prj file or specify the correct SRS during upload."
            );
        }
    }

    createTableFromSchema(conn, tableName, propertyNames);

    int count = 0;
    try (org.geotools.feature.FeatureIterator<SimpleFeature> iterator = featureSource.getFeatures().features()) {
        while (iterator.hasNext()) {
            SimpleFeature feature = iterator.next();
            org.locationtech.jts.geom.Geometry geom = (org.locationtech.jts.geom.Geometry) feature.getDefaultGeometry();
            insertFeature(conn, tableName, geom, feature, propertyNames, sourceSridForTransform, targetEpsg);
            count++;
        }
    }

    createSpatialIndex(conn, tableName);

    logger.info("Imported {} features from {} to table '{}'", count, fileName, tableName);
    return count;
}

/**
 * 解析 targetSrs 字符串获取 EPSG 代码
 * 支持格式: "EPSG:4490", "4490", "EPSG:4326"
 */
private int parseTargetSrs(String targetSrs) {
    if (targetSrs == null || targetSrs.isEmpty()) {
        return 4326; // 默认
    }

    try {
        // 提取数字部分
        String code = targetSrs.replaceAll("[^0-9]", "");
        if (code.isEmpty()) {
            return 4326;
        }
        return Integer.parseInt(code);
    } catch (NumberFormatException e) {
        logger.warn("Invalid target SRS format: '{}', using default 4326", targetSrs);
        return 4326;
    }
}
```

#### 2.4 修改 insertFeature 签名和实现

```java
private void insertFeature(Connection conn, String tableName,
                           org.locationtech.jts.geom.Geometry geometry,
                           SimpleFeature feature, Set<String> propertyNames,
                           int sourceSrid, int targetSrid) throws Exception {
    StringBuilder sql = new StringBuilder();
    sql.append("INSERT INTO \"").append(dbSchema).append("\".\"").append(tableName).append("\" (");
    sql.append("geometry");
    List<String> propList = new ArrayList<>(propertyNames);
    for (String prop : propList) {
        String colName = prop.replaceAll("[^a-zA-Z0-9_]", "_");
        sql.append(", \"").append(colName).append("\"");
    }

    // 如果源 SRS 和目标 SRS 相同，不需要转换
    if (sourceSrid == targetSrid) {
        sql.append(") VALUES (ST_GeomFromWKB(?, ").append(sourceSrid).append(")");
    } else {
        sql.append(") VALUES (ST_Transform(ST_GeomFromWKB(?, ").append(sourceSrid).append("), ").append(targetSrid).append(")");
    }
    sql.append(")".append(",?".repeat(propList.size())).append(")");

    try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
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

## 数据流对比

### 修复前

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  修复前: nativeSrid=0 → 错误使用 4326 作为源                                 │
└─────────────────────────────────────────────────────────────────────────────┘

  GeoTools 读取 .prj
        │
        ▼
  nativeCrs = "GCS_China_Geodetic_Coordinate_System_2000"
        │
        ▼
  getEpsgCode() → 0  (无法识别)
        │
        ▼
  sourceSrid = 0 > 0 ? nativeSrid : 4326  → 4326
        │
        ▼
  SQL: ST_Transform(ST_GeomFromWKB(?, 4326), 4326)
        │
        ▼
  PostGIS: 坐标直接存入，不转换!
  但数据实际上是 CGCS2000 经纬度...
```

### 修复后

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  修复后: 识别 CGCS2000 → 正确使用 4490 进行转换                              │
└─────────────────────────────────────────────────────────────────────────────┘

  GeoTools 读取 .prj
        │
        ▼
  nativeCrs = "GCS_China_Geodetic_Coordinate_System_2000"
        │
        ▼
  getEpsgCode()
        │
        ├── 尝试 identifiers → 无
        ├── 查找 China CRS mapping → 找到! 返回 4490
        │
        ▼
  nativeSrid = 4490
        │
        ▼
  sourceSrid = 4490, targetSrid = 4326
        │
        ▼
  SQL: ST_Transform(ST_GeomFromWKB(?, 4490), 4326)
        │
        ▼
  PostGIS: 正确从 CGCS2000 转换到 WGS84
        │
        ▼
  数据显示在正确位置 ✓
```

## 坐标系映射表扩展

### 中国常用坐标系 (需要支持)

| 坐标系名称 | EPSG 代码 | 说明 |
|-----------|----------|------|
| GCS_China_Geodetic_Coordinate_System_2000 | 4490 | CGCS2000 地理坐标系 |
| CGCS2000 | 4490 | CGCS2000 简称 |
| GCS_Beijing_1954 | 4214 | 北京 1954 地理坐标系 |
| GCS_Xian_1980 | 4610 | 西安 1980 地理坐标系 |
| GCS_WGS_1984 | 4326 | WGS 84 地理坐标系 |

### 中国常用投影坐标系 (需要支持)

| 坐标系名称 | EPSG 代码 | 说明 |
|-----------|----------|------|
| CGCS2000 / 3-degree Gauss-Kruger zone 37 | 4491 | 山西东部 |
| CGCS2000 / 3-degree Gauss-Kruger zone 38 | 4492 | 山西中部 |
| CGCS2000 / 3-degree Gauss-Kruger zone 39 | 4493 | 山西西部 |
| Beijing 1954 / 3-degree Gauss-Kruger CM 117E | 2433 | - |
| Beijing 1954 / 3-degree Gauss-Kruger CM 123E | 2434 | - |
| Xian 1980 / 3-degree Gauss-Kruger CM 117E | 2362 | - |
| Xian 1980 / 3-degree Gauss-Kruger CM 123E | 2363 | - |

### prj 文件示例

**CGCS2000 地理坐标系 (EPSG:4490)**:
```
GEOGCS["GCS_China_Geodetic_Coordinate_System_2000",
    DATUM["D_China_2000",
        SPHEROID["CGCS2000",6378137.0,298.257222101]],
    PRIMEM["Greenwich",0.0],
    UNIT["Degree",0.0174532925199433]]
```

**CGCS2000 投影坐标系 (EPSG:4491)**:
```
PROJCS["CGCS2000 / 3-degree Gauss-Kruger zone 38",
    GEOGCS["GCS_China_Geodetic_Coordinate_System_2000",
        DATUM["D_China_2000",
            SPHEROID["CGCS2000",6378137.0,298.257222101]],
        PRIMEM["Greenwich",0.0],
        UNIT["Degree",0.0174532925199433]],
    PROJECTION["Transverse_Mercator"],
    PARAMETER["False_Easting",500000.0],
    PARAMETER["False_Northing",0.0],
    PARAMETER["Central_Meridian",114.0],
    PARAMETER["Scale_Factor",1.0],
    PARAMETER["Latitude_Of_Origin",0.0],
    UNIT["Meter",1.0]]
```

## 测试策略

### 单元测试

1. **CrsTransformUtilTest**
   - 测试 CGCS2000 识别
   - 测试各种映射表匹配
   - 测试无效输入处理

2. **MultiFormatImportServiceTest**
   - 测试有效坐标系转换
   - 测试未知坐标系回退
   - 测试无 prj 文件情况

### 集成测试

1. 上传包含 CGCS2000 prj 的 Shapefile
2. 验证数据库中 geometry 的 SRID = 4326
3. 验证 ST_X, ST_Y 返回合理值（中国范围内）
4. 在地图上验证位置正确

### 手动验证

1. 下载中国山西 Shapefile 测试数据
2. 上传到系统
3. 在地图上验证位置在山西

## 风险和权衡

| 风险 | 缓解措施 |
|------|---------|
| 新映射表不完整 | 保留原有识别逻辑作为后备，添加警告日志 |
| 某些 CRS 正确定义了 identifier 但代码错误 | 详细日志记录识别过程 |
| 映射表匹配过于宽松 | 优先完整匹配，其次才用包含匹配 |
| 特殊格式的 prj 文件 | 在异常信息中提示用户检查 prj 文件 |

## 修改位置汇总

| 文件 | 行号 | 修改内容 |
|------|------|---------|
| `CrsTransformUtil.java` | 新增 | 添加 `CHINA_CRS_MAPPINGS` 映射表 |
| `CrsTransformUtil.java` | 83-101 | 重写 `getEpsgCode()` 方法 |
| `CrsTransformUtil.java` | 新增 | 添加 `findInChinaMapping()` 方法 |
| `MultiFormatImportServiceImpl.java` | 163-202 | 修改 `importUsingDataStore()` 签名和逻辑 |
| `MultiFormatImportServiceImpl.java` | 224-253 | 修改 `insertFeature()` 处理不同 SRS |
| `MultiFormatImportServiceImpl.java` | 新增 | 添加 `parseTargetSrs()` 方法 |

## 相关文件

- `backend/src/main/java/com/gisplatform/util/CrsTransformUtil.java`
- `backend/src/main/java/com/gisplatform/service/impl/MultiFormatImportServiceImpl.java`
- `backend/src/main/java/com/gisplatform/service/VectorDataStoreFactory.java`
- `backend/src/main/java/com/gisplatform/service/FormatDetector.java`

## 参考

- [EPSG Registry](https://epsg.org/)
- [GeoTools CRS Documentation](https://docs.geotools.org/latest/userguide/library/referencing/crs.html)
- [CGCS2000 Wikipedia](https://en.wikipedia.org/wiki/China_Geodetic_Coordinate_System_2000)
