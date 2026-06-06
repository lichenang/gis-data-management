## 1. 删除手写解析代码

- [ ] 1.1 删除手写二进制解析相关方法
- [ ] 1.2 删除 ShapefileHeader, ShapefileData 内部类
- [ ] 1.3 清理未使用的 import

## 2. 实现 GeoTools 解析

- [ ] 2.1 修改 parseShapefile() 使用 ShapefileDataStore
- [ ] 2.2 修改 extractShapefileFromZip() 提取到临时文件
- [ ] 2.3 修改 importShapefile() 使用 GeoTools 读取

## 3. 验证

- [ ] 3.1 编译验证通过
- [ ] 3.2 测试 Point 类型导入
- [ ] 3.3 测试 Polygon 类型导入
- [ ] 3.4 测试 ZIP 包导入
