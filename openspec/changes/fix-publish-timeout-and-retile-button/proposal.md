# fix-publish-timeout-and-retile-button

## Why

影像管理页面存在两个问题：

1. **发布接口超时** - 用户点击"发布"或"重新切片"时，后端处理时间较长（需要生成切片），当前 Axios 超时时间为 30 秒，导致大文件发布时经常超时失败
2. **重新切片按钮消失** - 当 `cacheSeedStatus === 'seeded'` 时，`tileStatus` 可能不是 `'completed'`，导致操作列的"重新切片"按钮不显示，用户无法再次触发切片

## What Changes

### 1. 增加发布和重新切片接口的超时时间

在 `frontend/src/api/request.ts` 中：
- 将默认超时时间从 30 秒增加到 120 秒
- 确保发布和重新切片操作有足够的完成时间

### 2. 修复重新切片按钮显示条件

在 `frontend/src/views/images/index.vue` 中：
- 将 `v-if="row.tileStatus === 'completed'"` 改为 `v-if="row.tileStatus === 'completed' || row.cacheSeedStatus === 'seeded'"`
- 确保在两种状态下都能显示重新切片按钮

## Capabilities

### Fixed Capabilities
- 发布接口支持长时间处理
- 重新切片按钮在切片完成后和缓存种子化后都能显示

## Impact

- 修改文件：
  - `frontend/src/api/request.ts`
  - `frontend/src/views/images/index.vue`

## Non-goals

- 不修改后端处理逻辑
- 不修改其他接口的超时配置
