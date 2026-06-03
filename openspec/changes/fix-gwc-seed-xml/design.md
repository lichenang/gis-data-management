## Context

GeoServer 2.28.3 集成 GeoWebCache，其 REST API 使用 XStream 进行 XML/JSON 反序列化。之前的 JSON 格式调用失败，可能是因为 XStream 对 JSON 的处理有限制。改用 XML 格式是更可靠的选择。

## Goals / Non-Goals

**Goals:**
- 修复自动切片功能，使其能成功调用 GWC REST API
- 使用 XML 格式替代 JSON 格式
- 实现种子任务状态监控
- 使用影像实际范围

**Non-Goals:**
- 不修改切片任务的业务逻辑
- 不修改数据库结构
- 不修改 GeoServer 配置

## Decisions

### Decision 1: 使用 XML 格式

**选择**: Content-Type = application/xml

**理由**:
- XStream 原生支持 XML，对 XML 的处理更可靠
- GeoServer 文档中明确记录了 XML 格式的用法
- 之前的 JSON 尝试多次失败

### Decision 2: 使用嵌套的 bounds 结构

**选择**: bounds 包含 coords 嵌套对象

**理由**:
- 这是 GeoServer GWC REST API 的标准格式
- 需要使用 `<double>` 标签包裹坐标值

### Decision 3: 先用 curl 验证

**选择**: 在代码修改前先用 curl 测试

**理由**:
- 快速验证 XML 格式是否正确
- 确认 GeoServer REST API 是否正常工作

## curl 测试命令

### 1. 检查 GWC 图层是否存在
```bash
curl -u admin:geoserver http://127.0.0.1:8080/geoserver/gwc/rest/layers/gisplatform:raster_40.json
```

### 2. 使用 XML 格式启动种子任务
```bash
curl -X POST \
  -u admin:geoserver \
  -H "Content-Type: application/xml" \
  -d '<seedRequest>
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
  </seedRequest>' \
  http://127.0.0.1:8080/geoserver/gwc/rest/seed/gisplatform:raster_40
```

### 3. 检查种子任务状态
```bash
curl -u admin:geoserver http://127.0.0.1:8080/geoserver/gwc/rest/seed/gisplatform:raster_40.json
```

### 4. 使用影像实际范围的 XML（推荐）
```bash
curl -X POST \
  -u admin:geoserver \
  -H "Content-Type: application/xml" \
  -d '<seedRequest>
    <name>gisplatform:raster_40</name>
    <zoomStart>0</zoomStart>
    <zoomStop>14</zoomStop>
    <format>image/png</format>
    <bounds>
      <coords>
        <double>12129263.4322</double>
        <double>4050247.7206</double>
        <double>12130944.1474</double>
        <double>4051356.3316</double>
      </coords>
    </bounds>
    <gridSetId>EPSG:3857</gridSetId>
    <threadCount>4</threadCount>
    <type>seed</type>
  </seedRequest>' \
  http://127.0.0.1:8080/geoserver/gwc/rest/seed/gisplatform:raster_40
```

## Risks / Trade-offs

| 风险 | 描述 | 缓解措施 |
|------|------|----------|
| XML 格式仍不工作 | XStream 配置可能有问题 | 尝试其他变体 |
| bounds 坐标系错误 | 使用错误的坐标系统 | 显式指定 gridSetId |
| 认证问题 | Basic Auth 可能不正确 | 测试时检查 401 响应 |

## Migration Plan

1. 使用 curl 命令测试 XML 格式
2. 修改 TileSeedService.triggerGwcSeedTask() 方法
3. Maven 编译验证
4. 启动服务测试
5. 验证切片是否正确生成
