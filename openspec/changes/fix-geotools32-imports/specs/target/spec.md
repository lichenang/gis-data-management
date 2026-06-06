# GeoTools 32.x Import 修复

技术修复规格，参考 openspec/specs/fix-geotools32-imports.md

## 修复内容

1. pom.xml: 添加 gt-csv, gt-kml, gt-xsd 依赖
2. DatasetServiceImpl.java: 修正 DataStore 等导入
3. MultiFormatImportService.java: 验证导入正确性
4. VectorDataStoreFactory.java: 验证导入正确性
