## 1. 增强 extent 获取逻辑

- [x] 1.1 在 `TilePackageServiceImpl.packageTiles` 方法中，当 `dataset.getExtent()` 为空时，注入 `RasterMetadataMapper` 并查询 raster_metadata 表获取 bounds 字段
- [x] 1.2 如果 raster_metadata 中也无可用范围，记录警告日志并继续

## 2. 实现宽松瓦片查找策略

- [x] 2.1 修改 `enumerateTileFiles` 方法，添加遍历目录的 fallback 逻辑
- [x] 2.2 当无法从 extent 计算有效瓦片范围时（Bounds 为全球默认范围且 zoom 范围较大），自动切换到宽松模式
- [x] 2.3 确保宽松模式只返回目录中实际存在的 `.png` 文件

## 3. 添加调试日志

- [x] 3.1 在 `enumerateTileFiles` 方法开头输出：实际使用的 bounds、计算的 tile range、遍历目录路径
- [x] 3.2 在方法结束时输出：找到的瓦片文件数、扫描耗时

## 4. 构建验证

- [x] 4.1 Maven 编译无错误
- [x] 4.2 测试切片包下载接口（extent 为空的数据集），确认能正确返回瓦片文件
