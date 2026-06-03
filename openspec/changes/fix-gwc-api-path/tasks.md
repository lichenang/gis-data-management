## 1. 修改 GeoServerCacheService.java

- [x] 1.1 修正 URL 路径
  - 将 `/rest/gwc/layers/{layerId}/seeds.json`
  - 改为 `/rest/gwc/layers/{layerId}/seed.json`

- [x] 1.2 修正 JSON 解析逻辑
  - 将 `root.get("runs")` 改为 `root.get("long")`
  - 将数组遍历逻辑改为单对象直接取值

## 2. 验证

- [ ] 2.1 启动应用并访问影像管理页面
- [ ] 2.2 确认切片状态 API 返回正确数据（无 404）
- [ ] 2.3 确认切片进度显示正常
