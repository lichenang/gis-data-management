# Tasks: fix-dataset-storage-type-default

## Task 1: Add storage_type default value

### Description
在 DatasetServiceImpl.createDataset 方法中添加 storage_type 默认值设置

### File
- `backend/src/main/java/com/gisplatform/service/impl/DatasetServiceImpl.java`

### Changes
在 createDataset 方法中添加：
```java
if (dataset.getStorageType() == null) {
    dataset.setStorageType("postgis");
}
```

### Location
在 DatasetServiceImpl.java 的 createDataset 方法中，第 63 行（SRS 设置之后）添加 storage_type 默认值处理

## Task 2: Verify NOT NULL fields coverage

### Description
验证所有有 NOT NULL 约束的字段都在 createDataset 方法中有处理

### Fields to check
- [x] name - Controller 层验证
- [x] type - 已添加默认值 "vector"
- [x] srs - 已有
- [x] storage_type - Task 1 添加
- [x] status - 已有
- [x] version - 已有
- [x] created_by - 已有
- [x] tenant_id - 已有

## Task 3: Test the fix

### Description
验证修复后的接口正常工作

### Test scenarios
1. 不传递 storage_type，验证使用默认值 "postgis"
2. 传递 storage_type="minio"，验证使用传入值
3. 验证 created_by 正确获取当前用户 ID（已修复）

---

**Total**: 3 tasks (2 implementation + 1 verification)
