# 多格式矢量数据导入/导出规范

## 1. 概述

本规范定义了通用的矢量数据导入导出功能，支持多种主流矢量格式的统一处理。

**技术栈：**
- 后端：Spring Boot 3.5 + MyBatis-Plus + GeoTools 32.x
- 数据库：PostgreSQL + PostGIS
- 前端：Vue 3 + Element Plus

**更新历史：**
- 2026-06-05: 扩展支持所有主流矢量格式，完善导出功能

## 2. 支持的格式

### 2.1 导入格式

| 格式 | 扩展名 | GeoTools DataStore | 备注 |
|------|--------|-------------------|------|
| GeoJSON | `.geojson`, `.json` | `GeoJSONDataStore` | RFC 7946 |
| Shapefile | `.shp`, `.zip` | `ShapefileDataStore` | ZIP 包包含 .shp/.shx/.dbf/.prj |
| KML/KMZ | `.kml`, `.kmz` | `KMLDataStore` | KMZ 为 zip 压缩的 KML |
| GML | `.gml` | `GMLDataStore` | OGC 标准格式 |
| GPX | `.gpx` | `GPXDataStore` | GPS 交换格式 |
| TopoJSON | `.topojson`, `.json` | 需转换 | 转换为 GeoJSON 处理 |
| CSV (带坐标) | `.csv` | `CSVDataStore` | 需指定坐标列 |
| WKT | `.wkt` | 自定义解析 | 文本格式 |

### 2.2 导出格式

| 格式 | 扩展名 | 编码 |
|------|--------|------|
| GeoJSON | `.geojson` | UTF-8 |
| Shapefile | `.zip` (含 .shp/.shx/.dbf/.prj) | GBK/UTF-8 |
| KML | `.kml` | UTF-8 |
| CSV | `.csv` | UTF-8 (带 BOM) |

## 3. 后端设计

### 3.1 架构设计

```
┌──────────────────────────────────────────────────────────────────┐
│                     多格式矢量数据处理架构                         │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│   ┌─────────────┐                                                │
│   │ Upload File │                                                │
│   └──────┬──────┘                                                │
│          │                                                       │
│          ▼                                                       │
│   ┌─────────────────────┐                                        │
│   │ FormatDetector      │  自动识别格式                          │
│   └──────────┬──────────┘                                        │
│              │                                                   │
│              ▼                                                   │
│   ┌─────────────────────────────────┐                           │
│   │      VectorDataStoreFactory     │  GeoTools 统一入口          │
│   └────────────┬────────────────────┘                           │
│                │                                                  │
│    ┌───────────┼───────────┬────────────┬─────────┐              │
│    ▼           ▼           ▼            ▼         ▼              │
│ ┌──────┐ ┌────────┐ ┌──────┐ ┌──────┐ ┌──────┐                   │
│ │GeoJSON│ │Shapefile│ │ KML  │ │ GML  │ │ ...  │                   │
│ │ Store │ │ Store  │ │Store │ │Store │ │Store │                   │
│ └──┬───┘ └───┬────┘ └──┬───┘ └──┬───┘ └──┬───┘                   │
│    │          │         │        │        │                        │
│    └──────────┴─────────┴────────┴────────┘                        │
│                       │                                           │
│                       ▼                                           │
│              ┌────────────────┐                                   │
│              │   FeatureSet   │  统一 Feature 抽象                 │
│              └───────┬────────┘                                   │
│                      │                                            │
│                      ▼                                            │
│   ┌─────────────────────────────────────────────┐                │
│   │         MultiFormatImportService             │                │
│   │  - 坐标转换                                  │                │
│   │  - 属性映射                                  │                │
│   │  - 批量插入                                  │                │
│   └─────────────────────┬───────────────────────┘                │
│                         │                                         │
│                         ▼                                         │
│              ┌────────────────────┐                              │
│              │   PostGIS 表        │                              │
│              │  geometry(Geometry, SRID) │                        │
│              └────────────────────┘                              │
│                                                                   │
└───────────────────────────────────────────────────────────────────┘
```

### 3.2 核心类设计

#### 3.2.1 VectorFileFormat 枚举扩展

