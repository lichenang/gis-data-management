# 瓦片包下载 (tile-package-download)

## 模块划分

| 模块 | 职责 | 文件 |
|------|------|------|
| 控制器层 | 接收下载请求，设置响应头，调用服务层（不捕获异常） | `TilePackageController.java` |
| 服务层 | 核心打包逻辑：解析范围、计算瓦片坐标、预检查瓦片、读取 GWC 磁盘缓存、写入 ZIP | `TilePackageServiceImpl.java` |
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
      → enumerateTileFiles() 预枚举瓦片文件
        ├─ 列表为空 → 提前抛出异常（response 未写入）
        └─ 列表非空 → 遍历列表写入 ZIP
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
- GWC 缓存目录不存在或路径格式不正确（应为 `{workspace}_{layerName}` 格式，如 `gisplatform_raster_38`）
- 没有匹配的瓦片文件（预检查无瓦片）

## ADDED Requirements

### Requirement: 空瓦片预检查在输出流打开之前

系统 SHALL 在创建 `ZipOutputStream` 之前预枚举匹配的瓦片文件。若枚举结果为空，系统 SHALL 直接抛出 `RuntimeException`，不打开输出流。此机制确保异常发生时 response 仍处于全新未写入状态，全局异常处理器可正常写入 JSON 错误响应。

#### Scenario: 预检查无瓦片时提前抛异常
- **WHEN** `enumerateTileFiles()` 返回空列表
- **THEN** 在 `ZipOutputStream` 创建之前抛出 `RuntimeException("该影像尚未生成切片缓存...")`
- **AND** 全局异常处理器返回 `{ code: 500, message: "该影像尚未生成切片缓存...", data: null }`

#### Scenario: 前置数据集校验异常
- **WHEN** 数据集不存在、未发布、目录不存在等
- **THEN** Controller 不捕获异常，由 `GlobalExceptionHandler` 返回 `{ code: 500, message: "...", data: null }`

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

### Requirement: 瓦片 Y 坐标使用 Web Mercator 公式

瓦片包下载时，系统 SHALL 使用 EPSG:900913 (Web Mercator) 网格集的瓦片 Y 坐标计算公式：`tileY = (1 - log(tan(lat) + 1/cos(lat)) / π) / 2 * 2^z`，以匹配 GWC 默认网格集。

#### Scenario: Web Mercator Y 坐标正确
- **WHEN** `lat = 22.5`, `z = 10`
- **THEN** `tileY ≈ 437`（与 GWC EPSG:900913 网格集存储瓦片路径匹配）

#### Scenario: 纬度范围对应正确的行范围
- **WHEN** `bounds` = `[114, 22, 116, 24]`, `z = 10`
- **THEN** yMin = `tileY(24, 10) ≈ 437`, yMax = `tileY(22, 10) ≈ 453`

### Requirement: 异常响应不受 Content-Type 冲突影响

当下载过程中抛出异常时，系统 SHALL 在写入 JSON 错误响应前重置 response 的 headers 和 Content-Type，确保错误响应能正确返回。

#### Scenario: GlobalExceptionHandler 重置 Response
- **WHEN** `packageTiles()` 抛出 RuntimeException，Controller 已设置 `Content-Type: application/zip`
- **THEN** GlobalExceptionHandler 调用 `response.reset()` 和 `setContentType("application/json;charset=UTF-8")`
- **AND** 返回 `{ code: 500, message: "...", data: null }`
