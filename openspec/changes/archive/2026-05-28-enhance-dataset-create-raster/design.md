# Design: enhance-dataset-create-raster

## 当前对话框结构

```
datasets/index.vue
├── 列表区 (el-table + el-pagination)
└── 创建/编辑对话框 (el-dialog)
    ├── 名称 + 类型 (el-row / el-col)
    ├── 描述 (el-input)
    ├── 📁 上传区域
    │   └── v-if="form.type === 'vector'"  ← 矢量上传，影像类型无内容
    └── 底部按钮: [取消] [仅创建] [导入并创建]
```

## 修改方案

### 1. Template — 新增影像上传区域

在 `<transition name="el-zoom-in-top">` 内，现有的 `<div v-if="form.type === 'vector'">` 之后，添加：

```vue
<div v-else-if="form.type === 'raster'" class="upload-section">
  <el-upload ref="rasterUploadRef" drag :auto-upload="false" :limit="1"
    :on-change="handleRasterFileChange" :on-exceed="handleExceed"
    :on-remove="handleRasterFileRemove" accept=".tif,.tiff">
    <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
    <div class="el-upload__text">拖拽文件到此处或 <em>点击上传</em></div>
    <template #tip><div class="el-upload__tip">支持 GeoTIFF (.tif/.tiff) 格式</div></template>
  </el-upload>
  <!-- 无坐标系选择和解析预览 — 元数据由后端自动解析 -->
</div>
```

### 2. Template — 底部按钮按类型切换

```vue
<template #footer>
  <el-button @click="dialogVisible = false">取消</el-button>
  <el-button :loading="loading" @click="handleCreateOnly">仅创建</el-button>
  <el-button v-if="form.type === 'vector'" type="primary" :loading="importLoading"
    :disabled="!uploadFile" @click="handleImport">
    导入并创建
  </el-button>
  <el-button v-else type="primary" :loading="rasterUploadLoading"
    :disabled="!rasterFile" @click="handleRasterSubmit">
    上传影像
  </el-button>
</template>
```

### 3. Script — 新增状态与引用

```ts
import { uploadImage } from '@/api/image'

const rasterUploadRef = ref<UploadInstance>()
const rasterFile = ref<File | null>(null)
const rasterUploadLoading = ref(false)
```

### 4. Script — 新增事件处理

```ts
const handleRasterFileChange = (file: any) => {
  const rawFile = file.raw as File
  if (!rawFile) return
  rasterFile.value = rawFile
  if (!form.name) {
    form.name = rawFile.name.replace(/\.[^/.]+$/, '')
  }
}

const handleRasterFileRemove = () => {
  rasterFile.value = null
}

const handleRasterSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (!rasterFile.value) {
      ElMessage.warning('请先选择文件')
      return
    }
    rasterUploadLoading.value = true
    try {
      await uploadImage(rasterFile.value, form.name, form.description)
      ElMessage.success('影像上传成功')
      dialogVisible.value = false
      fetchDatasets()
    } catch (error: any) {
      console.error('Upload failed:', error)
      ElMessage.error(error.message || '上传失败')
    } finally {
      rasterUploadLoading.value = false
    }
  })
}
```

### 5. Script — handleTypeChange 清理两种类型

```ts
const handleTypeChange = (type: string) => {
  uploadFile.value = null
  parseResult.value = null
  rasterFile.value = null
}
```

### 6. Script — handleCreate / handleEdit / handleDialogClose

在重置状态的位置增加 `rasterFile.value = null` 清理。

## 数据流

```
选择"影像"类型
  ↓
拖拽 GeoTIFF → handleRasterFileChange
  ↓ (自动填充名称)
点击"上传影像" → handleRasterSubmit
  ↓
uploadImage(file, name, description)
  ↓
POST /api/v1/images/upload
  ↓ (后端处理)
ImageController → ImageServiceImpl.uploadImage
  → MinIO 存储 + GeoTiffParser 解析
  → 创建 Dataset + raster_metadata
  ↓
返回 Dataset 对象
  ↓
关闭对话框 → fetchDatasets() 刷新列表
```

## 边界情况处理

| 场景 | 处理 |
|------|------|
| 切换到影像后切回矢量 | handleTypeChange 清理两种文件状态 |
| 编辑模式 | 编辑对话框禁用类型切换 (或只读) |
| 仅创建 (不传文件) | 已有 handleCreateOnly 正常工作 |
| 影像上传失败 | catch 后 ElMessage.error，对话框保持打开 |
| 文件超限 | handleExceed 提示仅允许单文件 |

## 不涉及

- 后端无变更
- Dataset API 无变更
- Image API 无变更
- 影像管理页 images/index.vue 无变更
