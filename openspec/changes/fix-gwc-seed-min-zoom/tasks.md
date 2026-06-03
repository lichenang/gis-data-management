## 1. 配置修改

- [x] 1.1 修改 application.yml 中 tiling.min-zoom 从 0 改为 12
- [x] 1.2 Maven 编译验证

## 2. 测试验证

- [ ] 2.1 启动后端服务
- [ ] 2.2 发布影像，检查 GWC seed 任务是否从 zoom 12 开始
- [ ] 2.3 验证切片任务可以成功完成（无 "bounds must intersect" 错误）
