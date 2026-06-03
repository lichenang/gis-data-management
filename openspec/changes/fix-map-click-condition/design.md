# Design: fix-map-click-condition

## Technical Design

### Current Problem

```typescript
// 第 28 行
import { click as clickCondition } from 'ol/events/condition'

// 第 94 行
const select = new Select({
  condition: Click,  // ❌ Click 未定义
  style: undefined
})
```

### Solution

修改使用方式：

```typescript
// 修改后
const select = new Select({
  condition: clickCondition,  // ✅ 使用正确的变量名
  style: undefined
})
```

### Alternative (Simplify)

OpenLayers 10 可以直接导入并使用 `click`：

```typescript
// 简化导入
import { click } from 'ol/events/condition'

// 使用 click
const select = new Select({
  condition: click,
  style: undefined
})
```

推荐使用第一种方式（保持现有导入，只修改变量使用）。
