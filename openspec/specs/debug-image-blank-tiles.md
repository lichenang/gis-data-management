# 诊断报告：影像图层 WMS 加载成功但地图空白

## 问题描述

**现象**：
- 影像图层勾选后，WMS 请求正常（200 OK）
- GeoServer 返回的 Preview 显示空白
- 地图仍停留在初始中心点（北京 `[116.4, 39.9]`，zoom 10）

**根因分析**：

```
┌─────────────────────────────────────────────────────────────────────┐
│                    问题根因链                                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  WMS GetMap 请求的 BBOX:                                            │
│  BBOX=39.02,116.01,39.37,116.36  (EPSG:4326 北京范围)               │
│                                                                     │
│  影像实际位置:                                                       │
│  山西长治 (约 36.1°N, 113.1°E)                                       │
│                                                                     │
│  两者完全不重合！影像在山西，但请求的是北京范围，                     │
│  所以 GeoServer 返回的空白 tiles。                                   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

## 检查清单

### 1. getImageWmsInfo() 是否返回 extent？

**状态**：✅ 代码正确（fix-image-extent-and-zoom 后已实现）

```java
// ImageServiceImpl.java:390-401
if (dataset.getExtent() != null && !dataset.getExtent().isEmpty()) {
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> extentMap = mapper.readValue(dataset.getExtent(), Map.class);
    double[] extent = new double[4];
    extent[0] = ((Number) extentMap.get("minX")).doubleValue();
    // ...
    info.setExtent(extent);
}
```

### 2. dataset.extent 字段是否有值？

**状态**：⚠️ **可能为空或格式错误**

可能的原因：

| 情况 | extent 值示例 | 说明 |
|------|---------------|------|
| 未发布过 | `null` | 需要重新发布 |
| 旧数据 | `"12129263,4050247,12130944,4051356"` | 逗号分隔格式，无法解析 |
| 正常 | `"{\"minX\":113.0,...}"` | JSON 格式 |

**验证方法**：
```sql
SELECT id, name, extent FROM dataset WHERE type = 'raster' AND status = 'published';
```

### 3. 前端 MapContainer.vue 是否调用 fit()？

**状态**：✅ 代码正确（fix-image-extent-and-zoom 后已实现）

```typescript
// MapContainer.vue:277-285
if (imageInfo.extent && imageInfo.extent.length === 4) {
  const extent: [number, number, number, number] = imageInfo.extent
  map.value.getView().fit(extent, {
    padding: [50, 50, 50, 50],
    maxZoom: 15,
    duration: 500
  })
}
```

### 4. 坐标系不匹配问题

**状态**：⚠️ **关键问题**

如果 `raster_metadata.transform` 存储的是 EPSG:3857（Web Mercator）坐标：
- 值范围：约 -20037508 到 20037508（米）
- 例如：`12129263,4050247` 是 EPSG:3857

而 OpenLayers 地图视图使用 EPSG:4326：
- 值范围：约 -180 到 180（度）
- 期望：`36.1,113.1,36.2,113.2`

**直接用 EPSG:3857 坐标调用 fit() 会导致地图跳到无效区域！**

```
┌─────────────────────────────────────────────────────────────────────┐
│                    坐标系问题                                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  extent 值: 12129263,4050247,12130944,4051356                       │
│                       ↓                                              │
│             EPSG:3857 (米)                                          │
│                       ↓                                              │
│        fit() 直接用这个值设置 view                                  │
│                       ↓                                              │
│          地图跳到 [12129263, 4050247] (无效坐标!)                   │
│                       ↓                                              │
│          实际想看的是 [36.1, 113.1] (长治)                         │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

## 修复方案

### 方案 A：重新发布（适用于 extent 为空）

如果 `dataset.extent` 为空或格式错误，重新调用发布接口：

```
POST /api/v1/images/{id}/publish
```

发布时 `publishImageDataset()` 会从 `raster_metadata.transform` 重新提取 extent。

### 方案 B：修复坐标系转换（适用于 extent 格式正确但值不对）

如果 extent 值本身就是错的（EPSG:3857 而非 EPSG:4326），需要：

1. 在 `getImageWmsInfo()` 中检测坐标系
2. 如果是 EPSG:3857，转换为 EPSG:4326
3. 或者前端使用 `ol/proj.transform()` 进行转换

### 方案 C：使用 GeoServer 原生 extent

GeoServer 发布时会计算正确的 extent，可通过 REST API 获取：

```
GET /geoserver/rest/workspaces/{workspace}/coveragestores/{store}/coverages/{layer}.json
```

## 验证步骤

1. **检查数据库 extent 值**：
   ```sql
   SELECT id, name, extent, srs FROM dataset WHERE type = 'raster';
   ```

2. **如果 extent 为空**：重新发布影像

3. **如果 extent 格式是逗号分隔**：`"12129263,4050247,12130944,4051356"`
   - 这是旧格式，需要迁移或重新发布

4. **如果 extent 是 EPSG:3857**：
   - 需要在前端/后端转换为 EPSG:4326

## 下一步行动

推荐执行以下操作：

1. **立即**：重新发布影像 dataset，让 extent 字段被正确写入
2. **检查**：`raster_metadata.transform` 中的值是什么坐标系
3. **如需要**：实现坐标系自动转换逻辑

是否需要创建相应的修复变更？
