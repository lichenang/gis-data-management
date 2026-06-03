## Context

GWC 种子任务（seed task）根据请求的 bounds 范围生成切片瓦片。当前代码使用全球范围 (-180, -90, 180, 90) 作为 bounds，这导致 GWC 在生成低级别瓦片时向 GeoServer 发送全球范围的 WMS 请求。由于影像数据仅覆盖特定区域，GeoServer 返回错误："must intersect with the image's bounds"。

解决方案：使用 GWC 的 `useCurrentBounds` 参数，让 GWC 自动使用发布图层时配置的实际范围。

## Goals / Non-Goals

**Goals:**
- 使用 `useCurrentBounds=true` 参数让 GWC 使用图层的实际范围

**Non-Goals:**
- 不修改切片任务的其他参数

## Decisions

### Decision 1: 使用 useCurrentBounds 参数

**选择**: `<useCurrentBounds>true</useCurrentBounds>`

**理由**:
- GWC 会自动使用已发布图层的边界范围
- 简单可靠，无需手动计算和传递范围
- 不依赖数据库中存储的范围值

## Risks / Trade-offs

| 风险 | 描述 | 缓解措施 |
|------|------|----------|
| GWC 版本兼容性 | 某些版本可能不支持 useCurrentBounds | 备用方案：手动传递正确的 bounds |

## curl 测试命令

```bash
curl -X POST \
  -u admin:geoserver \
  -H "Content-Type: text/xml" \
  -d '<?xml version="1.0" encoding="UTF-8"?>
<seedRequest>
  <name>gisplatform:raster_40</name>
  <gridSetId>EPSG:900913</gridSetId>
  <zoomStart>0</zoomStart>
  <zoomStop>14</zoomStop>
  <format>image/png</format>
  <type>seed</type>
  <threadCount>4</threadCount>
  <useCurrentBounds>true</useCurrentBounds>
</seedRequest>' \
  http://127.0.0.1:8080/geoserver/gwc/rest/seed/gisplatform:raster_40.xml
```
