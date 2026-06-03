# 数据导出功能设计文档

## 1. 概述

为系统添加数据导出功能，支持矢量数据集（PostGIS）和影像数据集（MinIO）的导出。

---

## 2. 数据集类型与导出格式

```
                        ┌──────────────────────────┐
                        │     GET /api/v1/datasets/ │
                        │   {id}/export?format=xxx  │
                        └────────────┬─────────────┘
                                     │
                        ┌────────────┴────────────┐
                        │                         │
                        ▼                         ▼
              ┌──────────────────┐     ┌──────────────────┐
              │  type="vector"   │     │  type="raster"   │
              │  postgis         │     │  minio           │
              └────────┬─────────┘     └────────┬─────────┘
                       │                        │
              ┌────────┼────────┐               │
              ▼        ▼        ▼               ▼
         ┌────────┐┌────────┐┌────────┐  ┌──────────┐
         │GeoJSON ││Shapefile││  KML   │  │ GeoTIFF  │
         │.geojson││ .zip   ││ .kml   │  │(原始文件) │
         └────────┘└────────┘└────────┘  └──────────┘
```

| 数据集类型 | 存储 | 导出格式 | 技术方案 |
|---|---|---|---|
| vector | PostGIS | GeoJSON | `ST_AsGeoJSON()` + JdbcTemplate |
| vector | PostGIS | Shapefile ZIP | GeoTools PostGIS DataStore → ShapefileDataStore → ZIP |
| vector | PostGIS | KML | `ST_AsKML()` + JdbcTemplate |
| raster | MinIO | GeoTIFF | 直接流式下载原始文件 |

---

## 3. 现有代码分析

### 3.1 已有的 GeoJSON 导出（模板参考）

`DatasetController.java` 已有一个类似端点：

```java
@GetMapping("/{id}/geojson")
public R<String> getGeoJSON(@PathVariable Long id) {
    String geojson = datasetService.getDatasetAsGeoJSON(id);
    return R.ok(geojson);
}
```

`DatasetServiceImpl.getDatasetAsGeoJSON()` 使用 `JdbcTemplate` + `ST_AsGeoJSON`。

### 3.2 可用的技术栈

| 组件 | 用途 |
|---|---|
| `GeoTools 32.0` | 已引入，含 `gt-main`, `gt-jdbc-postgis`, `gt-shapefile`, `gt-geotiff`, `gt-epsg-hsql` |
| `JdbcTemplate` | 直接执行 PostGIS SQL 函数 |
| `MinioClient` | 从 MinIO 读取文件流 |
| `HttpServletResponse` | 流式文件下载 |

---

## 4. API 设计

### 4.1 端点

```
GET /api/v1/datasets/{id}/export?format={format}
```

### 4.2 参数

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | path | 是 | 数据集 ID |
| `format` | query | 是 | 导出格式: `geojson`, `shapefile`, `kml`, `geotiff` |

### 4.3 响应

- **GeoJSON/KML**: 直接返回文件内容，`Content-Type: application/geo+json` / `application/vnd.google-earth.kml+xml`
- **Shapefile**: 返回 ZIP 包，`Content-Type: application/zip`
- **GeoTIFF**: 流式返回原始文件，`Content-Type: image/tiff`
- 所有文件响应携带 `Content-Disposition: attachment; filename="xxx"` 头

### 4.4 错误

| 场景 | HTTP Status | 响应体 |
|---|---|---|
| 数据集不存在 | 404 | `R.fail("数据集不存在")` |
| 不支持的格式 | 400 | `R.fail("不支持的导出格式: xxx")` |
| 数据集类型与格式不匹配 | 400 | `R.fail("矢量数据集不支持 geotiff 格式")` |
| 导出失败 | 500 | `R.fail("导出失败: " + message)` |

---

## 5. 后端实现方案

### 5.1 控制层 — ExportController

新建 `ExportController.java`:

```java
@RestController
@RequestMapping("/api/v1/datasets")
@Tag(name = "数据导出")
public class ExportController {

    @Autowired
    private DatasetService datasetService;

    @GetMapping("/{id}/export")
    @Operation(summary = "导出数据集")
    public void exportDataset(
            @PathVariable Long id,
            @RequestParam String format,
            HttpServletResponse response) {
        // 1. 查询数据集
        Dataset dataset = datasetService.getById(id);
        if (dataset == null || dataset.getDeleted() == 1) {
            throw new RuntimeException("数据集不存在");
        }

        // 2. 根据类型 + 格式分发
        if ("vector".equals(dataset.getType())) {
            if ("geojson".equals(format)) {
                exportGeoJson(dataset, response);
            } else if ("shapefile".equals(format)) {
                exportShapefileZip(dataset, response);
            } else if ("kml".equals(format)) {
                exportKml(dataset, response);
            } else {
                throw new RuntimeException("不支持的导出格式: " + format);
            }
        } else if ("raster".equals(dataset.getType())) {
            if ("geotiff".equals(format)) {
                exportGeoTiff(dataset, response);
            } else {
                throw new RuntimeException("不支持的导出格式: " + format);
            }
        }
    }
}
```

