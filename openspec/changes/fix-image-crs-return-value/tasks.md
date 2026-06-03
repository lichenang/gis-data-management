# Tasks: fix-image-crs-return-value

## Task 1: 修改 ImageServiceImpl.getImageWmsInfo() CRS 返回值 ✓

**文件**: `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java`

修改 `getImageWmsInfo` 方法（约第 408-418 行）：

在转换成功和转换失败的分支中，都将 `info.setCrs("EPSG:4326")` 改为 `info.setCrs(sourceCrs != null ? sourceCrs : "EPSG:4326")`。

修改后的代码：

```java
if (transformedExtent != null) {
    info.setExtent(transformedExtent);
    info.setCrs(sourceCrs != null ? sourceCrs : "EPSG:4326");
    log.info("Successfully transformed extent to EPSG:4326: [{}, {}, {}, {}]",
            transformedExtent[0], transformedExtent[1], transformedExtent[2], transformedExtent[3]);
} else {
    info.setExtent(extent);
    info.setCrs(sourceCrs != null ? sourceCrs : "EPSG:4326");
    log.warn("Failed to transform extent for dataset {}, using original extent. sourceCrs={}",
            id, sourceCrs);
}
```

## Task 2: 验证编译 ✓

## Task 3: 手动测试 ✓

1. 重启后端应用
2. 调用 `GET /api/v1/images/{id}/wms-url`
3. 检查返回 JSON:
   - `crs` 应为原始 CRS（如 "EPSG:3857"），不是 "EPSG:4326"
   - `extent` 应为 EPSG:4326 坐标
4. 前端测试：打开地图，添加影像图层，确认影像正确显示
