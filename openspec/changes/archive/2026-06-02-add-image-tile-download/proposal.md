## Why

原始影像下载（GeoTIFF）已实现，但用户需要下载已缓存的 PNG 切片包用于离线使用或第三方系统集成。当前缺少端到端的切片包打包下载能力，用户无法便捷获取 GeoWebCache 缓存的瓦片数据。

## What Changes

- 后端 `GeoServerProperties` 新增 `dataDir` 配置项，指向 GeoServer data_dir 路径
- 后端新增 `POST /api/v1/images/{id}/tile-package` 接口，接受 zoom 范围参数，流式返回 ZIP 包
- 后端实现 `TilePackageService`，通过 `ZipOutputStream` 流式打包 GWC 瓦片目录，含瓦片数量估算与硬上限校验（默认 100,000 片）
- 前端 `api/image.ts` 新增 `downloadTilePackage()` 函数
- 前端 `views/images/index.vue` 操作列新增"下载切片包"按钮，支持 zoom 范围选择对话框
- 新增 `application.yml` 配置项 `tile-package.max-tiles` 和 `tile-package.default-zoom-stop`

## Capabilities

### New Capabilities
- `tile-package-download`: 影像切片包流式打包与下载，支持 zoom 范围选择、瓦片数量上限校验

### Modified Capabilities
- （无）

## Non-goals

- 不实现 WMTS HTTP 代理瓦片降级方案（策略 B）
- 不实现异步 ZIP 生成 + 预签名 URL 下载
- 不实现断点续传
- 不实现并发请求限制

## Impact

| 文件 | 操作 | 说明 |
|------|------|------|
| `backend/.../config/GeoServerProperties.java` | 修改 | 新增 `dataDir` 字段 |
| `backend/.../dto/TilePackageRequest.java` | 新增 | 请求体 DTO |
| `backend/.../controller/TilePackageController.java` | 新增 | POST 接口控制器 |
| `backend/.../service/TilePackageService.java` | 新增 | 接口定义 |
| `backend/.../service/impl/TilePackageServiceImpl.java` | 新增 | 核心实现（打包 + 校验） |
| `backend/src/main/resources/application.yml` | 修改 | 新增 tile-package 配置段 |
| `frontend/src/api/image.ts` | 修改 | 新增 `downloadTilePackage()` |
| `frontend/src/views/images/index.vue` | 修改 | 新增按钮、对话框、事件处理 |
