# Tasks: add-dataset-publish-action

## Backend Tasks

### Task 1: 添加 publish 接口 [x]

**File**: `backend/src/main/java/com/gisplatform/controller/DatasetController.java`

添加：
```java
@PutMapping("/{id}/publish")
@Operation(summary = "发布数据集", description = "将数据集状态设置为 published")
public R<Dataset> publish(@PathVariable Long id) {
    Dataset dataset = datasetService.publishDataset(id);
    return R.ok(dataset);
}
```

### Task 2: 添加 unpublish 接口 [x]

**File**: `DatasetController.java`

添加：
```java
@PutMapping("/{id}/unpublish")
@Operation(summary = "取消发布", description = "将数据集状态设置为 draft")
public R<Dataset> unpublish(@PathVariable Long id) {
    Dataset dataset = datasetService.unpublishDataset(id);
    return R.ok(dataset);
}
```

### Task 3: 添加 service 方法 [x]

**File**: `backend/src/main/java/com/gisplatform/service/DatasetService.java`

添加方法签名：
```java
Dataset publishDataset(Long id);
Dataset unpublishDataset(Long id);
```

**File**: `backend/src/main/java/com/gisplatform/service/impl/DatasetServiceImpl.java`

实现：
```java
@Override
public Dataset publishDataset(Long id) {
    Dataset dataset = this.getById(id);
    if (dataset == null) {
        throw new RuntimeException("数据集不存在");
    }
    dataset.setStatus("published");
    dataset.setUpdateTime(LocalDateTime.now());
    this.updateById(dataset);
    return dataset;
}

@Override
public Dataset unpublishDataset(Long id) {
    Dataset dataset = this.getById(id);
    if (dataset == null) {
        throw new RuntimeException("数据集不存在");
    }
    dataset.setStatus("draft");
    dataset.setUpdateTime(LocalDateTime.now());
    this.updateById(dataset);
    return dataset;
}
```

---

## Frontend Tasks

### Task 4: 添加发布/取消发布按钮 [x]

**File**: `frontend/src/views/datasets/index.vue`

在操作列添加按钮，逻辑：
- status === 'published': 显示"取消发布"按钮
- status !== 'published': 显示"发布"按钮

### Task 5: 添加状态变更方法 [x]

添加 `handlePublish` 方法，调用 API 后刷新列表。

---

## Implementation Order

1. Task 3: Service 方法 (后端基础)
2. Task 1-2: Controller 接口
3. Task 4-5: 前端改动
