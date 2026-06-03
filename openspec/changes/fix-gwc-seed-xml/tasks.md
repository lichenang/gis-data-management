## 1. 验证

- [ ] 1.1 使用 curl 测试 XML 格式 GWC seed API（全球范围）
- [ ] 1.2 使用 curl 测试 XML 格式 GWC seed API（影像实际范围）
- [ ] 1.3 检查 GWC 种子任务状态 API

## 2. 后端修改

- [x] 2.1 修改 TileSeedService.triggerGwcSeedTask() 方法使用 XML 格式
- [x] 2.2 实现影像实际范围获取方法
- [x] 2.3 实现种子任务状态轮询方法
- [x] 2.4 Maven 编译验证

## 3. 集成测试

- [ ] 3.1 启动后端服务
- [ ] 3.2 发布影像，触发自动切片
- [ ] 3.3 检查日志确认 GWC seed 任务启动
- [ ] 3.4 验证 GWC 缓存目录是否生成了切片文件
