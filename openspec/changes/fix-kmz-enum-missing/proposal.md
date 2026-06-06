## Why

`VectorDataStoreFactory.createDataStore()` 中的 switch-case 包含 `case KMZ:` 但 VectorFileFormat 枚举中不存在 KMZ 值（KML 枚举已包含 "kmz" 扩展名）。这导致编译错误。同时，GeoTools 32.x 的 import 语句需要更新为 `org.geotools.api.data.*` 包路径。

## What Changes

- 删除 `VectorDataStoreFactory.createDataStore()` 中多余的 `case KMZ:` 语句
- 将 GeoTools import 从 `org.geotools.data.*` 改为 `org.geotools.api.data.*`

## Impact

- **受影响文件**:
  - `backend/src/main/java/com/gisplatform/service/VectorDataStoreFactory.java`

## Non-goals

- 不修改 VectorFileFormat 枚举结构
- 不添加新的枚举值
