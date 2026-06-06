## Why

项目中引入了 GeoTools 32.x 库，但 pom.xml 中部分依赖显式指定了版本，部分依赖未指定版本导致使用默认版本，存在版本不一致风险。此外，新编写的 MultiFormatImportService、VectorDataStoreFactory 等类使用了旧版的 org.opengis.* 包路径，而 GeoTools 32.x 已将 API 包迁移到 org.geotools.api.*，导致编译错误。统一版本和修复 import 路径是确保项目正常构建的前提。

## What Changes

1. **统一 pom.xml 中 GeoTools 依赖版本**
   - 将所有 GeoTools 依赖显式指定版本为 32.0
   - 移除未指定版本的 GeoTools 依赖，添加版本号

2. **修复 Java 代码中的 import 路径**
   - 将所有 `org.opengis.*` 包引用替换为 `org.geotools.api.*`
   - 检查 DataStore、FeatureReader、SimpleFeature 等核心类的 import 路径

3. **修复新增类的 import 问题**
   - VectorDataStoreFactory
   - MultiFormatImportService
   - FormatDetector

## Capabilities

### New Capabilities
无

### Modified Capabilities
- 无（此为纯技术修复，不改变需求）

## Impact

### 受影响的文件
- backend/pom.xml - 统一版本
- backend/src/main/java/com/gisplatform/service/FormatDetector.java - 修复 import
- backend/src/main/java/com/gisplatform/service/VectorDataStoreFactory.java - 修复 import
- backend/src/main/java/com/gisplatform/service/impl/MultiFormatImportService.java - 修复 import
- backend/src/main/java/com/gisplatform/service/impl/DatasetServiceImpl.java - 检查 import
- backend/src/main/java/com/gisplatform/util/GeoTiffParser.java - 检查 import
- 其他使用 GeoTools 的类

### 非目标
- 不修改业务逻辑
- 不改变 API 接口
- 不添加新功能
