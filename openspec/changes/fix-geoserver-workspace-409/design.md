# Design: fix-geoserver-workspace-409

## 问题分析

### 当前代码（有 Bug）

```java
public boolean workspaceExists(String workspace) {
    try {
        ResponseEntity<String> response = new RestTemplate().exchange(  // ← 无认证
            props.getUrl() + "/rest/workspaces/" + workspace,
            HttpMethod.GET,
            null,   // ← 无 HttpEntity，无认证头
            String.class
        );
        return response.getStatusCode().is2xxSuccessful();
    } catch (Exception e) {
        return false;  // ← 401 被 catch 为 false
    }
}
```

GeoServer REST API 对 `/rest/workspaces/{ws}` 返回：
- `200 OK` — workspace 存在
- `401 Unauthorized` — 未提供认证
- `404 Not Found` — workspace 不存在（带认证时）

当前代码中，401 被转换成了 false，而后续的 POST 携带了认证信息，导致 409。

### 修复方案

`workspaceExists()` 改用 `GeoServerClient.get()` 替代 `new RestTemplate()`：

```java
public boolean workspaceExists(String workspace) {
    try {
        client.get("/rest/workspaces/" + workspace, String.class);
        return true;
    } catch (Exception e) {
        return false;
    }
}
```

`GeoServerClient.get()` 已包含 Basic Auth 头，GET 请求会返回正确的响应：
- `200 OK` → 返回 true
- `404 Not Found` → HttpStatusCodeException → 返回 false

### 前置条件

- `GeoServerClient` 已配置正确的 `username`/`password`

### 后置条件

- 首次发布时：POST 创建 workspace（201 Created）
- 后续发布时：GET 检查 → 200 OK → 跳过创建（不再产生 409）
