# Proposal: add-image-upload

## Summary

实现影像数据上传和元数据自动解析功能，支持 GeoTIFF 文件上传、MinIO 存储、MyBatis-Plus 查询的前后端完整流程。

## Problem Statement

根据 gis-platform.md 规范，系统需要支持影像数据管理功能。当前：
- 后端已有 `raster_metadata` 表和 MinIO SDK 依赖
- 缺少影像上传 API 和前端页面
- 用户无法上传和管理影像数据

## Goals

1. 后端：创建影像上传 API，自动解析 GeoTIFF 元数据并存入 MinIO
2. 前端：添加影像管理菜单入口，支持拖拽上传和列表展示

## Success Criteria

- POST /api/v1/images/upload 成功接收 GeoTIFF 文件，自动提取元数据并存入 MinIO
- GET /api/v1/images 分页查询 type='raster' 的数据集列表
- 前端 /images 路由可访问，显示影像列表
- 上传对话框支持 .tif/.tiff 拖拽上传，自动显示解析的元数据

## Constraints

- 复用现有 Dataset 实体，设置 type='raster'
- MinIO 桶名为 gis-raster，文件路径为 images/{uuid}.tif
- 前端使用 Element Plus 现有组件风格
