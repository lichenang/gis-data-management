# Design: fix-map-full-height

## Overview

修改地图页面的 CSS 确保地图区域占满视口。

## Problem

当前 index.vue 中的高度计算:
```scss
.map-viewer {
  height: calc(100vh - 60px - 41px);  // 计算不准确
}
```

- 60px: 顶部导航栏
- 41px: 面包屑 (但实际应该是 40px 或更多)
- 结果: 高度不足

## Solution

### 方案 1: 使用 flex 布局自动计算 (推荐)

```scss
.map-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

.breadcrumb-bar {
  height: 40px;  // 固定高度
  flex-shrink: 0;
}

.map-viewer {
  flex: 1;
  overflow: hidden;
  // 不需要手动计算高度
}
```

### 方案 2: 更精确的 calc

如果方案1不生效，使用更保守的计算:
```scss
.map-viewer {
  height: calc(100vh - 100px);  // 60px 导航 + 40px 面包屑
}
```

## Files to Modify

1. `frontend/src/views/map/index.vue`:
   - 面包屑栏使用固定高度
   - 调整 map-viewer 高度计算

2. 检查 `MapContainer.vue`:
   - 确保 .map-wrapper 和 .map-container 也是 100% 高度
