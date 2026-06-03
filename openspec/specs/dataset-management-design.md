# 数据集管理模块设计方案

## 1. 项目现状探查结果

### 1.1 后端现状

| 组件 | 状态 | 说明 |
|------|------|------|
| Dataset 实体 | ❌ 不存在 | 需新建 |
| DatasetMapper | ❌ 不存在 | 需新建 |
| DatasetService | ❌ 不存在 | 需新建 |
| DatasetController | ❌ 不存在 | 需新建 |
| SpatialFileParser 解析器 | ❌ 不存在 | 需新建（支持 Shapefile/GeoJSON） |

**现有文件**:
- `backend/src/main/java/com/gisplatform/entity/` - 仅有 User.java, Role.java
- `backend/src/main/java/com/gisplatform/mapper/` - 仅有 UserMapper.java, RoleMapper.java
- `backend/src/main/java/com/gisplatform/controller/` - 仅有 AuthController.java, UserController.java
- `backend/src/main/resources/db/migration/V1__init_schema.sql` - 已定义 dataset 表结构

### 1.2 前端现状

| 组件 | 状态 | 说明 |
|------|------|------|
| /datasets 路由 | ❌ 不存在 | 需新建 |
| 数据集列表页 | ❌ 不存在 | 需新建 |
| 创建/编辑对话框 | ❌ 不存在 | 需新建 |
| 文件上传组件 | ❌ 不存在 | 需新建 |

**现有文件**:
- `frontend/src/views/home/index.vue` - 首页（包含菜单）
- `frontend/src/views/users/index.vue` - 用户管理页（可参考）

### 1.3 数据库表结构（已有）

根据 `V1__init_schema.sql`，dataset 表结构已定义：

```sql
CREATE TABLE dataset (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(128) NOT NULL,
    description         VARCHAR(512),
    type                VARCHAR(32) NOT NULL,  -- vector, raster
    geometry_type       VARCHAR(32),  -- Point, LineString, Polygon
    srs                 VARCHAR(64) NOT NULL DEFAULT 'EPSG:4326',
    storage_type        VARCHAR(32) NOT NULL,  -- postgis, minio
    table_name          VARCHAR(128),
    minio_key           VARCHAR(512),
    extent              JSONB,
    feature_count       INTEGER,
    status              VARCHAR(32) NOT NULL DEFAULT 'draft',
    version             INTEGER NOT NULL DEFAULT 1,
    workspace           VARCHAR(128),
    store_name          VARCHAR(128),
    layer_name          VARCHAR(128),
    tags                JSONB,
    created_by          BIGINT NOT NULL,
    tenant_id           VARCHAR(64) NOT NULL DEFAULT 'default',
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP,
    deleted             SMALLINT NOT NULL DEFAULT 0
);
```

---

## 2. 后端设计

### 2.1 模块结构

```
backend/src/main/java/com/gisplatform/
├── entity/
│   └── Dataset.java              (NEW)
├── mapper/
│   └── DatasetMapper.java        (NEW)
├── service/
│   ├── DatasetService.java       (NEW, interface)
│   └── impl/
│       └── DatasetServiceImpl.java (NEW)
├── controller/
│   └── DatasetController.java    (NEW)
├── dto/
│   ├── DatasetCreateRequest.java (NEW)
│   ├── DatasetUpdateRequest.java (NEW)
│   └── DatasetUploadResponse.java (NEW)
└── util/
    └── SpatialFileParser.java    (NEW, 文件解析)
```

### 2.2 REST API 设计

#### 2.2.1 数据集 CRUD

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | /api/v1/datasets | 分页查询数据集列表 | 登录用户 |
| GET | /api/v1/datasets/{id} | 获取数据集详情 | 登录用户 |
| POST | /api/v1/datasets | 创建数据集 | 编辑员+ |
| PUT | /api/v1/datasets/{id} | 更新数据集 | 编辑员+ |
| DELETE | /api/v1/datasets/{id} | 删除数据集 | 管理员 |

