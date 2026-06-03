## 1. 修改 GeoServerCacheService.java GWC API 路径

- [x] 1.1 修正 seedLayer 方法中的 POST 路径
  - 将 `/rest/gwc/layers/` 改为 `/gwc/rest/layers/`

- [x] 1.2 修正 getSeedStatus 方法中的 GET 路径
  - 将 `/rest/gwc/layers/` 改为 `/gwc/rest/layers/`

## 2. 验证

- [ ] 2.1 启动应用并触发切片任务
- [ ] 2.2 确认 GeoServer 日志无 404 错误
- [ ] 2.3 确认切片状态 API 返回正确数据
