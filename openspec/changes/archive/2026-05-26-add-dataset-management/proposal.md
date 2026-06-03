# add-dataset-management

## Why

系统需要数据集管理功能：
- 实现矢量/影像数据的导入、存储、查询
- 为后续图层发布、服务发布提供数据源
- 参考 `openspec/specs/dataset-management-design.md`

## What Changes

### 后端 (6个文件)
- `Dataset.java` 实体（对应 dataset 表）
- `DatasetMapper.java` Mapper 接口
- `DatasetService.java` + `DatasetServiceImpl.java` 业务层
- `DatasetController.java` REST API
- `DatasetApi.java` 前端 API 调用封装

### 前端 (3个文件/目录)
- `/datasets` 路由
- `views/datasets/index.vue` 数据集列表页
- `views/datasets/components/*` 组件（表格、表单、上传）

## Capabilities

### New Capabilities
- 数据集列表查询（分页 + 搜索）
- 数据集创建/编辑/删除（基础 CRUD）
- 数据集详情查看
- 数据集类型：矢量(vector) / 影像(raster)

## Impact

| 类型 | 文件 |
|------|------|
| 新增 | `backend/entity/Dataset.java` |
| 新增 | `backend/mapper/DatasetMapper.java` |
| 新增 | `backend/service/DatasetService.java` |
| 新增 | `backend/service/impl/DatasetServiceImpl.java` |
| 新增 | `backend/controller/DatasetController.java` |
| 修改 | `frontend/router/index.ts` |
| 新增 | `frontend/views/datasets/index.vue` |
| 新增 | `frontend/src/api/dataset.ts` |

## Non-goals

- 暂不实现空间文件解析（Shapefile/GeoJSON 上传）
- 暂不实现 GeoServer 发布
- 暂不实现版本管理
- 暂不实现数据权限控制
