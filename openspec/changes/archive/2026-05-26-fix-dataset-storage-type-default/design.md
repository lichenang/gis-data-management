# Design: fix-dataset-storage-type-default

## Overview

在 `DatasetServiceImpl.createDataset` 方法中为 `storage_type` 字段设置默认值，避免因前端未传递该字段导致数据库插入失败。

## Technical Design

### Modified Files

1. `backend/src/main/java/com/gisplatform/service/impl/DatasetServiceImpl.java`

### Changes

在 `createDataset` 方法中添加 `storage_type` 默认值设置：

```java
if (dataset.getStorageType() == null) {
    dataset.setStorageType("postgis");
}
```

### Field Analysis

| 字段 | 数据库约束 | 当前代码处理 | 状态 |
|------|-----------|-------------|------|
| name | NOT NULL | 由 Controller 验证 | OK |
| type | NOT NULL | 需确认是否需要默认值 | 待检查 |
| srs | NOT NULL DEFAULT 'EPSG:4326' | 已设置默认值 | OK |
| storage_type | NOT NULL | **未设置默认值** | **需修复** |
| status | NOT NULL DEFAULT 'draft' | 已设置默认值 | OK |
| version | NOT NULL DEFAULT 1 | 已设置默认值 | OK |
| created_by | NOT NULL | 已通过 CurrentUserUtils 设置 | OK |
| tenant_id | NOT NULL DEFAULT 'default' | 已设置默认值 | OK |
| create_time | NOT NULL DEFAULT CURRENT_TIMESTAMP | 已设置 | OK |

### type 字段分析

根据业务逻辑，`type` 字段应从上传的文件或前端请求中获取，但在"创建空白数据集"的场景下可能没有传递。需要确认：
- 如果 Controller 层已经验证必需字段，则无需修改
- 如果需要支持创建空白数据集，应添加默认值为 "vector"

## Testing

1. 调用 `POST /api/v1/datasets` 不传 storage_type，验证成功创建
2. 调用 `POST /api/v1/datasets` 传递 storage_type，验证使用传入值

## Risks

- 无明显风险，这是一个简单的修复
