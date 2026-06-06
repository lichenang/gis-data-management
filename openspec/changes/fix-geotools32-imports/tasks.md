## 1. 依赖修复

- [ ] 1.1 添加 gt-csv, gt-kml, gt-xsd 依赖到 pom.xml

## 2. 代码修复

- [ ] 2.1 修复 DatasetServiceImpl.java 的 import (org.geotools.api.data → org.geotools.data)
- [ ] 2.2 验证 MultiFormatImportService.java 导入
- [ ] 2.3 验证 VectorDataStoreFactory.java 导入
- [ ] 2.4 修复 FormatDetector.java (如需要)

## 3. 验证

- [ ] 3.1 运行 mvn compile 验证编译
- [ ] 3.2 修复其他编译错误