### 5.2 矢量导出 — GeoJSON

使用现有 `datasetService.getDatasetAsGeoJSON()` + 文件下载：

```java
private void exportGeoJson(Dataset dataset, HttpServletResponse response) {
    String geojson = datasetService.getDatasetAsGeoJSON(dataset.getId());
    String filename = dataset.getName() + ".geojson";

    response.setContentType("application/geo+json");
    response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
    response.getWriter().write(geojson);
}
```

### 5.3 矢量导出 — KML

在 `DatasetService` 新增 `getDatasetAsKML()`，使用 `ST_AsKML`：

```java
@Override
public String getDatasetAsKML(Long id) {
    Dataset dataset = this.getById(id);
    String tableName = dataset.getTableName();

    String sql = "SELECT ST_AsKML(geometry) as kml FROM \"" + dbSchema + "\".\"" + tableName + "\"";
    List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

    // 构建 KML 文档
    StringBuilder kml = new StringBuilder();
    kml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    kml.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">");
    kml.append("<Document>");
    kml.append("<name>").append(escapeXml(dataset.getName())).append("</name>");
    for (Map<String, Object> row : rows) {
        kml.append("<Placemark>");
        kml.append(row.get("kml"));
        kml.append("</Placemark>");
    }
    kml.append("</Document>");
    kml.append("</kml>");
    return kml.toString();
}
```

### 5.4 矢量导出 — Shapefile ZIP（核心复杂度）

Shapefile 需要多个文件（`.shp`, `.shx`, `.dbf`, `.prj`），通过 GeoTools 写入后打包 ZIP：

```
                  ┌──────────────────────────────────┐
                  │     Export Shapefile ZIP Flow    │
                  └──────────────────────────────────┘

  1. Connect to PostGIS via GeoTools DataStore
     ┌──────────────────────────────────────────────┐
     │ PostgisNGDataStoreFactory                    │
     │   → dbtype=postgis                           │
     │   → host/port/database/schema                │
     │   → user/password                            │
     └──────────────────────┬───────────────────────┘
                            ▼
  2. Read FeatureCollection from PostGIS table
     ┌──────────────────────────────────────────────┐
     │ dataStore.getFeatureSource(typeName)         │
     │   → FeatureCollection<SimpleFeatureType>     │
     └──────────────────────┬───────────────────────┘
                            ▼
  3. Write to temporary Shapefile via GeoTools
     ┌──────────────────────────────────────────────┐
     │ ShapefileDataStoreFactory.createDataStore()  │
     │   → Write FeatureIterator into Shapefile     │
     │   → Generates .shp, .shx, .dbf, .prj        │
     └──────────────────────┬───────────────────────┘
                            ▼
  4. Package into ZIP and stream to response
     ┌──────────────────────────────────────────────┐
     │ ZipOutputStream → response.getOutputStream() │
     │   → Add each .shp/.shx/.dbf/.prj file        │
     │   → Clean up temp files                      │
     └──────────────────────────────────────────────┘
```

新增服务方法 `exportShapefileAsZip()`：

