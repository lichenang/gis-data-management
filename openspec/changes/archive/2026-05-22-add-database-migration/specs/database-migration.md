# database-migration 数据库迁移规范

## 概述

使用 Flyway 实现数据库版本管理，确保数据库表结构和初始数据的自动化创建。

## 技术实现

- 使用 Flyway Core + Flyway PostgreSQL 驱动
- 迁移脚本放置于 `src/main/resources/db/migration/`
- 遵循 Flyway 命名规范：`V<版本号>__<描述>.sql`

## 包含内容

### V1__init_schema.sql

- 启用 PostGIS 扩展
- 创建核心表：sys_user, sys_role, sys_permission, sys_user_role, sys_role_permission
- 创建业务表：dataset, dataset_version, dataset_permission
- 创建影像表：raster_metadata
- 创建图层表：map_layer, map_layer_group
- 创建 GeoServer 映射表：gs_layer
- 创建系统表：sys_config, sys_operation_log
- 创建授权表：license, license_machine, license_log
- 创建备份表：backup_record, backup_settings
- 创建示例矢量要素表：vector_features_001
- 所有表均包含 tenant_id 字段，默认值 'default'

### V2__init_data.sql

- 插入基础角色：ADMIN, EDITOR, USER
- 插入基础权限：15 项权限
- 角色-权限关联：管理员拥有全部权限
- 插入管理员用户：admin / admin123（BCrypt 加密）
- 管理员关联 ADMIN 角色
- 插入默认系统配置

## 索引策略

- 所有主键使用 BIGSERIAL
- 外键字段创建索引
- 几何字段创建 GiST 索引
- tenant_id 字段创建索引（多租户查询）

## 验收标准

- Flyway 迁移成功执行，无报错
- 所有表创建成功
- admin 用户可登录
- 管理员拥有所有权限
