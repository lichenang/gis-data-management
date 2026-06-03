## 1. 后端修改

- [x] 1.1 添加必要的 import（MediaType, HttpMethod, HttpHeaders, HttpEntity, Base64）
- [x] 1.2 添加 getImageExtentBounds() 方法获取影像范围
- [x] 1.3 添加 triggerGwcSeedTask() 方法调用 GWC REST API
- [x] 1.4 修改 triggerSeed() 方法，调用 triggerGwcSeedTask() 替代 WMS 请求

## 2. 验证

- [x] 2.1 Maven 编译验证
- [ ] 2.2 启动后端服务
- [ ] 2.3 发布影像，检查日志是否显示 "GWC seed task started"
- [ ] 2.4 验证 GWC 缓存目录是否生成了切片文件
