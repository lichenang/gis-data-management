## 1. 后端修改

- [x] 1.1 移除 triggerGwcSeedTask() 方法中的 GWC REST API 调用代码
- [x] 1.2 实现坐标系转换方法 (EPSG:3857 转 EPSG:4326)
- [x] 1.3 实现 triggerWmsSeedRequests() 方法发送 WMS GetMap 请求
- [x] 1.4 修改 triggerSeed() 方法，调用 triggerWmsSeedRequests()

## 2. 验证

- [x] 2.1 Maven 编译验证
- [ ] 2.2 启动后端服务
- [ ] 2.3 发布影像，检查日志是否显示 WMS 请求日志
- [ ] 2.4 验证 GWC 缓存目录是否生成了切片文件
