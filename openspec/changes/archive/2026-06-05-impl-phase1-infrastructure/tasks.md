## 1. VectorFileFormat 枚举扩展

- [x] 1.1 在 `VectorFileFormat` 枚举中添加 `geoToolsSupported` boolean 字段
- [x] 1.2 添加 `isGeoToolsSupported()` 方法
- [x] 1.3 更新构造函数和 getter

## 2. FormatDetector 增强

- [x] 2.1 验证现有的 JSON 类型检测逻辑
- [x] 2.2 验证现有的 ZIP 内容检测逻辑
- [x] 2.3 确保 KML/KMZ 正确返回 KML 格式

## 3. VectorDataStoreFactory 实现

- [ ] 3.1 添加 `createDataStore(VectorFileFormat, MultipartFile, String)` 方法
- [ ] 3.2 实现 Shapefile DataStore 创建（含 ZIP 解压）
- [ ] 3.3 实现 GeoJSON DataStore 创建
- [ ] 3.4 添加必要的 import (org.geotools.data.DataStore, DataStoreFinder 等)

## 4. Maven 依赖确认

- [ ] 4.1 确认 pom.xml 中 `gt-shapefile` 依赖已配置
- [ ] 4.2 确认 pom.xml 中 `gt-geojson` 依赖已配置
- [ ] 4.3 确认 maven-compiler-plugin 使用 Java 17

## 5. 验证测试

- [ ] 5.1 执行 `mvn compile` 验证编译通过
- [ ] 5.2 单元测试 VectorDataStoreFactory.createDataStore()
- [ ] 5.3 集成测试 Shapefile ZIP 包导入流程
