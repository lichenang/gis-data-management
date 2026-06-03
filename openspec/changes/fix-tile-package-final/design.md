## Context

GWC 默认对 ImageMosaic 图层使用 EPSG:900913 (Web Mercator) 网格集，其瓦片 Y 坐标计算公式为：

```
y = (1 - log(tan(lat) + 1/cos(lat)) / π) / 2 * 2^z
```

而之前的代码错误地使用了 EPSG:4326 (GlobalCRS84Geographic) 的线性公式：

```
y = (90 - lat) / 180 * 2^z
```

两种公式对同一纬度计算出不同的 Y 坐标。以 lat=22.5°, z=10 为例：
- EPSG:4326: y ≈ 384
- Web Mercator: y ≈ 437

由于坐标不匹配，`enumerateTileFiles()` 遍历的瓦片路径与 GWC 磁盘上的实际路径完全不同，导致返回空列表。

## Goals / Non-Goals

**Goals:**
- 修正 `tileY()` 使用 Web Mercator 公式以匹配 GWC 默认网格集
- 修正 GlobalExceptionHandler 的 response 重置逻辑，确保 JSON 错误响应不受已设置的 Content-Type 影响

**Non-Goals:**
- 不修改 GWC 目录路径拼接逻辑（GWC 使用 `workspace_layerName` 格式是正确的）
- 不修改 GeoServer seed 相关逻辑

## Decisions

### 决策 1：tileY 恢复 Web Mercator 公式

**方案**：将 `tileY()` 方法从 EPSG:4326 线性公式改回 Web Mercator 非线性公式：

```java
private double tileY(double lat, int z) {
    double latRad = Math.toRadians(lat);
    return (1 - Math.log(Math.tan(latRad) + 1 / Math.cos(latRad)) / Math.PI) / 2 * (1 << z);
}
```

**理由**：GWC 对 ImageMosaic 默认使用 EPSG:900913 网格集，这是 GeoServer 的长期默认行为。X 坐标公式 `(lon + 180) / 360 * 2^z` 在两种网格集中相同，无需修改。

### 决策 2：GlobalExceptionHandler 添加 response.reset()

**方案**：在 `handleRuntimeException` 和 `handleException` 方法中添加 `response.reset()` 和 `setContentType("application/json;charset=UTF-8")`：

```java
@ExceptionHandler(RuntimeException.class)
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public R<Void> handleRuntimeException(RuntimeException e, HttpServletRequest request, HttpServletResponse response) {
    try {
        response.reset();
        response.setContentType("application/json;charset=UTF-8");
    } catch (Exception ex) {
        logger.warn("Failed to reset response: {}", ex.getMessage());
    }
    return R.fail("系统繁忙，请稍后再试");
}
```

**理由**：当 Controller 在设置 `Content-Type: application/zip` 后抛出异常，全局异常处理器需要重置 response 以清除已设置的 headers 和状态，确保 JSON 错误响应能正确写入。

## Risks / Trade-offs

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 如果 GWC 配置使用了非默认网格集（如 EPSG:4326），tileY 公式仍会不匹配 | 需在 GWC 配置中确认使用的网格集 | 当前 GeoServer 默认使用 EPSG:900913，该方案已知可行 |
| response.reset() 在 response 已部分提交时无效 | 错误响应仍可能失败 | reset() 调用放在 try-catch 中，最坏情况与之前相同 |
