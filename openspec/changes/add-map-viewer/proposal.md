# Proposal: add-map-viewer

## Summary

实现地图查看模块，支持用户浏览已发布的空间数据集。左侧显示图层列表（勾选加载/卸载），右侧地图展示 GeoJSON 矢量数据，支持点击要素查看属性信息。

## Problem Statement

当前系统已具备数据集管理和 GeoJSON 导入功能，但缺少地图可视化展示能力。用户无法直观地查看和交互已发布的空间数据。

## Background

参考设计文档 `openspec/specs/map-viewer-design.md`：
- 前端已安装 OpenLayers (`ol: ^10.0.0`)
- 菜单已有"地图查看"入口 (`/map`)，但路由未配置
- 后端已有数据集 CRUD 接口，但缺少 GeoJSON 导出和已发布数据集查询接口

## Goals

1. 后端：提供查询已发布数据集列表接口
2. 后端：提供 GeoJSON 导出接口
3. 前端：配置 `/map` 路由
4. 前端：实现左右布局的地图查看页面
5. 前端：实现图层勾选加载/卸载功能
6. 前端：实现要素点击 Popup 显示属性

## Success Criteria

- `GET /api/v1/datasets/published` 返回已发布数据集列表
- `GET /api/v1/datasets/{id}/geojson` 返回 GeoJSON 格式数据
- 前端 `/map` 路由正常访问
- 左侧显示已发布数据集，可勾选控制加载/卸载
- 地图显示 OSM 底图和 GeoJSON 矢量图层
- 点击地图要素弹出属性信息 Popup
