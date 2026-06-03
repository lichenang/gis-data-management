# restructure-dataset-menu

## 问题描述

当前左侧菜单结构扁平，缺少层次感：
- "数据集管理" 只对应一个菜单项 `/datasets`
- 矢量数据集和影像数据集混在一起，没有区分

## 根本原因

菜单设计没有区分数据类型：
- 矢量数据集 (`type='vector'`) → `/datasets`
- 影像数据集 (`type='raster'`) → `/images`

用户无法快速区分和管理不同类型的 dataset。

## 预期结果

```
当前结构:                    目标结构:
├── 仪表盘                   ├── 仪表盘
├── 数据集管理                └── 数据集管理 (可展开)
├── 图层管理                      ├── 矢量数据集 → /datasets/vector
├── 地图查看                     └── 影像数据集 → /datasets/raster
├── 用户管理                  ├── 地图查看
└── 系统管理                  ├── 用户管理
                              └── 系统管理
```

### 路由变化

| 原路由 | 新路由 | 说明 |
|--------|--------|------|
| /datasets | /datasets/vector | 矢量数据集列表 |
| /datasets (重定向) | /datasets/vector | 默认跳转 |
| /images | /datasets/raster 或保留 | 影像数据集列表 |

### 功能要求

1. `/datasets/vector` 路由显示矢量数据集列表（复用现有 datasets/index.vue）
2. `/datasets/raster` 路由显示影像数据集列表
3. `/datasets` 路由自动重定向到 `/datasets/vector`
4. 菜单使用 `el-sub-menu` 实现展开式二级结构
