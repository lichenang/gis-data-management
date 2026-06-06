## 1. 修复解析偏移

- [x] 1.1 在 parseShpFile 中，读取 recordShapeType 前额外跳过 4 字节记录号
- [x] 1.2 修正内容长度计算，确保正确跳到下一条记录

## 2. 添加调试日志

- [x] 2.1 在 parseShpFile 完成后打印解析到的几何数量
- [x] 2.2 添加详细的 recordShapeType 日志

## 3. 验证

- [x] 3.1 编译验证通过
- [ ] 3.2 使用测试 Shapefile 验证 Point 类型导入
- [ ] 3.3 使用测试 Shapefile 验证 Polygon 类型导入
