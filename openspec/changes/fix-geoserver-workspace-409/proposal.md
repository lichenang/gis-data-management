# Proposal: fix-geoserver-workspace-409

## 问题描述

影像发布时偶发 409 Conflict 错误，原因是 `GeoServerWorkspaceService.workspaceExists()` 使用未认证的 `RestTemplate` 实例检查 workspace 是否存在，导致：

1. `workspaceExists()` 用 `new RestTemplate()` 发起 GET 请求 — **不带 Basic Auth 头**
2. GeoServer 返回 401 Unauthorized → 异常被 catch → 返回 `false`
3. `createWorkspace()` 认为 workspace 不存在 → 执行 POST 创建
4. GeoServer 返回 **409 Conflict**（workspace 已存在）

```
GET  /rest/workspaces/gisplatform  → 401 (no auth)  → exists=false
POST /rest/workspaces/gisplatform  → 409 (already exists)  → FAIL
```

## 影响范围

| 文件 | 变更类型 | 说明 |
|------|---------|------|
| `GeoServerWorkspaceService.java` | 修改 | `workspaceExists()` 改用 `GeoServerClient` 替代 `new RestTemplate()` |

## 风险评估

- 极低风险：仅为认证方式修复，不涉及业务逻辑变更
- 修复后 workspace 检查与创建使用相同的认证凭证，一致性保障
