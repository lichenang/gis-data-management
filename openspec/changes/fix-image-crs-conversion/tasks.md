# Tasks: fix-image-crs-conversion

## Backend Tasks

### Task 1: 创建 CrsTransformUtil 工具类

**File**: `backend/src/main/java/com/gisplatform/util/CrsTransformUtil.java`

**内容**:

```java
package com.gisplatform.util;

import org.geotools.referencing.CRS;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.metadata.iso.citation.CitationImpl;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import lombok.extern.slf4j.Slf4j;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CRS 坐标转换工具类.
 * <p>
 * 提供从任意 CRS 到 EPSG:4326 的 extent 转换功能。
 * </p>
 */
@Slf4j
public class CrsTransformUtil {

    private static final String TARGET_CRS = "EPSG:4326";
    private static final Logger LOGGER = Logger.getLogger(CrsTransformUtil.class.getName());

    /**
     * 将 extent 从源 CRS 转换到 EPSG:4326.
     *
     * @param extent     源坐标系下的 extent [minX, minY, maxX, maxY]
     * @param sourceCrs  源坐标系 EPSG 代码 (如 "EPSG:32650")
     * @return EPSG:4326 下的 extent，如果转换失败返回 null
     */
    public static double[] transformExtentToWgs84(double[] extent, String sourceCrs) {
        if (extent == null || extent.length != 4) {
            LOGGER.log(Level.WARNING, "Invalid extent: " + (extent == null ? "null" : "length=" + extent.length));
            return null;
        }

        if (sourceCrs == null || sourceCrs.isEmpty()) {
            LOGGER.log(Level.WARNING, "Source CRS is empty");
            return null;
        }

        // 如果已经是 EPSG:4326，直接返回
        if ("EPSG:4326".equalsIgnoreCase(sourceCrs)) {
            return extent;
        }

        try {
            CoordinateReferenceSystem source = CRS.decode(sourceCrs);
            CoordinateReferenceSystem target = CRS.decode(TARGET_CRS);

            // 相同 CRS 直接返回
            if (source.equals(target)) {
                return extent;
            }

            MathTransform mathTransform = CRS.findMathTransform(source, target);

            // 转换四个角点 (minX, minY) 和 (maxX, maxY)
            DirectPosition2D minPoint = new DirectPosition2D(extent[0], extent[1]);
            DirectPosition2D maxPoint = new DirectPosition2D(extent[2], extent[3]);

            DirectPosition2D minTransformed = mathTransform.transform(minPoint, null);
            DirectPosition2D maxTransformed = mathTransform.transform(maxPoint, null);

            double[] result = new double[4];
            result[0] = minTransformed.x;
            result[1] = minTransformed.y;
            result[2] = maxTransformed.x;
            result[3] = maxTransformed.y;

            LOGGER.info(String.format("Transformed extent from %s to %s: [%.6f, %.6f, %.6f, %.6f] -> [%.6f, %.6f, %.6f, %.6f]",
                    sourceCrs, TARGET_CRS, extent[0], extent[1], extent[2], extent[3],
                    result[0], result[1], result[2], result[3]));

            return result;

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to transform extent from " + sourceCrs + " to " + TARGET_CRS + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * 检查 CRS 代码是否合法.
     */
    public static boolean isValidCrs(String crsCode) {
        if (crsCode == null || crsCode.isEmpty()) {
            return false;
        }
        try {
            CRS.decode(crsCode);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

### Task 2: 修改 ImageServiceImpl.getImageWmsInfo()

**File**: `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java`

**修改位置**: `getImageWmsInfo()` 方法

**原有代码** (约 373-407 行):

```java
@Override
public ImageWmsInfo getImageWmsInfo(Long id) {
    Dataset dataset = this.getById(id);
    if (dataset == null || dataset.getDeleted() == 1) {
        throw new RuntimeException("影像数据集不存在");
    }

    if (dataset.getWmsUrl() == null || dataset.getWmsUrl().isEmpty()) {
        throw new RuntimeException("影像数据集未发布到 GeoServer");
    }

    ImageWmsInfo info = new ImageWmsInfo();
    info.setLayerName("raster_" + id);
    info.setCrs(dataset.getSrs() != null ? dataset.getSrs() : "EPSG:4326");
    info.setOpacity(0.8);
    info.setWmsUrl(dataset.getWmsUrl());

    // 解析 extent
    if (dataset.getExtent() != null && !dataset.getExtent().isEmpty()) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> extentMap = mapper.readValue(dataset.getExtent(), Map.class);
            double[] extent = new double[4];
            extent[0] = ((Number) extentMap.get("minX")).doubleValue();
            extent[1] = ((Number) extentMap.get("minY")).doubleValue();
            extent[2] = ((Number) extentMap.get("maxX")).doubleValue();
            extent[3] = ((Number) extentMap.get("maxY")).doubleValue();
            info.setExtent(extent);
        } catch (Exception e) {
            log.warn("Failed to parse extent: {}", e.getMessage());
        }
    }

    return info;
}
```

**替换为**:

```java
@Override
public ImageWmsInfo getImageWmsInfo(Long id) {
    Dataset dataset = this.getById(id);
    if (dataset == null || dataset.getDeleted() == 1) {
        throw new RuntimeException("影像数据集不存在");
    }

    if (dataset.getWmsUrl() == null || dataset.getWmsUrl().isEmpty()) {
        throw new RuntimeException("影像数据集未发布到 GeoServer");
    }

    ImageWmsInfo info = new ImageWmsInfo();
    info.setLayerName("raster_" + id);
    info.setOpacity(0.8);
    info.setWmsUrl(dataset.getWmsUrl());

    // 解析 extent 并转换到 EPSG:4326
    if (dataset.getExtent() != null && !dataset.getExtent().isEmpty()) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> extentMap = mapper.readValue(dataset.getExtent(), Map.class);
            double[] extent = new double[4];
            extent[0] = ((Number) extentMap.get("minX")).doubleValue();
            extent[1] = ((Number) extentMap.get("minY")).doubleValue();
            extent[2] = ((Number) extentMap.get("maxX")).doubleValue();
            extent[3] = ((Number) extentMap.get("maxY")).doubleValue();

            // 获取源 CRS 并转换 extent 到 EPSG:4326
            String sourceCrs = dataset.getSrs();
            double[] transformedExtent = CrsTransformUtil.transformExtentToWgs84(extent, sourceCrs);

            if (transformedExtent != null) {
                info.setExtent(transformedExtent);
                info.setCrs("EPSG:4326");
            } else {
                // 转换失败，使用原始值作为后备
                info.setExtent(extent);
                info.setCrs(sourceCrs != null ? sourceCrs : "EPSG:4326");
                log.warn("Using original extent without transformation for dataset {}", id);
            }

        } catch (Exception e) {
            log.warn("Failed to parse extent for dataset {}: {}", id, e.getMessage());
        }
    }

    return info;
}
```

**新增 import**:

```java
import com.gisplatform.util.CrsTransformUtil;
```

---

## Implementation Order

1. Task 1: 创建 CrsTransformUtil.java
2. Task 2: 修改 getImageWmsInfo()

---

## Dependencies

无新增依赖。GeoTools `gt-referencing` 已在 pom.xml 中：

```xml
<dependency>
    <groupId>org.geotools</groupId>
    <artifactId>gt-referencing</artifactId>
    <version>32.0</version>
