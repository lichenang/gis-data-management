# GeoServer 配置 (geoserver-config)

## 模块划分

| 模块 | 职责 | 文件 |
|------|------|------|
| 配置属性类 | 映射 application.yml 中的 geoserver 配置段 | `GeoServerProperties.java` |
| 服务层 | 瓦片包下载时使用 dataDir 读取 GWC 缓存目录 | `TilePackageServiceImpl.java` |
| 异常处理 | 统一拦截 dataDir 相关校验异常 | `GlobalExceptionHandler.java` |

## 数据流设计

```
TilePackageServiceImpl.packageTiles()
  → 获取 geoServerProperties.getDataDir()
    ├─ 若为 null 或空字符串 → 抛出 "GeoServer data_dir 未配置" 异常（在 ZipOutputStream 创建之前）
    └─ 若非空 → 检查目录存在性
        ├─ 目录不存在 → 抛出 "GeoServer data_dir 目录不存在" 异常
        └─ 目录存在 → 继续打包流程
  → 异常由 GlobalExceptionHandler 捕获 → 返回 JSON { code: 500, message: "...", data: null }
```

## 接口列表

本能力不新增 REST API 接口，仅涉及配置属性和内部校验逻辑。

## ADDED Requirements

### Requirement: data-dir 配置校验

系统 SHALL 在 `TilePackageServiceImpl.packageTiles()` 中对 `geoserver.data-dir` 进行两级校验：

1. **空值检测**：若 `dataDir` 为 null 或空字符串，抛出 `RuntimeException("GeoServer data_dir 未配置，请在 application.yml 或环境变量中设置 GEOSERVER_DATA_DIR")`
2. **目录存在性检测**：若 `dataDir` 非空但目录不存在，抛出 `RuntimeException("GeoServer data_dir 目录不存在: {dataDir}，请检查配置路径是否正确")`

#### Scenario: data-dir 未配置时返回明确错误
- **WHEN** `geoserver.data-dir` 为空字符串或 null
- **THEN** 抛出 RuntimeException，消息包含"未配置"关键字
- **AND** 全局异常处理器返回 `{ code: 500, message: "GeoServer data_dir 未配置...", data: null }`

#### Scenario: data-dir 配置了不存在的路径时返回明确错误
- **WHEN** `geoserver.data-dir` 非空但 `new File(dataDir).exists()` 返回 false
- **THEN** 抛出 RuntimeException，消息包含"目录不存在"和实际路径
- **AND** 全局异常处理器返回 `{ code: 500, message: "GeoServer data_dir 目录不存在: /some/path...", data: null }`

#### Scenario: data-dir 配置正确时正常打包
- **WHEN** `geoserver.data-dir` 非空且目录存在
- **THEN** `packageTiles()` 正常执行，打开 ZipOutputStream 并打包瓦片
