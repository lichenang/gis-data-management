# Proposal: fix-image-crs-conversion

## Summary

修复影像图层 extent 坐标系转换问题，实现从任意 CRS 到 EPSG:4326 的自动转换，确保前端 fit() 正确缩放到影像区域。

## Root Cause

```
┌─────────────────────────────────────────────────────────────────────┐
│                    ROOT CAUSE ANALYSIS                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  Data Flow:                                                         │
│                                                                     │
│  1. GeoTiffParser 提取元数据:                                       │
│     - raster_metadata.crs = "EPSG:32650" (UTM zone 50N)            │
│     - raster_metadata.transform = {minX: 12129263, minY: 4050247,  │
│                                     maxX: 12130944, maxY: 4051356} │
│                                         ↑                           │
│                                         │                          │
│                                         这是 EPSG:32650 坐标       │
│                                         单位：米                   │
│                                                                     │
│  2. publishImageDataset():                                          │
│     - dataset.srs = "EPSG:32650"  ← 正确                           │
│     - dataset.extent = "{\"minX\":12129263,...}"  ← 未转换!        │
│                                                                     │
│  3. getImageWmsInfo():                                              │
│     - info.extent = [12129263, 4050247, 12130944, 4051356]         │
│       ↑                                                             │
│       在 EPSG:32650 中有效                                         │
│       但前端 map view 是 EPSG:4326                                 │
│       12129263 不是有效经度! (应 < 180)                            │
│                                                                     │
│  4. fit([12129263, 4050247, ...]) 在 EPSG:4326 view 中:           │
│     - minX = 12129263° ← 超出 [-180,180] 范围                      │
│     - 地图跳到无效区域或无响应                                      │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

## Goals

1. **通用 CRS 转换逻辑** - 根据 dataset.srs 动态识别源坐标系
2. **自动转换** - 将 extent 转换到 EPSG:4326，无需硬编码特定 CRS
3. **异常处理** - 优雅处理 CRS 解析失败、转换失败等情况
4. **最小改动** - 仅修改 `getImageWmsInfo()` 方法

## Non-Goals

- 不修改数据库结构
- 不修改前端逻辑
- 不修改其他后端方法

## Technical Approach

### GeoTools CRS 转换

使用 GeoTools 的 `gt-referencing` 模块进行 CRS 转换：

```java
import org.geotools.referencing.CRS;
import org.geotools.geometry.DirectPosition2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

// 解码源 CRS 和目标 CRS
CoordinateReferenceSystem sourceCRS = CRS.decode(dataset.getSrs());
CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:4326");

// 获取转换器
MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS);

// 转换每个角点
double[] transformed = transformExtent(extent, transform);
```

### 支持的 CRS 类型

| CRS 类型 | 示例 | 转换方式 |
|----------|------|----------|
| EPSG:4326 | WGS84 经纬度 | 直接返回 |
| EPSG:3857 | Web Mercator | 自动转换 |
| EPSG:326xx | UTM zones | 自动转换 |
| EPSG:327xx | UTM zones (southern) | 自动转换 |
| 其他合法 CRS | 用户定义 | GeoTools 自动处理 |

## Affected Files

- `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java` - getImageWmsInfo() 方法
- `backend/src/main/java/com/gisplatform/util/CrsTransformUtil.java` - **新增** CRS 转换工具类

## Dependencies

无新增依赖。GeoTools `gt-referencing` 已包含在现有依赖中：

```xml
<dependency>
    <groupId>org.geotools</groupId>
    <artifactId>gt-referencing</artifactId>
    <version>32.0</version>
</dependency>
```

## Success Criteria

1. `GET /api/v1/images/{id}/wms-url` 返回的 extent 始终是 EPSG:4326 坐标
2. 无论 dataset.srs 是什么值，extent 都能正确转换
3. 转换失败时有明确日志，不影响接口返回
4. 前端 fit() 能正确缩放到影像区域

## Verification

### 1. 数据库检查

```sql
SELECT id, name, srs, extent FROM dataset WHERE type = 'raster' AND status = 'published';
```

### 2. API 测试

```bash
# 获取影像 WMS URL，检查 extent 是否为经纬度值
curl http://localhost:8080/api/v1/images/1/wms-url

# 期望：extent 值在 [-180,180] x [-90,90] 范围内
```

### 3. 前端验证

1. 选中影像图层
2. 地图应自动缩放到影像区域
3. 不会出现"跳到空白区域"的问题
