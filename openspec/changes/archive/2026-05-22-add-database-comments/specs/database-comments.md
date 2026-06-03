# database-comments 数据库注释规范

## 概述

为 GIS Platform 数据库所有表和字段添加中文注释，符合项目代码规范要求。

## 表注释清单

| 表名 | 中文说明 |
|------|----------|
| sys_user | 系统用户表 |
| sys_role | 角色表 |
| sys_user_role | 用户角色关联表 |
| sys_permission | 权限表 |
| sys_role_permission | 角色权限关联表 |
| dataset | 数据集表 |
| dataset_version | 数据集版本记录表 |
| dataset_permission | 数据集权限分配表 |
| vector_features_001 | 矢量要素示例表 |
| raster_metadata | 影像元数据表 |
| map_layer | 地图图层配置表 |
| map_layer_group | 地图图层分组表 |
| gs_layer | GeoServer 图层映射表 |
| sys_config | 系统配置表 |
| sys_operation_log | 操作日志表 |
| license | 授权信息表 |
| license_machine | 机器指纹历史表 |
| license_log | 授权变更日志表 |
| backup_record | 备份记录表 |
| backup_settings | 备份配置表 |

## 技术实现

使用 Flyway 迁移脚本 V3__add_table_comments.sql：
- 使用 COMMENT ON TABLE 语句为表添加注释
- 使用 COMMENT ON COLUMN 语句为字段添加注释
- 语言：中文

## 验收标准

- 所有表都有中文 COMMENT
- 所有关键字段都有中文 COMMENT
- 实体类注释完整
