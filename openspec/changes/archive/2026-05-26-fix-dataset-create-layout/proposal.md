# Proposal: fix-dataset-create-layout

## Problem Statement

当前数据集创建页面将"基本信息"和"文件上传"拆成两个独立 Tab，各自包含独立的表单和按钮。这种设计导致以下问题：

1. 用户在"基本信息"填完后点击导入 → 可能忘记上传文件
2. 用户上传文件后自动解析成功 → 但可能没填名称
3. 两个 Tab 是平行关系，没有引导关系
4. 用户不知道应该先做什么、后做什么
5. Tab 切换时表单状态可能丢失

## Why This Matters

- **可用性**: 当前设计导致新用户困惑，不知道如何完成数据集创建
- **错误率高**: 用户经常遗漏必要步骤或信息
- **功能未充分利用**: 文件上传功能的存在感太弱

## Proposed Solution

采用**单页表单式**设计（方案2），将信息填写和文件上传合并到同一个页面：

1. **单页合一**: 所有字段平铺展示，不需要 Tab 切换
2. **文件上传可选**: 不上传文件也可以创建空数据集
3. **按钮区分意图**:
   - "仅创建" = 不带文件，仅创建数据库记录
   - "导入并创建" = 带文件，创建记录并导入数据到 PostGIS
4. **智能填充**: 上传文件后，如名称未填则用文件名自动填充

## Scope

- 前端页面：`frontend/src/views/datasets/index.vue`
- 无需后端改动（现有 API 已满足需求）
- 无需数据库变更

## Risk Assessment

- **Risk Level**: Low
- **Impact**: 仅修改前端页面，不影响后端
- **Rollback**: Easy - revert the file

## Dependencies

- 使用现有 API：POST `/datasets` 和 POST `/datasets/import`
- 需要读取 `openspec/specs/dataset-create-redesign.md` 作为设计参考
