## Context

GeoWebCache 1.28.x (随 GeoServer 2.28.x 发行) 的 REST API 通过 URL 后缀区分请求格式：
- `.xml` 后缀 → 接收 XML 格式请求体
- `.json` 后缀 → 接收 JSON 格式请求体
- 无后缀 → 默认使用 XML

之前的调用尝试失败可能是因为：
1. 未使用正确的 URL 后缀
2. 请求体结构不符合 GWC 预期

## Goals / Non-Goals

**Goals:**
- 修复自动切片功能，使其能成功调用 GWC REST API
- 使用 `.xml` 后缀的正确 URL
- 使用 text/xml 作为 Content-Type
- 使用正确的 XML 请求体结构

**Non-Goals:**
- 不修改切片任务的业务逻辑
- 不修改数据库结构
- 不修改 GeoServer 配置

## Decisions

### Decision 1: URL 使用 .xml 后缀

**选择**: `/gwc/rest/seed/{layerId}.xml`

**理由**:
- GWC 1.28.x 通过后缀确定请求格式
- .xml 后缀确保使用 XML 解析器

### Decision 2: 使用 text/xml 作为 Content-Type

**选择**: `text/xml`

**理由**:
- 与 .xml 后缀配合使用
- 确保 GWC 正确识别请求格式

### Decision 3: 完整的 XML 请求体

**选择**: 包含 seedRequest 根元素和所有必要子元素

**理由**:
- name: 图层名称 (workspace:layerName)
- bounds/coords: 地理范围 (4个double值)
- gridSetId: 坐标系 (EPSG:3857)
- zoomStart/zoomStop: 缩放级别范围
- format: 切片格式 (image/png)
- type: 任务类型 (seed)
- threadCount: 并发线程数

## curl 测试命令

### 1. 使用 XML 格式启动种子任务
```bash
curl -X POST \
  -u admin:geoserver \
  -H "Content-Type: text/xml" \
  -d '<?xml version="1.0" encoding="UTF-8"?>
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
</seedRequest>' \
  http://127.0.0.1:8080/geoserver/gwc/rest/seed/gisplatform:raster_40.xml
```

### 2. 检查种子任务状态
```bash
curl -u admin:geoserver http://127.0.0.1:8080/geoserver/gwc/rest/seed/gisplatform:raster_40.json
```

## Risks / Trade-offs

| 风险 | 描述 | 缓解措施 |
|------|------|----------|
| GWC 版本不匹配 | 可能使用的是旧版 GWC | 使用 .xml 后缀尝试多种格式 |
| 认证问题 | Basic Auth 可能不正确 | 检查 401 响应 |

## Migration Plan

1. 修改 TileSeedService.triggerGwcSeedTask() 方法
2. Maven 编译验证
3. 启动服务测试
4. 发布影像验证切片是否正确触发
