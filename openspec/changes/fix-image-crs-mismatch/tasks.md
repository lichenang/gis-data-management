# Tasks: fix-image-crs-mismatch

## Task 1: 修改后端 ImageServiceImpl.java setCrs 值 ✓

**文件**: `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java`

**位置**: 第 410 行和第 417 行

**修改内容**:

Line 410:
```java
// 修改前
info.setCrs("EPSG:3857");
// 修改后
info.setCrs("EPSG:4326");
```

Line 417:
```java
// 修改前
info.setCrs("EPSG:3857");
// 修改后
info.setCrs("EPSG:4326");
```

**验证**: Maven 编译通过 ✓

---

## Task 2: 修改前端 MapContainer.vue TileWMS 添加 projection ✓

**文件**: `frontend/src/views/map/MapContainer.vue`

**位置**: 第 255-264 行

**修改内容**:

在 TileWMS 构造函数中添加 `projection` 参数：

```typescript
const wmsSource = new TileWMS({
  url: imageInfo.wmsUrl,
  params: {
    'LAYERS': imageInfo.layerName,
    'TILED': true
  },
  projection: imageInfo.crs || 'EPSG:4326',  // ← 添加此行
  serverType: 'geoserver',
  transition: 0
})
```

**验证**: 前端编译通过 (npm run typecheck 或 vue-tsc) ✓

---

## Task 3: 手动测试验证

1. 重启后端服务
2. 启动前端开发服务器
3. 上传并发布 GeoTIFF 影像
4. 在地图上添加影像图层
5. 验证：
   - [ ] 地图自动缩放到正确区域（北京附近，而非海里）
   - [ ] 影像正确显示在地图上
   - [ ] TileWMS 请求的 BBOX 和 SRS 参数正确

### 测试检查清单

- [ ] 后端日志显示 extent 转换成功
- [ ] 前端控制台无 CRS 相关错误
- [ ] 地图 view.fit 跳转到正确位置
- [ ] WMS GetMap 请求返回有效影像
