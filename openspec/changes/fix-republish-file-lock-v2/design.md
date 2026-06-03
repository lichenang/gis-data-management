## Context

GeoServer 通过 REST API 创建 coverage store 时，如果该 store 已被使用（文件被锁定），会返回 500 错误。重新发布影像时需要先清理旧的 coverage store。

## Goals / Non-Goals

**Goals:**
- 在创建 coverage store 前先删除已有的 store
- 等待 2 秒确保文件句柄释放

**Non-Goals:**
- 不修改其他发布逻辑

## Decisions

### Decision 1: 先删除再创建

**选择**: 删除已有 store → 等待 2 秒 → 创建新 store

**理由**:
- 这是 GeoServer 推荐的工作流程
- 删除操作是幂等的（如果不存在会返回 404）
- 2 秒是经验值，足以释放文件句柄

### Decision 2: 使用 try-catch 处理 404

**选择**: 删除 store 时捕获 404 异常，继续执行

**理由**:
- 首次发布时 store 不存在是正常的应用场景

## Risks / Trade-offs

| 风险 | 描述 | 缓解措施 |
|------|------|----------|
| 删除等待时间不足 | 在某些系统上 2 秒可能不够 | 可配置等待时间 |