```java
public enum VectorFileFormat {
    GEOJSON("GeoJSON", new String[]{"geojson", "json"}, true),
    SHAPEFILE("Shapefile", new String[]{"shp", "zip"}, true),
    KML("KML", new String[]{"kml", "kmz"}, true),
    GML("GML", new String[]{"gml"}, true),
    GPX("GPX", new String[]{"gpx"}, true),
    CSV("CSV", new String[]{"csv"}, true),
    WKT("WKT", new String[]{"wkt"}, true),
    TOPOJSON("TopoJSON", new String[]{"topojson", "json"}, false),
    UNKNOWN("Unknown", new String[]{}, false);

    private final String displayName;
    private final String[] extensions;
    private final boolean geoToolsSupported;

    public boolean isGeoToolsSupported() {
        return geoToolsSupported;
    }
}
```

#### 3.2.2 FormatDetector 增强

```java
@Component
public class FormatDetector {

    public VectorFileFormat detect(MultipartFile file, String filename) {
        String extension = FilenameUtils.getExtension(filename).toLowerCase();
        VectorFileFormat format = VectorFileFormat.fromExtension(extension);

        if (format == VectorFileFormat.UNKNOWN) {
            return VectorFileFormat.UNKNOWN;
        }

        // 对于特殊格式进行内容检测
        if (format == VectorFileFormat.GEOJSON ||
            format == VectorFileFormat.TOPOJSON) {
            return detectJsonType(file);
        }

        // KML/KMZ 检测
        if (format == VectorFileFormat.KML && filename.toLowerCase().endsWith(".kmz")) {
            return VectorFileFormat.KML; // KMZ treated as KML
        }

        // Shapefile ZIP 包检测
        if (format == VectorFileFormat.SHAPEFILE && filename.toLowerCase().endsWith(".zip")) {
            return detectZipContent(file);
        }

        return format;
    }

    private VectorFileFormat detectJsonType(MultipartFile file) {
        try {
            String preview = readFirstChars(file.getInputStream(), 4096);
            if (preview.contains("\"type\"") && preview.contains("\"Topology\"")) {
                return VectorFileFormat.TOPOJSON;
            }
            return VectorFileFormat.GEOJSON;
        } catch (Exception e) {
            return VectorFileFormat.GEOJSON;
        }
    }

    private VectorFileFormat detectZipContent(MultipartFile file) {
        // 检测 ZIP 包内是否包含 Shapefile
        try {
            byte[] zipData = file.getBytes();
            Charset charset = detectZipCharset(zipData);
            try (ZipInputStream zis = new ZipInputStream(
                    new ByteArrayInputStream(zipData), charset)) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String name = entry.getName().toLowerCase();
                    if (name.endsWith(".shp")) {
                        return VectorFileFormat.SHAPEFILE;
                    }
                    zis.closeEntry();
                }
            }
        } catch (Exception e) {
            logger.warn("检测 ZIP 内容失败", e);
        }
        return VectorFileFormat.UNKNOWN;
    }
}
```

#### 3.2.3 VectorDataStoreFactory 实现

