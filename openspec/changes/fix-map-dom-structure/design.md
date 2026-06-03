# Design: fix-map-dom-structure

## Overview

修复 index.vue 中的 DOM 结构和 CSS 布局问题。

## Problem 1: 多余的闭合标签

**当前代码** (index.vue 第 35-36 行):
```vue
    </el-dialog>
  </div>   ← 多余！
</template>
```

**修复**: 删除第 36 行的多余 `</div>`。

## Problem 2: el-container 缺少高度

**当前代码** (index.vue):
```vue
<div class="map-viewer">
  <el-container>  ← 无高度
```

**修复**: 在 style 中添加:
```scss
.map-viewer .el-container {
  height: 100%;
}
```

## Problem 3: map-main 子元素高度

**当前代码**:
```vue
<el-main class="map-main">
  <MapContainer ... />
</el-main>
```

**修复**: 添加:
```scss
.map-main > * {
  height: 100%;
}
```

## Complete Style Changes

```scss
<style scoped>
.map-page {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100vh;
  overflow: hidden;
}

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

/* 新增 el-container 样式 */
.map-viewer .el-container {
  height: 100%;
}

.map-aside {
  padding: 0;
  overflow: hidden;
}

.map-main {
  padding: 0;
  overflow: hidden;
  flex: 1;
  display: flex;
}

.map-main > * {
  flex: 1;
  height: 100%;
}

/* 其他样式保持不变 */
</style>
```
