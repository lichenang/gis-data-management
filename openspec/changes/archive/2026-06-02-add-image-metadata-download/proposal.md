## Why

用户需要导出影像数据集的元数据信息，便于离线查看、分享或在 GIS 软件中加载。当前系统没有提供专门的元数据下载入口，用户只能通过 API 或数据库查询获取，效率低下。

## What Changes

1. **后端新增接口** — GET `/api/v1/images/{id}/metadata`
   - 从 `raster_metadata` 和 `dataset` 表查询元数据
   - 返回字段：文件名、文件大小、坐标系、分辨率、波段数、像素类型、四至范围 (minX, minY, maxX, maxY)、上传时间等
   - 返回 JSON 格式，前端可直接触发浏览器下载为 .json 文件

2. **前端新增下载按钮** — 在 images/index.vue 的影像列表操作栏添加"下载元数据"按钮
   - 调用上述接口
   - 使用 Blob 和 URL.createObjectURL 触发浏览器下载

## Capabilities

### New Capabilities
- **image-metadata-download**: 影像元数据下载功能

### Modified Capabilities
（无）

## Impact

- `backend/.../controller/ImageController.java` — 新增 metadata 接口
- `backend/.../service/ImageService.java` — 新增 getMetadata 方法
- `frontend/src/views/images/index.vue` — 添加下载按钮
- 不涉及数据库结构变更

## Non-goals

- 不修改已有接口的返回格式
- 不添加新的权限控制（复用现有认证）
- 不支持批量下载

## Affected Files

- `backend/src/main/java/com/gisplatform/controller/ImageController.java`
- `backend/src/main/java/com/gisplatform/service/ImageService.java`
- `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java`
- `frontend/src/views/images/index.vue`
