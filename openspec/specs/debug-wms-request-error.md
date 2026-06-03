# WMS 请求失败诊断报告

## 错误信息

直接访问 `http://localhost:8080/geoserver/gisplatform/raster_27/wms` 返回：

```xml
<ServiceExceptionReport xmlns="http://www.opengis.net/ogc" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" version="1.3.0" xsi:schemaLocation="http://www.opengis.net/ogc http://localhost:8080/geoserver/schemas/wms/1.3.0/exceptions_1_3_0.xsd">
  <ServiceException code="MissingParameterValue" locator="request">
    Could not determine geoserver request from http request org.geoserver.platform.AdvancedDispatchFilter$AdvancedDispatchHttpRequest@228ae176
  </ServiceException>
</ServiceExceptionReport>
```

---

## 1. 前端 MapContainer.vue 中 TileWMS 配置

**文件**: `frontend/src/views/map/MapContainer.vue:255-264`

```typescript
const wmsSource = new TileWMS({
  url: imageInfo.wmsUrl,           // http://localhost:8080/geoserver/gisplatform/raster_27/wms
  params: {
    'LAYERS': imageInfo.layerName,  // raster_27
    'TILED': true
  },
  projection: imageInfo.crs || 'EPSG:4326',
  serverType: 'geoserver',
  transition: 0
})
```

**关键配置分析**:

| 参数 | 值 | 来源 |
|------|-----|------|
| `url` | `http://localhost:8080/geoserver/gisplatform/raster_27/wms` | 后端 `GeoServerLayerService.getWmsUrl()` |
| `LAYERS` | `raster_27` | 后端 `ImageServiceImpl` 设置的 `layerName` |
| `projection` | `EPSG:4326` | 后端 `ImageServiceImpl.getImageWmsInfo()` 返回 (已修复) |
| `serverType` | `geoserver` | 前端硬编码 |

---

## 2. WMS URL 构建过程

**后端 GeoServerLayerService.getWmsUrl()** (line 61-63):

```java
public String getWmsUrl(String workspace, String layerName) {
    return props.getUrl() + "/" + workspace + "/" + layerName + "/wms";
}
```

**配置** (application-local.yml):
- `geoserver.url`: `http://localhost:8080/geoserver`
- `geoserver.workspace`: `gisplatform`

**最终 URL**:
```
http://localhost:8080/geoserver/gisplatform/raster_27/wms
```

---

## 3. 根因分析

### 根因 #1: 缺少 Vite 代理配置 (CORS 问题)

**问题**: 前端开发服务器运行在 `localhost:3000`，GeoServer 运行在 `localhost:8080`。

**Vite 当前代理配置**:
```typescript
// vite.config.ts
proxy: {
  '/api': {  // 只代理 /api 请求
    target: 'http://localhost:8088',
    changeOrigin: true
  }
}
```

**实际情况**:
```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        请求流程分析                                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  前端 (localhost:3000)                                                      │
│       │                                                                     │
│       │  TileWMS 请求:                                                      │
│       │  http://localhost:8080/geoserver/gisplatform/raster_27/wms?...      │
│       │                                                                     │
│       └──────────────────────────────────────────────────────────────────▶  │
│                                         │                                    │
│                                    浏览器 CORS 拦截!                         │
│                                         │                                    │
│                                         ▼                                    │
│                              GeoServer (localhost:8080)                     │
│                                   不允许跨域请求                              │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**影响**:
- 浏览器首先发送 OPTIONS 预检请求
- GeoServer 未配置 CORS 时返回拒绝
- 实际 GetMap 请求从未发出

### 根因 #2: 直接访问 WMS 端点缺少参数

用户直接访问 `http://localhost:8080/geoserver/gisplatform/raster_27/wms` 时：

```
MissingParameterValue" locator="request"> Could not determine geoserver request
```

**这是正常现象** - WMS 端点需要 REQUEST 参数（如 REQUEST=GetMap）。

**真正的 WMS GetMap 请求应该是**:
```
http://localhost:8080/geoserver/gisplatform/raster_27/wms?
  SERVICE=WMS&
  VERSION=1.3.0&
  REQUEST=GetMap&
  FORMAT=image/png&
  TRANSPARENT=true&
  LAYERS=gisplatform:raster_27&
  TILED=true&
  BBOX=116.1,39.5,116.7,40.1&
  SRS=EPSG:4326&
  WIDTH=256&
  HEIGHT=256
```

