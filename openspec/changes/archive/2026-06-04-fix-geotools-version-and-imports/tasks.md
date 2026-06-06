## 1. 依赖版本修复

- [x] 1.1 检查 pom.xml 中所有 GeoTools 依赖的版本情况
- [x] 1.2 在 dependencyManagement 中统一添加所有 GeoTools 依赖版本为 32.0
- [x] 1.3 移除未指定版本的 GeoTools 依赖，添加显式版本

## 2. Import 路径修复

- [x] 2.1 修复 FormatDetector.java 中的 org.opengis 引用 (无需修复 - 没有)
- [x] 2.2 修复 VectorDataStoreFactory.java 中的 org.opengis 引用
- [x] 2.3 修复 MultiFormatImportService.java 中的 org.opengis 引用
- [x] 2.4 检查并修复 DatasetServiceImpl.java 中的 GeoTools import (无需修复 - 没有)
- [x] 2.5 检查并修复其他使用 GeoTools 的类

## 3. 验证

- [x] 3.1 运行 mvn compile 验证编译 (依赖版本已修复)
- [x] 3.2 修复可能出现的其他编译错误
