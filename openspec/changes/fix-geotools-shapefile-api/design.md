## Context

Shapefile 导出功能使用 GeoTools 32.x 访问 PostGIS 并写入 Shapefile。初始实现使用了 GeoTools 28.x 时代的包路径和 API：`org.geotools.shapefile.*` 和 `org.geotools.data.Transaction`/`org.geotools.data.DataStore`，这些在 32.x 中已迁移或移除。当前代码无法通过编译，需要在 keep 32.x 版本的前提下修复依赖、导入和 API 调用。

相关文件：
- `DatasetServiceImpl.java`：Shapefile 导出逻辑，需修正 import 和写入 API
- `pom.xml`：需确认 `gt-shapefile` 依赖在两个区域（`dependencyManagement` 和 `dependencies`）均正确声明
- `openspec/specs/fix-geotools-shapefile-imports.md`：已有完整诊断报告

## Goals / Non-Goals

**Goals:**
- 修正 `DatasetServiceImpl.java` 中 Shapefile 相关 import 路径为 GeoTools 32.x 正确路径
- 用 `SimpleFeatureStore.addFeatures()` 替代已移除的 `setTransaction()`/`addFeatures()` 调用
- 确认 pom.xml 中 `gt-shapefile` 依赖正确配置
- 全部改动通过 `mvn compile`

**Non-Goals:**
- 不修改 CrsTransformUtil、GeoTiffParser 等已正确使用 `org.geotools.api.*` 的类
- 不涉及 GeoTools 版本升降级
- 不重构 Shapefile 导出业务逻辑
- 不修改前端代码

## Decisions

### Decision 1：实现类 vs 接口类——分开导入

- 实现类（`ShapefileDataStore`, `ShapefileDataStoreFactory`）使用 `org.geotools.data.shapefile.*`
- 接口类（`DataStore`, `DataStoreFinder`, `SimpleFeatureSource`, `SimpleFeatureStore`）使用 `org.geotools.api.data.*`
- `SimpleFeature`, `SimpleFeatureType` 使用 `org.geotools.api.feature.simple.*`
- `DataUtilities` 保持 `org.geotools.data.DataUtilities`
- **原因**：GeoTools 32.x 只将接口迁移到 `org.geotools.api`，实现类仍保持在 `org.geotools` 名空间

### Decision 2：Shapefile 写入不使用 `setTransaction()`/`addFeatures()`，改用 `getFeatureSource() → SimpleFeatureStore`

```java
// 旧 API（GeoTools ≤28.x）：
shpStore.setTransaction(transaction);
shpStore.addFeatures(collection);

// 新 API（GeoTools 32.x）：
String typeName = shpStore.getTypeNames()[0];
((SimpleFeatureStore) shpStore.getFeatureSource(typeName))
        .addFeatures(DataUtilities.collection(features));
```

- **原因**：`ContentDataStore`（`ShapefileDataStore` 的父类）不再暴露这些方法；`ShapefileFeatureStore`（通过 `getFeatureSource()` 返回）实现了 `SimpleFeatureStore` 接口，可以直接写入

### Decision 3：保持现有 pom.xml 配置

当前 `pom.xml` 中 `gt-shapefile` 已在 `dependencyManagement`（32.0 锁定版本）和 `dependencies` 区域正确声明。无需修改 pom.xml，仅需确认 JAR 已下载到本地仓库。

## Risks / Trade-offs

- **[低风险] `getFeatureSource()` 返回值类型**：编译时返回 `SimpleFeatureSource`，`ShapefileDataStore` 内部实际返回 `ShapefileFeatureStore`（implements `SimpleFeatureStore`）。强制转换 `(SimpleFeatureStore)` 在运行时安全，但若 GeoTools 未来版本改变此行为会出问题。
- **[无风险] gt-shapefile JAR 未下载**：运行 `mvn dependency:copy-dependencies` 或 `mvn compile` 即可自动下载到本地 `.m2` 仓库。
- **[已排除] 其他 GeoTools 类的包迁移**：CrsTransformUtil 和 GeoTiffParser 已使用正确路径，无需修改。
