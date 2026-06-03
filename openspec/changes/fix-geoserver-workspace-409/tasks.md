# Tasks: fix-geoserver-workspace-409

## Task 1: 修复 workspaceExists 方法

**文件**: `backend/src/main/java/com/gisplatform/service/geoserver/GeoServerWorkspaceService.java`

将 `workspaceExists()` 方法中的 `new RestTemplate().exchange()` 替换为 `client.get()`：

```java
// 修改前
public boolean workspaceExists(String workspace) {
    try {
        ResponseEntity<String> response = new RestTemplate().exchange(
            props.getUrl() + "/rest/workspaces/" + workspace,
            HttpMethod.GET,
            null,
            String.class
        );
        return response.getStatusCode().is2xxSuccessful();
    } catch (Exception e) {
        return false;
    }
}

// 修改后
public boolean workspaceExists(String workspace) {
    try {
        client.get("/rest/workspaces/" + workspace, String.class);
        return true;
    } catch (Exception e) {
        return false;
    }
}
```

同时移除不再需要的导入：
- `org.springframework.http.HttpMethod`
- `org.springframework.http.ResponseEntity`
- `org.springframework.web.client.RestTemplate`
- `java.util.Map`

## Task 2: 验证编译

**命令**:
```bash
cd backend && mvn compile
```

确认无编译错误。
