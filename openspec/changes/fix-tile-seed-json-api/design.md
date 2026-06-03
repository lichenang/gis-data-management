## Context

当前 TileSeedService.triggerGwcSeedTask() 方法使用 form-urlencoded 格式调用 GWC REST API，但返回 400 Bad Request。根据 diagnostic 分析，GeoServer 2.28.3 的 GeoWebCache REST API 需要 JSON 格式请求。

## Goals / Non-Goals

**Goals:**
- 修复自动切片功能，使其能成功调用 GWC REST API
- 使用 JSON 格式替代 form-urlencoded 格式
- 显式指定 gridSetId 参数
- 使用影像实际的 EPSG:3857 范围

**Non-Goals:**
- 不修改切片任务的业务逻辑（zoom 级别、线程数等）
- 不修改数据库结构
- 不修改 GeoServer 配置

## Decisions

### Decision 1: 使用 JSON 格式替代 form-urlencoded

**选择**: Content-Type = application/json

**理由**:
- 诊断报告中明确指出 JSON 格式更可靠
- JSON 格式能更好地处理复杂嵌套结构
- GeoServer 2.28.3 官方文档推荐使用 JSON

**备选方案**:
- form-urlencoded + gridSetId：可行性较低，已尝试过类似方式

### Decision 2: 显式指定 gridSetId

**选择**: 在请求体中添加 `"gridSetId": "EPSG:3857"`

**理由**:
- 明确指定坐标系，避免 GWC 使用默认配置导致的错误
- bounds 参数需要与 gridSetId 对应

### Decision 3: 使用影像实际范围

**选择**: 从 dataset.getExtent() 获取影像范围

**理由**:
- 使用全球范围 (-180,-90,180,90) 会处理不必要的区域
- 应该使用影像实际范围以优化切片效率

## Risks / Trade-offs

| 风险 | 描述 | 缓解措施 |
|------|------|----------|
| bounds 坐标系错误 | 影像的 extent 是 EPSG:4326 格式但当作 EPSG:3857 使用 | 在 getImageExtentBounds() 方法中确保返回 EPSG:3857 坐标 |
| JSON 序列化失败 |特殊字符导致 JSON 解析错误 | 使用 Jackson ObjectMapper 正确转义 |
| GWC 未发布图层 | 图层未发布到 GWC | 保持现有逻辑，调用失败时记录日志 |

## Migration Plan

1. 修改 TileSeedService.triggerGwcSeedTask() 方法
2. Maven 编译验证
3. 启动服务测试
4. 发布影像验证切片任务是否正常触发
