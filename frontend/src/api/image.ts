import { get, post, del, upload } from './request'

export interface ImageDataset {
  id?: number
  name: string
  description?: string
  type: string
  srs?: string
  storageType?: string
  minioKey?: string
  status?: string
  createTime?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
}

export interface ImageWmsInfo {
  wmsUrl: string
  layerName: string
  crs: string
  opacity: number
  extent?: [number, number, number, number]
}

export function getImages(params: {
  page?: number
  pageSize?: number
  name?: string
}) {
  return get<{ code: number; data: PageResult<ImageDataset> }>('/images', params)
}

export function getImage(id: number) {
  return get<{ code: number; data: ImageDataset }>(`/images/${id}`)
}

export function uploadImage(file: File, name?: string, description?: string) {
  const formData = new FormData()
  formData.append('file', file)
  if (name) {
    formData.append('name', name)
  }
  if (description) {
    formData.append('description', description)
  }
  return upload<{ code: number; data: ImageDataset }>('/images/upload', formData)
}

export function getPublishedImages() {
  return get<{ code: number; data: ImageDataset[] }>('/images/published')
}

export function getImageWmsUrl(id: number) {
  return get<{ code: number; data: ImageWmsInfo }>(`/images/${id}/wms-url`)
}

export function publishImage(id: number) {
  return post<{ code: number; data: ImageDataset }>(`/images/${id}/publish`)
}

export function unpublishImage(id: number) {
  return del<{ code: number; data: ImageDataset }>(`/images/${id}/publish`)
}

export function retileImage(id: number) {
  return post<{ code: number; data: any }>(`/images/${id}/retile`)
}

export function getTilingStatus(id: number) {
  return get<{ code: number; data: any }>(`/images/${id}/tiling-status`)
}

export interface ImageDownloadUrlResponse {
  downloadUrl: string
  fileName: string
  fileSize: number
  expiresIn: number
}

export function getImageDownloadUrl(id: number) {
  return get<{ code: number; data: ImageDownloadUrlResponse }>(`/images/${id}/download-url`)
}

export interface TilePackageRequest {
  zoomStart?: number
  zoomStop?: number
  bounds?: {
    minX: number
    minY: number
    maxX: number
    maxY: number
  }
}

export function downloadTilePackage(id: number, params: TilePackageRequest) {
  return post<Blob>(`/images/${id}/tile-package`, params, {
    responseType: 'blob',
    timeout: 300000
  })
}

export function deleteImage(id: number) {
  return del<{ code: number }>(`/images/${id}`)
}

export interface ImageMetadata {
  datasetId: number
  name: string
  type: string
  status: string
  crs?: string
  createTime?: string
  updateTime?: string
  fileName?: string
  fileSize?: number
  width?: number
  height?: number
  bands?: number
  pixelType?: string
  noDataValue?: number
  extent?: {
    minX: number
    minY: number
    maxX: number
    maxY: number
  }
}

export function getImageMetadata(id: number) {
  return get<{ code: number; data: ImageMetadata }>(`/images/${id}/metadata`)
}
