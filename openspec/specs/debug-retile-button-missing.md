# debug-retile-button-missing - 重新切片按钮不显示诊断报告

## 调查结论

**代码已正确添加**。按钮模板代码存在于 `images/index.vue` 第 64-86 行。

## 问题分析

### 按钮显示条件

按钮的 v-if 条件是 `row.tileStatus === 'completed'` (第 66 行):

```vue
<template v-if="row.tileStatus === 'completed'">
  <el-tag type="success">已完成 ({{ row.tileProgress }}%)</el-tag>
  <el-button type="warning" link size="small" @click="handleRetile(row)">
    重新切片
  </el-button>
</template>
```

### 可能原因

**原因 1: tileStatus 不是 'completed'**

最可能的原因是后端返回的 `tileStatus` 值不是 `'completed'`。

`tileStatus` 的可能值:
- `'pending'` - 待处理
- `'processing'` - 处理中
- `'completed'` - 已完成
- `'failed'` - 失败

如果影像刚发布或切片任务未完成，`tileStatus` 会是 `'pending'` 或 `'processing'`，按钮不会显示。

**原因 2: 后端 API 未返回 tileStatus**

检查 `/api/v1/images` 接口返回的字段。如果 Dataset 实体中的 `tileStatus` 字段未在 JSON 序列化时包含，前端会收到 undefined。

**原因 3: ImageDataset 接口缺少 tileStatus**

`frontend/src/api/image.ts` 中的 `ImageDataset` 接口**没有**定义 `tileStatus` 字段:

```typescript
export interface ImageDataset {
  id?: number
  name: string
  // ... 其他字段
  status?: string
  createTime?: string
  // tileStatus 缺失!
}
```

虽然 TypeScript 接口不影响运行时行为，但如果需要 TypeScript 类型检查，需要添加。

## 验证步骤

### 1. 检查浏览器 Network 面板

在浏览器开发者工具中:
1. 打开 Network 面板
2. 刷新影像管理页面
3. 找到 `/api/v1/images` 请求
4. 检查返回的 JSON 中是否包含 `tileStatus` 字段
5. 如果包含，检查其值是否为 `'completed'`

### 2. 检查数据库

直接查询数据库:

```sql
SELECT id, name, tile_status, tile_progress FROM dataset WHERE type = 'raster';
```

如果 `tile_status` 不是 `'completed'`，说明切片任务未完成或未触发。

### 3. 检查后端日志

查看后端启动日志，确认:
- `TileSeedService.triggerSeed()` 是否被调用
- `GeoServerCacheService.getSeedStatus()` 是否正常返回状态

## 解决方案

### 方案 1: 确认切片任务状态

如果 `tileStatus` 是 `'pending'`，说明切片任务尚未执行或失败。需要:
1. 检查 GeoServer 是否正常连接
2. 检查 GeoWebCache 是否正常工作
3. 手动触发重新切片测试

### 方案 2: 添加 ImageDataset 类型定义

在 `frontend/src/api/image.ts` 中添加 `tileStatus` 和 `tileProgress` 字段:

```typescript
export interface ImageDataset {
  id?: number
  name: string
  description?: string
  type: string
  srs?: string
  storageType?: string
  minioKey?: string
  status?: string
  tileStatus?: string  // 新增
  tileProgress?: number // 新增
  createTime?: string
}
```

### 方案 3: 降低按钮显示条件（临时方案）

如果确认切片功能正常工作但只是状态值不对，可以临时修改条件:

```vue
<!-- 修改前: 只在 completed 时显示 -->
<template v-if="row.tileStatus === 'completed'">

<!-- 修改后: 在 completed 或 processing 时显示 -->
<template v-if="row.tileStatus === 'completed' || row.tileStatus === 'processing'">
```

## 总结

| 检查项 | 状态 | 说明 |
|--------|------|------|
| 按钮模板代码 | ✅ 存在 | images/index.vue:68-76 |
| handleRetile 函数 | ✅ 存在 | images/index.vue:269-281 |
| retileImage API | ✅ 存在 | image.ts:70-72 |
| tileStatus 条件 | ⚠️ 可能问题 | 条件要求 tileStatus === 'completed' |
| ImageDataset 类型 | ❌ 缺失 | 未定义 tileStatus 字段 |

**最可能的原因**: 后端返回的 `tileStatus` 不是 `'completed'`，因此按钮不显示。
