# Tasks: fix-map-click-condition

## Task 1: 修复 Click 变量名

**File**: `frontend/src/views/map/MapContainer.vue`

**修改内容**:

将第 94 行：
```typescript
const select = new Select({
  condition: Click,  // ❌
  style: undefined
})
```

改为：
```typescript
const select = new Select({
  condition: clickCondition,  // ✅
  style: undefined
})
```

## Task 2: 验证

1. 刷新地图页面
2. 打开 Console 检查无报错
3. 验证左侧图层列表显示已发布数据集
4. 勾选图层验证地图显示要素
