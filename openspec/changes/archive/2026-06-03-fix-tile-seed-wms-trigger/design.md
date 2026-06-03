## Context

当前 TileSeedService 使用 GWC REST API 触发切片任务，但返回 400 Bad Request。GWC REST API 在 GeoServer 2.28.3 中不稳定。

改用 WMS GetMap 请求方式：每个 WMS 请求都会触发 GeoServer 在后台生成 GWC 切片缓存。这是一种更可靠的触发方式。

## Goals / Non-Goals

**Goals:**
- 修复自动切片功能，使其能成功触发切片缓存生成
- 使用 WMS GetMap 请求替代不稳定的 GWC REST API
- 为每个 zoom 级别发送代表性瓦片请求触发缓存

**Non-Goals:**
- 不修改 zoom 级别范围（仍为配置的 minZoom-maxZoom）
- 不修改数据库结构
- 不实现实时进度监控（使用现有逻辑）

## Decisions

### Decision 1: 使用 WMS 请求触发切片

**选择**: 发送 WMS GetMap 请求触发 GWC 缓存生成

**理由**:
- WMS 是 GeoServer 最稳定的接口
- GeoServer 处理 WMS 请求时会自动调用 GWC 生成切片
- 无需关注 GWC REST API 的内部格式

**备选方案**:
- 继续尝试 GWC REST API (JSON 格式): 已尝试多次，均失败
- 使用 WMTS 请求: 不如 WMS 稳定

### Decision 2: 代表性瓦片策略

**选择**: 每个 zoom 级别发送少量代表性瓦片（如 2x2 或 3x3 瓦片）

**理由**:
- 发送所有瓦片会耗时过长
- 发送代表性瓦片即可触发该级别的缓存初始化
- 用户首次访问其他区域时会自动填充缓存

### Decision 3: 坐标系转换

**选择**: 将 EPSG:3857 范围转换为 EPSG:4326 用于 WMS BBOX

**理由**:
- WMS 使用 CRS 参数指定坐标系
- 影像 extent 存储在 EPSG:3857 格式
- 需要通过 CrsTransformUtil 或手动公式转换

**转换公式**:
```
经度 = X / 20037508.34 * 180
纬度 = atan(exp(Y / 20037508.34 * PI)) * 360 / PI - 90
```

## Risks / Trade-offs

| 风险 | 描述 | 缓解措施 |
|------|------|----------|
| 范围不匹配 | WMS BBOX 超出影像实际范围导致空白图片 | 使用影像实际范围，通过 dataset.getExtent() 获取 |
| 坐标系转换错误 | 3857 转 4326 计算错误 | 验证转换公式，或使用 GeoTools 库 |
| 请求数量过多 | 每个 zoom 级别发送太多请求 | 限制为代表性瓦片（如每个级别 4-9 个） |
| 异步缓存生成 | WMS 触发的缓存生成是异步的，可能有延迟 | 记录日志说明已发送请求，实际缓存由 GeoServer 后台处理 |

## Migration Plan

1. 修改 TileSeedService.triggerSeed() 方法
2. 实现坐标系转换方法
3. 实现 WMS 请求发送逻辑
4. Maven 编译验证
5. 启动服务测试
6. 发布影像验证缓存是否生成
