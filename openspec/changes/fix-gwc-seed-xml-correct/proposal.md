## Why

之前的 GWC REST API 调用（包括 JSON 和 XML 格式）均未成功触发切片任务。GeoWebCache 1.28.x 的 REST API 通过 URL 后缀区分请求格式：`.xml` 接收 XML，`.json` 接收 JSON。当前实现未使用正确的 URL 后缀，导致格式无法被正确解析。

## What Changes

1. **修改 TileSeedService.triggerGwcSeedTask() 方法**
   - GWC 种子请求 URL 添加 `.xml` 后缀：从 `/gwc/rest/seed/{layerId}` 改为 `/gwc/rest/seed/{layerId}.xml`
   - Content-Type 改为 `text/xml`
   - 请求体使用正确的 XML 格式，包含完整的 seedRequest 结构

2. **XML 请求体格式**
   ```xml
   <?xml version="1.0" encoding="UTF-8"?>
   <seedRequest>
     <name>gisplatform:raster_40</name>
     <bounds>
       <coords>
         <double>-180.0</double>
         <double>-90.0</double>
         <double>180.0</double>
         <double>90.0</double>
       </coords>
     </bounds>
     <gridSetId>EPSG:3857</gridSetId>
     <zoomStart>0</zoomStart>
     <zoomStop>14</zoomStop>
     <format>image/png</format>
     <type>seed</type>
     <threadCount>4</threadCount>
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

## Non-goals

- 不修改切片任务的业务逻辑（zoom 级别、线程数等保持不变）
- 不修改数据库表结构
- 不修改 GeoServer 配置

## Affected Files

- `backend/src/main/java/com/gisplatform/service/tiling/TileSeedService.java`
