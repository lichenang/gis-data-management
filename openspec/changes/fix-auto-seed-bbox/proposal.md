## Why

当前 TileSeedService 构建的 WMS 请求使用全球 BBOX（从 (0,0) 瓦片计算），但影像仅覆盖西安区域。GeoServer 2.28.3 在处理全球范围 WMS 请求时抛出：

```
IllegalArgumentException: The specified region must intersect with the image's bounds
```

需要从影像元数据读取实际覆盖范围，使用该范围构建 WMS 请求 BBOX。

## What Changes

1. **查询影像实际范围**
   - 从 `dataset.extent` 字段读取 EPSG:4326 范围
   - 或从 `raster_metadata.transform` 读取原始范围
   - 将经纬度范围转换为 EPSG:3857 (Web Mercator)

2. **计算影像覆盖的瓦片坐标**
   - 将影像的 EPSG:3857 范围转换为瓦片坐标范围 [minX, maxX, minY, maxY]
   - 对每个 zoom 级别，遍历覆盖范围内的所有瓦片坐标
   - 如果范围太大，可以限制每个 zoom 级别的瓦片数量（采样）

3. **使用影像范围构建 WMS 请求**
   - BBOX 参数使用影像的实际范围，而非全球范围
   - 这确保 GeoServer 能够正确处理请求

## Impact

- `backend/.../service/tiling/TileSeedService.java` — 修改 triggerWmsSeeding、添加范围获取和转换方法

## Non-goals

- 不修改 dataset 表结构
- 不修改其他服务

## Affected Files

- `backend/src/main/java/com/gisplatform/service/tiling/TileSeedService.java`

