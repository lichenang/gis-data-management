## Context

GWC 种子任务根据请求的 bounds 范围生成切片瓦片。当前代码使用 useCurrentBounds=true 参数，但某些版本的 GWC 可能不支持该参数或行为不一致。需要显式传递影像的实际范围。

当前代码已经有 `getImageExtentInWebMercator()` 方法，可以从 dataset 的 extent 字段获取影像的实际范围并转换为 EPSG:900913 (等同于 EPSG:3857)。

## Goals / Non-Goals

**Goals:**
- 确保 GWC 种子请求使用影像的实际 EPSG:900913 范围
- 如果无法获取影像范围，回退到全球范围

**Non-Goals:**
- 不修改切片任务的其他参数

## Decisions

### Decision 1: 使用 getImageExtentInWebMercator() 方法

**选择**: 利用现有的 `getImageExtentInWebMercator()` 方法获取影像范围

**理由**:
- 该方法已经实现了从 dataset.getExtent() 获取范围并转换为 Web Mercator
- 已在之前的代码中使用

### Decision 2: 移除 useCurrentBounds 参数

**选择**: 在 XML 请求体中移除 useCurrentBounds=true

**理由**:
- 显式 bounds 比 useCurrentBounds 更可靠
- 与 useCurrentBounds 配合可能产生冲突

## Current Code Logic

```java
double[] extent = getImageExtentInWebMercator(datasetId);
if (extent != null) {
    minX, minY, maxX, maxY = extent;
} else {
    // 使用全球范围
    minX = "-180.0"; minY = "-90.0";
    maxX = "180.0"; maxY = "90.0";
}
```

这个逻辑已经正确，需要确认它在运行时被正确执行。

## Risks / Trade-offs

| 风险 | 描述 | 缓解措施 |
|------|------|----------|
| extent 为空 | dataset.getExtent() 可能为空 | 回退到全球范围 |
