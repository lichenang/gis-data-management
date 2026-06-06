## Why

项目中 MultiFormatImportService、VectorDataStoreFactory 等类存在 GeoTools 导入错误。GeoTools 32.x 经历了包重构，部分类路径发生变化导致编译错误。

## What Changes

1. 修复 DatasetServiceImpl.java 中的 DataStore 等类导入路径
2. 修复 MultiFormatImportService.java 中的导入
3. 修复 VectorDataStoreFactory.java 中的导入
4. 添加缺少的 GeoTools 依赖 (gt-csv, gt-kml, gt-xss)

## Capabilities

无新功能需求，纯技术修复。

## Impact

- backend/pom.xml: 添加依赖
- DatasetServiceImpl.java, MultiFormatImportService.java, VectorDataStoreFactory.java: 修复 import
