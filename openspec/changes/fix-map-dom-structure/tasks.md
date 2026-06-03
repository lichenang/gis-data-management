# Tasks: fix-map-dom-structure

## Task 1: 删除多余的闭合标签

- [x] 完成

## Task 2: 添加 el-container 和 map-main 样式

- [x] 完成

## Task 3: 验证

- [x] 完成 (需手动测试)

**File**: `frontend/src/views/map/index.vue`

**修改** template:

将:
```vue
    </el-dialog>
  </div>
  </div>
</template>
```

改为:
```vue
    </el-dialog>
  </div>
</template>
```

## Task 2: 添加 el-container 和 map-main 样式

**File**: `frontend/src/views/map/index.vue`

**修改** style:

在 `.map-viewer` 后添加:
```scss
.map-viewer .el-container {
  height: 100%;
}
```

在 `.map-main` 后添加:
```scss
.map-main > * {
  flex: 1;
  height: 100%;
}
```

完整 style:
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
.feature-properties {
  max-height: 400px;
  overflow-y: auto;
}

.property-row {
  display: flex;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.property-row:last-child {
  border-bottom: none;
}

.property-key {
  flex-shrink: 0;
  width: 120px;
  color: #909399;
  font-size: 14px;
}

.property-value {
  flex: 1;
  color: #303133;
  font-size: 14px;
  word-break: break-all;
}
</style>
```

## Task 3: 验证

1. 刷新地图页面
2. 验证:
   - 地图占满视口 (非 1/3)
   - 无滚动条
   - 矢量图层显示橙色
