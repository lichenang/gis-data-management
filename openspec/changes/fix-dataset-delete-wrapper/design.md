## Context

MyBatis-Plus 的 `@TableLogic` 注解会标记逻辑删除字段。当调用 `updateById` 方法时：
- 带 `@TableLogic` 注解的字段会被自动忽略
- 这是 MP 的设计，避免在更新时意外修改 deleted 字段

当前问题代码：
```java
Dataset dataset = new Dataset();
dataset.setId(id);
dataset.setDeleted(1);  // 会被忽略
datasetService.updateById(dataset);  // deleted 不在 SQL 中
```

## Goals / Non-Goals

**Goals:**
- 使用 LambdaUpdateWrapper 显式设置 deleted=1
- 仅更新 deleted 和 update_time 字段

**Non-goals:**
- 不修改查询逻辑

## Decisions

### Decision 1: 使用 LambdaUpdateWrapper

**选择**: 使用 `LambdaUpdateWrapper` 显式更新

**理由**:
- 可以绕过 @TableLogic 注解的自动忽略
- 使用 Lambda 方式避免硬编码列名

```java
datasetMapper.update(null, new LambdaUpdateWrapper<Dataset>()
    .eq(Dataset::getId, id)
    .set(Dataset::getDeleted, 1)
    .set(Dataset::getUpdateTime, LocalDateTime.now()));
```

### Decision 2: 分开处理清理逻辑

**选择**: 先执行逻辑删除，再清理附加资源

**理由**:
- 如果先清理失败，数据已被删除不可回滚
- 保持清理逻辑不变，只修改数据库更新方式

## Implementation Steps

1. 找到 DatasetServiceImpl.deleteDataset() 方法
2. 替换 updateById 为 LambdaUpdateWrapper 方式
3. 同样处理 ImageServiceImpl.deleteImage() 方法

## Risks / Trade-offs

| 风险 | 描述 | 缓解措施 |
|------|------|----------|
| 清理逻辑执行失败 | 资源清理失败但deleted已设1 | 先删除实体再清理，或使用事务 |
