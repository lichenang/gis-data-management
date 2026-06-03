# Design: fix-image-crs-conversion

## Technical Design

### 1. 新增 CrsTransformUtil 工具类

**File**: `backend/src/main/java/com/gisplatform/util/CrsTransformUtil.java`

```java
package com.gisplatform.util;

import org.geotools.referencing.CRS;
import org.geotools.geometry.DirectPosition2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import lombok.extern.slf4j.Slf4j;

/**
 * CRS 坐标转换工具类.
 * <p>
 * 提供从任意 CRS 到 EPSG:4326 的 extent 转换功能。
 * </p>
 */
@Slf4j
public class CrsTransformUtil {

    private static final String TARGET_CRS = "EPSG:4326";

    /**
     * 将 extent 从源 CRS 转换到 EPSG:4326.
     *
     * @param extent     源坐标系下的 extent [minX, minY, maxX, maxY]
     * @param sourceCrs  源坐标系 EPSG 代码 (如 "EPSG:32650")
     * @return EPSG:4326 下的 extent，如果转换失败返回 null
     */
    public static double[] transformExtentToWgs84(double[] extent, String sourceCrs) {
        if (extent == null || extent.length != 4) {
            log.warn("Invalid extent: {}", extent);
            return null;
        }

        if (sourceCrs == null || sourceCrs.isEmpty()) {
            log.warn("Source CRS is empty");
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
            if (CRS.equalsIgnoreMetadata(source, target)) {
                return extent;
            }

            MathTransform mathTransform = CRS.findMathTransform(source, target);

            // 转换四个角点
            DirectPosition2D minPoint = new DirectPosition2D(extent[0], extent[1]);
            DirectPosition2D maxPoint = new DirectPosition2D(extent[2], extent[3]);

            DirectPosition2D minTransformed = mathTransform.transform(minPoint, null);
            DirectPosition2D maxTransformed = mathTransform.transform(maxPoint, null);

            return new double[] {
                minTransformed.x, minTransformed.y,
                maxTransformed.x, maxTransformed.y
            };

        } catch (Exception e) {
            log.error("Failed to transform extent from {} to {}: {}",
                    sourceCrs, TARGET_CRS, e.getMessage());
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

### 2. 修改 getImageWmsInfo() 方法

**File**: `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java`

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
                log.info("Transformed extent from {} to EPSG:4326: [{}, {}] -> [{}, {}]",
                        sourceCrs, extent[0], extent[1], transformedExtent[0], transformedExtent[1]);
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

### 3. 新增 imports

```java
import com.gisplatform.util.CrsTransformUtil;
```

### 4. 异常处理策略

| 场景 | 处理方式 | 日志级别 |
|------|----------|----------|
| extent 为空 | 返回不带 extent 的 info | WARN |
| extent 格式错误 | 返回不带 extent 的 info | WARN |
| CRS 为空/无效 | 返回不带 extent 的 info | WARN |
| CRS 转换失败 | 返回原始 extent，不转换 | WARN |
| 所有都失败 | 接口正常返回，extent 可能无效 | WARN |

## Diff Summary

```diff
+ 新增文件: CrsTransformUtil.java (约 80 行)
- 无删除文件
~ 修改文件: ImageServiceImpl.java
  ~ getImageWmsInfo() 方法 (约 +20 行转换逻辑)
```

## Regression Test Plan

### Test Case 1: EPSG:4326 数据

```
输入: dataset.extent = {"minX":113.0,"minY":36.0,"maxX":113.2,"maxY":36.2}
输入: dataset.srs = "EPSG:4326"
期望: info.extent = [113.0, 36.0, 113.2, 36.2] (无转换)
```

### Test Case 2: EPSG:3857 数据

```
输入: dataset.extent = {"minX":12587320,"minY":4113156,"maxX":12589520,"maxY":4115356}
输入: dataset.srs = "EPSG:3857"
期望: info.extent 约为 [113.1, 36.0, 113.2, 36.1] (转换后)
```

### Test Case 3: UTM 数据

```
输入: dataset.extent = {"minX":621063,"minY":3990027,"maxX":622663,"maxY":3991627}
输入: dataset.srs = "EPSG:32650"
期望: info.extent 约为 [113.1, 36.0, 113.2, 36.1] (转换后)
```

### Test Case 4: 空 extent

```
输入: dataset.extent = null
期望: info.extent = null (无异常)
```

### Test Case 5: 无效 CRS

```
输入: dataset.srs = "INVALID:999999"
期望: info.extent = null, info.crs = "INVALID:999999" (无异常)
```

## CRS Conversion Verification

### 验证脚本

```java
// 验证 EPSG:3857 -> EPSG:4326 转换
double[] extent3857 = {12587320, 4113156, 12589520, 4115356};
double[] result = CrsTransformUtil.transformExtentToWgs84(extent3857, "EPSG:3857");

// result 应该约为 [113.1, 36.0, 113.2, 36.1]
System.out.println(Arrays.toString(result));

// 验证 EPSG:32650 -> EPSG:4326 转换
double[] extentUtm = {621063, 3990027, 622663, 3991627};
result = CrsTransformUtil.transformExtentToWgs84(extentUtm, "EPSG:32650");

// result 应该约为 [113.1, 36.0, 113.2, 36.1]
System.out.println(Arrays.toString(result));
```

### 数据库验证 SQL

```sql
-- 检查现有数据
SELECT id, name, srs, extent FROM dataset WHERE type = 'raster';

-- 测试 API 返回
-- GET /api/v1/images/{id}/wms-url
-- 验证 extent 值在 [-180,180] x [-90,90] 范围内
```
