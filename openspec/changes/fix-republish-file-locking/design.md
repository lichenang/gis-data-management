## Context

根据诊断报告 `openspec/specs/debug-file-locking.md`，问题不是 Java 端的文件流未关闭，而是发布流程缺少删除旧 store 的步骤。当重新发布同一数据集时，GeoServer 尝试覆盖旧文件但文件被锁定。

## Goals / Non-Goals

**Goals:**
- 修复重新发布影像时的 "unable to remove existing" 错误
- 在创建新 coverage store 前先删除已存在的旧 store

**Non-Goals:**
- 不修改首次发布流程
- 不修改 GeoServer 配置
- 不修改数据库结构

## Decisions

### Decision 1: 删除旧 store 后再创建

**选择**: 在 createImageMosaicStore() 之前调用 deleteStore()

**理由**:
- 这是最直接的解决方案
- deleteStore() 已有实现，只需在调用处添加

**备选方案**:
- 等待更长时间: 不够可靠
- 使用不同的 store 名称: 会导致旧的 store 残留

### Decision 2: 添加延迟

**选择**: 删除后等待 1 秒

**理由**:
- 给 GeoServer 足够时间清理文件和释放锁
- 避免立即创建新 store 导致冲突

## Implementation

修改 `ImageServiceImpl.publishImageDataset()` 方法：

```java
// 先删除已存在的 coverage store
try {
    coverageStoreService.deleteStore(workspace, storeName);
    log.info("Deleted existing coverage store: {}", storeName);
    Thread.sleep(1000);  // 等待 GeoServer 清理完成
} catch (Exception e) {
    log.info("No existing coverage store to delete: {}", storeName);
}

// 然后创建新的 coverage store
coverageStoreService.createImageMosaicStore(workspace, storeName, fileData);
```

## Risks / Trade-offs

| 风险 | 描述 | 缓解措施 |
|------|------|----------|
| 删除失败 | 旧 store 被其他进程锁定 | 记录日志，继续尝试创建 |
| 延迟导致超时 | Thread.sleep 可能导致响应变慢 | 只在需要删除时等待 |

## Migration Plan

1. 修改 ImageServiceImpl.publishImageDataset() 方法
2. Maven 编译验证
3. 测试重新发布同一影像