```java
@Component
public class VectorDataStoreFactory {

    @Autowired
    private FormatDetector formatDetector;

    public DataStore createDataStore(VectorFileFormat format,
                                     MultipartFile file,
                                     String filename) throws IOException {

        switch (format) {
            case SHAPEFILE:
                return createShapefileDataStore(file, filename);
            case GEOJSON:
                return createGeoJSONDataStore(file);
            case KML:
            case KMZ:
                return createKMLDataStore(file);
            case GML:
                return createGMLDataStore(file);
            case GPX:
                return createGPXDataStore(file);
            case CSV:
                return createCSVDataStore(file);
            case WKT:
                throw new UnsupportedOperationException(
                    "WKT 格式需要使用专用解析器");
            case TOPOJSON:
                return createTopoJSONDataStore(file);
            default:
                throw new UnsupportedOperationException(
                    "Unsupported format: " + format);
        }
    }

    private DataStore createShapefileDataStore(MultipartFile file, String filename)
            throws IOException {
        Map<String, Object> params = new HashMap<>();

        if (filename.toLowerCase().endsWith(".zip")) {
            // 解压 ZIP 包
            File tempDir = extractToTempDir(file);
            File shpFile = findFile(tempDir, ".shp");
            params.put("url", shpFile.toURI().toString());
        } else {
            params.put("url", file.getInputStream());
        }

        return DataStoreFinder.getDataStore(params);
    }

    private DataStore createGeoJSONDataStore(MultipartFile file) throws IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("url", file.getInputStream());
        return DataStoreFinder.getDataStore(params);
    }

    private DataStore createKMLDataStore(MultipartFile file) throws IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("url", file.getInputStream());
        return DataStoreFinder.getDataStore(params);
    }

    private DataStore createGMLDataStore(MultipartFile file) throws IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("url", file.getInputStream());
        params.put("GML_REMOVE_NULL_PROPERTIES", true);
        return DataStoreFinder.getDataStore(params);
    }

    private DataStore createGPXDataStore(MultipartFile file) throws IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("url", file.getInputStream());
        params.put("GPX_USE_EXTENSIONS", true);
        return DataStoreFinder.getDataStore(params);
    }

    private DataStore createCSVDataStore(MultipartFile file) throws IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("url", file.getInputStream());
        params.put("csvfile", file.getInputStream());
        params.put("latfield", "latitude");  // 可配置
        params.put("lonfield", "longitude"); // 可配置
        return DataStoreFinder.getDataStore(params);
    }

    private DataStore createTopoJSONDataStore(MultipartFile file) throws IOException {
        // TopoJSON 需要先转换为 GeoJSON
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        JSONObject topoJson = JSONUtil.parseObj(content);
        JSONObject geoJson = convertTopoJSONToGeoJSON(topoJson);
        return createGeoJSONDataStore(toInputStream(geoJson.toString()));
    }
}
```

#### 3.2.4 MultiFormatImportService 实现

```java
@Service
public class MultiFormatImportService {

    @Autowired
    private FormatDetector formatDetector;

    @Autowired
    private VectorDataStoreFactory dataStoreFactory;

    @Autowired
    private CoordinateTransformUtil transformUtil;

    @Autowired
    private DataSource dataSource;

    public DatasetImportResult importToPostGIS(MultipartFile file,
                                               String filename,
                                               String datasetName,
                                               String targetSRS) {
        try {
            // 1. 格式识别
            VectorFileFormat format = formatDetector.detect(file, filename);
            if (format == VectorFileFormat.UNKNOWN) {
                return DatasetImportResult.error("无法识别文件格式");
            }

            // 2. 创建 DataStore 并读取数据
            try (DataStore dataStore =
                     dataStoreFactory.createDataStore(format, file, filename)) {

                // 3. 获取要素类型
                String[] typeNames = dataStore.getTypeNames();
                if (typeNames == null || typeNames.length == 0) {
                    return DatasetImportResult.error("数据源中无要素类型");
                }
                SimpleFeatureSource source =
                    dataStore.getFeatureSource(typeNames[0]);
                SimpleFeatureType schema = source.getSchema();
                SimpleFeatureCollection features = source.getFeatures();

                // 4. 获取坐标系
                CoordinateReferenceSystem sourceCRS =
                    schema.getCoordinateReferenceSystem();
                String sourceSRS = extractSRS(sourceCRS);

                // 5. 坐标转换
                SimpleFeatureCollection transformedFeatures = features;
                if (targetSRS != null && !targetSRS.equals(sourceSRS)) {
                    transformedFeatures = transformUtil.transform(
                        features, sourceCRS,
                        CRS.decode(targetSRS));
                }

                // 6. 创建表并导入
                String tableName = generateTableName(datasetName);
                int count = importFeatures(
                    transformedFeatures, tableName,
                    schema, targetSRS);

                // 7. 推断几何类型
                String geometryType = inferGeometryType(features);

                return DatasetImportResult.success(datasetName, count, geometryType);
            }
        } catch (Exception e) {
            logger.error("导入失败", e);
            return DatasetImportResult.error("导入失败: " + e.getMessage());
        }
    }

    private int importFeatures(SimpleFeatureCollection features,
                               String tableName,
                               SimpleFeatureType schema,
                               String srs) throws Exception {
        // 获取几何属性名
        String geometryAttribute =
            schema.getGeometryDescriptor().getLocalName();

        // 创建表
        String createTableSQL = buildCreateTableSQL(schema, tableName, srs);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
        }

        // 批量插入
        String insertSQL = buildInsertSQL(schema, tableName);
        int count = 0;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            try (SimpleFeatureIterator iterator = features.features()) {
                while (iterator.hasNext()) {
                    SimpleFeature feature = iterator.next();
                    setParameters(pstmt, feature, schema, geometryAttribute);
                    pstmt.addBatch();

                    if (++count % 1000 == 0) {
                        pstmt.executeBatch();
                    }
                }
                pstmt.executeBatch();
            }
        }

        return count;
    }
}
```

