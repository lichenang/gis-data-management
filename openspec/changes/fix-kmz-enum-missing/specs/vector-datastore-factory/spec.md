# VectorDataStoreFactory 修复规格说明

## 模块划分

- **修复层**: VectorDataStoreFactory switch-case 和 import 语句

## ADDED Requirements

### Requirement: 删除多余的 KMZ case

VectorDataStoreFactory SHALL 不包含对不存在的 KMZ 枚举值的引用。

#### Scenario: KML 格式处理
- **WHEN** 调用 `createDataStore(KML, file, "data.kml")`
- **THEN** 返回 KMLDataStore

#### Scenario: KMZ 格式处理
- **WHEN** 调用 `createDataStore(KML, file, "data.kmz")`（KML 枚举处理 kmz 扩展名）
- **THEN** 返回 KMLDataStore

### Requirement: GeoTools API import 正确

VectorDataStoreFactory SHALL 使用正确的 GeoTools 32.x API 包路径。

#### Scenario: DataStore import
- **WHEN** 编译 VectorDataStoreFactory
- **THEN** import 语句使用 `org.geotools.api.data.*`
