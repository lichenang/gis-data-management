## 1. 后端修改

- [x] 1.1 修改 GeoServerCacheService.seedLayer() 方法：将 Content-Type 改为 application/json
- [x] 1.2 修改请求体构建：从 form-urlencoded 字符串改为嵌套 Map 结构（seedRequest > bounds > coords）
- [x] 1.3 修改异常处理：从 log.warn 改为 log.error + throw RuntimeException

## 2. 验证

- [x] 2.1 Maven 编译验证
- [ ] 2.2 启动后端服务
- [ ] 2.3 发布测试影像，检查日志是否显示 "Started seed task for layer..."
- [ ] 2.4 通过 GWC REST API 验证种子任务是否真正创建
