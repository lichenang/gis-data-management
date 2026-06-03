# Design: add-map-breadcrumb

## Overview

在地图页面模板中添加面包屑组件，使用 Element Plus 的 el-breadcrumb。

## Changes

**File**: `frontend/src/views/map/index.vue`

### Template 结构

```vue
<template>
  <div class="map-page">
    <div class="breadcrumb-bar">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>地图查看</el-breadcrumb-item>
      </el-breadcrumb>
    </div>
    
    <div class="map-viewer">
      <!-- 原有内容 -->
    </div>
  </div>
</template>
```

### Style 调整

```scss
.map-page {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
}

.breadcrumb-bar {
  padding: 8px 16px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
}

.map-viewer {
  flex: 1;
  height: calc(100vh - 60px - 40px);  // 减去导航栏和面包屑高度
  overflow: hidden;
}
```

## Implementation Notes

- 使用 Element Plus 的 `el-breadcrumb` 和 `el-breadcrumb-item`
- 面包屑高度约 40px
- 地图容器高度使用 `calc()` 动态计算
