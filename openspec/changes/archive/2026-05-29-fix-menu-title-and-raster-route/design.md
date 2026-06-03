# 设计: fix-menu-title-and-raster-route

## 问题 1: 页面标题动态化

### 当前问题

`datasets/index.vue` 第 6 行硬编码标题：
```vue
<span class="title">数据集管理</span>
```

虽然路由 meta.title 和浏览器标签页正确，但页面内标题无法区分数据类型。

### 修改方案

添加 computed 属性：

```ts
const pageTitle = computed(() => {
  if (route.path === '/datasets/vector') return '矢量数据集'
  if (route.path === '/datasets/raster') return '影像数据集'
  return '数据集管理'
})
```

模板修改：
```vue
<span class="title">{{ pageTitle }}</span>
```

## 问题 2: /datasets/raster 路由重定向

### 当前问题

`router/index.ts` 中 /datasets/raster 指向 `datasets/index.vue`，该页面缺少切片功能。

### 修改方案

将 /datasets/raster 从组件路由改为重定向路由：

```ts
{
  path: '/datasets/raster',
  redirect: '/images'
},
```

修改后的路由配置：
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
// 删除 RasterDatasets 组件路由，改为重定向
{
  path: '/datasets/raster',
  redirect: '/images'
},
```

## 验证步骤

1. 访问 /datasets/vector，确认页面标题显示"矢量数据集"
2. 访问 /datasets/raster，确认自动跳转到 /images
3. 访问 /datasets，确认自动跳转到 /datasets/vector
4. 确认浏览器标签页标题正确
