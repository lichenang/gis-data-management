# 设计: restructure-dataset-menu

## 当前菜单结构

```
el-menu
├── 仪表盘       /home
├── 数据集管理   /datasets    ← 需要改为子菜单
├── 图层管理     /layers
├── 地图查看     /map
├── 用户管理     /users       (v-if admin)
└── 系统管理     /settings    (v-if admin)
```

## 修改方案

### 1. 菜单结构调整

将"数据集管理"改为 `el-sub-menu`：

```vue
<el-sub-menu index="/datasets">
  <template #title>
    <el-icon><Folder /></el-icon>
    <span>数据集管理</span>
  </template>
  <el-menu-item index="/datasets/vector">
    <el-icon><MapLocation /></el-icon>
    <span>矢量数据集</span>
  </el-menu-item>
  <el-menu-item index="/datasets/raster">
    <el-icon><Picture /></el-icon>
    <span>影像数据集</span>
  </el-menu-item>
</el-sub-menu>
```

### 2. 路由调整

在 `router/index.ts` 中：

1. 新增 `/datasets/vector` 路由，指向 `datasets/index.vue`，默认 `type=vector`
2. 新增 `/datasets/raster` 路由，指向 `datasets/index.vue`，默认 `type=raster`
3. 修改 `/datasets` 为重定向到 `/datasets/vector`

```ts
{
  path: '/datasets',
  redirect: '/datasets/vector'
},
{
  path: '/datasets/vector',
  name: 'VectorDatasets',
  component: () => import('@/views/datasets/index.vue'),
  meta: { title: '矢量数据集', requiresAuth: true }
},
{
  path: '/datasets/raster',
  name: 'RasterDatasets',
  component: () => import('@/views/datasets/index.vue'),
  meta: { title: '影像数据集', requiresAuth: true }
}
```

### 3. datasets/index.vue 调整

接收路由参数 `type` 并自动设置搜索筛选：

```ts
const route = useRoute()

onMounted(() => {
  // 如果 URL 包含 type 参数，自动设置筛选
  if (route.path === '/datasets/vector') {
    searchForm.type = 'vector'
  } else if (route.path === '/datasets/raster') {
    searchForm.type = 'raster'
  }
  fetchDatasets()
})
```

### 4. 图标选择

使用 Element Plus 内置图标：
- 矢量数据集：`MapLocation` (表示地理/矢量数据)
- 影像数据集：`Picture` (表示图像)

## 验证步骤

1. 访问 /home，确认"数据集管理"是展开式子菜单
2. 点击"矢量数据集"，确认跳转 /datasets/vector，列表仅显示矢量数据
3. 点击"影像数据集"，确认跳转 /datasets/raster，列表仅显示影像数据
4. 访问 /datasets，确认重定向到 /datasets/vector
5. 确认菜单激活状态正确（高亮当前菜单项）
