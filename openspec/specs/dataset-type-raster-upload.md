# Spec: 数据集创建对话框 — 类型切换 & 影像上传

## 1. 现状分析

```
┌─────────────────────────────────────────────────────────────────┐
│                    数据集创建对话框 (当前)                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────┐       │
│  │ 名称: [________________]  类型: [矢量 ▼]            │       │
│  │ 描述: [________________]                            │       │
│  ├─────────────────────────────────────────────────────┤       │
│  │ 📁 上传空间数据（可选）                              │       │
│  │                                                     │       │
│  │  ┌─── v-if="form.type === 'vector'" ────────────┐  │       │
│  │  │  [拖拽 GeoJSON 上传区域]                       │  │       │
│  │  │  支持 .geojson/.json                          │  │       │
│  │  │  坐标系: [EPSG:4326 ▼]                       │  │       │
│  │  │  ✅ 文件解析成功 (几何类型: Polygon, N要素: 5)  │  │       │
│  │  └─────────────────────────────────────────────┘  │       │
│  │                                                    │       │
│  │  ┌─── type === 'raster' 时 ──────────────────────┐ │       │
│  │  │  (空 — 没有上传组件)                          │ │       │
│  │  └─────────────────────────────────────────────┘ │       │
│  ├─────────────────────────────────────────────────────┤       │
│  │              [取消]  [仅创建]  [导入并创建]          │       │
│  └─────────────────────────────────────────────────────┘       │
│                                                                 │
│  ❌ 选择"影像"类型时无上传组件可用                              │
│  ❌ "导入并创建" 仅调用 /api/v1/datasets/import (矢量专用)     │
│  ❌ 后端已存在 POST /api/v1/images/upload (但前端未集成)        │
└─────────────────────────────────────────────────────────────────┘
```

### 涉及文件

| 文件 | 角色 |
|------|------|
| `frontend/src/views/datasets/index.vue` | 数据集列表 + 创建/编辑对话框 |
| `frontend/src/api/dataset.ts` | Dataset API 类型定义与方法 |
| `frontend/src/api/image.ts` | Image API (已有 uploadImage) |
| `frontend/src/views/images/index.vue` | 影像管理页 (参考 upload 实现) |
| `backend/.../controller/ImageController.java` | 影像上传接口 (已实现) |

## 2. 目标状态

```
┌─────────────────────────────────────────────────────────────────┐
│                    数据集创建对话框 (改造后)                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────┐       │
│  │ 名称: [________________]  类型: [矢量/影像 ▼]      │       │
│  │ 描述: [________________]                            │       │
│  ├─────────────────────────────────────────────────────┤       │
│  │ 📁 上传空间数据（可选）                              │       │
│  │                                                     │       │
│  │  ┌─── type === 'vector' ─────────────────────────┐ │       │
│  │  │  [拖拽 GeoJSON 上传区域]   ← 保持现有不变      │ │       │
│  │  │  支持 .geojson/.json                          │ │       │
│  │  └─────────────────────────────────────────────┘ │       │
│  │                                                    │       │
│  │  ┌─── type === 'raster' ─────────────────────────┐ │       │
│  │  │  [拖拽 GeoTIFF 上传区域]   ← 新增             │ │       │
│  │  │  支持 .tif/.tiff                              │ │       │
│  │  │  (无需坐标系选择、无需解析结果展示)             │ │       │
│  │  └─────────────────────────────────────────────┘ │       │
│  ├─────────────────────────────────────────────────────┤       │
│  │   type='vector': [取消]  [仅创建]  [导入并创建]     │       │
│  │   type='raster':  [取消]  [仅创建]  [上传影像]      │       │
│  └─────────────────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────────────┘
```

## 3. 修改方案

### 3.1 声明式：上传区域按类型切换

```vue
<!-- 矢量上传区域 — 保持现有逻辑不变 -->
<transition name="el-zoom-in-top">
  <div v-if="form.type === 'vector'" class="upload-section">
    <el-upload ref="uploadRef" ... accept=".geojson,.json"> ... </el-upload>
    <div v-if="uploadFile" class="file-info">
      <el-form-item label="坐标系"> ... </el-form-item>
    </div>
    <div v-if="parseResult && parseResult.success" class="parse-result"> ... </div>
  </div>

  <div v-else-if="form.type === 'raster'" class="upload-section">
    <el-upload ref="rasterUploadRef" ... accept=".tif,.tiff"> ... </el-upload>
    <!-- 无坐标系选择、无需解析预览 — GeoTIFF 元数据由后端自动解析 -->
  </div>
</transition>
```

### 3.2 逻辑层变更

| 变更点 | 说明 |
|--------|------|
| **新增 state** | `rasterUploadRef`, `rasterFile`, `rasterUploadLoading` |
| **新增 import** | `import { uploadImage } from '@/api/image'` |
| **handleTypeChange** | 清除两种类型的文件状态 |
| **新增 handleRasterFileChange** | 类似 handleFileChange，但不调 parseDatasetFile |
| **新增 handleRasterSubmit** | 调 uploadImage(file, name, description) → 刷新列表 |
| **handleImport 保留** | 仅对 vector 类型有效 |
| **底部按钮** | `type === 'raster'` 时，"导入并创建" 改为 "上传影像"，调用 handleRasterSubmit |

### 3.3 数据流对比

```
矢量上传 (现有):
  UI drag GeoJSON → parseDatasetFile() → 展示解析结果
  ↓
  用户点击"导入并创建" → importDataset(file, name, desc, srs)
  ↓
  POST /api/v1/datasets/import → 后端解析入库 → 返回 Dataset
  ↓
  关闭对话框 → fetchDatasets()

影像上传 (新增):
  UI drag GeoTIFF → (无前端解析)
  ↓
  用户点击"上传影像" → uploadImage(file, name, desc)
  ↓
  POST /api/v1/images/upload → 后端 MinIO + GeoTiffParser → 返回 Dataset
  ↓
  关闭对话框 → fetchDatasets()
```

### 3.4 状态管理变化

当前 form 对象：
```ts
const form = reactive<Dataset>({
  name: '', description: '', type: 'vector', srs: 'EPSG:4326'
})
```

新增状态：
```ts
const rasterFile = ref<File | null>(null)
const rasterUploadLoading = ref(false)
```

handleCreate / handleEdit / handleDialogClose 中需额外清除 `rasterFile`。

## 4. 边界情况

| 场景 | 处理方式 |
|------|----------|
| 编辑已有数据集时切换类型 | 编辑对话框中 type 不可变（仅创建时可选）或禁用切换 |
| 未选文件点击"上传影像" | disabled 状态 + ElMessage.warning('请先选择文件') |
| 非 GeoTIFF 文件上传 | el-upload accept 限制 .tif/.tiff，后端也做二次校验 |
| 上传失败 | 捕获异常，ElMessage.error 展示后端错误信息 |
| 名称自动填充 | 参考 images/index.vue，选中文件后自动填充 name |

## 5. 不涉及的范围

- 不修改后端代码 (`ImageController`, `ImageServiceImpl`, `GeoTiffParser` 等已就绪)
- 不修改 `api/dataset.ts` 中的现有方法
- 不影响 `api/image.ts` 中的 `uploadImage` 签名
- 不涉及影像列表页面 (`images/index.vue`)
