# GeoTools Import 修正方案

## 问题概述

GeoTools 32.x 版本对包结构进行了重大调整，`org.opengis.*` 和部分 `org.geotools.*` 包的路径发生变化。

## 依赖版本

```
GeoTools: 32.0
JTS: 1.19.0
```

## 错误 Import 修正对照表

### GeoTiffParser.java

| 行号 | 当前错误的 Import | 正确的 Import (GeoTools 32.x) |
|------|------------------|------------------------------|
| 5 | `org.geotools.coverage.CoverageFactoryFinder` | **删除**（未使用）|
| 6 | `org.geotools.coverage.grid.GridCoverage2D` | ✓ 可用，但推荐 `org.geotools.coverage2.GridCoverage2D` |
| 7 | `org.geotools.coverage.grid.io.AbstractGridCoverageWriter` | **删除**（未使用）|
| 8 | `org.geotools.coverage.grid.io.AbstractGridFormat` | **删除**（未使用）|
| 9 | `org.geotools.coverage.grid.io.GridFormatFinder` | **删除**（未使用）|
| 10 | `org.geotools.gce.geotiff.GeoTiffFormat` | **删除**（未使用）|
| 11 | `org.geotools.gce.geotiff.GeoTiffReader` | ✓ 可用 |
| 12 | `org.geotools.geometry.Envelope2D` | **删除**（未使用）|
| 13 | `org.opengis.coverage.SampleDimension` | `org.geotools.api.coverage.SampleDimension` |
| 14 | `org.opengis.coverage.grid.GridGeometry` | `org.geotools.api.coverage.grid.GridGeometry` |
| 15 | `org.opengis.referencing.crs.CoordinateReferenceSystem` | `org.geotools.api.referencing.crs.CoordinateReferenceSystem` |
| 16 | `org.opengis.referencing.cs.CoordinateSystemAxis` | `org.geotools.api.referencing.cs.CoordinateSystemAxis` |
| 19 | `java.awt.image.DataBuffer` | **删除**（未使用）|

### GeoTools 32.x 新 API 路径映射

```
旧路径 (GeoTools < 32)          →  新路径 (GeoTools 32.x)
─────────────────────────────────────────────────────────────
org.opengis.coverage.*           →  org.geotools.api.coverage.*
org.opengis.coverage.grid.*      →  org.geotools.api.coverage.grid.*
org.opengis.referencing.*        →  org.geotools.api.referencing.*
org.opengis.parameter.*          →  org.geotools.api.parameter.*
org.opengis.metadata.*           →  org.geotools.api.metadata.*
org.opengis.util.*               →  org.geotools.api.util.*
org.geotools.coverage.grid.GridCoverage2D → org.geotools.coverage2.grid.GridCoverage2D
org.geotools.geometry.Envelope2D →  org.geotools.api.geometry.Envelope (使用 JTS)
org.geotools.coverage.CoverageFactoryFinder → 已废弃，使用 GridFormatFinder
```

## 需要添加的 Maven 依赖

确认 `gt-api` 模块已包含在依赖中（通常 `gt-main` 已包含）：

```xml
<dependency>
    <groupId>org.geotools</groupId>
    <artifactId>gt-api</artifactId>
    <version>32.0</version>
</dependency>
```

## 修正后的 Import 列表 (GeoTiffParser.java)

```java
package com.gisplatform.util;

import cn.hutool.json.JSONUtil;
import com.gisplatform.entity.RasterMetadata;
import org.geotools.coverage2.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.api.coverage.SampleDimension;
import org.geotools.api.coverage.grid.GridGeometry;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
```

## 注释缺失清单

根据 `config.yaml` 注释规范（强制），以下文件缺少 Javadoc：

### 1. RasterMetadata.java
- [x] 类级别 Javadoc ✓（已有 @Schema 注释）
- [ ] 缺少字段的 @Schema 注释说明

### 2. RasterMetadataMapper.java
- [ ] 缺少类级别 Javadoc
- [ ] 缺少接口方法的 Javadoc

### 3. GeoTiffParser.java
- [ ] 缺少类级别 Javadoc（描述职责：GeoTIFF 文件解析工具类）
- [ ] 缺少 `parse()` 方法的 Javadoc（参数、返回值、抛出异常）
- [ ] 缺少 `getPixelType()` 方法的 Javadoc
- [ ] 缺少 `extractCrs()` 方法的 Javadoc

### 4. ImageService.java
- [ ] 缺少类级别 Javadoc
- [ ] 缺少接口方法的 Javadoc

### 5. ImageServiceImpl.java
- [ ] 缺少类级别 Javadoc
- [ ] 缺少 `uploadImage()` 方法的 Javadoc
- [ ] 缺少 `listImages()` 方法的 Javadoc

### 6. ImageController.java
- [x] 类级别 Javadoc ✓（已有 @Tag 注释）
- [ ] 缺少 `upload()` 方法的详细 Javadoc
- [ ] 缺少 `list()` 方法的详细 Javadoc

