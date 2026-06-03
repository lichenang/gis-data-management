<template>
  <div class="map-page">
    <div class="breadcrumb-bar">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>地图查看</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="map-viewer">
      <el-container>
      <el-aside width="280px" class="map-aside">
        <LayerPanel
          @layer-change="handleLayerChange"
          @image-layer-change="handleImageLayerChange"
        />
      </el-aside>

      <el-main class="map-main">
        <MapContainer
          :layers="selectedLayers"
          :image-layers="selectedImageLayers"
          @feature-click="handleFeatureClick"
        />
      </el-main>
      </el-container>
    </div>

    <el-dialog v-model="featureDialogVisible" title="要素属性" width="400px">
      <div v-if="selectedFeature" class="feature-properties">
        <div v-for="(value, key) in selectedFeature" :key="key" class="property-row">
          <span class="property-key">{{ formatKey(key) }}:</span>
          <span class="property-value">{{ value }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="featureDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import LayerPanel from './LayerPanel.vue'
import MapContainer from './MapContainer.vue'

interface LayerInfo {
  id: number
  name: string
  geometryType: string
}

interface ImageLayerDisplayInfo {
  id: number
  name: string
  wmsUrl: string
  layerName: string
  crs: string
  opacity: number
}

const selectedLayers = ref<LayerInfo[]>([])
const selectedImageLayers = ref<ImageLayerDisplayInfo[]>([])
const featureDialogVisible = ref(false)
const selectedFeature = ref<Record<string, any> | null>(null)

function handleLayerChange(layers: LayerInfo[]) {
  selectedLayers.value = layers
}

function handleImageLayerChange(layers: ImageLayerDisplayInfo[]) {
  selectedImageLayers.value = layers
}

function handleFeatureClick(properties: Record<string, any>) {
  selectedFeature.value = properties
}

function formatKey(key: string): string {
  if (key === 'geom' || key === 'geometry') return ''
  return key
}
</script>

<style scoped>
.map-page {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100vh;
  overflow: hidden;
}

.breadcrumb-bar {
  height: 40px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
}

.map-viewer {
  flex: 1;
  overflow: hidden;
}

.map-viewer .el-container {
  height: 100%;
}

.map-aside {
  padding: 0;
  overflow: hidden;
}

.map-main {
  padding: 0;
  overflow: hidden;
  flex: 1;
  display: flex;
}

.map-main > * {
  flex: 1;
  height: 100%;
}

.feature-properties {
  max-height: 400px;
  overflow-y: auto;
}

.property-row {
  display: flex;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.property-row:last-child {
  border-bottom: none;
}

.property-key {
  flex-shrink: 0;
  width: 120px;
  color: #909399;
  font-size: 14px;
}

.property-value {
  flex: 1;
  color: #303133;
  font-size: 14px;
  word-break: break-all;
}
</style>
