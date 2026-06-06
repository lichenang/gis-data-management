## Context

当前 `parseFile()` 方法对于 Shapefile 格式，只返回固定的 `srs: "EPSG:4326"`，没有实际检测文件中的 CRS 信息。前端上传对话框始终显示源坐标系下拉框，无论 .prj 文件是否存在。

## Goals / Non-Goals

**Goals:**
- 在 parseFile() 时实际检测 Shapefile 的 CRS
- 通过 `crsDetected` 字段告知前端 CRS 是否被成功识别
- 前端根据 `crsDetected` 条件显示源坐标系选择器

**Non-Goals:**
- 不修改导入时的 CRS 转换逻辑（已在 fix-crs-missing-prj 中实现）
- 不在前端 parse 阶段验证用户选择的 sourceSrs 是否有效

## Decisions

### Decision 1: parseFile 时检测 CRS

**方案**：在 `parseFile()` 方法中，对于 Shapefile 格式，创建临时 DataStore 并获取 Schema 中的 CRS 信息。

```java
if (format == VectorFileFormat.SHAPEFILE) {
    DataStore dataStore = vectorDataStoreFactory.createDataStore(format, file, fileName);
    SimpleFeatureType schema = dataStore.getFeatureSource(typeNames[0]).getSchema();
    CoordinateReferenceSystem nativeCrs = schema.getGeometryDescriptor().getCoordinateReferenceSystem();
    int nativeSrid = CrsTransformUtil.getEpsgCode(nativeCrs);
    result.setCrsDetected(nativeSrid > 0);
    result.setSrs(nativeSrid > 0 ? "EPSG:" + nativeSrid : null);
}
```

### Decision 2: crsDetected 字段语义

**方案**：`crsDetected = true` 表示 CRS 被成功识别，此时 `srs` 包含有效值；`crsDetected = false` 表示识别失败，此时 `srs = null`。

前端判断逻辑：
```typescript
const isCrsDetected = computed(() => parseResult.value?.crsDetected !== false)
```

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|---------|
| parseFile 时创建 DataStore 有性能开销 | 仅在 Shapefile 格式时执行，GeoJSON 等格式保持现状 |
| temp 文件清理 | 使用 `vectorDataStoreFactory` 的现有逻辑，temp 文件在方法结束时清理 |
