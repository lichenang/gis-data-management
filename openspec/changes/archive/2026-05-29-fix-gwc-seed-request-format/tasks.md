## 1. 修改 GeoServerCacheService.java seedLayer 方法

- [x] 1.1 将请求体从 XML 格式改为 URL 编码参数格式
  - 构建 `name=value&key=value` 格式的请求体

- [x] 1.2 使用 `exchangeWithContentType` 方法发送请求
  - 指定 `MediaType.APPLICATION_FORM_URLENCODED`

## 2. 验证

- [ ] 2.1 启动应用并触发切片任务
- [ ] 2.2 确认 GeoServer 日志无解析错误
- [ ] 2.3 确认切片任务成功启动
