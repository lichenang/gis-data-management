# WMS CORS 代理问题诊断报告

## 问题确认

### 1. Vite 代理配置

**文件**: `frontend/vite.config.ts:31-36`

```typescript
proxy: {
  '/api': {
    target: 'http://localhost:8088',
    changeOrigin: true
  }
  // ❌ 没有 /geoserver 代理
}
```

**结论**: Vite 代理只配置了 `/api`，没有 `/geoserver` 代理。

---

### 2. 后端返回的 WMS URL 类型

**文件**: `backend/.../GeoServerLayerService.java:61-63`

```java
public String getWmsUrl(String workspace, String layerName) {
    return props.getUrl() + "/" + workspace + "/" + layerName + "/wms";
}
```

**配置**: `application-local.yml`
```yaml
geoserver:
  url: http://localhost:8080/geoserver
  workspace: gisplatform
```

**实际返回的 URL**:
```
http://localhost:8080/geoserver/gisplatform/raster_27/wms
```

**结论**: 后端返回的是**绝对 URL**（包含完整域名和端口）。

---

### 3. 前端 TileWMS 使用 URL

**文件**: `frontend/src/views/map/MapContainer.vue:255-256`

```typescript
const wmsSource = new TileWMS({
  url: imageInfo.wmsUrl,  // ← 直接使用后端返回的绝对 URL
  // ...
})
```

**结论**: 前端直接使用绝对 URL `http://localhost:8080/geoserver/...`，导致请求不走 Vite 代理，被浏览器 CORS 拦截。

---

## 问题分析图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        当前请求流程                                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  前端 (localhost:3000)                                                      │
│       │                                                                     │
│       │  TileWMS 构造                                                       │
│       │  url = "http://localhost:8080/geoserver/gisplatform/raster_27/wms" │
│       │                                                                     │
│       │  OpenLayers 发起请求:                                               │
│       │  http://localhost:8080/geoserver/gisplatform/raster_27/wms?        │
│       │    SERVICE=WMS&REQUEST=GetMap&...                                  │
│       │                                                                     │
│       └──────────────────────────────────────────────────────────────────▶  │
│                                         │                                    │
│                                    浏览器 CORS 拦截!                         │
│                                         │                                    │
│                                         ▼                                    │
│                              GeoServer (localhost:8080)                     │
│                                   拒绝跨域请求                                │
│                                                                             │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │  CORS 错误典型特征:                                                     │  │
│  │  • 请求发出去了（Network 有记录）                                       │  │
│  │  • Response 状态码可能是 200，但被浏览器拦截                            │  │
│  │  • Console 有 CORS 错误日志                                            │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 对比：代理 vs 非代理请求

| 场景 | URL 类型 | 走代理？ | CORS |
|------|----------|----------|------|
| 当前 | 绝对 URL `http://localhost:8080/geoserver/...` | ❌ 不走 | ❌ 被拦截 |
| 期望 | 相对 URL `/geoserver/...` | ✅ 走 Vite 代理 | ✅ 无问题 |

---

## 解决方案

### 方案 A: 添加 Vite 代理 + 前端使用相对路径（推荐）

**步骤 1**: 修改 `vite.config.ts` 添加 `/geoserver` 代理

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

**步骤 2**: 修改后端返回相对路径

修改 `GeoServerLayerService.java`:

```java
public String getWmsUrl(String workspace, String layerName) {
    // 返回相对路径而非绝对路径
    return "/geoserver" + "/" + workspace + "/" + layerName + "/wms";
}
```

**或 步骤 2 替代方案**: 前端转换 URL

修改 `MapContainer.vue`:

```typescript
function loadImageLayer(imageInfo: ImageLayerInfo) {
  // 将绝对 URL 转换为相对路径
  const wmsUrl = imageInfo.wmsUrl.replace(/^http:\/\/localhost:8080/, '')

  const wmsSource = new TileWMS({
    url: wmsUrl,
    // ...
  })
}
```

---

### 方案 B: 配置 GeoServer CORS（生产环境）

在 GeoServer 的 `web.xml` 中添加：

```xml
<CORS>
  <allowedOrigins>http://localhost:3000</allowedOrigins>
  <allowedMethods>GET,POST,PUT,DELETE,HEAD,OPTIONS</allowedMethods>
  <allowedHeaders>*</allowedHeaders>
</CORS>
```

---

## 修复后数据流

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        修复后请求流程                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  前端 (localhost:3000)                                                      │
│       │                                                                     │
│       │  TileWMS 构造                                                       │
│       │  url = "/geoserver/gisplatform/raster_27/wms"                      │
│       │                                                                     │
│       │  OpenLayers 发起请求:                                               │
│       │  http://localhost:3000/geoserver/gisplatform/raster_27/wms?        │
│       │                                                                     │
│       └──────────────────────────────────────────────────────────────────▶  │
│                                         │                                    │
│                                    Vite 代理拦截                             │
│                                         │                                    │
│                                         ▼                                    │
│                              代理转发到:                                      │
│                              http://localhost:8080/geoserver/...            │
│                                         │                                    │
│                                         ▼                                    │
│                              GeoServer (localhost:8080)                     │
│                                   正常返回影像                                │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 待实施修复清单

- [ ] 修改 `vite.config.ts` 添加 `/geoserver` 代理
- [ ] 修改前端使用相对路径 `/geoserver/...` 而非绝对 URL
- [ ] 测试影像图层正确显示

## 相关文件

| 文件 | 问题 | 修复 |
|------|------|------|
| `frontend/vite.config.ts` | 缺少 /geoserver 代理 | 添加代理配置 |
| `backend/.../GeoServerLayerService.java` | 返回绝对 URL | 改为相对路径 |
| `frontend/src/views/map/MapContainer.vue` | 直接使用绝对 URL | 转换或使用相对 URL |
