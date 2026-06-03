## ADDED Requirements

### 模块划分

| 模块 | 位置 | 职责 |
|---|---|---|
| DatasetServiceImpl（修复） | `.../service/impl/DatasetServiceImpl.java` | 修正 Shapefile 导入路径和写入 API |
| pom.xml（确认） | `backend/pom.xml` | 确认 gt-shapefile 依赖配置 |

### Requirement: Shapefile 导出使用正确的包路径

系统的 Shapefile 导出功能 SHALL 使用 GeoTools 32.x 的正确包路径：

- `org.geotools.data.shapefile.ShapefileDataStore`
- `org.geotools.data.shapefile.ShapefileDataStoreFactory`
- `org.geotools.api.data.DataStore`
- `org.geotools.api.data.DataStoreFinder`
- `org.geotools.api.data.SimpleFeatureSource`
- `org.geotools.api.data.SimpleFeatureStore`
- `org.geotools.api.feature.simple.SimpleFeature`
- `org.geotools.api.feature.simple.SimpleFeatureType`

#### Scenario: Import 路径指向 GeoTools 32.x 正确位置

- **WHEN** 项目执行 `mvn compile`
- **THEN** 所有 Shapefile 相关 import 语句解析成功，编译通过

### Requirement: Shapefile 写入使用 SimpleFeatureStore API

系统的 Shapefile 写入逻辑 SHALL 使用 `ShapefileFeatureStore`（通过 `ShapefileDataStore.getFeatureSource()` 返回）进行要素写入，而非调用 `ShapefileDataStore` 上已移除的 `setTransaction()`/`addFeatures()` 方法。

#### Scenario: 写入 Shapefile 时不调用已移除方法

- **WHEN** 执行 Shapefile 导出
- **THEN** 系统通过 `shpStore.getFeatureSource(typeName)` 获取 `SimpleFeatureStore` 并调用 `addFeatures()`
- **AND** 不调用 `shpStore.setTransaction()` 或 `shpStore.addFeatures()`

### Requirement: gt-shapefile 模块依赖正确配置

pom.xml SHALL 在 `dependencyManagement`（版本锁定 32.0）和 `dependencies`（实际引用）两个区域均包含 `gt-shapefile` 声明。

#### Scenario: gt-shapefile 依赖可用

- **WHEN** Maven 解析项目依赖
- **THEN** `org.geotools:gt-shapefile:32.0` JAR 存在于本地 `.m2` 仓库
- **AND** `ShapefileDataStore` 和 `ShapefileDataStoreFactory` 类可被 Java 编译器找到

### Requirement: 编译通过

代码修改后 MUST 通过 `mvn compile`，无编译错误。

#### Scenario: 后端编译

- **WHEN** 执行 `mvn compile`
- **THEN** 编译成功（BUILD SUCCESS），错误数为 0
