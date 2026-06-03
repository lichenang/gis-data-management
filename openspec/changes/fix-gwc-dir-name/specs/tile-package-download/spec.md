# 瓦片包下载 (tile-package-download) — 变更 Delta

此 Delta 反映 `fix-gwc-dir-name` 变更对现有规格的修改。

## UPDATED Interface List

**触发异常的校验**（更新项标 *）:
- 数据集不存在或已删除
- 数据集类型非 raster
- 数据集未发布
- 切片未完成（cacheSeedStatus != "seeded"）
- *GeoServer data_dir 未配置或目录不存在
- 缩放级别超出范围
- 瓦片数量超出限制
- *GWC 缓存目录路径不正确（workspace_layerName 格式）
- 没有匹配的瓦片文件（预检查无瓦片）