#### 2.2.2 文件上传

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | /api/v1/datasets/upload | 上传空间数据文件 | 编辑员+ |
| GET | /api/v1/datasets/{id}/download | 下载数据集 | 查看权限 |

#### 2.2.3 API 详细定义

**GET /api/v1/datasets** - 分页查询

```
Query Parameters:
- page: int (default: 1)
- pageSize: int (default: 10)
- name: string (搜索名称)
- type: string (vector/raster)
- status: string (draft/published)

Response:
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "name": "城市道路",
        "description": "某市道路数据",
        "type": "vector",
        "geometryType": "LineString",
        "srs": "EPSG:4326",
        "status": "draft",
        "featureCount": 1250,
        "createTime": "2026-05-26T10:00:00"
      }
    ],
    "total": 100,
    "page": 1,
    "pageSize": 10
  }
}
```

**POST /api/v1/datasets/upload** - 上传文件

```
Request (multipart/form-data):
- file: binary (Shapefile.zip / .geojson / .kml)
- name: string (数据集名称)
- description: string (可选)

Response:
{
  "code": 200,
  "data": {
    "id": 1,
    "name": "城市道路",
    "type": "vector",
    "geometryType": "LineString",
    "featureCount": 1250,
    "extent": {"minX": 116.0, "minY": 39.0, "maxX": 117.0, "maxY": 40.0},
    "status": "draft"
  }
}
```

### 2.3 空间数据上传处理流程

```
┌─────────────────────────────────────────────────────────────────────┐
│                    空间数据上传处理流程                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  1. 前端 POST /api/v1/datasets/upload (multipart)                  │
│                          │                                          │
│                          ▼                                          │
│  2. DatasetController.upload()                                      │
│           │                                                         │
│           ▼                                                         │
│  3. 文件类型检测 (.shp/.geojson/.kml)                               │
│           │                                                         │
│           ▼                                                         │
│  4. SpatialFileParser 解析文件                                       │
│     - Shapefile: 使用 GeoTools DataStore                           │
│     - GeoJSON: 使用 GeoJSONReader                                   │
│     - KML: 使用 KmlReader                                           │
│           │                                                         │
│           ▼                                                         │
│  5. 坐标转换（如需要） → EPSG:4326                                  │
│           │                                                         │
│           ▼                                                         │
│  6. 创建数据集记录到 dataset 表                                      │
│           │                                                         │
│           ▼                                                         │
│  7. 创建要素表 vector_features_{id}                                 │
│     - 动态创建 PostGIS 要素表                                        │
│     - 创建 GiST 空间索引                                             │
│           │                                                         │
│           ▼                                                         │
│  8. 导入要素到要素表                                                 │
│           │                                                         │
│           ▼                                                         │
│  9. 更新 dataset.feature_count                                      │
│           │                                                         │
│           ▼                                                         │
│  10. 返回创建结果                                                   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.4 Dataset 实体类

```java
@Data
@TableName("dataset")
@Schema(description = "数据集实体")
public class Dataset {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "数据集名称")
    private String name;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "类型: vector/raster")
    private String type;

    @Schema(description = "几何类型: Point/LineString/Polygon")
    private String geometryType;

    @Schema(description = "坐标系")
    private String srs;

    @Schema(description = "存储类型: postgis/minio")
    private String storageType;

    @Schema(description = "PostGIS表名")
    private String tableName;

    @Schema(description = "MinIO对象路径")
    private String minioKey;

    @Schema(description = "空间范围")
    private String extent;

    @Schema(description = "要素数量")
    private Integer featureCount;

    @Schema(description = "状态: draft/published")
    private String status;

    @Schema(description = "版本号")
    private Integer version;

    @Schema(description = "GeoServer工作区")
    private String workspace;

    @Schema(description = "GeoServer数据存储")
    private String storeName;

    @Schema(description = "GeoServer图层名")
    private String layerName;

    @Schema(description = "标签")
    private String tags;

    @Schema(description = "创建人ID")
    private Long createdBy;

    private String tenantId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
