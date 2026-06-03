<template>
  <div class="layer-panel">
    <div class="panel-header">
      <el-icon><Menu /></el-icon>
      <span>图层列表</span>
    </div>

    <div class="panel-content">
      <div v-if="loading" class="loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <el-collapse v-else :model-value="['vector', 'image']">
        <el-collapse-item title="矢量图层" name="vector">
          <template #title>
            <span class="collapse-title">矢量图层</span>
          </template>
          <div v-if="layers.length === 0" class="empty-small">
            <span>暂无已发布矢量数据</span>
          </div>
          <el-checkbox-group v-else v-model="selectedLayerIds" @change="handleLayerChange">
            <el-checkbox
              v-for="layer in layers"
              :key="layer.id"
              :value="layer.id"
              class="layer-item"
            >
              <div class="layer-info">
                <div class="layer-color" :style="{ backgroundColor: getGeometryColor(layer.geometryType) }"></div>
                <div class="layer-detail">
                  <div class="layer-name">{{ layer.name }}</div>
                  <div class="layer-type">{{ getGeometryTypeLabel(layer.geometryType) }}</div>
                </div>
              </div>
            </el-checkbox>
          </el-checkbox-group>
        </el-collapse-item>

        <el-collapse-item title="影像图层" name="image">
          <template #title>
            <span class="collapse-title">影像图层</span>
          </template>
          <div v-if="imageLayers.length === 0" class="empty-small">
            <span>暂无已发布影像</span>
          </div>
          <div v-else class="image-layer-list">
            <div
              v-for="layer in imageLayers"
              :key="layer.id"
              class="image-layer-item"
            >
              <el-checkbox
                :value="layer.id"
                v-model="selectedImageLayerIds"
                @change="handleImageLayerChange"
              >
                <span class="image-layer-name">{{ layer.name }}</span>
              </el-checkbox>
              <el-slider
                v-if="selectedImageLayerIds.includes(layer.id)"
                v-model="imageLayerOpacities[layer.id]"
                :min="0"
                :max="100"
                :step="5"
                :show-tooltip="false"
                class="opacity-slider"
                @change="handleImageLayerChange"
              />
              <span v-if="selectedImageLayerIds.includes(layer.id)" class="opacity-value">
                {{ imageLayerOpacities[layer.id] }}%
              </span>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>
    </div>

    <div class="panel-footer">
      <span>矢量: {{ layers.length }} | 影像: {{ imageLayers.length }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Menu, InfoFilled, Loading } from '@element-plus/icons-vue'
import { getPublishedDatasets } from '@/api/dataset'
import { getPublishedImages, getImageWmsUrl, type ImageWmsInfo } from '@/api/image'

interface LayerInfo {
  id: number
  name: string
  geometryType: string
  description?: string
}

interface ImageLayerInfo {
  id: number
  name: string
  wmsUrl: string
  layerName: string
  crs: string
  opacity: number
  visible: boolean
}

const emit = defineEmits<{
  (e: 'layer-change', layers: LayerInfo[]): void
  (e: 'image-layer-change', layers: ImageLayerInfo[]): void
}>()

const layers = ref<LayerInfo[]>([])
const imageLayers = ref<ImageLayerInfo[]>([])
const selectedLayerIds = ref<number[]>([])
const selectedImageLayerIds = ref<number[]>([])
const imageLayerOpacities = ref<Record<number, number>>({})
const loading = ref(false)

const GEOMETRY_COLORS: Record<string, string> = {
  Point: '#FF6B6B',
  LineString: '#4ECDC4',
  Polygon: '#45B7D1',
  MultiPoint: '#FF6B6B',
  MultiLineString: '#4ECDC4',
  MultiPolygon: '#45B7D1'
}

const GEOMETRY_LABELS: Record<string, string> = {
  Point: '点',
  LineString: '线',
  Polygon: '面',
  MultiPoint: '点',
  MultiLineString: '线',
  MultiPolygon: '面'
}

function getGeometryColor(geometryType: string | undefined): string {
  if (!geometryType) return '#45B7D1'
  return GEOMETRY_COLORS[geometryType] || '#45B7D1'
}