### 3.3 Shapefile 多文件处理

Shapefile 由多个文件组成：
- `.shp` - 几何数据
- `.shx` - 几何索引
- `.dbf` - 属性数据
- `.prj` - 坐标系定义
- `.cpg` - 编码

**处理方案：**

```
方案 A: Zip 包上传 (推荐)
┌────────────────────┐
│  data.zip          │
│  ├─ region.shp     │
│  ├─ region.shx     │
│  ├─ region.dbf     │
│  ├─ region.prj     │
│  └─ region.cpg     │
└────────────────────┘
    │
    ▼ 自动解压
┌────────────────────┐
│  ShapefileDataStore│
│  (读取 .shp 即可)  │
└────────────────────┘

方案 B: 多文件分别上传 (未来支持)
分别上传 .shp, .shx, .dbf, .prj 后端组合
```

### 3.4 坐标系转换

```java
@Component
public class CoordinateTransformUtil {

    public SimpleFeatureCollection transform(
            SimpleFeatureCollection features,
            CoordinateReferenceSystem sourceCRS,
            CoordinateReferenceSystem targetCRS) throws Exception {

        MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS);
        SimpleFeatureType sourceType = features.getSchema();
        SimpleFeatureType targetType = createTargetSchema(sourceType, targetCRS);

        List<SimpleFeature> transformedList = new ArrayList<>();

        try (SimpleFeatureIterator iterator = features.features()) {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                SimpleFeature transformed = transformFeature(
                    feature, transform, targetType);
                transformedList.add(transformed);
            }
        }

        return new SimpleFeatureCollectionImpl(
            transformedList, targetType);
    }

    private SimpleFeature transformFeature(SimpleFeature feature,
                                           MathTransform transform,
                                           SimpleFeatureType targetType) {
        // 使用 JTS Transformer 进行坐标转换
    }
}
```

### 3.5 导出功能设计

#### 3.5.1 MultiFormatExportService

```java
@Service
public class MultiFormatExportService {

    @Autowired
    private DataSource dataSource;

    public void exportAsFormat(Long datasetId,
                               VectorFileFormat format,
                               OutputStream outputStream) throws Exception {
        Dataset dataset = datasetMapper.selectById(datasetId);
        if (dataset == null) {
            throw new DatasetException("数据集不存在");
        }

        String tableName = dataset.getTableName();
        List<Feature> features = fetchFeatures(tableName);

        switch (format) {
            case GEOJSON:
                exportGeoJSON(features, outputStream);
                break;
            case SHAPEFILE:
                exportShapefile(features, outputStream);
                break;
            case KML:
                exportKML(features, outputStream);
                break;
            case CSV:
                exportCSV(features, outputStream);
                break;
            default:
                throw new UnsupportedOperationException(
                    "不支持的导出格式: " + format);
        }
    }

    private void exportShapefile(List<Feature> features,
                                 OutputStream outputStream) throws Exception {
        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
            // 创建 .shp
            addToZip(zos, "data.shp", createShapefileBytes(features));
            // 创建 .shx
            addToZip(zos, "data.shx", createShxBytes(features));
            // 创建 .dbf
            addToZip(zos, "data.dbf", createDbfBytes(features));
            // 创建 .prj
            addToZip(zos, "data.prj", createPrjBytes(features));
        }
    }

    private void exportGeoJSON(List<Feature> features,
                               OutputStream outputStream) throws Exception {
        // 生成 RFC 7946 GeoJSON
    }

    private void exportKML(List<Feature> features,
                           OutputStream outputStream) throws Exception {
        // 使用 GeoTools KML Encoder
    }
}
```

