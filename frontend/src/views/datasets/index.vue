<template>
  <div class="datasets-container">
    <el-card class="datasets-card">
      <template #header>
        <div class="card-header">
          <span class="title">{{ pageTitle }}</span>
          <div>
            <el-button type="primary" @click="handleCreate">
              <el-icon><Plus /></el-icon>
              新建数据集
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
          <el-form-item label="类型">
            <el-select v-model="searchForm.type" placeholder="请选择" clearable>
              <el-option label="矢量" value="vector" />
              <el-option label="影像" value="raster" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="searchForm.status" placeholder="请选择" clearable>
              <el-option label="草稿" value="draft" />
              <el-option label="已发布" value="published" />
            </el-select>
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
        <el-table-column prop="name" label="名称" min-width="150" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.type === 'vector' ? 'success' : 'warning'">
              {{ row.type === 'vector' ? '矢量' : '影像' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="geometryType" label="几何类型" width="120">
          <template #default="{ row }">
            {{ row.geometryType || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="srs" label="坐标系" width="120" />
        <el-table-column prop="featureCount" label="要素数" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'published' ? 'success' : 'info'">
              {{ row.status === 'published' ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button
              type="success"
              link
              @click="handlePublish(row)"
            >
              {{ row.status === 'published' ? '取消发布' : '发布' }}
            </el-button>
            <el-dropdown @command="(format) => handleExport(row, format)">
              <el-button type="warning" link>
                导出<el-icon><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="row.type === 'vector'" command="geojson">GeoJSON</el-dropdown-item>
                  <el-dropdown-item v-if="row.type === 'vector'" command="shapefile">Shapefile (ZIP)</el-dropdown-item>
                  <el-dropdown-item v-if="row.type === 'vector'" command="kml">KML</el-dropdown-item>
                  <el-dropdown-item v-if="row.type === 'vector'" command="csv">CSV</el-dropdown-item>
                  <el-dropdown-item v-if="row.type === 'raster'" command="geotiff">GeoTIFF</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
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
      :title="dialogTitle"
      width="700px"
      @close="handleDialogClose"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-row :gutter="20">
          <el-col :span="16">
            <el-form-item label="名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入数据集名称" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="类型" prop="type" v-if="isEdit">
              <el-select v-model="form.type" style="width: 100%" :disabled="isEdit" @change="handleTypeChange">
                <el-option label="矢量" value="vector" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入描述" />
        </el-form-item>

        <el-divider content-position="left">
          <span>📁 上传空间数据（可选）</span>
        </el-divider>

        <transition name="el-zoom-in-top">
          <div v-if="form.type === 'vector'" class="upload-section">
            <div v-if="hasExistingFile && !uploadFile" class="existing-file-notice">
              <el-alert type="success" :closable="false" show-icon>
                <template #default>
                  <span>已上传空间数据文件（可重新上传替换）</span>
                </template>
              </el-alert>
            </div>
            <el-upload
              ref="uploadRef"
              class="upload-demo"
              drag
              :auto-upload="false"
              :limit="1"
              :on-change="handleFileChange"
              :on-exceed="handleExceed"
              :on-remove="handleFileRemove"
              accept=".geojson,.json,.shp,.zip,.kml,.kmz,.gml,.gpx,.csv,.wkt,.topojson"
            >
              <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
              <div class="el-upload__text">
                拖拽文件到此处或 <em>点击上传</em>
              </div>
              <template #tip>
                <div class="el-upload__tip">
                  支持格式：GeoJSON、Shapefile、KML/KMZ、GML、GPX、CSV、WKT、TopoJSON
                </div>
              </template>
            </el-upload>

            <div v-if="uploadFile" class="file-info">
              <el-form-item label="坐标系" style="margin-top: 16px">
                <el-select v-model="form.srs" style="width: 200px">
                  <el-option label="WGS84 (EPSG:4326)" value="EPSG:4326" />
                  <el-option label="Web墨卡托 (EPSG:3857)" value="EPSG:3857" />
                </el-select>
              </el-form-item>

              <div v-if="isCrsDetected && parseResult?.srs" class="crs-detected-info">
                <el-alert type="success" :closable="false" show-icon>
                  <template #default>
                    <span>已自动识别: {{ parseResult.srs }}</span>
                  </template>
                </el-alert>
              </div>

              <div v-if="missingPrj" class="missing-prj-warning">
                <el-alert type="warning" :closable="false" show-icon>
                  <template #default>
                    <span>请手动选择原始投影</span>
                  </template>
                </el-alert>
                <el-form-item label="源坐标系" style="margin-top: 12px" :rules="sourceSrsRules">
                  <el-select v-model="form.sourceSrs" style="width: 200px" placeholder="请选择">
                    <el-option label="CGCS2000 (EPSG:4490)" value="EPSG:4490" />
                    <el-option label="WGS84 (EPSG:4326)" value="EPSG:4326" />
                    <el-option label="Web墨卡托 (EPSG:3857)" value="EPSG:3857" />
                    <el-option label="北京54 (EPSG:2433)" value="EPSG:2433" />
                    <el-option label="西安80 (EPSG:2443)" value="EPSG:2443" />
                  </el-select>
                </el-form-item>
              </div>
            </div>

            <div v-if="parseResult && parseResult.success" class="parse-result">
              <el-alert
                title="文件解析成功"
                type="success"
                :closable="false"
                show-icon
              >
                <template #default>
                  <div class="parse-info">
                    <span>几何类型: {{ parseResult.geometryType }}</span>
                    <span class="ml-4">要素数量: {{ parseResult.featureCount }}</span>
                  </div>
                </template>
              </el-alert>
            </div>
          </div>

          <div v-else-if="form.type === 'raster'" class="upload-section">
            <el-upload
              ref="rasterUploadRef"
              class="upload-demo"
              drag
              :auto-upload="false"
              :limit="1"
              :on-change="handleRasterFileChange"
              :on-exceed="handleExceed"
              :on-remove="handleRasterFileRemove"
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
          </div>
        </transition>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :loading="loading" @click="handleCreateOnly">
          仅创建
        </el-button>
        <el-button
          v-if="form.type === 'vector'"
          type="primary"
          :loading="importLoading"
          :disabled="!uploadFile || (missingPrj && !form.sourceSrs)"
          @click="handleImport"
        >
          导入并创建
        </el-button>
        <el-button
          v-else
          type="primary"
          :loading="rasterUploadLoading"
          :disabled="!rasterFile"
          @click="handleRasterSubmit"
        >
          上传影像
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadInstance } from 'element-plus'
import { UploadFilled, ArrowDown } from '@element-plus/icons-vue'
import { uploadImage, publishImage, unpublishImage } from '@/api/image'
import { getDatasets, createDataset, updateDataset, deleteDataset, parseDatasetFile, importDataset, publishDataset, unpublishDataset, type Dataset, type GisDataParseResult } from '@/api/dataset'

const router = useRouter()
const route = useRoute()

const pageTitle = computed(() => {
  if (route.path === '/datasets/vector') return '矢量数据集'
  if (route.path === '/datasets/raster') return '影像数据集'
  return '数据集管理'
})

const loading = ref(false)
const tableData = ref<Dataset[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('新建数据集')
const isEdit = ref(false)
const currentId = ref<number>()
const uploadRef = ref<UploadInstance>()

const searchForm = reactive({
  name: '',
  type: '',
  status: ''
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const form = reactive<Dataset>({
  name: '',
  description: '',
  type: 'vector',
  srs: 'EPSG:4326',
  sourceSrs: ''
})

const formRef = ref<FormInstance>()

const hasExistingFile = ref(false)

const formRules: FormRules = {
  name: [{ required: true, message: '请输入数据集名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

const uploadFile = ref<File | null>(null)
const parseResult = ref<GisDataParseResult | null>(null)
const importLoading = ref(false)
const rasterUploadRef = ref<UploadInstance>()
const rasterFile = ref<File | null>(null)
const rasterUploadLoading = ref(false)

const isShapefile = computed(() => {
  if (!uploadFile.value) return false
  const name = uploadFile.value.name.toLowerCase()
  return name.endsWith('.shp') || name.endsWith('.zip')
})

const isCrsDetected = computed(() => parseResult.value?.crsDetected !== false)

const missingPrj = computed(() => {
  if (!isShapefile.value) return false
  return !isCrsDetected.value
})

const sourceSrsRules = computed(() => {
  if (!missingPrj.value) return []
  return [{ required: true, message: '请选择源坐标系', trigger: 'change' }]
})

const fetchDatasets = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize,
      name: searchForm.name || undefined,
      type: searchForm.type || undefined,
      status: searchForm.status || undefined
    }
    const res = await getDatasets(params)
    tableData.value = res.data.records
    pagination.total = res.data.total
  } catch (error) {
    console.error('Failed to fetch datasets:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  fetchDatasets()
}

const handleReset = () => {
  searchForm.name = ''
  searchForm.type = ''
  searchForm.status = ''
  pagination.page = 1
  fetchDatasets()
}

const handleSizeChange = () => {
  pagination.page = 1
  fetchDatasets()
}

const handleCurrentChange = () => {
  fetchDatasets()
}

const handleBack = () => {
  router.push('/home')
}

const handleCreate = () => {
  dialogTitle.value = '新建数据集'
  isEdit.value = false
  form.name = ''
  form.description = ''
  form.type = 'vector'
  form.srs = 'EPSG:4326'
  form.sourceSrs = ''
  currentId.value = undefined
  uploadFile.value = null
  parseResult.value = null
  rasterFile.value = null
  hasExistingFile.value = false
  dialogVisible.value = true
}

const handleEdit = (row: Dataset) => {
  dialogTitle.value = '编辑数据集'
  isEdit.value = true
  currentId.value = row.id
  form.name = row.name || ''
  form.description = row.description || ''
  form.type = row.type || 'vector'
  form.srs = row.srs || 'EPSG:4326'
  form.sourceSrs = ''
  uploadFile.value = null
  parseResult.value = null
  rasterFile.value = null
  hasExistingFile.value = !!(row.minioKey || row.filePath)
  dialogVisible.value = true
}

const handleTypeChange = () => {
  uploadFile.value = null
  parseResult.value = null
  rasterFile.value = null
}

const handleDelete = async (row: Dataset) => {
  try {
    await ElMessageBox.confirm('确定要删除该数据集吗？', '提示', {
      type: 'warning'
    })
    await deleteDataset(row.id!)
    ElMessage.success('删除成功')
    fetchDatasets()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Delete failed:', error)
    }
  }
}

const handlePublish = async (row: Dataset) => {
  try {
    const isPublished = row.status === 'published'
    const action = isPublished ? '取消发布' : '发布'
    await ElMessageBox.confirm(`确定要${action}该数据集吗？`, '提示', {
      type: 'warning'
    })
    const isRaster = row.type === 'raster'
    if (isPublished) {
      isRaster ? await unpublishImage(row.id!) : await unpublishDataset(row.id!)
      ElMessage.success('取消发布成功')
    } else {
      isRaster ? await publishImage(row.id!) : await publishDataset(row.id!)
      ElMessage.success('发布成功')
    }
    fetchDatasets()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Publish failed:', error)
    }
  }
}

const baseURL = (import.meta as any).env?.VITE_API_BASE_URL || 'http://localhost:8088/api/v1'

const handleExport = async (row: Dataset, format: string) => {
  const token = localStorage.getItem('access_token')
  const url = `${baseURL}/datasets/${row.id}/export?format=${format}`
  try {
    const response = await fetch(url, {
      headers: token ? { Authorization: `Bearer ${token}` } : {}
    })
    if (!response.ok) {
      ElMessage.error(`导出失败 (${response.status})`)
      return
    }
    const blob = await response.blob()
    const blobUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = blobUrl
    const extMap: Record<string, string> = {
      geojson: 'geojson',
      kml: 'kml',
      shapefile: 'shp.zip',
      csv: 'csv',
      geotiff: 'tiff'
    }
    link.download = `${row.name}.${extMap[format] || format}`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(blobUrl)
  } catch (e) {
    ElMessage.error('导出请求失败')
  }
}

const handleDialogClose = () => {
  formRef.value?.resetFields()
  uploadFile.value = null
  parseResult.value = null
  rasterFile.value = null
  hasExistingFile.value = false
  form.sourceSrs = ''
}

const handleFileChange = async (file: any) => {
  const rawFile = file.raw as File
  if (!rawFile) return

  uploadFile.value = rawFile

  try {
    ElMessage.info('正在解析文件，请稍候...')
    const res = await parseDatasetFile(rawFile)
    parseResult.value = res.data
    if (res.data.success) {
      ElMessage.success('文件解析成功')
      if (!form.name) {
        const nameWithoutExt = rawFile.name.replace(/\.[^/.]+$/, '')
        form.name = nameWithoutExt
      }
    } else {
      ElMessage.error(res.data.message || '文件解析失败')
    }
  } catch (error: any) {
    console.error('Parse file failed:', error)
    ElMessage.error(error.message || '文件解析失败')
    parseResult.value = null
  }
}

const handleExceed = () => {
  ElMessage.warning('只能上传一个文件，请先移除当前文件')
}

const handleFileRemove = () => {
  uploadFile.value = null
  parseResult.value = null
}

const handleRasterFileChange = (file: any) => {
  const rawFile = file.raw as File
  if (!rawFile) return
  rasterFile.value = rawFile
  if (!form.name) {
    form.name = rawFile.name.replace(/\.[^/.]+$/, '')
  }
}

const handleRasterFileRemove = () => {
  rasterFile.value = null
}

const handleRasterSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (!rasterFile.value) {
      ElMessage.warning('请先选择文件')
      return
    }
    rasterUploadLoading.value = true
    try {
      await uploadImage(rasterFile.value, form.name, form.description)
      ElMessage.success('影像上传成功')
      dialogVisible.value = false
      fetchDatasets()
    } catch (error: any) {
      console.error('Upload failed:', error)
      ElMessage.error(error.message || '上传失败')
    } finally {
      rasterUploadLoading.value = false
    }
  })
}

const handleCreateOnly = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      if (isEdit.value && currentId.value) {
        await updateDataset(currentId.value, form)
        ElMessage.success('更新成功')
      } else {
        await createDataset(form)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      fetchDatasets()
    } catch (error) {
      console.error('Create failed:', error)
    } finally {
      loading.value = false
    }
  })
}

const handleImport = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    if (!uploadFile.value) {
      ElMessage.warning('请先上传文件')
      return
    }

    importLoading.value = true
    try {
      const res = await importDataset(
        uploadFile.value,
        form.name,
        form.description,
        form.srs,
        form.sourceSrs || undefined
      )

      if (res.data.success) {
        ElMessage.success(`导入成功，共导入 ${res.data.importedCount} 条记录`)
        dialogVisible.value = false
        fetchDatasets()
      } else {
        ElMessage.error(res.data.message || '导入失败')
      }
    } catch (error: any) {
      console.error('Import failed:', error)
      ElMessage.error(error.message || '导入失败')
    } finally {
      importLoading.value = false
    }
  })
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

onMounted(() => {
  if (route.path === '/datasets/vector') {
    searchForm.type = 'vector'
  } else if (route.path === '/datasets/raster') {
    searchForm.type = 'raster'
  }
  fetchDatasets()
})
</script>

<style lang="scss" scoped>
.datasets-container {
  display: flex;
  justify-content: center;
  padding: 20px;
  min-height: 100vh;
  background: #f5f7fa;
}

.datasets-card {
  width: 100%;
  max-width: 1400px;

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

.upload-section {
  padding: 20px 0;

  .upload-demo {
    width: 100%;
  }

  .file-info {
    margin-top: 20px;
  }

  .parse-result {
    margin-top: 20px;

    .parse-info {
      p {
        margin: 5px 0;
      }
    }
  }

  .missing-prj-warning {
    margin-top: 12px;
  }

  .crs-detected-info {
    margin-top: 12px;
  }
}
</style>
