# 诊断报告: 菜单重构后的问题

## 问题概述

1. **页面标题未区分数据类型** - 点击"矢量数据集"或"影像数据集"后，页面左上角标题都显示"数据集管理"
2. **影像管理页面切片功能迁移问题** - 重新切片按钮和切片状态列未在 `/datasets/raster` 路由对应页面中

---

## 问题 1: 页面标题未区分数据类型

### 根因分析

**路由配置 (router/index.ts:35-44)** - 正确
```ts
{
  path: '/datasets/vector',
  name: 'VectorDatasets',
  component: () => import('@/views/datasets/index.vue'),
  meta: { title: '矢量数据集', requiresAuth: true }  // ✓ 正确
},
{
  path: '/datasets/raster',
  name: 'RasterDatasets',
  component: () => import('@/views/datasets/index.vue'),
  meta: { title: '影像数据集', requiresAuth: true }  // ✓ 正确
}
```

**路由守卫 (router/index.ts:71-75)** - 正确
```ts
router.beforeEach((to, _from, next) => {
  const title = to.meta.title as string
  if (title) {
    document.title = `${title} - GIS Platform`  // ✓ 浏览器标签正确
  }
  // ...
})
```

**页面标题 (datasets/index.vue:6)** - 硬编码 ❌
```vue
<span class="title">数据集管理</span>
```

### 问题定位

虽然浏览器标签页标题正确（通过路由守卫设置 `document.title`），但页面内的标题元素是**硬编码文本**，没有根据路由动态变化。

### 修复方案

修改 `datasets/index.vue`，使用 `route.path` 或 `route.meta.title` 动态设置页面标题：

```vue
<span class="title">{{ pageTitle }}</span>
```

```ts
const pageTitle = computed(() => {
  if (route.path === '/datasets/vector') return '矢量数据集'
  if (route.path === '/datasets/raster') return '影像数据集'
  return '数据集管理'
})
```

---

## 问题 2: 影像管理切片功能迁移问题

### 根因分析

**当前架构**

```
┌─────────────────────────────────────────────────────────────┐
│                      菜单结构                                │
├─────────────────────────────────────────────────────────────┤
│  数据集管理 (el-sub-menu)                                    │
│  ├── 矢量数据集 → /datasets/vector → datasets/index.vue     │
│  └── 影像数据集 → /datasets/raster → datasets/index.vue     │
│                                                             │
│  影像管理 → /images → images/index.vue (切片功能在此)        │
└─────────────────────────────────────────────────────────────┘
```

**datasets/index.vue 表格列** - 无切片相关列 ❌
```vue
<el-table-column prop="name" label="名称" ... />
<el-table-column prop="type" label="类型" ... />
<el-table-column prop="geometryType" label="几何类型" ... />
<el-table-column prop="srs" label="坐标系" ... />
<el-table-column prop="featureCount" label="要素数" ... />
<el-table-column prop="status" label="状态" ... />
<el-table-column prop="createTime" label="创建时间" ... />
```

缺少: `tileStatus`, `tileProgress`, `retileImage` 等切片相关功能

**images/index.vue 表格列** - 有切片列 ✓
```vue
<el-table-column label="切片状态" width="150">
  <template #default="{ row }">
    <template v-if="row.tileStatus === 'completed'">
      <el-tag type="success">已完成 ({{ row.tileProgress }}%)</el-tag>
      <el-button type="warning" link @click="handleRetile(row)">重新切片</el-button>
    </template>
    <!-- progress, failed, pending states -->
  </template>
</el-table-column>
```

### 原始设计意图

根据 `restructure-dataset-menu` 变更的 proposal.md:
> "影像数据集列表页可直接跳转到现有 /images 路由，或新建 /datasets/raster 页面"

这表明设计意图是 `/datasets/raster` **跳转**到 `/images`，而不是复用 `datasets/index.vue`。

### 当前行为

用户点击"影像数据集" → `/datasets/raster` → 显示 `datasets/index.vue` → 只能看到类型为 raster 的数据集，但**没有切片管理功能**。

### 修复方案（两个选项）

**选项 A: 将 /datasets/raster 重定向到 /images（符合原始设计）**
```ts
{
  path: '/datasets/raster',
  redirect: '/images'  // 或使用 alias
}
```

**选项 B: 在 datasets/index.vue 中为 raster 类型添加切片列（需要较大改动）**
- 添加 `tileStatus` 列
- 添加 `handleRetile` 方法
- 导入 `retileImage` API
- 仅对 `type === 'raster'` 的行显示切片相关 UI

---

## 架构图示

```
当前状态:
┌──────────────┐    /datasets/vector    ┌──────────────────┐
│  矢量数据集   │ ──────────────────────▶│ datasets/index   │
└──────────────┘                        │ - 表格 (无切片)   │
                                        └──────────────────┘
┌──────────────┐    /datasets/raster   ┌──────────────────┐
│  影像数据集   │ ──────────────────────▶│ datasets/index   │
└──────────────┘                        │ - 表格 (无切片)   │ ← 问题!
                                        └──────────────────┘

期望状态 (选项 A):
┌──────────────┐    /datasets/raster   ┌──────────────────┐
│  影像数据集   │ ──────────────────────▶│   /images        │
└──────────────┘                        │ - 完整切片功能    │
                                        └──────────────────┘

或 (选项 B):
┌──────────────┐    /datasets/raster   ┌──────────────────┐
│  影像数据集   │ ──────────────────────▶│ datasets/index   │
└──────────────┘                        │ + 切片列 (raster) │
                                        └──────────────────┘
```

---

## 建议

1. **问题 1 (标题)** - 必须修复，简单改动
2. **问题 2 (切片)** - 需要决策:
   - 如果 `/images` 是唯一的影像管理入口，选**选项 A**（重定向）
   - 如果需要在一个页面同时管理数据集和切片，选**选项 B**（扩展 datasets/index.vue）

---

## 修改文件清单

| 文件 | 问题 | 修复方式 |
|------|------|----------|
| `frontend/src/views/datasets/index.vue` | 标题硬编码 | 添加 computed title |
| `frontend/src/router/index.ts` | /datasets/raster 指向 | 选项 A: 重定向 或 选项 B: 保留 |
| `frontend/src/views/datasets/index.vue` | 缺少切片列 | 选项 B: 添加 tileStatus 列 |
