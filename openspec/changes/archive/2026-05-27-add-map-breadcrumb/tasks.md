# Tasks: add-map-breadcrumb

## Task 1: 添加面包屑组件和样式

- [x] 完成

## Task 2: 验证

- [x] 完成 (需手动测试)

**File**: `frontend/src/views/map/index.vue`

**修改** template:

将:
```vue
<template>
  <div class="map-viewer">
```

改为:
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
```

**修改** style:

将:
```scss
<style scoped>
.map-viewer {
  display: flex;
  width: 100%;
  height: calc(100vh - 60px);
  overflow: hidden;
}
</style>
```

改为:
```scss
<style scoped>
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
  height: calc(100vh - 60px - 41px);
  overflow: hidden;
}
</style>
```

## Task 2: 验证

1. 访问地图页面 `/map`
2. 验证:
   - 页面顶部显示面包屑导航
   - "首页" 文字可点击
   - 点击后跳转至首页
   - 地图区域仍占满剩余高度
