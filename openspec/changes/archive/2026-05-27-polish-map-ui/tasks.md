# Tasks: polish-map-ui

## Task 1: 调整页面布局

**File**: `frontend/src/views/map/index.vue`

**修改** 样式:

```scss
<style scoped>
.map-viewer {
  display: flex;
  height: calc(100vh - 60px);
  width: 100%;
  overflow: hidden;
}

.map-main {
  flex: 1;
  padding: 0;
  overflow: hidden;
}
</style>
```

确保 el-container 自动填充高度，或移除 el-container 直接使用 div。

## Task 2: 添加缩放到数据按钮

**File**: `frontend/src/views/map/MapContainer.vue`

**修改** template:

```vue
<div class="map-wrapper">
  <div ref="mapContainer" class="map-container"></div>
  <el-button 
    class="zoom-to-extent" 
    :icon="Aim" 
    circle
    size="small"
    @click="zoomToExtent"
  />
</div>
```

**添加** script 导入:

```typescript
import { Aim } from '@element-plus/icons-vue'
```

**添加** zoomToExtent 函数:

```typescript
function zoomToExtent() {
  let combinedExtent = null
  
  for (const layer of Object.values(vectorLayers.value)) {
    const source = layer.getSource()
    const extent = source?.getExtent()
    if (extent && isFinite(extent[0])) {
      combinedExtent = combinedExtent 
        ? import('ol/extent').extend(combinedExtent, extent)
        : [...extent]
    }
  }
  
  if (combinedExtent && map.value) {
    map.value.getView().fit(combinedExtent, {
      padding: [50, 50, 50, 50],
      maxZoom: 15,
      duration: 500
    })
  } else {
    ElMessage.warning('没有可缩放的数据')
  }
}
```

**添加** 样式:

```scss
.map-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
}

.zoom-to-extent {
  position: absolute;
  bottom: 16px;
  right: 16px;
  z-index: 100;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}
```

## Task 3: 优化矢量图层样式

**File**: `frontend/src/views/map/MapContainer.vue`

**修改** getGeometryColor 函数返回值:

```typescript
function getGeometryColor(geometryType: string | undefined): string {
  if (!geometryType) return '#ff6b00'
  return GEOMETRY_COLORS[geometryType] || '#ff6b00'
}

const GEOMETRY_COLORS: Record<string, string> = {
  Point: '#ff6b00',
  LineString: '#ff6b00',
  Polygon: '#ff6b00',
  MultiPoint: '#ff6b00',
  MultiLineString: '#ff6b00',
  MultiPolygon: '#ff6b00'
}
```

**修改** VectorLayer 样式:

```typescript
const layer = new VectorLayer({
  source,
  style: {
    'fill-color': 'rgba(255, 140, 0, 0.4)',
    'stroke-color': '#ff6b00',
    'stroke-width': 2
  }
})
```

## Task 4: 验证

1. 启动前端开发服务器
2. 访问地图页面 `/map`
3. 验证:
   - 地图占满视口高度
   - 矢量图形颜色清晰可见 (橙色)
   - 点击缩放按钮可以缩放到数据范围
   - 左侧面板正常显示
