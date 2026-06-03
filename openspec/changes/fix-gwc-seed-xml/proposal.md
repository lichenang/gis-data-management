## Why

之前尝试的 GWC REST API 调用（JSON 和 form-urlencoded 格式）均返回 400/500 错误。根据深入分析，GeoServer 2.28.3 的 GWC REST API 使用 XStream 解析请求体，需要发送 XML 格式才能正确解析。

## What Changes

1. **修改 TileSeedService.triggerGwcSeedTask() 方法**
   - 将 Content-Type 从 `application/json` 改为 `application/xml`
   - 请求体改用正确的 XML 结构
   - bounds 参数使用嵌套的 coords 结构

2. **XML 请求体格式**
   ```xml
   <seedRequest>
     <name>gisplatform:raster_40</name>
     <zoomStart>0</zoomStart>
     <zoomStop>14</zoomStop>
     <format>image/png</format>
     <bounds>
       <coords>
         <double>-180.0</double>
         <double>-90.0</double>
         <double>180.0</double>
         <double>90.0</double>
       </coords>
     </bounds>
     <threadCount>4</threadCount>
     <type>seed</type>
   </seedRequest>
   ```

3. **使用影像实际范围**
   - 从 dataset.getExtent() 获取影像实际范围
   - 转换为 EPSG:3857 坐标用于 bounds

## Capabilities

### New Capabilities
（无）

### Modified Capabilities
（无）

## Impact

- `backend/src/main/java/com/gisplatform/service/tiling/TileSeedService.java` — 修改 triggerGwcSeedTask() 方法
- `backend/src/main/java/com/gisplatform/service/geoserver/GeoServerCacheService.java` — 可能需要修改 seedLayer() 方法

## Non-goals

- 不修改数据库表结构
- 不修改 GeoServer 配置
- 不修改切片任务的业务逻辑（zoom 级别等）

## Affected Files

- `backend/src/main/java/com/gisplatform/service/tiling/TileSeedService.java`
