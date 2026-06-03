# Tasks: fix-dataset-create-layout

## Task List

### Task 1: Restructure dialog layout - remove tabs
- [x] **ID**: remove-tabs-structure
- **Description**: Remove el-tabs wrapper and flatten the form into single page
- **File**: `frontend/src/views/datasets/index.vue`
- **Dependencies**: None

**Implementation**:
1. Remove `<el-tabs v-model="activeTab">` and both `<el-tab-pane>` elements
2. Move form content directly under `<el-form>`
3. Remove `activeTab` ref if only used for tabs

---

### Task 2: Reorganize form fields layout
- [x] **ID**: reorganize-form-fields
- **Description**: Arrange name+type side by side, add divider before upload section
- **File**: `frontend/src/views/datasets/index.vue`
- **Dependencies**: remove-tabs-structure

**Implementation**:
1. Use `<el-row>` and `<el-col>` to put name (span=16) and type (span=8) side by side
2. Add `<el-divider>` with "📁 上传空间数据（可选）" text before upload section
3. Wrap file upload in `<div class="upload-section">`
4. Use `v-if="form.type === 'vector'"` to conditionally show upload section

---

### Task 3: Add type change handler
- [x] **ID**: add-type-change-handler
- **Description**: Handle type change to clear file upload when switching to raster
- **File**: `frontend/src/views/datasets/index.vue`
- **Dependencies**: reorganize-form-fields

**Implementation**:
1. Add `@change="handleTypeChange"` to type select
2. Create `handleTypeChange` method that clears upload file and parse result when type changes to raster

---

### Task 4: Update button group and logic
- [x] **ID**: update-button-group
- **Description**: Replace single import button with two buttons: "仅创建" and "导入并创建"
- **File**: `frontend/src/views/datasets/index.vue`
- **Dependencies**: reorganize-form-fields

**Implementation**:
1. Change footer buttons to:
   - Cancel button (unchanged)
   - "仅创建" button - calls `handleCreateOnly`
   - "导入并创建" button - calls `handleImport`, disabled when no file
2. "导入并创建" button should be `type="primary"` and have `:disabled="!uploadFile"`

---

### Task 5: Implement handleCreateOnly method
- [x] **ID**: implement-create-only
- **Description**: Create method that calls createDataset API without file
- **File**: `frontend/src/views/datasets/index.vue`
- **Dependencies**: update-button-group

**Implementation**:
Add method:
```typescript
const handleCreateOnly = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await createDataset(form)
      ElMessage.success('创建成功')
      dialogVisible.value = false
      fetchDatasets()
    } catch (error) {
      console.error('Create failed:', error)
    } finally {
      loading.value = false
    }
  })
}
```

---

### Task 6: Simplify handleImport method
- [x] **ID**: simplify-import-handler
- **Description**: Simplify existing handleImport to work with new UI
- **File**: `frontend/src/views/datasets/index.vue`
- **Dependencies**: update-button-group

**Implementation**:
1. Remove the branch for simple create (now handled by handleCreateOnly)
2. Keep only the file import logic
3. Remove edit mode handling (can reuse handleCreateOnly for that)

---

### Task 7: Test and verify
- [x] **ID**: test-and-verify
- **Description**: Build and verify the changes work correctly
- **Dependencies**: All above tasks

**Implementation**:
1. Run `npm run build` to verify no compilation errors ✓
2. Build successful - ready for runtime testing
