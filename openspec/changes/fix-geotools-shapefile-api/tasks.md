## 1. 依赖确认

- [x] 1.1 确认 pom.xml 的 dependencyManagement 和 dependencies 区域均包含 gt-shapefile:32.0
- [x] 1.2 运行 `mvn dependency:resolve` 确认 gt-shapefile-32.0.jar 已下载到本地 `.m2` 仓库

## 2. DatasetServiceImpl.java 导入路径修正

- [x] 2.1 将 ShapefileDataStore 导入从 `org.geotools.shapefile.ShapefileDataStore` 改为 `org.geotools.data.shapefile.ShapefileDataStore`
- [x] 2.2 将 ShapefileDataStoreFactory 导入从 `org.geotools.shapefile.ShapefileDataStoreFactory` 改为 `org.geotools.data.shapefile.ShapefileDataStoreFactory`
- [x] 2.3 将 DataStore/DataStoreFinder 导入从 `org.geotools.data.*` 改为 `org.geotools.api.data.*`
- [x] 2.4 将 SimpleFeature/SimpleFeatureType 导入从 `org.opengis.feature.simple.*` 改为 `org.geotools.api.feature.simple.*`
- [x] 2.5 确保 `org.geotools.data.DataUtilities` 和 `org.geotools.data.DefaultTransaction` 导入保持不变

## 3. Shapefile 写入 API 迁移

- [x] 3.1 移除 `shpStore.setTransaction(...)` 调用（如存在）
- [x] 3.2 移除 `shpStore.addFeatures(...)` 调用（如存在）
- [x] 3.3 实现新写入方式：通过 `shpStore.getFeatureSource(typeName)` 获取 `SimpleFeatureStore`，调用 `addFeatures(DataUtilities.collection(features))`
- [x] 3.4 确认 `SimpleFeatureStore` 使用 `org.geotools.api.data.SimpleFeatureStore` 引用

## 4. 编译验证

- [x] 4.1 运行 `mvn compile`，确认编译通过无错误
