# Tasks: fix-map-layer-separation

## Backend Tasks

### Task 1: 修复 DatasetServiceImpl.listPublishedDatasets() 添加矢量类型过滤

**File**: `backend/src/main/java/com/gisplatform/service/impl/DatasetServiceImpl.java`

**当前代码** (约第 127-132 行):
```java
@Override
public List<Dataset> listPublishedDatasets() {
    return this.list(new LambdaQueryWrapper<Dataset>()
            .eq(Dataset::getStatus, "published")
            .eq(Dataset::getDeleted, 0));
}
```

**修改为**:
```java
@Override
public List<Dataset> listPublishedDatasets() {
    return this.list(new LambdaQueryWrapper<Dataset>()
            .eq(Dataset::getStatus, "published")
            .eq(Dataset::getDeleted, 0)
            .eq(Dataset::getType, "vector"));
}
```

### Task 2: 修复 ImageServiceImpl.getImageWmsInfo() 使用 dataset.getWmsUrl()

**File**: `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java`

**当前代码** (约第 355-370 行):
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

**修改为**:
```java
@Override
public ImageWmsInfo getImageWmsInfo(Long id) {
    Dataset dataset = this.getById(id);
    if (dataset == null || dataset.getDeleted() == 1) {
        throw new RuntimeException("影像数据集不存在");
    }

    if (dataset.getWmsUrl() == null || dataset.getWmsUrl().isEmpty()) {
        throw new RuntimeException("影像数据集未发布到 GeoServer");
    }

    ImageWmsInfo info = new ImageWmsInfo();
    info.setLayerName("raster_" + id);
    info.setCrs(dataset.getSrs() != null ? dataset.getSrs() : "EPSG:4326");
    info.setOpacity(0.8);
    info.setWmsUrl(dataset.getWmsUrl());

    return info;
}
```

---

## Implementation Order

1. Task 1: DatasetServiceImpl
2. Task 2: ImageServiceImpl

## Notes

- Bug 1 修复后，"矢量图层"列表将不再显示影像数据
- Bug 2 修复后，TileWMS 将能正确请求 GeoServer 获取影像
- 两个修复都只涉及后端 Service 层，无需前端修改
