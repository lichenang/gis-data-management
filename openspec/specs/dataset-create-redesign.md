# 数据集创建流程重新设计

## 当前问题分析

```
┌─────────────────────────────────────────────────────────────────────┐
│                        当前实现的问题                                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                    创建数据集 Dialog                         │   │
│  ├─────────────────────────────────────────────────────────────┤   │
│  │                                                             │   │
│  │   [基本信息]  [文件上传]                                     │   │
│  │        │            │                                        │   │
│  │        │            │                                        │   │
│  │   ┌────▼────┐   ┌───▼────┐                                   │   │
│  │   │ 名称    │   │上传组件 │     ← 两个 Tab 物理分离           │   │
│  │   │ 类型    │   │        │                                   │   │
│  │   │ 描述    │   │        │                                   │   │
│  │   └─────────┘   └────────┘                                   │   │
│  │                                                             │   │
│  │              [导入数据集] ← 单一按钮                           │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  问题:                                                               │
│  1. 用户在"基本信息"填完后点击导入 → 可能忘记上传文件                 │
│  2. 用户上传文件后自动解析成功 → 但可能没填名称                       │
│  3. 两个 Tab 是平行关系，没有引导关系                                 │
│  4. 用户不知道应该先做什么、后做什么                                  │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 方案对比

### 方案 1: 两步向导式 (Step Wizard)

```
Step 1: 基础信息          Step 2: 上传文件
┌─────────────────┐      ┌─────────────────┐
│  数据集名称     │ ──→  │                 │
│  *              │      │  ┌───────────┐  │
│                 │      │  │  上传区域  │  │
│  数据集类型     │      │  │  (drag)   │  │
│  * [矢量 ▾]     │      │  └───────────┘  │
│                 │      │                 │
│  描述           │      │  解析结果:      │
│  [.........]   │      │  ✓ Point        │
│                 │      │  ✓ 1234 要素    │
│  [上一步] [下一步]    │  ✓ EPSG:4326    │
│                 │      │                 │
│                 │      │  [上一步] [完成] │
└─────────────────┘      └─────────────────┘
```

**优点**:
- 流程清晰，用户知道当前在哪一步
- 每一步有明确的目标
- 可以做每步校验

**缺点**:
- 需要维护步进状态
- 用户想修改上一步需要返回
- 如果只需要创建空数据集（不导入文件），流程显得繁琐

---

### 方案 2: 单页表单式 (推荐)

```
┌─────────────────────────────────────────────────────────────────────┐
│                         推荐方案: 单页表单                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  创建数据集                                                   │   │
│  ├─────────────────────────────────────────────────────────────┤   │
│  │                                                             │   │
│  │  数据集名称 *                                                │   │
│  │  ┌─────────────────────────────────────────┐ [自动填充]     │   │
│  │  │ 请输入数据集名称                         │                │   │
│  │  └─────────────────────────────────────────┘                │   │
│  │                                                             │   │
│  │  数据集类型 *                                                │   │
│  │  ┌─────────────┐                                             │   │
│  │  │ 矢量       ▾│  (切换时动态显示/隐藏上传区域)               │   │
│  │  └─────────────┘                                             │   │
│  │                                                             │   │
│  │  ─────────────────────────────────────────────────────     │   │
│  │                                                             │   │
│  │  📁 上传空间数据文件（可选）                                  │   │
│  │      支持 GeoJSON, Shapefile 等格式                          │   │
│  │                                                             │   │
│  │  ┌─────────────────────────────────────────────────────────┐│   │
│  │  │                                                         ││   │
│  │  │            📂 拖拽文件到此处或点击上传                   ││   │
│  │  │                                                         ││   │
│  │  │              支持格式：GeoJSON (.geojson/.json)          ││   │
│  │  │                                                         ││   │
│  │  └─────────────────────────────────────────────────────────┘│   │
│  │                                                             │   │
│  │  ┌─────────────────────────────────────────────────────────┐│   │
│  │  │  ✓ 文件解析成功                                          ││   │
│  │  │  • 几何类型: Point                                       ││   │
│  │  │  • 要素数量: 1,234                                       ││   │
│  │  │  • 坐标系: EPSG:4326                                     ││   │
│  │  └─────────────────────────────────────────────────────────┘│   │
│  │                               [仅创建] [导入并创建]          │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

**核心设计理念**:

1. **单页合一**: 信息填写 + 文件上传在同一个页面，平铺展示
2. **文件上传可选**: 可单独创建空数据集（仅填基本信息）
3. **按钮区分意图**: 
   - "仅创建" = 不带文件，仅创建数据库记录
   - "导入并创建" = 带文件，创建记录并导入数据到 PostGIS
4. **智能提示**: 上传文件后自动解析并填充名称（如用户未填）

---

## 详细设计

### 布局结构

