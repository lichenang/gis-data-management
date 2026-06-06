## 1. 删除手写二进制解析代码

- [ ] 1.1 删除 parseShpFile() 方法及相关的辅助方法
- [ ] 1.2 删除 ShapefileHeader, ShapefileData 内部类
- [ ] 1.3 删除 parseShpRecord, parsePoint, parsePolygon 等方法

## 2. 使用 GeoTools 解析

- [ ] 2.1 修改 parseShapefile() 使用 ShapefileDataStore
- [ ] 2.2 修改 extractShapefileFromZip() 提取到临时文件
- [ ] 2.3 修改 importShapefile() 使用 GeoTools 读取特征

## 3. 测试验证

- [ ] 3.1 编译验证通过
- [ ] 3.2 使用 Point 类型 Shapefile 测试导入
- [ ] 3.3 使用 Polygon 类型 Shapefile 测试导入
- [ ] 3.4 使用 ZIP 打包的 Shapefile 测试导入
