## 1. 后端工具类

- [x] 1.1 创建 `ExportUtil.java`，实现文件名清理（移除非法字符）、XML 转义、ZIP 打包工具方法
- [x] 1.2 确保 `ExportUtil` 通过 Spring `@Component` 或静态工具类方式可被 Service 层调用

## 2. Service 层扩展

- [x] 2.1 在 `DatasetService` 接口中声明 `getDatasetAsKML(Long id)` 和 `exportShapefileAsZip(Long id, OutputStream os)` 方法
- [x] 2.2 在 `DatasetServiceImpl` 中实现 `getDatasetAsKML()`：使用 `ST_AsKML()` SQL 查询，组装 KML 文档
- [x] 2.3 在 `DatasetServiceImpl` 中实现 `exportShapefileAsZip()`：通过 GeoTools PostGIS DataStore 读取要素 → ShapefileDataStore 写入临时文件 → ZIP 打包 → 清理临时文件

## 3. Controller 层

- [x] 3.1 新建 `ExportController.java`，映射 `GET /api/v1/datasets/{id}/export`
- [x] 3.2 实现格式分发逻辑：`vector` 类型支持 `geojson/shapefile/kml`，`raster` 类型支持 `geotiff`
- [x] 3.3 实现 GeoJSON 导出：调用已有 `getDatasetAsGeoJSON()`，设置 `application/geo+json` 响应头
- [x] 3.4 实现 GeoTIFF 导出：通过 `MinioClient.getObject()` 流式写入 response
- [x] 3.5 添加参数校验和错误处理：数据集不存在（404）、格式不支持（400）、类型格式不匹配（400）

## 4. 前端集成

- [x] 4.1 在数据集管理页面操作列添加 `el-dropdown` 导出按钮
- [x] 4.2 按数据集类型（`row.type`）条件渲染格式选项（vector→GeoJSON/Shapefile/KML，raster→GeoTIFF）
- [x] 4.3 实现 `handleExport()` 函数，通过 `<a>` 标签触发浏览器下载

## 5. 验证

- [x] 5.1 后端编译通过：`mvn compile`
- [x] 5.2 前端编译通过：TypeScript 类型检查无新增错误
- [ ] 5.3 手动测试各格式导出（GeoJSON、KML、Shapefile ZIP、GeoTIFF）——需启动后端验证
- [ ] 5.4 测试异常场景：不存在的数据集、不支持的格式、类型格式不匹配——需启动后端验证
