## Context

`VectorFileFormat` 枚举定义：
```java
KML("KML", new String[]{"kml", "kmz"}, true)
```

`VectorDataStoreFactory.createDataStore()` 当前 switch-case：
```java
switch (format) {
    case SHAPEFILE: ...
    case GEOJSON: ...
    case KML:
    case KMZ:              // 编译错误：KMZ 枚举值不存在
        return createKMLDataStore(file);
    ...
}
```

## Goals / Non-Goals

**Goals:**
- 删除多余的 `case KMZ:` 语句
- 更新 GeoTools import 包路径

**Non-Goals:**
- 不修改 VectorFileFormat 枚举

## Decisions

### Decision 1: 删除 case KMZ:

VectorFileFormat.fromExtension("kmz") 已返回 KML（因为 KML 的扩展名包含 "kmz"），所以 switch-case 中的 `case KMZ:` 是冗余的。

### Decision 2: GeoTools import 更新

GeoTools 32.x 使用 `org.geotools.api.data.*` 作为 API 接口包路径。

## Risks / Trade-offs

无显著风险，仅为代码清理。
