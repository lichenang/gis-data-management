## Context

当前 `application.yml` 中 `geoserver.data-dir` 的默认值为 `D:\Program Files\geoserver-2.28.3-bin\data_dir`，这是一个 Windows 特定路径。在 Linux/macOS 环境中，即使未配置此值，Spring Boot 也会使用这个无效的默认值，导致 `TilePackageServiceImpl` 的 `dataDir` 校验认为已配置（因为非 null），但后续的 GWC 目录检查会因目录不存在而失败。错误消息"切片缓存目录不存在"不足以让用户意识到根本原因是 `data_dir` 未正确配置。

## Goals / Non-Goals

**Goals:**
- 修复 `data-dir` 默认值为空字符串，消除 Windows 特定路径
- 改进错误消息，明确区分"未配置"和"目录不存在"两种情况
- 确保所有 dataDir 相关校验在 `ZipOutputStream` 打开之前完成

**Non-Goals:**
- 不修改 GlobalExceptionHandler 的行为
- 不改变 API 接口规范
- 不添加新配置项（仅修改默认值和行为）

## Decisions

### 决策 1：data-dir 默认值改为空字符串

**方案**：将 `application.yml` 中 `data-dir` 的默认值从 `D:\Program Files\geoserver-2.28.3-bin\data_dir` 改为空字符串。

```yaml
data-dir: ${GEOSERVER_DATA_DIR:}
```

**理由**：空字符串意味着"未配置"，触发明确的"未配置"错误消息，而非误导性的"目录不存在"错误。在生产环境中应通过环境变量 `GEOSERVER_DATA_DIR` 显式配置。

### 决策 2：改进 TilePackageServiceImpl 中的 dataDir 校验

**方案**：重构 `dataDir` 校验逻辑，分两步检测：

1. **第一步**：检查 `dataDir` 是否为空（`null` 或 `isEmpty()`），若为空则抛出 `"GeoServer data_dir 未配置，请在 application.yml 或环境变量中设置 GEOSERVER_DATA_DIR"`
2. **第二步**：若非空，检查目录是否存在，不存在则抛出 `"GeoServer data_dir 目录不存在: {dataDir}，请检查配置路径是否正确"`

两步均在 `ZipOutputStream` 创建之前执行，确保异常被 GlobalExceptionHandler 正确捕获。

### 决策 3：GeoServerProperties.dataDir 增加 @Builder.Default

**方案**：在 `GeoServerProperties.dataDir` 字段上添加 Lombok 的 `@Builder.Default` 注解，确保在反序列化时该字段有明确定义默认值。

```java
@Builder.Default
private String dataDir = "";
```

**理由**：Lombok 的 `@Data` 与 `@Builder` 共用时，若未显式设置，builder 会将字段设为 `null`。`@Builder.Default` 确保即使 builder 未设置该字段，也使用指定的默认值。

## Risks / Trade-offs

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 现有部署若依赖默认值路径会失效 | Windows 环境下使用内置 GeoServer 的用户可能受影响 | 这些用户原本就能工作，只是路径无效；改为空后能收到明确错误提示 |
| 环境变量 GEOSERVER_DATA_DIR 未设置时行为变更 | 原本使用 Windows 默认路径的用户会看到"未配置"错误 | 需要用户显式配置正确的 data_dir 路径 |
