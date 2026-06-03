# Tasks: fix-wms-cors-proxy

## Task 1: 修改 vite.config.ts 添加 /geoserver 代理 ✓

**文件**: `frontend/vite.config.ts`

**位置**: 第 28-37 行

**修改内容**:

在 `proxy` 配置中添加 `/geoserver` 代理：

```typescript
server: {
  port: 3000,
  host: '0.0.0.0',
  proxy: {
    '/api': {
      target: 'http://localhost:8088',
      changeOrigin: true
    },
    '/geoserver': {  // ← 添加此代理
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

**验证**: 重启前端开发服务器后，`/geoserver/*` 请求应被代理 ✓

---

## Task 2: 修改 GeoServerLayerService.getWmsUrl() 返回相对路径 ✓

**文件**: `backend/src/main/java/com/gisplatform/service/geoserver/GeoServerLayerService.java`

**位置**: 第 61-67 行

**修改内容**:

Line 62:
```java
// 修改前
return props.getUrl() + "/" + workspace + "/" + layerName + "/wms";
// 修改后
return "/geoserver" + "/" + workspace + "/" + layerName + "/wms";
```

Line 66:
```java
// 修改前
return props.getUrl() + "/" + workspace + "/" + layerName + "/wmts";
// 修改后
return "/geoserver" + "/" + workspace + "/" + layerName + "/wmts";
```

**验证**: Maven 编译通过 ✓

---

## Task 3: 手动测试验证

1. 重启后端服务
2. 启动前端开发服务器
3. 在地图上添加影像图层
4. 验证：
   - [ ] 浏览器 Network 面板显示 WMS 请求为 `/geoserver/...` 路径
   - [ ] 无 CORS 错误
   - [ ] 影像正确显示

### 测试检查清单

- [ ] WMS GetMap 请求 URL 以 `/geoserver/` 开头
- [ ] 请求状态码为 200
- [ ] 无 CORS 预检请求失败
- [ ] 影像瓦片正确加载
