## 1. 后端修改

- [x] 1.1 添加方法 getImageExtent(Long datasetId) 从 dataset.extent 读取影像范围
- [x] 1.2 添加方法 convertToWebMercator() 将 EPSG:4326 转换为 EPSG:3857
- [x] 1.3 添加方法 getTileCoordsInExtent() 计算影像覆盖范围内的瓦片坐标
- [x] 1.4 修改 triggerWmsSeeding() 使用影像实际范围构建 WMS 请求
- [x] 1.5 添加回退方法 triggerWmsSeedingFallback() 保持向后兼容

## 2. 验证

- [x] 2.1 Maven 编译验证
- [ ] 2.2 启动后端服务
- [ ] 2.3 发布西安区域测试影像，检查日志是否显示正确的影像范围
- [ ] 2.4 检查 GeoServer 是否成功生成切片，不再报 "region must intersect" 错误
