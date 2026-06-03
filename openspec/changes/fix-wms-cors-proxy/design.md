# Design: fix-wms-cors-proxy

## 修复方案

### 1. 添加 Vite 代理配置

**文件**: `frontend/vite.config.ts`

**位置**: 第 28-37 行

**修改前**:
```typescript
server: {
  port: 3000,
  host: '0.0.0.0',
  proxy: {
    '/api': {
      target: 'http://localhost:8088',
      changeOrigin: true
    }
  }
}
```

**修改后**:
```typescript
server: {
  port: 3000,
  host: '0.0.0.0',
  proxy: {
    '/api': {
      target: 'http://localhost:8088',
      changeOrigin: true
    },
    '/geoserver': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

**修改点**: 添加 `/geoserver` 代理配置

### 2. 修改后端返回相对路径

**文件**: `backend/src/main/java/com/gisplatform/service/geoserver/GeoServerLayerService.java`

**位置**: 第 61-63 行

**修改前**:
```java
public String getWmsUrl(String workspace, String layerName) {
    return props.getUrl() + "/" + workspace + "/" + layerName + "/wms";
}
```

**修改后**:
```java
public String getWmsUrl(String workspace, String layerName) {
    return "/geoserver" + "/" + workspace + "/" + layerName + "/wms";
}
```

**修改点**: 使用相对路径 `/geoserver` 替代 `props.getUrl()`

### 3. 前端无需改动

`MapContainer.vue` 中 `TileWMS` 的 `url: imageInfo.wmsUrl` 可直接使用后端返回的相对路径。

## 数据流（修复后）

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        修复后请求流程                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  1. 后端返回: wmsUrl = "/geoserver/gisplatform/raster_27/wms"              │
│                                                                             │
│  2. TileWMS 构造:                                                           │
│     url = "/geoserver/gisplatform/raster_27/wms"                           │
│                                                                             │
│  3. OpenLayers 发起请求:                                                    │
│     http://localhost:3000/geoserver/gisplatform/raster_27/wms?             │
│       SERVICE=WMS&REQUEST=GetMap&...                                       │
│                                                                             │
│  4. Vite 代理拦截:                                                          │
│     /geoserver/* → http://localhost:8080/geoserver/*                       │
│                                                                             │
│  5. GeoServer 正常返回影像                                                   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 完整代码修改

### Frontend: vite.config.ts

```typescript
server: {
  port: 3000,
  host: '0.0.0.0',
  proxy: {
    '/api': {
      target: 'http://localhost:8088',
      changeOrigin: true
    },
    '/geoserver': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

### Backend: GeoServerLayerService.java

```java
public String getWmsUrl(String workspace, String layerName) {
    return "/geoserver" + "/" + workspace + "/" + layerName + "/wms";
}

public String getWmtsUrl(String workspace, String layerName) {
    return "/geoserver" + "/" + workspace + "/" + layerName + "/wmts";
}
```

## 验证步骤

1. Maven 编译后端
2. 启动前端开发服务器 (npm run dev)
3. 在地图上添加影像图层
4. 检查浏览器 Network 面板：
   - WMS 请求 URL 是否为 `/geoserver/...` 开头
   - 无 CORS 错误
5. 确认影像正确显示
