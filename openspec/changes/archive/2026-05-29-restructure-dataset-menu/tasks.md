## 1. 修改菜单结构

- [x] 1.1 在 `home/index.vue` 中将"数据集管理"菜单项改为 `el-sub-menu`
  - 使用 `Folder` 图标作为一级菜单图标
  - 添加 title slot

- [x] 1.2 添加子菜单项
  - 矢量数据集：`index="/datasets/vector"`，图标 `MapLocation`
  - 影像数据集：`index="/datasets/raster"`，图标 `Picture`

## 2. 新增路由

- [x] 2.1 在 `router/index.ts` 中修改 `/datasets` 路由为重定向到 `/datasets/vector`

- [x] 2.2 新增 `/datasets/vector` 路由
  - 指向 `datasets/index.vue`
  - meta.title: '矢量数据集'

- [x] 2.3 新增 `/datasets/raster` 路由
  - 指向 `datasets/index.vue`
  - meta.title: '影像数据集'

## 3. 修改 datasets/index.vue

- [x] 3.1 导入 `useRoute` from vue-router

- [x] 3.2 在 `onMounted` 中根据路由路径自动设置 `searchForm.type`
  - `/datasets/vector` → `type = 'vector'`
  - `/datasets/raster` → `type = 'raster'`

## 4. 验证

- [ ] 4.1 访问 /home，确认菜单显示为展开式子菜单
- [ ] 4.2 点击"矢量数据集"，确认跳转到 /datasets/vector
- [ ] 4.3 点击"影像数据集"，确认跳转到 /datasets/raster
- [ ] 4.4 访问 /datasets，确认重定向到 /datasets/vector
- [ ] 4.5 确认页面标题正确显示
