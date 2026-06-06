## Context

当 Shapefile 缺少 .prj 文件时，GeoTools 返回的 nativeCrs 可能为 null 或 EPSG 代码为 0。当前代码在 importUsingDataStore 中有以下逻辑：

```java
CoordinateReferenceSystem nativeCrs = schema.getGeometryDescriptor().getCoordinateReferenceSystem();
int nativeSrid = CrsTransformUtil.getEpsgCode(nativeCrs);
String nativeCrsName = nativeCrs != null ? nativeCrs.getName().toString() : "unknown";

int targetEpsg = parseTargetSrs(targetSrs);

int sourceSridForTransform;
if (nativeSrid > 0) {
    sourceSridForTransform = nativeSrid;
} else {
    if (targetEpsg > 0) {
        sourceSridForTransform = targetEpsg;  // 问题: targetSrs 是目标坐标系，不是源坐标系
    } else {
        throw new IllegalStateException(...);
    }
}
```

问题在于：当 nativeSrid == 0 时，代码错误地使用 targetSrs（目标坐标系，如 4326）作为源坐标系，而不是用户指定的源坐标系。

## Goals / Non-Goals

**Goals:**
- 当 nativeCrs 无法识别时，使用用户指定的 sourceSrs 参数作为源坐标系
- 在前端上传对话框中添加提示，告知用户手动指定投影

**Non-Goals:**
- 不修改数据库存储坐标系（仍然是 EPSG:4326）
- 不修改 GeoJSON 导入逻辑

## Decisions

### Decision 1: 新增 sourceSrs 参数

**方案**：在 importUsingDataStore 方法中新增 sourceSrs 参数，当 nativeCrs 无法识别时使用它。

```java
private int importUsingDataStore(Connection conn, MultipartFile file, String fileName,
                                  VectorFileFormat format, String tableName, String datasetName,
                                  String targetSrs, String sourceSrs) throws Exception {
```

### Decision 2: 分层处理坐标系识别

**方案**：

1. 首先尝试从 nativeCrs 获取 EPSG 代码
2. 如果获取失败（nativeSrid == 0），使用用户指定的 sourceSrs
3. 如果 sourceSrs 也没有指定，抛出明确异常

### Decision 3: 前端提示

**方案**：在上传表单的坐标系选择区域添加提示文字：
- 如果 Shapefile 有 .prj 文件：显示"将自动识别"
- 如果 Shapefile 缺少 .prj 文件：提示"请手动选择原始投影"

## 实现方案

### 后端修改

1. 修改 `MultiFormatImportServiceImpl.importUsingDataStore()` 签名，添加 `sourceSrs` 参数
2. 修改 `MultiFormatImportServiceImpl.importToPostGIS()` 调用点，传入 `sourceSrs` 参数
3. 当 `nativeSrid == 0` 且 `sourceSrs` 有效时，使用 `sourceSrs` 作为源坐标系

### 前端修改

在上传对话框的坐标系选择区域添加条件提示：
- 当用户选择了 Shapefile 且检测到缺少 .prj 文件时，显示警告提示

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|---------|
| 用户错误指定 sourceSrs | 在提示中说明如何查看 Shapefile 的坐标系信息 |
| sourceSrs 与 targetSrs 相同 | 跳过 ST_Transform 转换 |
