# Proposal: add-image-publish

## 问题描述

当前影像数据集上传后只能存储在 MinIO 中，无法通过 Web 地图服务访问。用户希望：
1. 将 GeoTIFF 发布为 GeoServer ImageMosaic 图层
2. 通过 WMS/WMTS 服务在前端加载和显示影像
3. 利用 GeoWebCache 实现切片缓存，提升访问性能

现状：
- ImageService.uploadImage() 已实现文件上传和 raster_metadata 记录创建
- Dataset.status 字段存在，但 publishDataset() 仅更新状态值，未实际发布
- GeoServer 配置存在于 application.yml，但无 REST API 调用代码

## 目标

实现影像数据的发布与切片功能：

1. **POST /api/v1/images/{id}/publish** — 创建 GeoServer 图层并触发异步切片
2. **异步切片任务** — 自动生成 0-18 级切片缓存
3. **切片状态追踪** — 通过 API 查询切片进度
4. **手动重切片** — 数据更新后可刷新缓存
5. **与现有流程整合** — 前端发布按钮根据类型调用不同接口

## 影响范围

**后端：**
- 新增 GeoServer REST Client 模块 (workspace/store/layer/cache)
- 新增 async 线程池配置
- 新增数据库字段
- 扩展 ImageService.publish() 和 ImageController

**不涉及：**
- 数据库已有 workspace/storeName/layerName 字段（暂不用于影像）
- 现有矢量发布流程
- 前端改造（本任务范围外）

## 风险评估

- 中等风险：GeoServer REST API 集成需处理认证和异常
- 低风险：异步任务通过线程池管理可控制并发
