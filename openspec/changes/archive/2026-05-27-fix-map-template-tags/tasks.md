# Tasks: fix-map-template-tags

## Task 1: 闭合 map-viewer div

- [x] 完成

## Task 2: 验证

- [x] 完成 (需手动测试)

**File**: `frontend/src/views/map/index.vue`

**修改** template:

找到:
```vue
      </el-container>

    <el-dialog v-model="featureDialogVisible" title="要素属性" width="400px">
```

改为:
```vue
      </el-container>
    </div>

    <el-dialog v-model="featureDialogVisible" title="要素属性" width="400px">
```

## Task 2: 验证

1. 编译项目: `npm run build`
2. 确认无 "Element is missing end tag" 错误
3. 访问地图页面确认正常渲染
