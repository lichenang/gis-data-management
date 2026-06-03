## Context

当前系统已有 raster_metadata 和 dataset 表存储影像元数据，但缺少一个方便用户导出这些信息的入口。用户需要能够下载影像的元数据为 JSON 文件。

## Goals / Non-Goals

**Goals:**
- 新增 `/api/v1/images/{id}/metadata` 接口，返回影像元数据 JSON
- 前端添加下载元数据按钮，触发浏览器下载
- 复用现有认证机制，需要登录后才能下载

**Non-Goals:**
- 不支持批量下载
- 不修改已有接口

## Decisions

### Decision 1: API 设计

**接口**: `GET /api/v1/images/{id}/metadata`

**响应格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "datasetId": 38,
    "name": "影像名称",
    "type": "raster",
    "fileName": "original.tif",
    "fileSize": 15728640,
    "width": 1024,
    "height": 2048,
    "bands": 3,
    "pixelType": "Float32",
    "crs": "EPSG:4326",
    "extent": {
      "minX": 108.0,
      "minY": 33.0,
      "maxX": 110.0,
      "maxY": 35.0
    },
    "createTime": "2024-01-15T10:30:00"
  }
}
```

### Decision 2: 前端下载实现

使用 Axios 请求获取 Blob，设置 responseType: 'blob'，然后通过以下方式触发下载：
```typescript
const blob = new Blob([data], { type: 'application/json' })
const url = URL.createObjectURL(blob)
const link = document.createElement('a')
link.href = url
link.download = `metadata-${id}.json`
link.click()
URL.revokeObjectURL(url)
```

## Risks / Trade-offs

- **[低] 字符编码** — 确保返回 JSON 使用 UTF-8 编码
- **[低] 大文件** — 元数据 JSON 通常很小，风险可忽略

## Migration Plan

1. 后端新增接口和 Service 方法
2. 前端添加下载按钮和 API 调用
3. 测试下载功能

无数据库迁移，需要回滚时删除新增代码即可。

## Open Questions

- 是否需要返回 transform 字段（GeoTIFF 变换矩阵）？
