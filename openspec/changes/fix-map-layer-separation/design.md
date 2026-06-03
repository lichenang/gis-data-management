# Design: fix-map-layer-separation

## Technical Design

### Bug 1: DatasetServiceImpl.listPublishedDatasets()

**当前代码**:
```java
@Override
public List<Dataset> listPublishedDatasets() {
    return this.list(new LambdaQueryWrapper<Dataset>()
            .eq(Dataset::getStatus, "published")
            .eq(Dataset::getDeleted, 0));
}
```

**问题**: 没有过滤 `type` 字段，导致 `type='raster'` 的影像数据集也返回。

**修复**:
```java
@Override
public List<Dataset> listPublishedDatasets() {
    return this.list(new LambdaQueryWrapper<Dataset>()
            .eq(Dataset::getStatus, "published")
            .eq(Dataset::getDeleted, 0)
            .eq(Dataset::getType, "vector"));
}
```

### Bug 2: ImageServiceImpl.getImageWmsInfo()

**当前代码**:
```java
@Override
public ImageWmsInfo getImageWmsInfo(Long id) {
    Dataset dataset = this.getById(id);
    if (dataset == null || dataset.getDeleted() == 1) {
        throw new RuntimeException("影像数据集不存在");
    }

    ImageWmsInfo info = new ImageWmsInfo();
    info.setLayerName("raster_" + id);
    info.setCrs(dataset.getSrs() != null ? dataset.getSrs() : "EPSG:4326");
    info.setOpacity(0.8);
    info.setWmsUrl(geoServerProperties.getUrl() + "/" + geoServerProperties.getWorkspace() + "/wms");

    return info;
}
```

**问题**: WMS URL 拼接缺少图层名。正确的 URL 应为 `{geoserverUrl}/{workspace}/raster_{id}/wms`。

**修复**:
```java
@Override
public ImageWmsInfo getImageWmsInfo(Long id) {
    Dataset dataset = this.getById(id);
    if (dataset == null || dataset.getDeleted() == 1) {
        throw new RuntimeException("影像数据集不存在");
    }

    ImageWmsInfo info = new ImageWmsInfo();
    info.setLayerName("raster_" + id);
    info.setCrs(dataset.getSrs() != null ? dataset.getSrs() : "EPSG:4326");
    info.setOpacity(0.8);
    info.setWmsUrl(dataset.getWmsUrl()); // Use stored WMS URL

    return info;
}
```

## Data Flow After Fix

```
┌─────────────────────────────────────────────────────────────────────┐
│                    VECTOR LAYER LIST                                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  DatasetService.listPublishedDatasets()                             │
│            │                                                        │
│            ▼                                                        │
│  .eq(status, "published")                                           │
│  .eq(deleted, 0)                                                    │
│  .eq(type, "vector")  ◄─── NEW FILTER                              │
│            │                                                        │
│            ▼                                                        │
│      Returns only vector datasets                                   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                    IMAGE LAYER LIST                                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ImageService.listPublishedImages()                                 │
│            │                                                        │
│            ▼                                                        │
│  .eq(type, "raster")                                                │
│  .eq(status, "published")                                           │
│  .eq(deleted, 0)                                                    │
│            │                                                        │
│            ▼                                                        │
│  For each image dataset:                                            │
│    getImageWmsInfo()                                                │
│            │                                                        │
│            ▼                                                        │
│  dataset.getWmsUrl() ──► WMS URL with layer name                   │
│                                                                     │
│  e.g. http://localhost:8080/geoserver/gisplatform/raster_123/wms   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```
