## 1. 后端修改

- [x] 1.1 在 TileSeedService 中添加 RestTemplate 依赖
- [x] 1.2 添加私有方法 getRepresentativeTileCoords(int zoom) 计算代表性瓦片坐标
- [x] 1.3 添加私有方法 getTileBounds(int x, int y, int zoom) 计算 EPSG:3857 边界
- [x] 1.4 添加私有方法 triggerWmsSeeding() 发送 WMS GetMap 请求
- [x] 1.5 修改 triggerSeed() 方法，调用 triggerWmsSeeding() 替代 seedLayer()

## 2. 验证

- [x] 2.1 Maven 编译验证
- [ ] 2.2 启动后端服务
- [ ] 2.3 发布测试影像，检查日志是否显示 WMS 请求发送
- [ ] 2.4 检查 GeoServer 数据目录中是否生成了切片文件
