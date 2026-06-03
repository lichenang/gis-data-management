# Design: add-image-upload

## Technical Design

### 1. 后端设计

#### 1.1 数据模型

复用现有 `Dataset` 实体和 `raster_metadata` 表：

```
┌─────────────────────────────────────────────────────────────────┐
│                       影像数据存储架构                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Dataset 表                    raster_metadata 表               │
│  ┌─────────────────┐          ┌─────────────────────┐          │
│  │ id              │◀─────────│ dataset_id          │          │
│  │ name            │          │ file_name           │          │
│  │ type = 'raster' │          │ file_size           │          │
│  │ srs             │          │ minio_bucket        │          │
│  │ storage_type    │          │ minio_key           │          │
│  │   = 'minio'     │          │ width               │          │
│  │ minio_key       │          │ height              │          │
│  │ status          │          │ bands               │          │
│  │ ...             │          │ pixel_type          │          │
│  └─────────────────┘          │ crs                 │          │
│                                │ capture_time        │          │
│                                │ ...                 │          │
│                                └─────────────────────┘          │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

#### 1.2 新增实体类 RasterMetadata

位置：`backend/src/main/java/com/gisplatform/entity/RasterMetadata.java`

字段映射 raster_metadata 表：
- id, datasetId, fileName, fileSize, minioBucket, minioKey
- width, height, bands, pixelType, noDataValue
- crs, transform (JSON), overviews (JSON)
- captureTime, validationStatus, validationMessage
- createTime, updateTime

#### 1.3 新增 Mapper

- `RasterMetadataMapper.java` - 继承 BaseMapper<RasterMetadata>

#### 1.4 新增 Service

`ImageService.java`:

```java
public interface ImageService {
    /**
     * 上传影像文件
     * 1. 解析 GeoTIFF 元数据
     * 2. 上传到 MinIO
     * 3. 写入 Dataset 和 raster_metadata 表
     */
    Dataset uploadImage(MultipartFile file, String name, String description);

    /**
     * 分页查询影像数据集列表
     */
    Page<Dataset> listImages(int page, int pageSize, String name);
}
```

实现类 `ImageServiceImpl.java`:
- 使用 GeoTools 读取 GeoTIFF：GridCoverage2D → getCoordinateReferenceSystem(), getEnvelope()
- MinIO 上传：Bucket = "gis-raster", Key = "images/{uuid}.tif"
- 写入 raster_metadata 表

#### 1.5 新增 Controller

`ImageController.java`:

```java
@RestController
@RequestMapping("/api/v1/images")
@Tag(name = "影像管理", description = "影像数据上传与查询")
public class ImageController {

    @PostMapping("/upload")
    @Operation(summary = "上传影像文件", description = "接收 GeoTIFF 文件，上传到 MinIO 并解析元数据")
    public R<Dataset> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description)

    @GetMapping
    @Operation(summary = "获取影像列表", description = "分页查询影像数据集")
    public R<Page<Dataset>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String name)
}
```

#### 1.6 MinIO 配置

已有配置（application.yml）：
- minio.endpoint
- minio.access-key
- minio.secret-key

存储策略：
- 桶名：`gis-raster`（需要在应用启动时创建）
- 文件路径：`images/{uuid}.tif`
- 访问 URL：`{minio-endpoint}/gis-raster/images/{uuid}.tif`

### 2. 前端设计

#### 2.1 路由配置

在 `frontend/src/router/index.ts` 添加：

```typescript
{
  path: '/images',
  name: 'ImageManagement',
  component: () => import('@/views/images/index.vue'),
  meta: { title: '影像管理', requiresAuth: true }
}
```

#### 2.2 菜单配置

在左侧菜单栏添加"影像管理"入口，位于"数据集管理"下方。

#### 2.3 影像列表页

位置：`frontend/src/views/images/index.vue`

功能：
- 表格展示：名称、分辨率（宽×高）、波段数、坐标系、上传时间、状态
- 分页查询
- 点击"上传"按钮打开上传对话框

#### 2.4 上传对话框

功能：
- 拖拽上传区域，支持 .tif/.tiff 格式
- 上传成功后自动解析并显示元数据：
  - 文件名、文件大小
  - 分辨率（宽×高）
  - 波段数、像素类型
  - 坐标系
  - 拍摄时间（如果有）
- 填写影像名称和描述
- 确认提交

#### 2.5 API 封装

`frontend/src/api/image.ts`:

```typescript
export function uploadImage(file: File, name?: string, description?: string) {
  const formData = new FormData()
  formData.append('file', file)
  if (name) formData.append('name', name)
  if (description) formData.append('description', description)
  return upload('/images/upload', formData)
}

export function getImages(params: { page?: number, pageSize?: number, name?: string }) {
  return get('/images', params)
}
```

### 3. GeoTIFF 元数据解析

使用 GeoTools 解析 GeoTIFF：

```java
// 读取 GridCoverage
GridCoverage2D coverage = new GeoTiffReader(file.getInputStream()).read(null);

// 获取坐标系
CoordinateReferenceSystem crs = coverage.getCoordinateReferenceSystem();
String crsCode = crs.getName().toString(); // e.g., "EPSG:4326"

// 获取范围
Envelope envelope = coverage.getEnvelope();
double minX = envelope.getMinimum(0);
double maxX = envelope.getMaximum(0);
double minY = envelope.getMinimum(1);
double maxY = envelope.getMaximum(1);

// 获取分辨率
double resolution = coverage.getGridGeometry().getGridToCRS().getScaleX();

// 获取波段数
int bands = coverage.getNumSampleDimensions();

// 获取像素类型
SampleType sampleType = coverage.getSampleDimension(0).getSampleType();
```