```vue
<el-dialog>
  <!-- 头部: 标题 -->
  <template #header>创建数据集</template>

  <!-- 主体: 单页表单 -->
  <el-form :model="form" :rules="rules">
    
    <!-- 第一行: 名称 + 类型并排 -->
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

    <!-- 第二行: 描述 -->
    <el-form-item label="描述">
      <el-input v-model="form.description" type="textarea" :rows="2" />
    </el-form-item>

    <!-- 分隔线 -->
    <el-divider content-position="left">
      <span class="divider-text">📁 上传空间数据（可选）</span>
    </el-divider>

    <!-- 文件上传区域: 仅矢量类型显示 -->
    <transition name="el-zoom-in-top">
      <div v-if="form.type === 'vector'" class="upload-section">
        
        <!-- 上传组件 -->
        <el-upload
          drag
          :auto-upload="false"
          :limit="1"
          :on-change="handleFileChange"
          :on-exceed="handleExceed"
          :on-remove="handleFileRemove"
          accept=".geojson,.json,.shp"
        >
          <el-icon><UploadFilled /></el-icon>
          <div class="el-upload__text">拖拽文件到此处或<em>点击上传</em></div>
          <template #tip>
            <div class="el-upload__tip">支持 GeoJSON 格式</div>
          </template>
        </el-upload>

        <!-- 坐标系选择: 上传后显示 -->
        <el-form-item v-if="uploadFile" label="坐标系" style="margin-top: 16px">
          <el-select v-model="form.srs" style="width: 200px">
            <el-option label="WGS84 (EPSG:4326)" value="EPSG:4326" />
            <el-option label="Web墨卡托 (EPSG:3857)" value="EPSG:3857" />
          </el-select>
        </el-form-item>

        <!-- 解析结果: 上传并解析成功后显示 -->
        <el-alert
          v-if="parseResult?.success"
          title="文件解析成功"
          type="success"
          :closable="false"
          show-icon
          style="margin-top: 12px"
        >
          <template #default>
            <div class="parse-info">
              <span>几何类型: {{ parseResult.geometryType }}</span>
              <span class="ml-4">要素数量: {{ parseResult.featureCount }}</span>
            </div>
          </template>
        </el-alert>

        <!-- 解析失败提示 -->
        <el-alert
          v-if="parseError"
          title="文件解析失败"
          type="error"
          :closable="true"
          style="margin-top: 12px"
        >
          {{ parseError }}
        </el-alert>
      </div>
    </transition>

  </el-form>

  <!-- 底部: 按钮组 -->
  <template #footer>
    <el-button @click="dialogVisible = false">取消</el-button>
    <el-button 
      :loading="loading"
      @click="handleCreateOnly"
    >
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
</el-dialog>
```

### 交互逻辑

```
┌─────────────────────────────────────────────────────────────────────┐
│                         用户交互流程                                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  打开对话框                                                           │
│       │                                                             │
│       ▼                                                             │
│  ┌────────────────┐    是                    否                    │
│  │ 用户上传文件？ │────────→ 解析文件 ──────→ 显示输入框            │
│  └────────────────┘         │                   │                  │
│       │                    │ (成功)            │ (跳过)           │
│       │                    ▼                   ▼                  │
│       │              显示解析结果      用户填基本信息                │
│       │                    │         (名称、类型、描述)            │
│       │                    │                   │                  │
│       │                    └────────┬──────────┘                  │
│       ▼                             │                              │
│  ┌─────────────────┐                ▼                              │
│  │ 用户点击按钮？   │         ┌────────────────┐                   │
│  ├─────────────────┤         │ [仅创建]       │ [导入并创建]      │
│  │                 │         │   或           │                   │
│  │                 │         │  [导入并创建]  │                   │
│  └─────────────────┘         └────────────────┘                   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 按钮行为

| 按钮 | 场景 | API 调用 | 说明 |
|------|------|----------|------|
| 仅创建 | 用户只想创建空数据集记录 | POST `/datasets` (JSON) | 不带文件，创建后可直接编辑 |
| 导入并创建 | 用户有数据文件需要导入 | POST `/datasets/import` (Form) | 带文件，导入到 PostGIS |

### 字段校验规则

```typescript
const rules = {
  name: [
    { required: true, message: '请输入数据集名称', trigger: 'blur' },
    { max: 100, message: '名称不能超过100个字符', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择数据类型', trigger: 'change' }
  ]
}
```

### 边缘情况处理

1. **用户上传文件后删除**: 解析结果清空，按钮回退到"仅创建"
2. **用户先填名称，再上传文件**: 如果名称为空，用文件名（去掉扩展名）自动填充
3. **上传失败**: 显示错误提示，保留上传框，用户可重试
4. **网络错误**: 按钮 loading 状态，超时提示
5. **类型切换**: 从"矢量"切到"影像"，清空已上传文件和解析结果

---

## 数据结构

```typescript
interface DatasetForm {
  name: string           // 数据集名称 (必填)
  type: 'vector' | 'raster'  // 数据类型 (必填)
  description?: string   // 描述 (可选)
  srs?: string           // 坐标系 (仅矢量且上传文件时)
}

interface FileUploadState {
  file: File | null
  parseResult: ParseResult | null
  parseError: string | null
}
```

---

## 后端 API 适配

### 现有 API（已实现）

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | `/api/v1/datasets` | 创建数据集（仅基本信息） |
| POST | `/api/v1/datasets/import` | 导入数据集（带文件） |
| POST | `/api/v1/datasets/parse` | 解析文件（可选，用于预览） |

### 改造建议

前端调用逻辑调整:

```typescript
// 仅创建
async function handleCreateOnly() {
  await formRef.value?.validate()
  await createDataset(form)
}

// 导入并创建  
async function handleImport() {
  await formRef.value?.validate()
  if (!uploadFile.value) {
    ElMessage.warning('请先上传文件')
    return
  }
  await importDataset(uploadFile.value, form.name, form.description, form.srs)
}
```

---

## 总结

推荐**方案 2 (单页表单式)**，原因：

1. **用户体验更好**: 用户可以同时看到所有需要填写的信息
2. **灵活性强**: 支持纯创建和导入创建两种模式
3. **学习成本低**: 不需要理解"步骤"的概念
4. **减少困惑**: 不再有两个 Tab 的割裂感
5. **减少错误**: 通过按钮文案明确用户的操作意图
