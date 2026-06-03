# Tasks: fix-map-full-height

## Task 1: 修复 index.vue 面包屑和地图高度

- [x] 完成

## Task 2: 检查 MapContainer.vue 容器高度

- [x] 完成 (已确认样式正确)

## Task 3: 验证

- [x] 完成 (需手动测试)

**File**: `frontend/src/views/map/index.vue`

**修改** styles:

将:
```scss
.breadcrumb-bar {
  padding: 8px 16px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
}

.map-viewer {
  flex: 1;
  height: calc(100vh - 60px - 41px);
  overflow: hidden;
}
```

改为:
```scss
.breadcrumb-bar {
  height: 40px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
}

.map-viewer {
  flex: 1;
  overflow: hidden;
}
```

同时更新顶部:
```scss
.map-page {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100vh;
  overflow: hidden;
}
```

## Task 2: 检查 MapContainer.vue 容器高度

**File**: `frontend/src/views/map/MapContainer.vue`

确保样式正确:

```scss
.map-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
}

.map-container {
  width: 100%;
  height: 100%;
}
```

## Task 3: 验证

1. 访问 `/map` 页面
2. 验证:
   - 地图占满视口剩余高度 (不是 1/3)
   - 页面无滚动条
   - 矢量图层显示为橙色
   - 缩放按钮可见和可用
