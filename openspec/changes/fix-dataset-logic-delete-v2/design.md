## Context

当前的 delete方法实现：
```java
// DatasetController 示例
datasetMapper.update(null, new LambdaUpdateWrapper<Dataset>()
    .eq(Dataset::getId, id)
    .set(Dataset::getUpdateTime, LocalDateTime.now()));
```

这种方式绕过了 MyBatis-Plus 的逻辑删除机制，只更新了 updateTime 而未设置 deleted=1。

正确的方式应该是使用 `removeById`（或 `deleteById`），MyBatis-Plus 会检测到 `@TableLogic` 注解，自动将 DELETE 转换为 UPDATE ... SET deleted=1。

## Goals / Non-Goals

**Goals:**
- 修改 DatasetController 的 delete 方法使用 removeById
- 修改 ImageController 的 delete 方法使用 removeById

**Non-goals:**
- 不修改查询逻辑
- 不修改其他接口

## Decisions

### Decision 1: 使用 Service 层的 removeById

**选择**: 使用 `datasetService.removeById(id)` 或 `BaseMapper.deleteById(id)`

**理由**:
- MyBatis-Plus 的 Service 层方法会自动处理逻辑删除
- 简化代码，无需手动构造条件

### Decision 2: 保留清理附加资源逻辑

**选择**: 在逻辑删除后，保留清理 GeoServer、MinIO 等附加资源的逻辑

**理由**:
- 删除数据集时需要同时清理发布到 GeoServer 的图层、切片缓存等
- 这些清理逻辑应该保留

## Implementation

### DatasetController
```java
@DeleteMapping("/{id}")
public R<?> delete(@PathVariable Long id) {
    // 逻辑删除核心（自动设置 deleted=1）
    datasetService.removeById(id);
    
    // 保留清理逻辑...
    // 清理 GeoServer 图层、切片等
}
```

### ImageController
类似处理

## Risks / Trade-offs

| 风险 | 描述 | 缓解措施 |
|------|------|----------|
| 清理逻辑依赖实体 | removeById 后实体对象被删除 | 保留原有删除前的清理逻辑 |