```java
public void exportShapefileAsZip(Long datasetId, OutputStream outputStream) {
    Dataset dataset = this.getById(datasetId);
    String tableName = dataset.getTableName();

    // 1. 创建临时目录
    Path tempDir = Files.createTempDirectory("shp_export_");
    String baseName = sanitizeFilename(dataset.getName());

    // 2. 通过 GeoTools PostGIS DataStore 读取
    Map<String, Object> params = new HashMap<>();
    params.put(PostgisNGDataStoreFactory.DBTYPE.key, "postgis");
    params.put(PostgisNGDataStoreFactory.HOST.key, dataSource.getUrl());
    params.put(PostgisNGDataStoreFactory.PORT.key, dataSource.getPort());
    params.put(PostgisNGDataStoreFactory.DATABASE.key, dataSource.getDatabase());
    params.put(PostgisNGDataStoreFactory.SCHEMA.key, dbSchema);
    params.put(PostgisNGDataStoreFactory.USER.key, dataSource.getUsername());
    params.put(PostgisNGDataStoreFactory.PASS.key, dataSource.getPassword());

    DataStore pgStore = DataStoreFinder.getDataStore(params);
    String typeName = pgStore.getTypeNames()[0];
    SimpleFeatureSource source = pgStore.getFeatureSource(typeName);
    SimpleFeatureType schema = source.getSchema();

    // 3. 创建 Shapefile DataStore
    File shpFile = new File(tempDir.toFile(), baseName + ".shp");
    Map<String, Serializable> shpParams = new HashMap<>();
    shpParams.put("url", shpFile.toURI().toURL());
    shpParams.put("create spatial index", true);

    ShapefileDataStoreFactory shpFactory = new ShapefileDataStoreFactory();
    ShapefileDataStore shpStore = (ShapefileDataStore) shpFactory.createNewDataStore(shpParams);
    shpStore.createSchema(schema);

    // 4. 写入数据
    Transaction t = new DefaultTransaction("write");
    shpStore.setTransaction(t);
    try (FeatureWriter<SimpleFeatureType, SimpleFeature> writer =
            shpStore.getFeatureWriterAppend(typeName, t);
         FeatureIterator<SimpleFeature> iterator = source.getFeatures().features()) {
        while (iterator.hasNext()) {
            SimpleFeature feature = iterator.next();
            SimpleFeature copy = writer.next();
            copy.setAttributes(feature.getAttributes());
            writer.write();
        }
        t.commit();
    }

    // 5. 打包 ZIP
    try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
        for (File f : tempDir.toFile().listFiles()) {
            if (f.getName().startsWith(baseName)) {
                zos.putNextEntry(new ZipEntry(f.getName()));
                Files.copy(f.toPath(), zos);
                zos.closeEntry();
            }
        }
    }

    // 6. 清理临时文件
    FileUtils.deleteDirectory(tempDir.toFile());
}
```

### 5.5 影像导出 — GeoTIFF

直接从 MinIO 流式下载：

```java
private void exportGeoTiff(Dataset dataset, HttpServletResponse response) throws IOException {
    String filename = dataset.getName() + ".tiff";
    response.setContentType("image/tiff");
    response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

    GetObjectArgs args = GetObjectArgs.builder()
            .bucket(minioProperties.getBucketName())
            .object(dataset.getMinioKey())
            .build();
    try (InputStream is = minioClient.getObject(args);
         OutputStream os = response.getOutputStream()) {
        IOUtils.copy(is, os);
    }
}
```

---

## 6. 前端实现方案

### 6.1 操作列下拉按钮

在数据集管理页面（`datasets/index.vue` 或 `images/index.vue`）的操作列添加下拉按钮：

```vue
<el-table-column label="操作" width="180" fixed="right">
  <template #default="{ row }">
    <el-button type="primary" link @click="handleView(row)">查看</el-button>
    <el-dropdown @command="(format) => handleExport(row, format)">
      <el-button type="success" link>
        导出<el-icon><ArrowDown /></el-icon>
      </el-button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item v-if="row.type === 'vector'" command="geojson">GeoJSON</el-dropdown-item>
          <el-dropdown-item v-if="row.type === 'vector'" command="shapefile">Shapefile (ZIP)</el-dropdown-item>
          <el-dropdown-item v-if="row.type === 'vector'" command="kml">KML</el-dropdown-item>
          <el-dropdown-item v-if="row.type === 'raster'" command="geotiff">GeoTIFF</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </template>
</el-table-column>
```

### 6.2 导出函数

```typescript
const handleExport = (row: Dataset, format: string) => {
  const url = `${baseURL}/api/v1/datasets/${row.id}/export?format=${format}`
  // 触发文件下载
  const link = document.createElement('a')
  link.href = url
  link.setAttribute('download', '')
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}
```

---

## 7. 文件变更清单

| 文件 | 操作 | 说明 |
|---|---|---|
| `.../controller/ExportController.java` | 新增 | 导出端点 |
| `.../service/DatasetService.java` | 修改 | 新增 export 方法声明 |
| `.../service/impl/DatasetServiceImpl.java` | 修改 | 新增 GeoJSON/KML/Shapefile 导出实现 |
| `.../util/ExportUtil.java` | 新增 | 工具方法（ZIP 打包、文件名清理等） |
| `frontend/src/views/datasets/index.vue` | 修改 | 添加导出下拉按钮 |

---

## 8. 未涉及范围

- 不支持矢量数据集导出为 GeoTIFF
- 不支持影像数据集导出为矢量格式
- 不支持进度条（大文件导出直接流式下载）
- 不涉及权限校验（复用现有认证机制）
