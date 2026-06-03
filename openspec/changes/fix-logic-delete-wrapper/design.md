## Context

MyBatis-Plus 的 `@TableLogic` 注解会标记逻辑删除字段。当调用 `updateById` 方法时，带 `@TableLogic` 注解的字段会被自动忽略。

当前问题代码：
```java
dataset.setDeleted(1);
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

```java
datasetMapper.update(null, new LambdaUpdateWrapper<Dataset>()
    .eq(Dataset::getId, id)
    .set(Dataset::getDeleted, 1)
    .set(Dataset::getUpdateTime, LocalDateTime.now()));
```

## Risks / Trade-offs

| 风险 | 描述 | 缓解措施 |
|------|------|----------|
| 清理逻辑需在删除后 | 确保清理逻辑在 database 更新之后执行 |
