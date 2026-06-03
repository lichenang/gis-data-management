# Proposal: fix-wms-cors-proxy

## 问题描述

影像图层勾选后无法加载，直接访问 GeoServer WMS 返回 CORS 跨域错误。

## 根因分析

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        问题根因                                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  前端 (localhost:3000) → TileWMS → GeoServer (localhost:8080)              │
│                                           ↑                                  │
│                                      浏览器 CORS 拦截!                       │
│                                                                             │
│  1. 后端 GeoServerLayerService.getWmsUrl() 返回绝对 URL:                    │
│     http://localhost:8080/geoserver/gisplatform/raster_27/wms              │
│                                                                             │
│  2. 前端 TileWMS 直接使用此绝对 URL                                         │
│                                                                             │
│  3. Vite 代理只配置了 /api，没有 /geoserver                                 │
│                                                                             │
│  4. 请求直接发往 localhost:8080，被浏览器 CORS 拦截                          │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 修复方案

### 方案 A: Vite 代理 + 相对路径（推荐）

1. **添加 Vite 代理**: 在 `vite.config.ts` 中添加 `/geoserver` 代理指向 `http://localhost:8080`
2. **后端返回相对路径**: 修改 `GeoServerLayerService.getWmsUrl()` 返回 `/geoserver/...`
3. **前端无需改动**: TileWMS 直接使用 `imageInfo.wmsUrl`

### 影响范围

| 文件 | 修改内容 | 风险 |
|------|----------|------|
| `frontend/vite.config.ts` | 添加 /geoserver 代理 | 低 |
| `backend/.../GeoServerLayerService.java` | 返回相对路径 | 低 |

## 验证方法

1. 启动前端开发服务器
2. 在地图上添加影像图层
3. 检查浏览器 Network 面板：
   - WMS 请求是否走 `/geoserver/...` 路径
   - 是否有 CORS 错误
4. 确认影像正确显示
