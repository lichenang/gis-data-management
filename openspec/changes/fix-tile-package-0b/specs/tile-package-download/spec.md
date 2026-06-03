# 瓦片包下载 (tile-package-download)

## 模块划分

| 模块 | 职责 | 文件 |
|------|------|------|
| 控制器层 | 接收下载请求，设置响应头，调用服务层 | `TilePackageController.java` |
| 服务层 | 核心打包逻辑：解析范围、计算瓦片坐标、读取 GWC 磁盘缓存、写入 ZIP | `TilePackageServiceImpl.java` |
| 异常处理 | 统一拦截服务层异常，返回标准 JSON 错误响应 | `GlobalExceptionHandler.java` |
| CRS 工具 | 将 dataset.extent 从源 CRS 转换到 EPSG:4326 | `CrsTransformUtil.java` |

## 数据流设计

```
Client → POST /api/v1/images/{id}/tile-package
  → TilePackageController
    → 设置 response headers (Content-Type: application/zip)
    → 调用 tilePackageService.packageTiles()
      → Dataset 前置校验
      → parseBounds(request, dataset)
        ├─ 请求中 bounds 存在 → 直接使用（假设 EPSG:4326）
        └─ 从 dataset.extent 读取 → CrsTransformUtil → EPSG:4326
      → getTileRange() 计算瓦片范围
        ├─ X: (lon + 180) / 360 * 2^z
        └─ Y: (90 - lat) / 180 * 2^z   (EPSG:4326 线性公式)
      → 三层循环读取 GWC 磁盘文件
      → ZipOutputStream 流式写入
  ← ZIP 文件 / JSON 错误响应
```

## 接口列表

### POST /api/v1/images/{id}/tile-package

**请求参数**:
| 参数 | 位置 | 类型 | 必需 | 说明 |
|------|------|------|------|------|
| id | Path | Long | 是 | 影像数据集 ID |
| zoomStart | Body | Integer | 否 | 起始缩放级别（默认 0） |
| zoomStop | Body | Integer | 否 | 结束缩放级别（默认 14） |
| bounds | Body | Bounds | 否 | 地理范围 { minX, minY, maxX, maxY } |

**成功响应**: `application/zip` 二进制流

**错误响应**: `application/json`
```json
{ "code": 500, "message": "错误描述", "data": null }
```

**触发异常的校验**:
- 数据集不存在或已删除
- 数据集类型非 raster
- 数据集未发布
- 切片未完成（cacheSeedStatus != "seeded"）
- GeoServer data_dir 未配置
- 缩放级别超出范围
- 瓦片数量超出限制
- GWC 缓存目录不存在
- 没有匹配的瓦片文件（totalWritten == 0）

## ADDED Requirements

### Requirement: 错误响应不使用 Servlet Writer 流

当下载过程中抛出异常时，系统 SHALL 不调用 `response.getWriter()`，而是将异常抛出给全局异常处理器处理，避免 `getOutputStream()`/`getWriter()` 冲突。

#### Scenario: 服务层异常时返回 JSON 错误
- **WHEN** `packageTiles()` 抛出 RuntimeException（如数据集不存在、目录不存在等）
- **THEN** Controller 不捕获该异常，由 `GlobalExceptionHandler` 返回 `{ code: 500, message: "...", data: null }`
- **AND** 响应 Content-Type 为 `application/json;charset=UTF-8`

### Requirement: 从 dataset 解析 bounds 时需做 CRS 转换

当从 `dataset.extent` 解析地理范围时，系统 SHALL 使用 `dataset.srs` 指定的源 CRS，通过 `CrsTransformUtil.transformExtentToWgs84()` 转换到 EPSG:4326。若 `dataset.srs` 为 null 或为空，系统 SHALL 直接使用原始 extent 值。若 `dataset.srs` 已为 EPSG:4326，系统 SHALL 跳过转换直接使用。

#### Scenario: 源 CRS 不是 EPSG:4326 时转换成功
- **WHEN** `dataset.extent` = `{"minX":200000,"minY":3000000,"maxX":300000,"maxY":3100000}`, `dataset.srs` = `"EPSG:32650"`
- **THEN** parseBounds 返回的 Bounds 为转换后的 EPSG:4326 经纬度值（如经 CrsTransformUtil 转换）

#### Scenario: 源 CRS 为 null 时直接使用原始 extent
- **WHEN** `dataset.srs` 为 null
- **THEN** parseBounds 直接使用 `dataset.extent` 的原始值，并输出 WARN 日志

#### Scenario: 源 CRS 为 EPSG:4326 时跳过转换
- **WHEN** `dataset.srs` = `"EPSG:4326"`
- **THEN** parseBounds 直接使用 `dataset.extent` 的原始值，不做 CRS 转换

#### Scenario: 请求体中提供 bounds 时优先级最高
- **WHEN** `request.bounds` 包含有效的 minX/minY/maxX/maxY
- **THEN** parseBounds 直接返回请求中的 bounds，不读取 `dataset.extent`

### Requirement: 瓦片 Y 坐标使用 EPSG:4326 线性公式

瓦片包下载时，系统 SHALL 使用 EPSG:4326 网格集（GlobalCRS84Geographic）的瓦片 Y 坐标计算公式：`tileY = (90 - lat) / 180 * 2^z`。

#### Scenario: 相同纬度下 Y 坐标正确
- **WHEN** `lat = 22.5`, `z = 10`
- **THEN** `tileY = (90 - 22.5) / 180 * 1024 = 384.0`

#### Scenario: 纬度范围对应正确的行范围
- **WHEN** `bounds` = `[114, 22, 116, 24]`, `z = 10`
- **THEN** yMin = `tileY(24, 10)` = `(90 - 24) / 180 * 1024 = 375`, yMax = `tileY(22, 10)` = `(90 - 22) / 180 * 1024 = 386`
