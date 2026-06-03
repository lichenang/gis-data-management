## 1. 后端修改

- [x] 1.1 修改 TileSeedService.triggerGwcSeedTask() 中的 gridSetId 从 "EPSG:3857" 改为 "EPSG:900913"
- [x] 1.2 Maven 编译验证

## 2. 测试验证

- [ ] 2.1 启动后端服务
- [ ] 2.2 发布影像，检查日志是否显示 GWC seed 任务启动
- [ ] 2.3 验证 GWC 缓存目录是否生成了切片文件
