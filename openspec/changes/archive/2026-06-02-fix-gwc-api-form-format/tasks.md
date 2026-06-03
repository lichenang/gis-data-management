## 1. 后端修改

- [x] 1.1 修改 GeoServerCacheService.seedLayer() 方法：将 Content-Type 改回 application/x-www-form-urlencoded
- [x] 1.2 修改请求体构建：使用 MultiValueMap 构造表单参数（name, zoomStart, zoomStop, format, bounds, threadCount, type）
- [x] 1.3 移除错误的 seedRequest JSON 包装

## 2. 验证

- [x] 2.1 Maven 编译验证
- [ ] 2.2 启动后端服务
- [ ] 2.3 发布测试影像，检查日志是否显示 "Started seed task for layer..."
- [ ] 2.4 通过 GWC REST API 验证种子任务是否真正创建
