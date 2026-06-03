本次变更引入新能力，需要创建规范文档。

## 模块划分

- `backend/.../controller/ImageController.java` — 新增 /images/{id}/metadata 接口
- `backend/.../service/ImageService.java` — 新增 getMetadata 方法
- `frontend/src/views/images/index.vue` — 新增下载元数据按钮

## 数据流设计

```
前端点击下载按钮 → GET /api/v1/images/{id}/metadata 
    → ImageService 查询 raster_metadata + dataset 
    → 返回 JSON → 前端触发浏览器下载
```

## 接口列表

### 新增接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/v1/images/{id}/metadata | 下载影像元数据为 JSON |

### 接口详情

**请求**
- Auth: Bearer Token (必需)
- ID: 影像数据集 ID (路径参数)

**响应**
- 成功 (200): 返回元数据 JSON
- 404: 数据集不存在
- 401: 未登录

## ADDED Requirements

### Requirement: 影像元数据下载
系统 SHALL 提供影像元数据下载功能，用户可以通过点击按钮下载影像的元数据信息为 JSON 文件。

#### Scenario: 成功下载元数据
- **WHEN** 用户登录后，在影像列表点击某条影像的"下载元数据"按钮
- **THEN** 浏览器下载名为 `metadata-{id}.json` 的文件，内容为该影像的完整元数据

#### Scenario: 数据集不存在
- **WHEN** 用户请求不存在的影像 ID 的元数据
- **THEN** 返回 404 错误，提示"数据集不存在"

#### Scenario: 未登录
- **WHEN** 未登录用户尝试访问元数据接口
- **THEN** 返回 401 错误，重定向到登录页面