### 根因 #3: GeoServer 认证问题 (可能)

GeoServer 默认可能需要认证。如果 TileWMS 没有携带认证信息，请求会被拒绝。

---

## 4. 解决方案

### 方案 A: 添加 Vite 代理配置 (推荐开发环境)

修改 `vite.config.ts`:

```typescript
server: {
  port: 3000,
  host: '0.0.0.0',
  proxy: {
    '/api': {
      target: 'http://localhost:8088',
      changeOrigin: true
    },
    '/geoserver': {  // ← 添加 GeoServer 代理
      target: 'http://localhost:8080',
      changeOrigin: true,
      rewrite: (path) => path.replace(/^\/geoserver/, '/geoserver')
    }
  }
}
```

同时修改前端 TileWMS URL:

```typescript
// 将 imageInfo.wmsUrl 转换为代理路径
const wmsUrl = imageInfo.wmsUrl.replace('http://localhost:8080/geoserver', '/geoserver')
```

### 方案 B: 后端添加 WMS 代理接口

创建后端代理 endpoint：

```
GET /api/v1/proxy/wms?url=xxx&params=...
```

前端通过后端代理访问 GeoServer。

### 方案 C: 配置 GeoServer CORS (生产环境)

在 GeoServer 的 `web.xml` 或 ` jetty-servlet.xml` 中配置 CORS 允许前端域名访问。

---

## 5. 验证步骤

### 验证 CORS 问题

在浏览器控制台执行：

```javascript
fetch('http://localhost:8080/geoserver/gisplatform/raster_27/wms?SERVICE=WMS&REQUEST=GetCapabilities')
  .then(r => r.text())
  .then(console.log)
```

如果看到 CORS 错误，说明是 CORS 拦截问题。

### 验证 WMS 服务可用性

在后台服务器（Postman/curl）中测试：

```bash
curl "http://localhost:8080/geoserver/gisplatform/raster_27/wms?SERVICE=WMS&REQUEST=GetCapabilities"
```

如果返回 XML，说明 GeoServer 本身正常。

---

## 6. 数据流图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     完整数据流                                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  1. LayerPanel.vue: getImageWmsUrl(id=27)                                   │
│     ↓                                                                       │
│  2. Backend: ImageServiceImpl.getImageWmsInfo()                             │
│     ↓ 返回 {                                                                │
│        wmsUrl: "http://localhost:8080/geoserver/gisplatform/raster_27/wms",│
│        layerName: "raster_27",                                              │
│        crs: "EPSG:4326",                                                    │
│        extent: [...]                                                        │
│     }                                                                       │
│     ↓                                                                       │
│  3. MapContainer.vue: new TileWMS({ url: wmsUrl, ... })                     │
│     ↓                                                                       │
│  4. OpenLayers 发起 GetMap 请求:                                            │
│     http://localhost:8080/geoserver/gisplatform/raster_27/wms?              │
│       SERVICE=WMS&REQUEST=GetMap&LAYERS=gisplatform:raster_27&...           │
│     ↓                                                                       │
│  5. 浏览器: CORS 预检 OPTIONS 请求 → 失败！                                  │
│     ↓                                                                       │
│  6. GetMap 请求从未发出                                                      │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 7. 待验证项

请在浏览器 Network 面板中检查：

1. **是否有任何以 `wms` 结尾的请求？**
   - 如果没有，说明请求被 CORS 拦截
   - 如果有，检查请求参数是否正确

2. **是否有 OPTIONS 预检请求失败？**
   - 检查 Response 代码是否是 4xx/5xx

3. **实际 WMS 请求的 URL 是什么？**
   - 完整粘贴到诊断报告

---

## 8. 快速修复建议

如果 CORS 是问题，可以在 vite.config.ts 中添加代理：

```typescript
proxy: {
  '/api': {
    target: 'http://localhost:8088',
    changeOrigin: true
  },
  '/geoserver': {  // NEW
    target: 'http://localhost:8080',
    changeOrigin: true
  }
}
```

然后前端需要将 WMS URL 改为使用代理路径。
