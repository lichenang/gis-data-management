## 1. 后端修改

- [x] 1.1 修改 TriggerGwcSeedTask() 方法，使用 JSON 格式
- [x] 1.2 修改 Content-Type 为 application/json
- [x] 1.3 在请求体中添加 gridSetId 参数
- [x] 1.4 确保使用影像实际范围作为 bounds

## 2. 验证

- [x] 2.1 Maven 编译验证
- [ ] 2.2 启动后端服务
- [ ] 2.3 发布影像，检查日志是否显示 "GWC seed task started"
- [ ] 2.4 验证 GWC 缓存目录是否生成了切片文件
