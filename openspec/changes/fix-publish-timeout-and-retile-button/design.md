# 设计: fix-publish-timeout-and-retile-button

## 问题 1: 发布接口超时

### 当前问题

`frontend/src/api/request.ts` 中的 Axios 实例配置了 30 秒超时：

```typescript
const service: AxiosInstance = axios.create({
  baseURL,
  timeout: 30000,  // 30 秒超时
  headers: {
    'Content-Type': 'application/json'
  }
})
```

当用户发布大文件或重新切片时，后端处理时间可能超过 30 秒，导致请求超时失败。

### 修改方案

将 `timeout` 从 `30000` 改为 `120000`（120 秒）：

```typescript
const service: AxiosInstance = axios.create({
  baseURL,
  timeout: 120000,  // 120 秒超时
  headers: {
    'Content-Type': 'application/json'
  }
})
```

## 问题 2: 重新切片按钮消失

### 当前问题

`images/index.vue` 中重新切片按钮的显示条件只有 `tileStatus === 'completed'`：

```vue
<template v-if="row.tileStatus === 'completed'">
  <el-tag type="success">已完成 ({{ row.tileProgress }}%)</el-tag>
  <el-button
    type="warning"
    link
    size="small"
    style="margin-left: 4px"
    @click="handleRetile(row)"
  >
    重新切片
  </el-button>
</template>
```

当 `cacheSeedStatus === 'seeded'` 但 `tileStatus` 不是 `'completed'` 时，按钮不会显示。

### 修改方案

修改显示条件，增加 `cacheSeedStatus === 'seeded'` 的判断：

```vue
<template v-if="row.tileStatus === 'completed' || row.cacheSeedStatus === 'seeded'">
  <el-tag type="success">已完成 ({{ row.tileProgress }}%)</el-tag>
  <el-button
    type="warning"
    link
    size="small"
    style="margin-left: 4px"
    @click="handleRetile(row)"
  >
    重新切片
  </el-button>
</template>
```

这样在以下情况都会显示重新切片按钮：
- `tileStatus === 'completed'`
- `cacheSeedStatus === 'seeded'`

## 验证步骤

1. 修改超时配置后，发布大文件影像，确认不会超时
2. 当 `cacheSeedStatus === 'seeded'` 时，确认重新切片按钮正确显示
3. 点击重新切片按钮，确认功能正常工作