### 7. MinioConfig.java
- [ ] 缺少类级别 Javadoc
- [ ] 缺少 `@Value` 配置项的注释

## 完整的修正代码

### GeoTiffParser.java (修正后)

```java
package com.gisplatform.util;

import cn.hutool.json.JSONUtil;
import com.gisplatform.entity.RasterMetadata;
import org.geotools.coverage2.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.api.coverage.SampleDimension;
import org.geotools.api.coverage.grid.GridGeometry;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * GeoTIFF 文件解析工具类。
 * <p>
 * 使用 GeoTools 32.x 读取 GeoTIFF 文件的元数据信息，
 * 包括坐标系、分辨率、波段数、空间范围等。
 * </p>
 *
 * @author GIS Platform Team
 * @since 1.0.0
 */
public class GeoTiffParser {

    /**
     * 解析 GeoTIFF 文件并提取元数据。
     *
     * @param file 上传的 GeoTIFF 文件
     * @return 包含文件元数据的 RasterMetadata 对象
     * @throws Exception 文件读取或解析失败时抛出
     */
    public static RasterMetadata parse(MultipartFile file) throws Exception {
        RasterMetadata metadata = new RasterMetadata();

        metadata.setFileName(file.getOriginalFilename());
        metadata.setFileSize(file.getSize());
        metadata.setValidationStatus("pending");

        try (InputStream inputStream = file.getInputStream()) {
            GeoTiffReader reader = new GeoTiffReader(inputStream);
            GridCoverage2D coverage = reader.read(null);

            if (coverage == null) {
                throw new Exception("无法读取 GeoTIFF 文件");
            }

            GridGeometry gridGeometry = coverage.getGridGeometry();
            org.geotools.api.geometry.Envelope envelope = coverage.getEnvelope();

            metadata.setWidth(gridGeometry.getGridRange().getSpan(0));
            metadata.setHeight(gridGeometry.getGridRange().getSpan(1));

            metadata.setBands(coverage.getNumSampleDimensions());

            String pixelType = getPixelType(coverage);
            metadata.setPixelType(pixelType);

            if (coverage.getNumSampleDimensions() > 0) {
                SampleDimension sd = coverage.getSampleDimension(0);
                double noData = sd.getNoDataValues().peek();
                if (!Double.isNaN(noData)) {
                    metadata.setNoDataValue(noData);
                }
            }

            String crs = extractCrs(coverage.getCoordinateReferenceSystem());
            metadata.setCrs(crs);

            Map<String, Object> transformMap = new HashMap<>();
            transformMap.put("minX", envelope.getMinimum(0));
            transformMap.put("minY", envelope.getMinimum(1));
            transformMap.put("maxX", envelope.getMaximum(0));
            transformMap.put("maxY", envelope.getMaximum(1));
            metadata.setTransform(JSONUtil.toJsonStr(transformMap));

            double[] res = new double[2];
            res[0] = Math.abs(envelope.getSpan(0) / metadata.getWidth());
            res[1] = Math.abs(envelope.getSpan(1) / metadata.getHeight());
            metadata.setOverviews(JSONUtil.toJsonStr(res));

            reader.dispose();
        }

        return metadata;
    }

    /**
     * 获取影像像素类型。
     *
     * @param coverage GridCoverage2D 对象
     * @return 像素类型字符串（如 "UnsignedByte", "Float32" 等）
     */
    private static String getPixelType(GridCoverage2D coverage) {
        if (coverage.getNumSampleDimensions() == 0) {
            return "Unknown";
        }
        SampleDimension sd = coverage.getSampleDimension(0);
        org.geotools.api.coverage.SampleType type = sd.getSampleType();
        if (type == null) {
            return "Unknown";
        }
        return type.toString();
    }

    /**
     * 提取坐标参考系统标识符。
     *
     * @param crs CoordinateReferenceSystem 对象
     * @return EPSG 代码字符串（如 "EPSG:4326"）
     */
    private static String extractCrs(CoordinateReferenceSystem crs) {
        if (crs == null) {
            return null;
        }
        String code = null;
        try {
            if (crs.getIdentifiers() != null && !crs.getIdentifiers().isEmpty()) {
                code = crs.getIdentifiers().iterator().next().toString();
            }
        } catch (Exception e) {
            // 忽略异常，返回备用值
        }
        if (code == null || code.isEmpty()) {
            code = crs.getName().toString();
        }
        return code;
    }
}
```

## 验证步骤

1. 修改 pom.xml 确保包含 `gt-api` 依赖
2. 修正 `GeoTiffParser.java` 的 import 路径
3. 为所有新建的 Java 文件添加 Javadoc 注释
4. 执行 Maven 编译验证：
   ```bash
   cd backend
   mvn compile
   ```

## 风险评估

| 风险项 | 等级 | 缓解措施 |
|--------|------|----------|
| GeoTools API 变更导致运行时错误 | 中 | 使用 try-catch 包装解析逻辑 |
| 部分 GeoTIFF 格式不支持 | 低 | 返回友好错误信息 |
| 未使用的 import 导致编译警告 | 低 | 清理未使用的 import |
