<template>
  <div class="images-container">
    <el-card class="images-card">
      <template #header>
        <div class="card-header">
          <span class="title">影像管理</span>
          <div>
            <el-button type="primary" @click="handleUpload">
              <el-icon><Upload /></el-icon>
              上传影像
            </el-button>
            <el-button @click="handleBack">
              <el-icon><Back /></el-icon>
              返回首页
            </el-button>
          </div>
        </div>
      </template>

      <div class="search-form">
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="名称">
            <el-input
              v-model="searchForm.name"
              placeholder="请输入名称"
              clearable
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon>
              搜索
            </el-button>
            <el-button @click="handleReset">
              <el-icon><Refresh /></el-icon>
              重置
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
        border
        style="width: 100%"
      >
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column prop="srs" label="坐标系" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'published' ? 'success' : 'info'">
              {{ row.status === 'published' ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="上传时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="切片状态" width="150">
          <template #default="{ row }">
            <template v-if="row.tileStatus === 'completed' || row.cacheSeedStatus === 'seeded'">
              <el-tag type="success">已完成 ({{ row.tileProgress }}%)</el-tag>
              <el-button
                type="warning"
                link
                size="small"
                style="margin-left: 4px"
                @click="handleRetile(row)"
              >
                重新切片
              </el-button>
            </template>
            <el-progress
              v-else-if="row.tileStatus === 'processing'"
              :percentage="row.tileProgress || 0"
              :stroke-width="10"
            />
            <el-tag v-else-if="row.tileStatus === 'failed'" type="danger">失败</el-tag>
            <el-tag v-else type="info">待处理</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">查看</el-button>
            <el-dropdown @command="(cmd) => handleDownloadCommand(row, cmd)">
              <el-button type="warning" link>
                下载<el-icon><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="original">下载原始影像</el-dropdown-item>
                  <el-dropdown-item v-if="row.status === 'published'" command="tiles">下载切片包</el-dropdown-item>
                  <el-dropdown-item command="metadata">下载元数据</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button
              type="success"
              link
              @click="handlePublish(row)"
            >
              {{ row.status === 'published' ? '取消发布' : '发布' }}
            </el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      title="上传影像"
      width="600px"
      @close="handleDialogClose"
    >
      <el-upload
        ref="uploadRef"
        class="upload-demo"
        drag
        :auto-upload="false"
        :limit="1"
        :on-change="handleFileChange"
        :on-exceed="handleExceed"
        :on-remove="handleFileRemove"
        accept=".tif,.tiff"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">
          拖拽文件到此处或 <em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            支持 GeoTIFF (.tif/.tiff) 格式
          </div>
        </template>
      </el-upload>

      <div v-if="uploadFile" class="file-info">
        <el-form :model="form" label-width="80px" style="margin-top: 20px">
          <el-form-item label="名称">
            <el-input v-model="form.name" placeholder="请输入影像名称" />
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入描述" />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="uploadLoading"
          :disabled="!uploadFile"
          @click="handleSubmit"
        >
          上传
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="tileDialog.visible"
      title="下载切片包"
      width="420px"
    >
      <el-form label-width="100px">
        <el-form-item label="起始级别">
          <el-input-number v-model="tileDialog.zoomStart" :min="0" :max="18" />
        </el-form-item>
        <el-form-item label="结束级别">
          <el-input-number v-model="tileDialog.zoomStop" :min="0" :max="18" />
        </el-form-item>
        <el-form-item label="预估瓦片数">
          <span>{{ tileDialog.estimatedTiles }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tileDialog.visible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="tileDialog.loading"
          @click="confirmDownloadTiles"
        >
          下载
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type UploadInstance } from 'element-plus'
import { UploadFilled, ArrowDown } from '@element-plus/icons-vue'
import { getImages, uploadImage, publishImage, unpublishImage, retileImage, deleteImage, getTilingStatus, getImageDownloadUrl, downloadTilePackage, getImageMetadata, type ImageDataset, type TilePackageRequest } from '@/api/image'

const router = useRouter()

const loading = ref(false)
const tableData = ref<ImageDataset[]>([])
const dialogVisible = ref(false)
const uploadRef = ref<UploadInstance>()
const uploadLoading = ref(false)
const uploadFile = ref<File | null>(null)
const tilingStatusTimer = ref<NodeJS.Timeout | null>(null)

const searchForm = reactive({
  name: ''
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const form = reactive({
  name: '',
  description: ''
})

const tileDialog = reactive({
  visible: false,
  loading: false,
  zoomStart: 0,
  zoomStop: 14,
  estimatedTiles: '—',
  currentRow: null as ImageDataset | null
})

const fetchImages = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize,
      name: searchForm.name || undefined
    }
    const res = await getImages(params)
    tableData.value = res.data.records
    pagination.total = res.data.total
  } catch (error) {
    console.error('Failed to fetch images:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  fetchImages()
}

const handleReset = () => {
  searchForm.name = ''
  pagination.page = 1
  fetchImages()
}

const handleSizeChange = () => {
  pagination.page = 1
  fetchImages()
}

const handleCurrentChange = () => {
  fetchImages()
}

const handleBack = () => {
  router.push('/home')
}

const handleUpload = () => {
  form.name = ''
  form.description = ''
  uploadFile.value = null
  dialogVisible.value = true
}

const handleView = (row: ImageDataset) => {
  ElMessage.info(`查看影像: ${row.name}`)
}

const handleDownloadCommand = async (row: ImageDataset, command: string) => {
  if (command === 'original') {
    try {
      const res = await getImageDownloadUrl(row.id!)
      window.open(res.data.downloadUrl, '_blank')
    } catch (error: any) {
      console.error('Download failed:', error)
      ElMessage.error(error.message || '下载失败')
    }
  } else if (command === 'tiles') {
    handleDownloadTiles(row)
  } else if (command === 'metadata') {
    try {
      const res = await getImageMetadata(row.id!)
      const metadata = res.data
      const blob = new Blob([JSON.stringify(metadata, null, 2)], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `metadata-${row.id}.json`
      link.click()
      URL.revokeObjectURL(url)
      ElMessage.success('元数据下载完成')
    } catch (error: any) {
      console.error('Download metadata failed:', error)
      ElMessage.error(error.message || '下载元数据失败')
    }
  }
}

const handleDownloadTiles = (row: ImageDataset) => {
  tileDialog.currentRow = row
  tileDialog.zoomStart = 0
  tileDialog.zoomStop = 14
  tileDialog.estimatedTiles = '—'
  tileDialog.visible = true
}

const confirmDownloadTiles = async () => {
  if (!tileDialog.currentRow) return

  tileDialog.loading = true
  try {
    const res = await downloadTilePackage(tileDialog.currentRow.id!, {
      zoomStart: tileDialog.zoomStart,
      zoomStop: tileDialog.zoomStop
    }) as any
    const blob = res instanceof Blob ? res : (res as any).data
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${tileDialog.currentRow.name}_tiles_z${tileDialog.zoomStart}-z${tileDialog.zoomStop}.zip`
    link.click()
    URL.revokeObjectURL(url)
    tileDialog.visible = false
    ElMessage.success('切片包下载完成')
  } catch (error: any) {
    console.error('Tile package download failed:', error)
    ElMessage.error(error.message || '下载切片包失败')
  } finally {
    tileDialog.loading = false
  }
}

const handlePublish = async (row: ImageDataset) => {
  try {
    if (row.status === 'published') {
      await unpublishImage(row.id!)
      ElMessage.success('取消发布成功')
    } else {
      await publishImage(row.id!)
      ElMessage.success('发布成功')
    }
    fetchImages()
  } catch (error: any) {
    console.error('Publish failed:', error)
    ElMessage.error(error.message || '操作失败')
  }
}

const handleRetile = async (row: ImageDataset) => {
  try {
    await ElMessageBox.confirm('确定要重新生成切片吗？', '提示', {
      type: 'warning'
    })
    await retileImage(row.id!)
    ElMessage.success('重新切片任务已提交')
    fetchImages()
  } catch (error: any) {
    console.error('Retile failed:', error)
    ElMessage.error(error.message || '操作失败')
  }
}

const handleDelete = async (row: ImageDataset) => {
  try {
    await ElMessageBox.confirm('确定要删除该影像吗？', '提示', {
      type: 'warning'
    })
    await deleteImage(row.id!)
    ElMessage.success('删除成功')
    fetchImages()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Delete failed:', error)
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const handleDialogClose = () => {
  uploadFile.value = null
}

const handleFileChange = (file: any) => {
  const rawFile = file.raw as File
  if (!rawFile) return
  uploadFile.value = rawFile
  if (!form.name) {
    form.name = rawFile.name.replace(/\.[^/.]+$/, '')
  }
}

const handleExceed = () => {
  ElMessage.warning('只能上传一个文件，请先移除当前文件')
}

const handleFileRemove = () => {
  uploadFile.value = null
}

const handleSubmit = async () => {
  if (!uploadFile.value) return

  uploadLoading.value = true
  try {
    await uploadImage(uploadFile.value, form.name, form.description)
    ElMessage.success('上传成功')
    dialogVisible.value = false
    fetchImages()
  } catch (error: any) {
    console.error('Upload failed:', error)
    ElMessage.error(error.message || '上传失败')
  } finally {
    uploadLoading.value = false
  }
}

const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

const startTilingStatusPolling = () => {
  stopTilingStatusPolling()
  tilingStatusTimer.value = setInterval(async () => {
    const processingRows = tableData.value.filter(row => row.tileStatus === 'processing')
    if (processingRows.length === 0) {
      stopTilingStatusPolling()
      return
    }
    for (const row of processingRows) {
      try {
        const res = await getTilingStatus(row.id!)
        if (res.data) {
          row.tileStatus = res.data.status
          row.tileProgress = res.data.progress
        }
      } catch (error) {
        console.error('Failed to fetch tiling status:', error)
      }
    }
  }, 5000)
}

const stopTilingStatusPolling = () => {
  if (tilingStatusTimer.value) {
    clearInterval(tilingStatusTimer.value)
    tilingStatusTimer.value = null
  }
}

onMounted(() => {
  fetchImages()
  startTilingStatusPolling()
})

onUnmounted(() => {
  stopTilingStatusPolling()
})
</script>

<style lang="scss" scoped>
.images-container {
  display: flex;
  justify-content: center;
  padding: 20px;
  min-height: 100vh;
  background: #f5f7fa;
}

.images-card {
  width: 100%;
  max-width: 1200px;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .title {
      font-size: 18px;
      font-weight: 600;
    }
  }

  .search-form {
    margin-bottom: 20px;
  }

  .pagination-container {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;
  }
}

.upload-demo {
  width: 100%;
}

.file-info {
  margin-top: 20px;
}
</style>
