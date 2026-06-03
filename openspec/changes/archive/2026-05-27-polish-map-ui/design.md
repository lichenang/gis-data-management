# Design: polish-map-ui

## Overview

对地图页面进行 UI 优化，包括布局调整、样式优化和交互增强。

## Changes

### Change 1: 页面布局调整

**File**: `frontend/src/views/map/index.vue`

修改页面布局结构，使地图区域占满视口:

```vue
<template>
  <div class="map-viewer">
    <!-- 可折叠侧边栏 -->
    <el-aside :width="sidebarCollapsed ? '0' : '280px'" class="map-aside">
      <LayerPanel @layer-change="handleLayerChange" />
    </el-aside>
    
    <!-- 地图主区域 -->
    <el-main class="map-main">
      <MapContainer />
    </el-main>
  </div>
</template>
```

```scss
.map-viewer {
  display: flex;
  height: calc(100vh - 60px);  // 减去顶部导航栏高度
  width: 100%;
}

.map-aside {
  transition: width 0.3s;
  overflow: hidden;
}
```

### Change 2: 矢量图层样式优化

**File**: `frontend/src/views/map/MapContainer.vue`

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

### Change 3: 悬停高亮样式

添加条件样式实现悬停效果:

```typescript
import { asArray } from 'ol/color'

function getStyle(feature: Feature, resolution: number) {
  const geometryType = feature.getGeometry()?.getType()
  const isHovered = feature.get('hovered')
  
  return new Style({
    fill: new Fill({
      color: isHovered 
        ? 'rgba(255, 140, 0, 0.6)' 
        : 'rgba(255, 140, 0, 0.4)'
    }),
    stroke: new Stroke({
      color: isHovered ? '#ff8800' : '#ff6b00',
      width: isHovered ? 3 : 2
    })
  })
}
```

### Change 4: 缩放到数据按钮

在 MapContainer 中添加浮动按钮:

```vue
<template>
  <div class="map-wrapper">
    <div ref="mapContainer" class="map-container"></div>
    <el-button 
      class="zoom-to-fit" 
      :icon="Aim" 
      circle
      @click="zoomToData"
    />
  </div>
</template>

<script>
function zoomToData() {
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
  
  if (combinedExtent) {
    map.value.getView().fit(combinedExtent, {
      padding: [50, 50, 50, 50],
      maxZoom: 15,
      duration: 500
    })
  }
}
</script>

<style scoped>
.zoom-to-fit {
  position: absolute;
  bottom: 20px;
  right: 20px;
  z-index: 100;
}
</style>
```

## Implementation Order

1. 修改 index.vue 布局
2. 修改 MapContainer.vue 添加按钮
3. 调整矢量图层样式
4. 添加悬停高亮逻辑 (可选)
