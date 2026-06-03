## 1. 修改 datasets/index.vue 页面标题

- [x] 1.1 添加 `pageTitle` computed 属性
  - `/datasets/vector` → '矢量数据集'
  - `/datasets/raster` → '影像数据集'
  - 其他 → '数据集管理'

- [x] 1.2 修改模板中的标题绑定
  - 将 `<span class="title">数据集管理</span>`
  - 改为 `<span class="title">{{ pageTitle }}</span>`

## 2. 修改 router/index.ts 路由

- [x] 2.1 将 /datasets/raster 路由从组件路由改为重定向
  - 删除 `component: () => import('@/views/datasets/index.vue')`
  - 添加 `redirect: '/images'`

## 3. 验证

- [ ] 3.1 访问 /datasets/vector，确认标题显示"矢量数据集"
- [ ] 3.2 访问 /datasets/raster，确认跳转到 /images 且标题显示"影像管理"
- [ ] 3.3 访问 /datasets，确认跳转到 /datasets/vector
