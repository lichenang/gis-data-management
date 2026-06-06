## Context

当前 `datasets/index.vue` 中的源坐标系选择器在 `crsDetected=false` 时显示，但未设置必填验证。用户可以跳过选择直接提交导入请求。

## Goals / Non-Goals

**Goals:**
- 当 `crsDetected=false` 时，强制用户选择源坐标系
- 未选择源坐标系时，禁用"导入并创建"按钮

**Non-Goals:**
- 不修改后端验证逻辑
- 不修改 crsDetected 的检测逻辑

## Decisions

### Decision 1: 使用动态必填验证

**方案**：通过 `el-select` 的 `rules` 属性动态设置验证规则。

```typescript
const sourceSrsRules = computed(() => {
  if (!missingPrj.value) return []
  return [{ required: true, message: '请选择源坐标系', trigger: 'change' }]
})
```

### Decision 2: 导入按钮禁用条件

**方案**：导入按钮增加 `disabled` 状态，当缺少 .prj 且未选择源坐标系时禁用。

```html
<el-button
  type="primary"
  :loading="importLoading"
  :disabled="!uploadFile || (missingPrj && !form.sourceSrs)"
  @click="handleImport"
>
  导入并创建
</el-button>
```

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|---------|
| 用户体验过于严格 | 仅在 crsDetected=false 时要求必填 |
