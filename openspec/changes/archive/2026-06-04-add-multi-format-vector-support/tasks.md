## 1. 后端基础设施

- [x] 1.1 创建 VectorFileFormat 枚举，定义支持的格式常量
- [x] 1.2 创建 FormatDetector 格式识别类
- [x] 1.3 创建 VectorDataStoreFactory 工厂类

## 2. 导入服务实现

- [x] 2.1 实现 GeoJSON DataStore 解析逻辑
- [x] 2.2 实现 Shapefile DataStore 解析逻辑（支持 zip 包）
- [x] 2.3 实现 KML/KMZ DataStore 解析逻辑
- [x] 2.4 实现 GML DataStore 解析逻辑
- [x] 2.5 实现 GPX DataStore 解析逻辑
- [x] 2.6 实现 CSV DataStore 解析逻辑
- [x] 2.7 实现 WKT 格式解析逻辑
- [x] 2.8 实现坐标系转换工具 CoordinateTransformUtil
- [x] 2.9 实现 PostGIS 批量写入逻辑
- [x] 2.10 集成到 MultiFormatImportService

## 3. 导出服务实现

- [x] 3.1 实现 GeoJSON 导出功能
- [x] 3.2 实现 Shapefile ZIP 导出功能
- [x] 3.3 实现 KML 导出功能
- [x] 3.4 实现 CSV 导出功能
- [x] 3.5 扩展 ExportController 支持 CSV 格式

## 4. 前端改造

- [x] 4.1 扩展上传组件 accept 属性支持多格式
- [x] 4.2 更新上传提示信息显示支持格式
- [x] 4.3 添加 CSV 导出选项到导出下拉菜单

## 5. 测试与验证

- [ ] 5.1 导入 GeoJSON 文件测试
- [ ] 5.2 导入 Shapefile zip 包测试
- [ ] 5.3 导入 KML 文件测试
- [ ] 5.4 导入 CSV 文件测试
- [ ] 5.5 导出为 GeoJSON 验证
- [ ] 5.6 导出为 Shapefile 验证
- [ ] 5.7 导出为 CSV 验证
- [ ] 5.8 坐标系转换测试

## 6. 代码优化与文档

- [ ] 6.1 添加所有必要的 Javadoc 注释
- [ ] 6.2 添加单元测试
- [ ] 6.3 更新 API 文档描述
