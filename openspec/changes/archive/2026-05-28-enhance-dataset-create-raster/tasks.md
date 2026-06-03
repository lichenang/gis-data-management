# Tasks: enhance-dataset-create-raster

## Task 1: 新增影像上传模板区域 ✓

**文件**: `frontend/src/views/datasets/index.vue`

在 `<transition name="el-zoom-in-top">` 内 `<div v-if="form.type === 'vector'">` 之后，添加 `<div v-else-if="form.type === 'raster'">` 影像上传区域，包含：
- `el-upload` 组件，`accept=".tif,.tiff"`，`ref="rasterUploadRef"`
- 绑定 `on-change="handleRasterFileChange"`, `on-remove="handleRasterFileRemove"`

## Task 2: 改造底部按钮（按类型切换） ✓

**文件**: `frontend/src/views/datasets/index.vue`

将原来的"导入并创建"按钮改为条件渲染：
- `v-if="form.type === 'vector'"` → 保持原「导入并创建」按钮，调用 handleImport
- `v-else` → 显示「上传影像」按钮，调用 handleRasterSubmit，受 rasterUploadLoading / rasterFile 控制

## Task 3: 新增状态变量与 uploadImage 导入 ✓

**文件**: `frontend/src/views/datasets/index.vue`

1. 添加 import: `import { uploadImage } from '@/api/image'`
2. 新增响应式变量：
   - `const rasterUploadRef = ref<UploadInstance>()`
   - `const rasterFile = ref<File | null>(null)`
   - `const rasterUploadLoading = ref(false)`

## Task 4: 新增影像上传事件处理函数 ✓

**文件**: `frontend/src/views/datasets/index.vue`

添加：
- `handleRasterFileChange(file)` — 设置 rasterFile，自动填充名称
- `handleRasterFileRemove()` — 清空 rasterFile
- `handleRasterSubmit()` — 校验表单 → uploadImage → 成功关闭+刷新，失败提示错误

## Task 5: 更新 handleTypeChange 和初始化逻辑 ✓

**文件**: `frontend/src/views/datasets/index.vue`

1. `handleTypeChange(type)` — 增加 `rasterFile.value = null`
2. `handleCreate()` — 增加 `rasterFile.value = null`
3. `handleEdit(row)` — 增加 `rasterFile.value = null`
4. `handleDialogClose()` — 增加 `rasterFile.value = null`

## Task 6: 验证编译 ✓

```bash
cd frontend
npm run build  # 或 npm run lint
```

确认 TypeScript 编译无错误、Element Plus 组件绑定正确。
