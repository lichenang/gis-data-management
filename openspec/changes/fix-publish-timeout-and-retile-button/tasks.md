## 1. 前端 - 增加接口超时时间

- [x] 1.1 修改 `frontend/src/api/request.ts`
  - 将 `timeout: 30000` 改为 `timeout: 120000`

## 2. 前端 - 修复重新切片按钮显示条件

- [x] 2.1 修改 `frontend/src/views/images/index.vue`
  - 将 `<template v-if="row.tileStatus === 'completed'">` 改为 `<template v-if="row.tileStatus === 'completed' || row.cacheSeedStatus === 'seeded'">`

## 3. 验证

- [ ] 3.1 发布大文件影像，确认不会因超时失败
- [ ] 3.2 当 `cacheSeedStatus === 'seeded'` 时，确认重新切片按钮正确显示
- [ ] 3.3 点击重新切片按钮，确认功能正常工作