function getGeometryTypeLabel(geometryType: string | undefined): string {
  if (!geometryType) return '未知'
  return GEOMETRY_LABELS[geometryType] || geometryType
}

async function fetchPublishedLayers() {
  loading.value = true
  try {
    const response = await getPublishedDatasets()
    if (response.code === 200) {
      layers.value = (response.data || []).map(d => ({
        id: d.id!,
        name: d.name || '',
        geometryType: d.geometryType || 'Point',
        description: d.description
      }))
    } else {
      ElMessage.error('获取图层列表失败')
    }
  } catch (error) {
    ElMessage.error('获取图层列表失败')
  } finally {
    loading.value = false
  }
}

async function fetchPublishedImageLayers() {
  try {
    const response = await getPublishedImages()
    if (response.code === 200) {
      for (const d of response.data || []) {
        try {
          const wmsResponse = await getImageWmsUrl(d.id!)
          if (wmsResponse.code === 200) {
            imageLayers.value.push({
              id: d.id!,
              name: d.name || '',
              ...wmsResponse.data,
              visible: false
            })
            imageLayerOpacities.value[d.id!] = wmsResponse.data.opacity * 100 || 80
          }
        } catch (e) {
          console.error(`Failed to get WMS URL for image ${d.id}:`, e)
        }
      }
    }
  } catch (error) {
    console.error('Failed to fetch image layers:', error)
  }
}

function handleLayerChange() {
  const selectedLayers = layers.value.filter(l => selectedLayerIds.value.includes(l.id))
  emit('layer-change', selectedLayers)
}

function handleImageLayerChange() {
  const selected = imageLayers.value
    .filter(l => selectedImageLayerIds.value.includes(l.id))
    .map(l => ({
      ...l,
      opacity: (imageLayerOpacities.value[l.id] || 80) / 100
    }))
  emit('image-layer-change', selected)
}

onMounted(() => {
  fetchPublishedLayers()
  fetchPublishedImageLayers()
})
</script>

<style scoped>
.layer-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: white;
  border-right: 1px solid #e4e7ed;
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 16px;
  border-bottom: 1px solid #e4e7ed;
  font-weight: 500;
  color: #303133;
}

.panel-content {
  flex: 1;
  overflow-y: auto;
  padding: 0;
}

.loading,
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #909399;
}

.loading .el-icon,
.empty .el-icon {
  margin-bottom: 12px;
}

.empty p {
  margin: 8px 0 0;
  font-size: 14px;
}

.empty-small {
  padding: 12px 0;
  color: #909399;
  font-size: 13px;
  text-align: center;
}

.collapse-title {
  font-size: 14px;
  font-weight: 500;
}

.layer-item {
  display: block;
  margin-bottom: 8px;
}

.layer-item :deep(.el-checkbox__label) {
  padding-left: 8px;
}

.layer-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.layer-color {
  width: 12px;
  height: 12px;
  border-radius: 2px;
  flex-shrink: 0;
}

.layer-detail {
  flex: 1;
  min-width: 0;
}

.layer-name {
  font-size: 14px;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.layer-type {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.panel-footer {
  padding: 12px 16px;
  border-top: 1px solid #e4e7ed;
  font-size: 12px;
  color: #909399;
}

.image-layer-list {
  padding: 4px 0;
}

.image-layer-item {
  margin-bottom: 12px;
}

.image-layer-name {
  font-size: 14px;
  color: #303133;
}

.opacity-slider {
  margin-top: 6px;
  width: calc(100% - 20px);
}

.opacity-value {
  font-size: 11px;
  color: #909399;
  margin-left: 4px;
}

:deep(.el-collapse-item__header) {
  padding: 0 12px;
  font-size: 13px;
}

:deep(.el-collapse-item__content) {
  padding: 8px 12px 12px;
}

:deep(.el-collapse-item__wrap) {
  border-bottom: none;
}

:deep(.el-checkbox-group) {
  padding-top: 4px;
}
</style>