```

---

## 3. 前端设计

### 3.1 路由配置

```typescript
// frontend/src/router/index.ts
{
  path: '/datasets',
  name: 'Datasets',
  component: () => import('@/views/datasets/index.vue'),
  meta: { title: '数据集管理', requiresAuth: true }
}
```

### 3.2 页面结构

```
frontend/src/views/datasets/
├── index.vue           # 数据集列表页
└── components/
    ├── DatasetTable.vue     # 数据集表格组件
    ├── DatasetForm.vue      # 创建/编辑对话框
    ├── FileUpload.vue       # 文件上传组件
    └── DatasetDetail.vue    # 详情侧边栏
```

### 3.3 页面布局

```
┌─────────────────────────────────────────────────────────────────────┐
│  数据集管理                                                          │
├─────────────────────────────────────────────────────────────────────┤
│  [名称搜索] [类型▼] [状态▼]  [搜索] [重置]  [+ 新建数据集]          │
├─────────────────────────────────────────────────────────────────────┤
│  ┌────────┬────────────┬────────┬─────────┬────────┬────────────┐  │
│  │ 名称   │ 类型       │ 几何   │ 要素数  │ 状态   │ 创建时间   │  │
│  ├────────┼────────────┼────────┼─────────┼────────┼────────────┤  │
│  │ 城市道路│ 矢量      │ Line   │ 1250    │ 草稿   │ 2026-05-26│  │
│  │ ...    │ ...        │ ...    │ ...     │ ...    │ ...        │  │
│  └────────┴────────────┴────────┴─────────┴────────┴────────────┘  │
├─────────────────────────────────────────────────────────────────────┤
│  << < 1 2 3 ... 10 > >>   共 100 条  每页 10 条                    │
└─────────────────────────────────────────────────────────────────────┘
```

### 3.4 创建数据集对话框

```
┌─────────────────────────────────────┐
│  新建数据集                    X    │
├─────────────────────────────────────┤
│  * 数据集名称                       │
│  [________________________________] │
│                                     │
│  数据类型                           │
│  (•) 矢量    ( ) 影像              │
│                                     │
│  [上传文件]                         │
│  📁 city_roads.zip (2.5MB)         │
│  支持 .shp .geojson .kml 格式       │
│                                     │
│  描述                               │
│  [________________________________] │
│                                     │
│           [取消]  [创建]             │
└─────────────────────────────────────┘
```

### 3.5 API 调用

```typescript
// frontend/src/api/dataset.ts
import { get, post, put, del } from './request'

export interface Dataset {
  id: number
  name: string
  description: string
  type: string
  geometryType: string
  srs: string
  status: string
  featureCount: number
  createTime: string
}

export function getDatasets(params: {
  page: number
  pageSize: number
  name?: string
  type?: string
  status?: string
}) {
  return get<{ code: number; data: any }>('/datasets', params)
}

