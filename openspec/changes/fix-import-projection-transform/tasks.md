## 1. 扩展 CrsTransformUtil

- [x] 1.1 在 `CrsTransformUtil` 中添加 `transformGeometry(Geometry, CoordinateReferenceSystem)` 方法
- [x] 1.2 添加对 `org.locationtech.jts.geom.Geometry` 的 import

## 2. 修改 MultiFormatImportServiceImpl

- [x] 2.1 在 `importUsingDataStore()` 方法中获取 FeatureSource 的原生 CRS
- [x] 2.2 创建 `MathTransform` (原生 CRS → EPSG:4326)
- [x] 2.3 在遍历 Feature 时对 Geometry 进行转换后再插入
- [x] 2.4 修改 `insertFeature()` 方法签名以支持直接接收 Geometry

## 3. 验证

- [x] 3.1 执行 `mvn compile` 验证编译通过
