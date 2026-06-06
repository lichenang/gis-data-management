## 1. 验证当前映射

- [ ] 1.1 验证 CrsTransformUtil 中 Zone 35-40 的映射已正确配置为 EPSG:4524-4529
- [ ] 1.2 验证下划线格式（如 CGCS2000_3_Degree_GK_Zone_38）和斜杠格式（如 CGCS2000 / 3-degree Gauss-Kruger zone 38）都能正确匹配
- [ ] 1.3 验证 Zone 38 映射到 EPSG:4527（而非 EPSG:4490）

## 2. 单元测试

- [ ] 2.1 为 CrsTransformUtil.getEpsgCode() 添加单元测试，验证各 Zone 的 EPSG 代码
- [ ] 2.2 测试 contains() 匹配逻辑能否正确工作

## 3. 集成验证（如需要）

- [ ] 3.1 使用包含 CGCS2000_3_Degree_GK_Zone_38 CRS 的测试数据验证坐标转换
