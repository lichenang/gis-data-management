# GeoTools 影像解析模块修正方案

## 问题概述

影像上传模块编译失败，主要原因：
1. **缺少 GeoTIFF 模块依赖** - pom.xml 中未添加 `gt-geotiff`
2. **Import 路径问题** - GeoTools 32.x 对包结构进行了调整

## 当前依赖分析

### pom.xml 中已有的 GeoTools 依赖

```xml
<!-- 已有依赖 -->
<dependency>
    <groupId>org.geotools</groupId>
    <artifactId>gt-main</artifactId>       <!-- 基础模块 -->
</dependency>
<dependency>
    <groupId>org.geotools.jdbc</groupId>
    <artifactId>gt-jdbc-postgis</artifactId> <!-- PostGIS 支持 -->
</dependency>
<dependency>
    <groupId>org.geotools</groupId>
    <artifactId>gt-referencing</artifactId>
    <version>32.0</version>
</dependency>
```

### 缺少的依赖

```xml
<!-- 必须添加：GeoTIFF 读写支持 -->
<dependency>
    <groupId>org.geotools</groupId>
    <artifactId>gt-geotiff</artifactId>
    <version>32.0</version>
</dependency>
```

## 依赖版本对照表

| 模块 | artifactId | 版本 | 用途 |
|------|-----------|------|------|
| gt-main | gt-main | 32.0 | 核心 API |
| gt-geotiff | **gt-geotiff** | **32.0** | **GeoTIFF 读写（必需）** |
| gt-referencing | gt-referencing | 32.0 | 坐标参考系统 |
| gt-jdbc-postgis | gt-jdbc-postgis | 32.0 | PostGIS 数据库支持 |

## Import 路径修正

### GeoTiffParser.java 当前 Import（存在问题）

| 行号 | 当前 Import | 状态 | 修正后 Import |
|------|-------------|------|---------------|
| 5 | `org.geotools.coverage2.grid.GridCoverage2D` | ✓ 正确 |
| 6 | `org.geotools.gce.geotiff.GeoTiffReader` | ⚠️ 需要 gt-geotiff 依赖 |
| 7 | `org.geotools.api.coverage.SampleDimension` | ✓ 正确 |
| 8 | `org.geotools.api.coverage.grid.GridGeometry` | ✓ 正确 |
| 9 | `org.geotools.api.referencing.crs.CoordinateReferenceSystem` | ✓ 正确 |
| 51 | `org.geotools.api.geometry.Envelope` | ✓ 正确 |
| 101 | `org.geotools.api.coverage.SampleType` | ✓ 正确 |

### 修正说明

GeoTools 32.x 包结构变更：

```
旧路径 (< 30.x)              →  新路径 (32.x)
─────────────────────────────────────────────────────────
org.opengis.coverage.*        →  org.geotools.api.coverage.*
org.opengis.referencing.*     →  org.geotools.api.referencing.*
org.geotools.coverage.grid.*  →  org.geotools.coverage2.grid.*
```

## 修正后的代码

### 1. pom.xml 添加依赖

在 `<dependencies>` 标签内添加：

```xml
<!-- GeoTools GeoTIFF 支持（必需） -->
<dependency>
    <groupId>org.geotools</groupId>
    <artifactId>gt-geotiff</artifactId>
    <version>32.0</version>
</dependency>
```

### 2. GeoTiffParser.java 完整修正

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

## 实施步骤

### 步骤 1：修改 pom.xml

在 `backend/pom.xml` 的 `<dependencies>` 中添加：

```xml
<dependency>
    <groupId>org.geotools</groupId>
    <artifactId>gt-geotiff</artifactId>
    <version>32.0</version>
</dependency>
```

### 步骤 2：验证

执行 Maven 编译：

```bash
cd backend
mvn compile
```

预期结果：无编译错误

## 问题排查

### 错误 1: Cannot find GeoTiffReader

```
java.lang.NoClassDefFoundError: org/geotools/gce/geotiff/GeoTiffReader
```

**原因**: 缺少 gt-geotiff 依赖  
**解决**: 添加 gt-geotiff 依赖到 pom.xml

### 错误 2: Package does not exist: org.opengis.*

```
error: package org.opengis.coverage does not exist
```

**原因**: 使用了旧版 GeoTools API  
**解决**: 将 `org.opengis.*` 改为 `org.geotools.api.*`

### 错误 3: Cannot find GridCoverage2D

```
error: cannot find symbol: class GridCoverage2D
```

**原因**: GridCoverage2D 从 org.geotools.coverage.grid 移至 org.geotools.coverage2.grid  
**解决**: 使用正确的 import 路径

## 附录：GeoTools 32.x 常用 API 对照

| 功能 | 旧版 API | 新版 API |
|------|---------|---------|
| 读取 GeoTIFF | org.geotools.gce.geotiff.GeoTiffReader | 保持不变 |
| GridCoverage2D | org.geotools.coverage.grid.GridCoverage2D | org.geotools.coverage2.grid.GridCoverage2D |
| SampleDimension | org.opengis.coverage.SampleDimension | org.geotools.api.coverage.SampleDimension |
| GridGeometry | org.opengis.coverage.grid.GridGeometry | org.geotools.api.coverage.grid.GridGeometry |
| CoordinateReferenceSystem | org.opengis.referencing.crs.CoordinateReferenceSystem | org.geotools.api.referencing.crs.CoordinateReferenceSystem |
| Envelope | org.geotools.geometry.Envelope2D | org.geotools.api.geometry.Envelope |