</dependency>
```

---

## Regression Test Cases

### Test Case 1: EPSG:4326 (无转换)

```sql
-- 准备数据
UPDATE dataset SET srs = 'EPSG:4326', extent = '{"minX":113.0,"minY":36.0,"maxX":113.2,"maxY":36.2}' WHERE id = 1;

-- 调用 API
GET /api/v1/images/1/wms-url

-- 验证返回的 extent 仍为 [113.0, 36.0, 113.2, 36.2]
-- 验证 info.crs = "EPSG:4326"
```

### Test Case 2: EPSG:3857 (需转换)

```sql
-- 准备数据 (约 113°E, 36°N 在 EPSG:3857 中)
UPDATE dataset SET srs = 'EPSG:3857', extent = '{"minX":12587320,"minY":4113156,"maxX":12589520,"maxY":4115356}' WHERE id = 2;

-- 调用 API
GET /api/v1/images/2/wms-url

-- 验证返回的 extent 约为 [113.1, 36.0, 113.1, 36.1]
-- 验证 info.crs = "EPSG:4326"
```

### Test Case 3: EPSG:32650 (UTM Zone 50N)

```sql
-- 准备数据 (约 113°E, 36°N 在 EPSG:32650 中)
UPDATE dataset SET srs = 'EPSG:32650', extent = '{"minX":621063,"minY":3990027,"maxX":622663,"maxY":3991627}' WHERE id = 3;

-- 调用 API
GET /api/v1/images/3/wms-url

-- 验证返回的 extent 约为 [113.1, 36.0, 113.1, 36.1]
-- 验证 info.crs = "EPSG:4326"
```

### Test Case 4: 空 extent

```sql
UPDATE dataset SET extent = NULL WHERE id = 4;

GET /api/v1/images/4/wms-url

-- 验证返回成功，info.extent = null (无异常)
```

### Test Case 5: 已发布数据的完整测试

```sql
-- 检查所有已发布影像
SELECT id, name, srs, extent FROM dataset WHERE type = 'raster' AND status = 'published';

-- 对每个影像调用 API
GET /api/v1/images/{id}/wms-url

-- 验证所有返回的 extent 在 [-180,180] x [-90,90] 范围内
```

---

## Notes

- GeoTools CRS 转换自动处理 UTM、Mercator 等各种投影
- 不需要手动处理 EPSG:3857 或其他特定 CRS
- 转换失败时回退到原始值，保证接口可用性
- 已发布数据需要重新调用发布接口或手动更新 extent 字段
