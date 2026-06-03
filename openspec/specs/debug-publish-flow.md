# debug-publish-flow - 影像发布全链路诊断报告

## 问题现象

用户点击"发布"按钮后，地图查看页面提示"影像数据集未发布到 GeoServer"。

## 全链路分析

```
┌─────────────────────────────────────────────────────────────────┐
│                     发布全链路流程                               │
└─────────────────────────────────────────────────────────────────┘

  前端                    后端                      数据库
    │                       │                          │
    │  POST /images/{id}    │                          │
    │  /publish             │                          │
    ├──────────────────────>│                          │
    │                       │  publishImageDataset()   │
    │                       ├─────────────────────────>│
    │                       │  1. 创建 GeoServer 图层  │
    │                       │  2. 设置 wms_url         │
    │                       │  3. status='published'   │
    │                       │  4. 触发切片任务         │
    │                       │<─────────────────────────│
    │   200 OK              │                          │
    │<──────────────────────┤                          │
    │                       │                          │
    │                       │                          │
  地图页面                 │                          │
    │                       │                          │
    │  GET /images/published│                          │
    ├──────────────────────>│                          │
    │                       │  listPublishedImages()   │
    │                       │  WHERE type='raster'     │
    │                       │  AND status='published'  │
    │                       │<─────────────────────────│
    │   返回已发布列表       │                          │
    │<──────────────────────┤                          │
    │                       │                          │
    │  GET /images/{id}     │                          │
    │  /wms-url             │                          │
    ├──────────────────────>│                          │
    │                       │  getImageWmsInfo()       │
    │                       │  检查 wms_url 是否为空   │
    │                       │<─────────────────────────│
    │   ImageWmsInfo        │                          │
    │<──────────────────────┤                          │
```

## 断裂点诊断

### 🔴 断裂点 1: 前端缺少发布 API 函数

**位置**: `frontend/src/api/image.ts`

**问题**: 后端有 `POST /api/v1/images/{id}/publish` 接口，但前端 `image.ts` 没有对应的 API 调用函数。

```typescript
// 当前 image.ts 缺少:
export function publishImage(id: number) {
  return post<{ code: number; data: ImageDataset }>(`/images/${id}/publish`)
}

export function unpublishImage(id: number) {
  return del<{ code: number; data: ImageDataset }>(`/images/${id}/publish`)
}
```

**对比矢量数据集** (`frontend/src/api/dataset.ts`):
```typescript
export function publishDataset(id: number) {
  return put<{ code: number; data: Dataset }>(`/datasets/${id}/publish`)
}

export function unpublishDataset(id: number) {
  return put<{ code: number; data: Dataset }>(`/datasets/${id}/unpublish`)
}
```

### 🔴 断裂点 2: 前端页面缺少发布按钮

**位置**: `frontend/src/views/images/index.vue`

**问题**: `images/index.vue` 只有"上传影像"按钮，没有"发布"按钮。

对比 `datasets/index.vue` 有完整的"发布"/"取消发布"按钮。

### 🟡 断裂点 3: getImageWmsInfo 错误消息

**位置**: `backend/.../ImageServiceImpl.java:381-383`

```java
if (dataset.getWmsUrl() == null || dataset.getWmsUrl().isEmpty()) {
    throw new RuntimeException("影像数据集未发布到 GeoServer");
}
```

这个错误消息正确指出了问题，但根本原因是断裂点 1 和 2。

## 各层检查结果

### 1. 前端发布接口 ✅ 后端存在

```
ImageController.java:99-108
✅ POST /api/v1/images/{id}/publish
✅ DELETE /api/v1/images/{id}/publish (unpublish)
```

### 2. 前端 API 函数 ❌ 缺失

```
frontend/src/api/image.ts
❌ publishImage(id)  -- 不存在
❌ unpublishImage(id) -- 不存在
```

### 3. 数据库更新 ✅ 应正常

```java
// ImageServiceImpl.publishImageDataset() 第 291-297 行
dataset.setStatus("published");
dataset.setWmsUrl(layerService.getWmsUrl(workspace, layerName));
this.updateById(dataset);  // 事务提交
```

### 4. 已发布列表查询 ✅ 正确

```java
// ImageServiceImpl.listPublishedImages() 第 367-372 行
return this.list(new LambdaQueryWrapper<Dataset>()
    .eq(Dataset::getType, "raster")
    .eq(Dataset::getStatus, "published")
    .eq(Dataset::getDeleted, 0));
```

### 5. wmsUrl 检查 ✅ 正确

```java
// ImageServiceImpl.getImageWmsInfo() 第 381-383 行
if (dataset.getWmsUrl() == null || dataset.getWmsUrl().isEmpty()) {
    throw new RuntimeException("影像数据集未发布到 GeoServer");
}
```

## 结论

| 层级 | 组件 | 状态 |
|------|------|------|
| 后端 Controller | ImageController | ✅ 正常 |
| 后端 Service | ImageServiceImpl | ✅ 正常 |
| 前端 API | image.ts | ❌ 缺少 publishImage/unpublishImage |
| 前端页面 | images/index.vue | ❌ 缺少发布按钮 |

## 修复方案

### 方案 1: 添加前端 API 函数 (最小改动)

在 `frontend/src/api/image.ts` 添加:

```typescript
export function publishImage(id: number) {
  return post<{ code: number; data: ImageDataset }>(`/images/${id}/publish`)
}

export function unpublishImage(id: number) {
  return del<{ code: number; data: ImageDataset }>(`/images/${id}/publish`)
}
```

### 方案 2: 添加发布按钮 (完整功能)

参考 `datasets/index.vue` 的实现，在 `images/index.vue` 添加:
- 操作列添加"发布"/"取消发布"按钮
- 对应的事件处理函数调用 `publishImage()` / `unpublishImage()`

## 建议

1. **先实施方案 1**，快速补全 API
2. **再实施方案 2**，提供完整的 UI 操作入口
