import { get, post, put, del, upload } from './request'

export interface Dataset {
  id?: number
  name: string
  description?: string
  type: string
  geometryType?: string
  srs?: string
  storageType?: string
  tableName?: string
  minioKey?: string
  extent?: string
  featureCount?: number
  status?: string
  version?: number
  workspace?: string
  storeName?: string
  layerName?: string
  tags?: string
  createdBy?: number
  createTime?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
}

export interface GisDataParseResult {
  success: boolean
  message: string
  geometryType?: string
  srs?: string
  featureCount?: number
  bounds?: number[]
  tableName?: string
}

export interface DatasetImportResult {
  success: boolean
  datasetId?: number
  message: string
  importedCount?: number
}

export function getDatasets(params: {
  page?: number
  pageSize?: number
  name?: string
  type?: string
  status?: string
}) {
  return get<{ code: number; data: PageResult<Dataset> }>('/datasets', params)
}

export function getDataset(id: number) {
  return get<{ code: number; data: Dataset }>(`/datasets/${id}`)
}

export function createDataset(data: Dataset) {
  return post<{ code: number; data: boolean }>('/datasets', data)
}

export function updateDataset(id: number, data: Partial<Dataset>) {
  return put<{ code: number; data: boolean }>(`/datasets/${id}`, data)
}

export function deleteDataset(id: number) {
  return del<{ code: number; data: boolean }>(`/datasets/${id}`)
}

export function parseDatasetFile(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return upload<{ code: number; data: GisDataParseResult }>('/datasets/parse', formData)
}

export function importDataset(file: File, name: string, description?: string, srs?: string) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('name', name)
  if (description) {
    formData.append('description', description)
  }
  if (srs) {
    formData.append('srs', srs)
  }
  return upload<{ code: number; data: DatasetImportResult }>('/datasets/import', formData)
}

export function publishDataset(id: number) {
  return put<{ code: number; data: Dataset }>(`/datasets/${id}/publish`)
}

export function unpublishDataset(id: number) {
  return put<{ code: number; data: Dataset }>(`/datasets/${id}/unpublish`)
}

export function getPublishedDatasets() {
  return get<{ code: number; data: Dataset[] }>('/datasets/published')
}
