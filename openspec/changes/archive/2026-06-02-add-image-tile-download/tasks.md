## 1. 配置层

- [x] 1.1 `GeoServerProperties.java` 新增 `dataDir` 字段（`private String dataDir`）并添加 Javadoc
- [x] 1.2 `application.yml` 新增 `tile-package` 配置段：`max-tiles: 100000` 和 `default-zoom-stop: 14`，在 `geoserver` 段下新增 `data-dir` 配置项

## 2. DTO

- [x] 2.1 新建 `dto/TilePackageRequest.java`，包含 `zoomStart`（Integer）、`zoomStop`（Integer）、`bounds`（内嵌类 `Bounds` 含 minX/minY/maxX/maxY）

## 3. 业务层

- [x] 3.1 新建 `service/TilePackageService.java` 接口，定义 `void packageTiles(Long datasetId, TilePackageRequest request, OutputStream outputStream)` 方法
- [x] 3.2 新建 `service/impl/TilePackageServiceImpl.java`，实现瓦片数量估算方法 `estimateTileCount(zoomStart, zoomStop, bounds)`
- [x] 3.3 `TilePackageServiceImpl` 实现核心打包逻辑：遍历 zoom/x/y 目录、写入 `ZipOutputStream`，单个瓦片通过 `FileInputStream.transferTo()` 流式写入

## 4. 控制层

- [x] 4.1 新建 `controller/TilePackageController.java`，实现 `POST /api/v1/images/{id}/tile-package`，接收 `@RequestBody TilePackageRequest`，直接通过 `HttpServletResponse.getOutputStream()` 流式返回 ZIP

## 5. 前端 API

- [x] 5.1 `api/image.ts` 新增 `TilePackageRequest` 接口和 `downloadTilePackage(id, params)` 函数，使用 `post` 并设置 `responseType: 'blob'`

## 6. 前端视图

- [x] 6.1 `views/images/index.vue` 操作列新增"下载切片包"按钮（在"下载原始影像"旁），仅 `status === 'published'` 时显示
- [x] 6.2 新增 zoom 范围选择对话框组件，包含 zoomStart/zoomStop 输入框和预估瓦片数提示
- [x] 6.3 新增 `handleDownloadTiles` 方法：调用 `downloadTilePackage`，通过 Blob URL 触发浏览器下载
- [x] 6.4 `views/images/index.vue` `<script setup>` 引入 `downloadTilePackage` 和 `TilePackageRequest`
