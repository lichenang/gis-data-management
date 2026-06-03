## Why

当前矢量/影像数据集管理功能存在以下问题：
1. 矢量数据集编辑模式下，类型字段未被禁用，用户可以修改数据集类型
2. 矢量数据集编辑模式下，空间数据上传区域显示状态不明确
3. 影像数据集的"查看"按钮仅显示简单文字，未展示有价值的元数据信息
4. 删除功能可能因前端未传 ID 或后端未放行 DELETE 请求而失败

## What Changes

1. 矢量数据集编辑：禁用类型字段（type 字段 disabled）
2. 矢量数据集编辑：已上传文件时显示"已上传"提示
3. 影像数据集"查看"按钮：改为弹窗展示完整元数据（分辨率、波段数、坐标系、上传时间等）
4. 删除功能：检查前端 API 调用是否正确传递数据集 ID
5. 删除功能：检查后端 SecurityConfig 是否放行 DELETE 请求

## Capabilities

### New Capabilities
（无）

### Modified Capabilities
（无）

## Impact

- `frontend/src/views/dataset/vector/**` — 编辑模式类型字段禁用、上传提示
- `frontend/src/views/dataset/raster/**` — 查看按钮弹窗
- `backend/src/main/java/com/gisplatform/config/SecurityConfig.java` — DELETE 请求放行
- `backend/src/main/java/com/gisplatform/controller/**` — 检查 DELETE 端点

## Non-goals

- 不修改其他业务逻辑
- 不修改数据库表结构
- 不修改 API 接口响应格式
