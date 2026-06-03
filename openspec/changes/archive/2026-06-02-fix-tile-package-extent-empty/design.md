## Context

当前 `TilePackageServiceImpl.packageTiles` 方法中，当 `dataset.getExtent()` 为空时，会回退到全球范围 `Bounds(-180, -90, 180, 90)`。然后使用 Web Mercator 瓦片坐标公式计算需要查找的瓦片坐标范围 (`getTileRange`)。问题在于：

1. 实际 GWC 只在西安区域（约 lon: 108-110, lat: 33-35）生成了瓦片
2. 全球范围计算出的瓦片坐标（如 zoom=5 时 x=18-20, y=6-8）与实际 GWC 存储的坐标不匹配
3. `enumerateTileFiles` 遍历检查这些不存在的坐标点，返回空列表

## Goals / Non-Goals

**Goals:**
- 当 `dataset.extent` 为空时，优先从 `raster_metadata` 表获取实际影像范围
- 当无法获取 extent 时，改用宽松策略：直接遍历 GWC 目录下所有 `.png` 文件
- 添加调试日志，帮助后续快速定位类似问题

**Non-Goals:**
- 不修改瓦片坐标计算公式（保持 Web Mercator 标准）
- 不修改 API 接口签名
- 不修改数据集表结构

## Decisions

### Decision 1: raster_metadata 表获取 extent

**方案**: 在 `packageTiles` 方法中，当 `dataset.getExtent()` 为空时，查询 `raster_metadata` 表获取 `bounds` 或 `extent` 字段。

**理由**:
- `raster_metadata` 表在导入影像时已存储了原始影像的边界信息
- 复用已有数据，避免重复计算

**备选方案**: 使用 GeoTools 读取 GeoTIFF 头信息 - 复杂度高，性能差。

### Decision 2: 宽松瓦片查找策略

**方案**: 当无法获取 extent 时，使用 `Files.walkFileTree` 递归遍历 GWC 目录，返回所有 `.png` 文件。

**理由**:
- 实现简单，适用于任何 GWC 目录结构
- 作为 fallback 策略，保证基本可用性

**备选方案**: 假设特定目录结构 - GWC 可能使用不同命名规范。

### Decision 3: 调试日志

**方案**: 在 `enumerateTileFiles` 开头输出：
- 实际使用的 bounds
- 计算的 tile range
- 遍历的目录路径
- 找到的瓦片文件数

**理由**: 帮助后续快速诊断类似问题，无需修改代码添加日志。

## Risks / Trade-offs

- **[低] 宽松策略可能返回不需要的瓦片**: 当 extent 不准确时，会打包非请求区域的瓦片。Mitigation: 优先使用 extent 匹配，仅在 extent 为空时使用宽松策略。
- **[低] raster_metadata 表可能不存在**: 新部署环境可能没有该表。Mitigation: 捕获异常并回退到宽松策略。

## Migration Plan

1. 部署修复后的 `TilePackageServiceImpl.java`
2. 测试切片包下载功能（extent 为空的数据集）
3. 验证日志输出正确

无数据库迁移，无回滚需求（代码级回滚）。

## Open Questions

- 是否需要前置检查：检测到 extent 为空时是否需要警告用户完善元数据？