export function createDataset(data: FormData) {
  return post<{ code: number; data: Dataset }>('/datasets/upload', data, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function updateDataset(id: number, data: Partial<Dataset>) {
  return put<{ code: number }>(`/datasets/${id}`, data)
}

export function deleteDataset(id: number) {
  return del<{ code: number }>(`/datasets/${id}`)
}
```

---

## 4. 数据流设计

### 4.1 上传完整流程

```
浏览器                    后端 Spring                    数据库/存储
  │                         │                              │
  │  POST /datasets/upload │                              │
  │  (multipart/form-data) │                              │
  │───────────────────────>│                              │
  │                        │ 接收文件                       │
  │                        │──▶ tmp/                       │
  │                        │                              │
  │                        │ 识别文件类型 (Shapefile)      │
  │                        │                              │
  │                        │ GeoTools 解析                 │
  │                        │──▶ 读取 FeatureCollection    │
  │                        │                              │
  │                        │ 获取几何范围                   │
  │                        │──▶ extent JSON               │
  │                        │                              │
  │                        │ INSERT dataset               │
  │                        │─────────────────────────────>│
  │                        │ ← ID=1                       │
  │                        │                              │
  │                        │ CREATE TABLE                 │
  │                        │ vector_features_1            │
  │                        │─────────────────────────────>│
  │                        │                              │
  │                        │ INSERT features              │
  │                        │─────────────────────────────>│
  │                        │                              │
  │                        │ UPDATE dataset               │
  │                        │ SET feature_count=1250       │
  │                        │─────────────────────────────>│
  │                        │                              │
  │  200 OK               │                              │
  │  {id:1, featureCount} │                              │
  │<──────────────────────│                              │
```

### 4.2 数据集与图层关系

```
┌─────────────────────────────────────────────────────────────────────┐
│                     数据集与图层关系图                                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────┐         ┌─────────────┐                          │
│  │   dataset   │────────▶│  map_layer  │                          │
│  │  (源数据)   │  1:N    │  (地图图层)  │                          │
│  └─────────────┘         └─────────────┘                          │
│        │                        │                                   │
│        │ id=1                   │ layer_name                       │
│        │ name=城市道路          │ name=城市道路图层                 │
│        │ type=vector            │ source_url=/geoserver/...        │
│        │ table_name=vector_1    │ dataset_id=1                     │
│        └────────────────────────┘                                   │
│                                                                     │
│  前端展示流程:                                                      │
│  1. 用户选择数据集发布为图层                                         │
│  2. 后端调用 GeoServer REST API 创建工作区+数据存储+图层             │
│  3. 返回 WMS/WMTS 服务地址                                          │
│  4. 前端 map_layer.source_url = "http://geoserver/wms"            │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 5. 实施任务拆分

### 5.1 后端任务

| 任务 | 说明 | 依赖 |
|------|------|------|
| 1.1 | 创建 Dataset 实体类 | - |
| 1.2 | 创建 DatasetMapper 接口 | 1.1 |
| 1.3 | 创建 DatasetService 接口和实现 | 1.2 |
| 1.4 | 创建 DatasetController REST API | 1.3 |
| 1.5 | 实现空间文件解析器 SpatialFileParser | - |
| 1.6 | 实现 Shapefile/GeoJSON 解析逻辑 | 1.5 |

### 5.2 前端任务

| 任务 | 说明 | 依赖 |
|------|------|------|
| 2.1 | 添加 /datasets 路由 | - |
| 2.2 | 创建数据集列表页 index.vue | - |
| 2.3 | 实现表格组件（搜索、分页） | 2.2 |
| 2.4 | 实现创建数据集对话框 | 2.2 |
| 2.5 | 实现文件上传组件 | 2.4 |
| 2.6 | 调用后端 API 完成 CRUD | 1.4 + 2.2 |

### 5.3 验证任务

| 任务 | 说明 |
|------|------|
| 3.1 | 上传 Shapefile 测试 |
| 3.2 | 上传 GeoJSON 测试 |
| 3.3 | 列表分页测试 |
| 3.4 | 编辑/删除测试 |

---

## 6. 技术依赖

### 6.1 后端依赖

```xml
<!-- pom.xml 需要添加 -->
<dependency>
    <groupId>org.geotools</groupId>
    <artifactId>gt-shapefile</artifactId>
    <version>28.5</version>
</dependency>
<dependency>
    <groupId>org.geotools</groupId>
    <artifactId>gt-geojson</artifactId>
    <version>28.5</version>
</dependency>
<dependency>
    <groupId>org.geotools</groupId>
    <artifactId>gt-epsg-h2</artifactId>
    <version>28.5</version>
</dependency>
```

### 6.2 前端依赖

- element-plus (已安装)
- @element-plus/icons-vue (已安装)
- 可选：shapefile.js (如果实现纯前端解析)

---

## 7. 下一步行动

1. **创建变更** `add-dataset-management` 
2. **按顺序实现** 后端实体 → Mapper → Service → Controller → 前端页面
3. **测试验证** 确保文件上传、空间数据存储正常工作

是否需要我创建此变更并开始实现？
