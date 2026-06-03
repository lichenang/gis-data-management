# add-image-tile-cache

## 问题描述

当前影像切片使用模拟进度（TileSeedService.simulateTilingProgress），不是真实的 GeoWebCache 状态。前端也缺少切片状态展示和手动重新切片功能。

## 根本原因

1. `TileSeedService.triggerSeed()` 触发切片后，使用 `simulateTilingProgress()` 模拟进度更新
2. 前端 `images/index.vue` 没有切片状态列
3. 缺少手动重新切片的前端入口

## 影响

- 用户无法知道切片真实进度
- 数据更新后无法刷新缓存
- 影像可能加载缓慢（切片未预生成）

## 预期结果

1. 后端实现真实的 GeoWebCache 状态轮询
2. 前端显示切片状态（待处理/处理中/已完成/失败）和进度百分比
3. 提供"重新切片"按钮手动刷新缓存
