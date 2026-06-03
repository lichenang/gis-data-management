# Design: fix-dataset-create-layout

## Layout Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                         单页表单式布局                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  创建数据集                                                   │   │
│  ├─────────────────────────────────────────────────────────────┤   │
│  │                                                             │   │
│  │  数据集名称 *                                                │   │
│  │  ┌─────────────────────────────────────────┐                │   │
│  │  │ 请输入数据集名称                         │                │   │
│  │  └─────────────────────────────────────────┘                │   │
│  │                                                             │   │
│  │  数据集类型 *                                                │   │
│  │  ┌─────────────┐                                             │   │
│  │  │ 矢量       ▾│  (选择影像时隐藏上传区域)                    │   │
│  │  └─────────────┘                                             │   │
│  │                                                             │   │
│  │  描述                                                         │   │
│  │  ┌─────────────────────────────────────────┐                │   │
│  │  │                                         │                │   │
│  │  └─────────────────────────────────────────┘                │   │
│  │                                                             │   │
│  │  ─────────────────────────────────────────────────────     │   │
│  │                                                             │   │
│  │  📁 上传空间数据文件（可选）                                  │   │
│  │      支持 GeoJSON 格式                                       │   │
│  │                                                             │   │
│  │  ┌─────────────────────────────────────────────────────────┐│   │
│  │  │                                                         ││   │
│  │  │            📂 拖拽文件到此处或点击上传                   ││   │
│  │  │                                                         ││   │
│  │  └─────────────────────────────────────────────────────────┘│   │
│  │                                                             │   │
│  │  [坐标系选择，仅上传后显示]                                   │   │
│  │                                                             │   │
│  │  ┌─────────────────────────────────────────────────────────┐│   │
│  │  │  ✓ 文件解析成功    几何类型: Point  要素: 1234          ││   │
│  │  └─────────────────────────────────────────────────────────┘│   │
│  │                               [取消]  [仅创建] [导入并创建]   │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

## Implementation Details

### 1. Remove Tab Structure

Current code to remove:
```vue
<el-tabs v-model="activeTab">
  <el-tab-pane label="基本信息" name="basic">...</el-tab-pane>
  <el-tab-pane label="文件上传" name="upload">...</el-tab-pane>
</el-tabs>
```

Replace with single form structure.

### 2. Reorganize Form Fields

```vue
<el-form ref="formRef" :model="form" :rules="formRules">
  
  <!-- Row 1: Name + Type side by side -->
  <el-row :gutter="20">
    <el-col :span="16">
      <el-form-item label="名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入数据集名称" />
      </el-form-item>
    </el-col>
    <el-col :span="8">
      <el-form-item label="类型" prop="type">
        <el-select v-model="form.type" style="width: 100%">
          <el-option label="矢量" value="vector" />
          <el-option label="影像" value="raster" />
        </el-select>
      </el-form-item>
    </el-col>
  </el-row>

  <!-- Row 2: Description -->
  <el-form-item label="描述">
    <el-input v-model="form.description" type="textarea" :rows="2" />
  </el-form-item>

  <!-- Row 3: Divider -->
  <el-divider content-position="left">
    <span>📁 上传空间数据（可选）</span>
  </el-divider>

  <!-- Row 4: File Upload - shown only for vector type -->
  <transition name="el-zoom-in-top">
    <div v-if="form.type === 'vector'" class="upload-section">
      <el-upload
        drag
        :auto-upload="false"
        :limit="1"
        :on-change="handleFileChange"
        :on-remove="handleFileRemove"
        accept=".geojson,.json"
      >
        <el-icon><UploadFilled /></el-icon>
        <div>拖拽文件到此处或<em>点击上传</em></div>
        <template #tip>支持 GeoJSON 格式</template>
      </el-upload>
      
      <!-- Coordinate system: shown after file upload -->
      <el-form-item v-if="uploadFile" label="坐标系" style="margin-top: 16px">
        <el-select v-model="form.srs" style="width: 200px">
          <el-option label="WGS84 (EPSG:4326)" value="EPSG:4326" />
          <el-option label="Web墨卡托 (EPSG:3857)" value="EPSG:3857" />
        </el-select>
      </el-form-item>

      <!-- Parse result -->
      <el-alert v-if="parseResult?.success" type="success" :closable="false" style="margin-top: 12px">
        几何类型: {{ parseResult.geometryType }} | 要素数量: {{ parseResult.featureCount }}
      </el-alert>
    </div>
  </transition>
</el-form>
```

### 3. Update Button Group

```vue
<template #footer>
  <el-button @click="dialogVisible = false">取消</el-button>
  <el-button :loading="loading" @click="handleCreateOnly">
    仅创建
  </el-button>
  <el-button 
    type="primary"
    :loading="loading"
    :disabled="!uploadFile"
    @click="handleImport"
  >
    导入并创建
  </el-button>
</template>
```

### 4. Button Logic

| Button | When Clicked | Action |
|--------|--------------|--------|
| 仅创建 | Always | Call `createDataset()` - creates empty dataset |
| 导入并创建 | Only when file uploaded | Call `importDataset()` - creates dataset with data |

### 5. Type Change Handler

```typescript
const handleTypeChange = (type: string) => {
  if (type === 'raster') {
    // Clear file upload state when switching to raster
    uploadFile.value = null
    parseResult.value = null
  }
}
```

### 6. Auto-fill Name from Filename

In `handleFileChange`:
```typescript
if (!form.name) {
  const nameWithoutExt = file.name.replace(/\.[^/.]+$/, '')
  form.name = nameWithoutExt
}
```

## Code Changes Summary

### Files to Modify
- `frontend/src/views/datasets/index.vue`

### State Variables (keep same or add)
- `form`: Dataset form data (existing)
- `uploadFile`: Currently uploaded file (rename from `uploadFile` or keep)
- `parseResult`: File parse result
- `loading`: Button loading state

### Methods to Update/ADD
- `handleCreateOnly()` - New method for simple create
- `handleImport()` - Existing method, simplify logic
- `handleTypeChange()` - New method

### Methods to Remove
- `handleCreate()` - Old combined method, no longer needed

## Acceptance Criteria

1. ✓ 单页面显示所有字段，无需 Tab 切换
2. ✓ 上传区域仅在选择"矢量"类型时显示
3. ✓ 点击"仅创建"调用 POST `/datasets`
4. ✓ 点击"导入并创建"调用 POST `/datasets/import`
5. ✓ "导入并创建"按钮在无文件时禁用
6. ✓ 上传文件后自动解析并显示结果
7. ✓ 未填名称时自动用文件名填充
8. ✓ 类型切换时清空文件上传状态
