<template>
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
  <div v-if="popupVisible" :style="popupStyle" class="feature-popup">
    <div class="popup-header">
      <span>要素属性</span>
      <el-button text @click="popupVisible = false" :icon="Close" size="small" />
    </div>
    <div class="popup-content">
      <div v-for="(value, key) in selectedFeatureProperties" :key="key" class="popup-row">
        <span class="popup-key">{{ formatKey(key) }}:</span>
        <span class="popup-value">{{ value }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import Map from 'ol/Map'
import View from 'ol/View'
import TileLayer from 'ol/layer/Tile'
import VectorLayer from 'ol/layer/Vector'
import ImageLayer from 'ol/layer/Image'
import { type VectorLayer as VectorLayerType, type ImageLayer as ImageLayerType } from 'ol/layer'
import OSM from 'ol/source/OSM'
import VectorSource from 'ol/source/Vector'
import TileWMS from 'ol/source/TileWMS'
import GeoJSON from 'ol/format/GeoJSON'
import { Select } from 'ol/interaction'
import { click as clickCondition } from 'ol/events/condition'
import { Close, Aim } from '@element-plus/icons-vue'
import type { Style } from 'ol/style'

interface LayerInfo {
  id: number
  name: string
  geometryType: string
}

interface ImageLayerInfo {
  id: number
  name: string
  wmsUrl: string
  layerName: string
  crs: string
  opacity: number
  extent?: [number, number, number, number]
}

const props = defineProps<{
  layers: LayerInfo[]
  imageLayers: ImageLayerInfo[]
}>()

const emit = defineEmits<{
  (e: 'feature-click', properties: Record<string, any>): void
}>()

const mapContainer = ref<HTMLElement>()
const map = ref<Map | null>(null)
const vectorLayers = ref<Record<number, VectorLayerType>>({})
const imageLayersMap = ref<Record<number, {
  layer: ImageLayerType
  source: TileWMS
}>>({})
const selectInteraction = ref<Select | null>(null)

const popupVisible = ref(false)
const popupPosition = ref({ x: 0, y: 0 })
const selectedFeatureProperties = ref<Record<string, any>>({})

const popupStyle = ref({
  position: 'absolute' as const,
  left: '0px',
  top: '0px',
  zIndex: 1000
})

const GEOMETRY_COLORS: Record<string, string> = {
  Point: '#ff6b00',
  LineString: '#ff6b00',
  Polygon: '#ff6b00',
  MultiPoint: '#ff6b00',
  MultiLineString: '#ff6b00',
  MultiPolygon: '#ff6b00'
}

function getGeometryColor(geometryType: string | undefined): string {
  if (!geometryType) return '#ff6b00'
  return GEOMETRY_COLORS[geometryType] || '#ff6b00'
}

function initMap() {
  if (!mapContainer.value) return

  const baseLayer = new TileLayer({
    source: new OSM()
  })

  map.value = new Map({
    target: mapContainer.value,
    layers: [baseLayer],
    view: new View({
      center: [116.4, 39.9],
      zoom: 10,
      projection: 'EPSG:4326'
    })
  })

  window.__map__ = map.value

  const select = new Select({
    condition: clickCondition,
    style: undefined
  })

  select.on('select', (event) => {
    const features = event.target.getFeatures()
    if (features.getLength() > 0) {
      const feature = features.item(0)
      const props = feature.getProperties()
      selectedFeatureProperties.value = props

      const coordinate = map.value?.getCoordinateFromPixel(event.mapBrowserEvent.pixel)
      if (coordinate) {
        popupPosition.value = {
          x: event.mapBrowserEvent.pixel[0] + 10,
          y: event.mapBrowserEvent.pixel[1] - 10
        }
        popupStyle.value = {
          position: 'absolute',
          left: `${popupPosition.value.x}px`,
          top: `${popupPosition.value.y}px`,
          zIndex: 1000
        }
        popupVisible.value = true
      }

      emit('feature-click', props)
    } else {
      popupVisible.value = false
    }
  })

  map.value.addInteraction(select)
  selectInteraction.value = select
}

function loadLayer(layerInfo: LayerInfo) {
  console.log('[MapContainer] loadLayer called:', layerInfo.id, 'map:', !!map.value)

  if (!map.value) {
    console.warn('[MapContainer] Map not ready, skipping layer:', layerInfo.id)
    return
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const source: any = new VectorSource({
    projection: 'EPSG:4326',
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    loader: (extent: any, resolution: any, projection: any) => {
      const token = localStorage.getItem('access_token')
      const url = `/api/v1/datasets/${layerInfo.id}/geojson`

      fetch(url, {
        headers: {
          'Authorization': token ? `Bearer ${token}` : ''
        }
      })
        .then(response => {
          if (!response.ok) {
            throw new Error(`HTTP ${response.status}`)
          }
          return response.text()
        })
        .then(text => {
          const jsonData = JSON.parse(text)

          let geojson
          try {
            geojson = JSON.parse(jsonData.data)
          } catch (e) {
            console.error('[MapContainer] Failed to parse GeoJSON data:', e)
            return
          }

          const features = new GeoJSON().readFeatures(geojson, {
            featureProjection: 'EPSG:4326'
          })
          source.addFeatures(features)

          console.log('[MapContainer] Loaded features:', features.length)

          if (map.value) {
            const extent = source.getExtent()
            const isValidExtent = extent &&
              isFinite(extent[0]) && isFinite(extent[1]) &&
              isFinite(extent[2]) && isFinite(extent[3])

            if (isValidExtent) {
              map.value.getView().fit(extent, {
                padding: [50, 50, 50, 50],
                maxZoom: 15,
                duration: 500
              })
            } else {
              console.warn('[MapContainer] Invalid extent, skipping fit:', extent)
            }
          }
        })
        .catch(error => {
          console.error('[MapContainer] Failed to load GeoJSON:', error)
        })
    },
    format: new GeoJSON()
  } as any)

  const layer = new VectorLayer({
    source,
    style: {
      'fill-color': 'rgba(255, 140, 0, 0.4)',
      'stroke-color': '#ff6b00',
      'stroke-width': 2
    },
    properties: { layerId: layerInfo.id }
  })

  map.value.addLayer(layer)
  vectorLayers.value[layerInfo.id] = layer
}

function unloadLayer(layerId: number) {
  if (!map.value) return

  const layer = vectorLayers.value[layerId]
  if (layer) {
    map.value.removeLayer(layer)
    delete vectorLayers.value[layerId]
  }
}

function loadImageLayer(imageInfo: ImageLayerInfo) {
  if (!map.value) return

  const wmsSource = new TileWMS({
    url: imageInfo.wmsUrl,
    params: {
      'LAYERS': imageInfo.layerName,
      'CRS': imageInfo.crs || 'EPSG:4326'
    },
    projection: imageInfo.crs || 'EPSG:4326',
    serverType: 'geoserver',
    transition: 0
  })

  const imageLayer = new TileLayer({
    source: wmsSource,
    opacity: imageInfo.opacity,
    properties: { layerId: imageInfo.id, type: 'image' }
  })

  map.value.addLayer(imageLayer)
  imageLayersMap.value[imageInfo.id] = {
    layer: imageLayer,
    source: wmsSource
  }

  // 缩放到影像范围 - 简化版
  // 后端返回的 extent 已经是 EPSG:4326 经纬度坐标，直接使用
  if (imageInfo.extent && imageInfo.extent.length === 4) {
    map.value.getView().fit(imageInfo.extent, {
      padding: [50, 50, 50, 50],
      maxZoom: 15,
      duration: 500
    })
  }
}

function unloadImageLayer(imageId: number) {
  if (!map.value) return

  const entry = imageLayersMap.value[imageId]
  if (entry) {
    map.value.removeLayer(entry.layer)
    delete imageLayersMap.value[imageId]
  }
}

function updateImageOpacity(imageId: number, opacity: number) {
  const entry = imageLayersMap.value[imageId]
  if (entry) {
    entry.layer.setOpacity(opacity)
  }
}

function zoomToExtent() {
  let combinedExtent: [number, number, number, number] | null = null

  for (const layer of Object.values(vectorLayers.value)) {
    const source = layer.getSource()
    const extent = source?.getExtent()
    if (extent && isFinite(extent[0]) && isFinite(extent[1]) &&
        isFinite(extent[2]) && isFinite(extent[3])) {
      if (!combinedExtent) {
        combinedExtent = [...extent] as [number, number, number, number]
      } else {
        import('ol/extent').then(ext => {
          ext.extend(combinedExtent!, extent)
        })
      }
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

watch(() => props.layers, (newLayers, oldLayers) => {
  const oldIds = new Set((oldLayers || []).map(l => l.id))
  const newIds = new Set(newLayers.map(l => l.id))

  newLayers.forEach(layer => {
    if (!(layer.id in vectorLayers.value)) {
      loadLayer(layer)
    }
  })

  oldIds.forEach(id => {
    if (!newIds.has(id)) {
      unloadLayer(id)
    }
  })
}, { deep: true })

watch(() => props.imageLayers, (newLayers, oldLayers) => {
  const oldIds = new Set((oldLayers || []).map(l => l.id))
  const newIds = new Set(newLayers.map(l => l.id))

  newLayers.forEach(layer => {
    if (!(layer.id in imageLayersMap.value)) {
      loadImageLayer(layer)
    } else {
      updateImageOpacity(layer.id, layer.opacity)
    }
  })

  oldIds.forEach(id => {
    if (!newIds.has(id)) {
      unloadImageLayer(id)
    }
  })
}, { deep: true })

function formatKey(key: string): string {
  if (key === 'geom' || key === 'geometry') return ''
  return key
}

onMounted(() => {
  initMap()

  if (props.layers.length > 0) {
    props.layers.forEach(layer => {
      if (!(layer.id in vectorLayers.value)) {
        loadLayer(layer)
      }
    })
  }

  if (props.imageLayers.length > 0) {
    props.imageLayers.forEach(layer => {
      if (!(layer.id in imageLayersMap.value)) {
        loadImageLayer(layer)
      }
    })
  }
})

onUnmounted(() => {
  if (map.value) {
    map.value.setTarget(undefined)
  }
})
</script>

<style scoped>
.map-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
}

.map-container {
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

.feature-popup {
  background: white;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  min-width: 200px;
  max-width: 300px;
}

.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-bottom: 1px solid #eee;
  font-weight: 500;
  background: #f5f7fa;
  border-radius: 4px 4px 0 0;
}

.popup-content {
  padding: 12px;
  max-height: 300px;
  overflow-y: auto;
}

.popup-row {
  display: flex;
  margin-bottom: 6px;
  font-size: 13px;
}

.popup-key {
  color: #909399;
  min-width: 80px;
  flex-shrink: 0;
}

.popup-value {
  color: #303133;
  word-break: break-all;
}
</style>
