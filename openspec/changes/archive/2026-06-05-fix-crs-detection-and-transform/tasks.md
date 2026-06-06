## 1. CrsTransformUtil 增强

- [x] 1.1 添加 `CHINA_CRS_MAPPINGS` 静态映射表，包含 CGCS2000、Beijing 1954、Xian 1980 等中国常用坐标系
- [x] 1.2 重写 `getEpsgCode()` 方法，实现多级识别策略（identifier > 映射表 > EPSG 字符串）
- [x] 1.3 添加 `findInChinaMapping()` 私有方法，支持完整匹配和包含匹配
- [x] 1.4 添加必要的 Javadoc 注释，说明方法职责和参数返回值

## 2. MultiFormatImportServiceImpl 优化

- [x] 2.1 修改 `importUsingDataStore()` 方法签名，新增 `targetSrs` 参数
- [x] 2.2 修改 `importToPostGIS()` 调用点，传入 `targetSrs` 参数
- [x] 2.3 添加 `parseTargetSrs()` 方法，解析 "EPSG:4490"、"4490" 等格式的 SRS 字符串
- [x] 2.4 实现坐标系识别失败时的分层处理逻辑（正常 / 警告回退 / 抛出异常）
- [x] 2.5 修改 `insertFeature()` 方法签名，新增 `sourceSrid` 和 `targetSrid` 参数
- [x] 2.6 当 `sourceSrid == targetSrid` 时，生成不包含 `ST_Transform` 的 SQL
- [x] 2.7 添加详细的 Slf4J 日志记录（INFO 级别成功识别，WARN 级别回退，ERROR 级别异常）
- [x] 2.8 添加必要的 Javadoc 注释

## 3. 测试与验证

- [ ] 3.1 使用 CGCS2000 (EPSG:4490) Shapefile 测试导入，验证坐标正确转换到 EPSG:4326
- [ ] 3.2 使用无 prj 文件的 Shapefile 测试，验证抛出明确的异常信息
- [ ] 3.3 使用 WGS84 (EPSG:4326) Shapefile 测试，验证无需转换直接存储
- [ ] 3.4 验证日志输出包含正确的 CRS 名称和 EPSG 代码信息
