## Overview

本次变更是一个简单的修复任务，不涉及新功能或需求变更。修改 TileSeedService 中使用的网格集 ID 从 EPSG:3857 改为 EPSG:900913 以匹配 GWC 的实际配置。

## Summary

- 变更类型: 缺陷修复
- 涉及模块: 切片服务 (TileSeedService)
- 风险等级: 低