## 4. 前端设计

### 4.1 上传组件扩展

```vue
<template>
  <el-upload
    ref="uploadRef"
    class="upload-demo"
    :auto-upload="false"
    :limit="1"
    :on-change="handleFileChange"
    :file-list="fileList"
    accept=".geojson,.json,.shp,.zip,.kml,.kmz,.gml,.gpx,.csv,.topojson"
  >
    <el-button type="primary">
      <el-icon><Upload /></el-icon>
      选择文件
    </el-button>
    <template #tip>
      <div class="el-upload__tip">
        支持格式：
        <el-tag size="small" type="info">GeoJSON</el-tag>
        <el-tag size="small" type="info">Shapefile</el-tag>
        <el-tag size="small" type="info">KML/KMZ</el-tag>
        <el-tag size="small" type="info">GML</el-tag>
        <el-tag size="small" type="info">GPX</el-tag>
        <el-tag size="small" type="info">CSV</el-tag>
        <el-tag size="small" type="info">TopoJSON</el-tag>
      </div>
    </template>
  </el-upload>
</template>
```

### 4.2 导出格式选择

```vue
<el-dropdown @command="handleExportCommand">
  <el-button type="primary">
    导出 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
  </el-button>
  <template #dropdown>
    <el-dropdown-menu>
      <el-dropdown-item command="geojson">GeoJSON</el-dropdown-item>
      <el-dropdown-item command="shapefile">Shapefile (.zip)</el-dropdown-item>
      <el-dropdown-item command="kml">KML</el-dropdown-item>
      <el-dropdown-item command="csv">CSV</el-dropdown-item>
    </el-dropdown-menu>
  </template>
</el-dropdown>
```

## 5. 数据库设计

### 5.1 Dataset 表扩展

```sql
ALTER TABLE dataset ADD COLUMN IF NOT EXISTS format VARCHAR(50);
ALTER TABLE dataset ADD COLUMN IF NOT EXISTS srs VARCHAR(50);
ALTER TABLE dataset ADD COLUMN IF NOT EXISTS geometry_type VARCHAR(50);
ALTER TABLE dataset ADD COLUMN IF NOT EXISTS feature_count INTEGER;
ALTER TABLE dataset ADD COLUMN IF NOT EXISTS properties JSONB;
```

### 5.2 存储格式

PostGIS 使用通用 `geometry` 类型存储：

```sql
CREATE TABLE region (
    id SERIAL PRIMARY KEY,
    dataset_id BIGINT REFERENCES dataset(id),
    geometry geometry(Geometry, 4326),
    properties JSONB
);

-- 空间索引
CREATE INDEX idx_region_geometry ON region USING GIST(geometry);
```

## 6. 实施计划

### Phase 1: 基础设施
- [ ] 扩展 VectorFileFormat 枚举
- [ ] 增强 FormatDetector
- [ ] 实现 VectorDataStoreFactory

### Phase 2: 导入功能
- [ ] GeoJSON 导入 (已有部分)
- [ ] Shapefile 导入 (已有部分)
- [ ] KML/KMZ 导入
- [ ] GML 导入
- [ ] GPX 导入
- [ ] CSV 导入
- [ ] TopoJSON 导入

### Phase 3: 导出功能
- [ ] GeoJSON 导出 (已有)
- [ ] Shapefile 导出
- [ ] KML 导出
- [ ] CSV 导出

### Phase 4: 前端
- [ ] 多格式文件选择器
- [ ] 导出格式下拉菜单
- [ ] 格式自动识别提示

## 7. 风险与注意事项

### 7.1 编码问题
- Shapefile .dbf 文件使用不同编码（GBK/UTF-8）
- 解决方案：添加 .cpg 文件或自动检测编码

### 7.2 坐标系
- 默认使用 EPSG:4326 (WGS84)
- 导入时自动转换目标坐标系

### 7.3 性能
- 大文件批量插入使用 JDBC batch
- 建议文件大小限制：100MB

### 7.4 内存
- GeoTools DataStore 流式处理避免内存溢出
- 使用 SimpleFeatureIterator 而非全部加载
