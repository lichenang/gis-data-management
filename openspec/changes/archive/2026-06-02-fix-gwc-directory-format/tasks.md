## 1. 添加目录格式检测

- [x] 1.1 在 `TilePackageServiceImpl` 中添加 `detectGwcDirectoryFormat` 方法，检测第一层子目录是标准格式还是 EPSG 分层格式
- [x] 1.2 将检测出的格式类型（STANDARD 或 LAYERED）缓存供后续使用

## 2. 修改 enumerateTileFiles 方法

- [x] 2.1 根据检测出的格式类型，选择不同的遍历策略
- [x] 2.2 实现 `enumerateTileFilesLayered` 方法，处理 `EPSG_4326_{z}/{x}_{y}/{xx}_{yy}.png` 格式
- [x] 2.3 保留原有标准格式遍历作为回退方案

## 3. 调整 getTileRange 坐标计算

- [x] 3.1 确保 getTileRange 返回的 x, y 坐标范围适用于分层格式
- [x] 3.2 根据需要调整 xx, yy 子坐标的计算逻辑

## 4. 构建验证

- [x] 4.1 Maven 编译无错误
- [x] 4.2 测试切片包下载接口，确认能正确找到并打包瓦片文件
