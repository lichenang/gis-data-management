## 1. 前端导出下载逻辑改造

- [x] 1.1 重写 `datasets/index.vue` 中的 `handleExport()`：使用 `fetch()` 替代 `<a>` 标签，从 `localStorage` 读取 `access_token` 并添加 `Authorization: Bearer <token>` 请求头
- [x] 1.2 使用 `response.blob()` 获取文件内容，通过 `URL.createObjectURL()` 创建下载链接
- [x] 1.3 设置 `a.download` 属性为 `{row.name}.{format}`，确保下载文件名正确
- [x] 1.4 下载完成后调用 `URL.revokeObjectURL()` 释放内存

## 2. 编译验证

- [x] 2.1 运行 `npx vue-tsc --noEmit` 检查 TypeScript 类型无新增错误（`datasets/index.vue` 0 错误）
- [ ] 2.2 启动前后端，登录后测试导出功能：点击导出 → 确认浏览器下载文件 → 文件内容正确
