# 地图图层列表不显示问题 - 诊断报告

## 问题现象

1. **左侧图层列表显示"暂无已发布数据集"** - 但 API 实际返回了 2 条数据
2. **Console 报错**: `ReferenceError: Click is not defined` at `MapContainer.vue:94`

## 根因分析

### 问题 1: MapContainer 中的 Click 变量未定义

**文件**: `frontend/src/views/map/MapContainer.vue`

```typescript
// 第 28 行: 导入的是 clickCondition
import { click as clickCondition } from 'ol/events/condition'

// 第 94 行: 使用的是 Click (未定义!)
const select = new Select({
  condition: Click,  // ❌ Click 未定义，应该是 clickCondition
  style: undefined
})
```

**问题**: 导入别名 `click as clickCondition` 后，实际变量名是 `clickCondition`，但代码中使用了 `Click`。

### 问题 2: 为什么图层列表显示为空

API 实际返回了数据：
```json
{
  "code": 200,
  "data": [
    {"id": 2, "name": "卫片执法", "status": "published"},
    {"id": 6, "name": "AAA", "status": "published"}
  ]
}
```

但前端报错的 **MapContainer 组件在 mounted 钩子中报错**，导致整个页面可能无法正常渲染。

```javascript
// MapContainer.vue:186 (mounted)
at initMap (MapContainer.vue:186:3)  // ← onMounted 调用 initMap
```

由于 MapContainer 报错，可能会影响父组件 Index 的渲染，导致 LayerPanel 的数据也无法正确显示。

## 问题流程

```
LayerPanel 加载
  │
  │ API 调用成功，返回 data
  │
  ▼
layers.value = response.data.map(...)  ← 应该显示数据
  │
  │ 但 MapContainer 报错
  │
  ▼
Vue 组件渲染出错
  │
  ▼
整个页面可能异常
```

## 修复方案

### 修复 MapContainer.vue

1. 将 `condition: Click` 改为 `condition: clickCondition`

```typescript
// 修改前
const select = new Select({
  condition: Click,  // ❌
  style: undefined
})

// 修改后
const select = new Select({
  condition: clickCondition,  // ✅
  style: undefined
})
```

2. 或者移除未使用的 import

```typescript
// 删除这个无用导入
import { click as clickCondition } from 'ol/events/condition'

// 直接使用 click
const select = new Select({
  condition: click,  // OpenLayers 10 中直接导入 click
  style: undefined
})
```

## 验证步骤

1. 修复 MapContainer 中的 Click 变量
2. 重新访问 `/map` 页面
3. 验证左侧图层列表显示已发布数据集
4. 验证勾选图层后地图显示要素

## 修改的文件

| 文件 | 行号 | 修改内容 |
|------|------|---------|
| `MapContainer.vue` | 94 | `Click` → `clickCondition` |
