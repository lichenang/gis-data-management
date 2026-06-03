# Proposal: fix-remove-empty-store-step

## 问题描述

当前两步法发布影像的 Step 1 存在根本性缺陷：

**Step 1: `POST /rest/workspaces/{ws}/coveragestores`** — 创建空 store

发送的 XML body 不包含 `<workspace>` 元素，而 GeoServer 2.28.x 的 `CoverageStoreController.coverageStorePost` 在反序列化 `CoverageStoreInfo` 时不从 URL 路径绑定 workspace，导致 `catalog.validate()` 校验失败：

```
POST /rest/workspaces/gisplatform/coveragestores
Body: <coverageStore><name>raster_27</name><enabled>true</enabled></coverageStore>
                                                      ↑
                                              workspace=null → validate FAIL

"Store must be part of a workspace"
```

**Step 2: `PUT .../external.geotiff?configure=first`** 已具备 store 创建能力

`CoverageStoreFileController.coverageStorePut` 的源码逻辑：
```java
CoverageStoreInfo info = catalog.getCoverageStoreByName(workspaceName, storeName);
if (info == null) {
    // CatalogBuilder 从 URL 路径绑定 workspace 创建 store
    CatalogBuilder builder = new CatalogBuilder(catalog);
    builder.setWorkspace(catalog.getWorkspaceByName(workspaceName));
    info = builder.buildCoverageStore(storeName);
    add = true;  // 后续执行 catalog.add(info)
}
// 设置 external URL → 创建 coverage → 自动发布 layer
```

Step 2 的 PUT handler 已正确处理 workspace 绑定（从 URL 路径获取），且 `configure=first` 会一并创建 coverage 和 layer。

## 影响范围

| 文件 | 变更类型 | 说明 |
|------|---------|------|
| `GeoServerCoverageStoreService.java` | 修改 | 移除 `createEmptyStore()` 和 `storeExists()`，`createImageMosaicStore()` 直接调用 PUT |
| `GeoServerCoverageStoreService.java` | 移除 | 删除 Step 1 的 XML 构造和 POST 调用，删除 `storeExists` 前置检查 |

## 风险评估

- 低风险：移除的是已知有缺陷的代码（Step 1 始终失败，从未成功创建过 store）
- Step 2 的 PUT 是替代方案，已被验证可独立完成 store 创建
- 如果 PUT 因缺少前置条件失败，错误信息会被 `GeoServerClient` 的增强错误处理完整捕获
