-- Flyway Migration: V3__add_table_comments.sql
-- GIS Platform Database Table Comments
-- Author: GIS Platform Team
-- Description: Add Chinese comments to all tables and columns

-- ========================================
-- 1. User and Role Tables (RBAC)
-- ========================================

-- sys_user table comments
COMMENT ON TABLE sys_user IS '系统用户表';
COMMENT ON COLUMN sys_user.id IS '用户ID';
COMMENT ON COLUMN sys_user.username IS '用户名（登录账号），唯一';
COMMENT ON COLUMN sys_user.password IS '密码（BCrypt加密存储）';
COMMENT ON COLUMN sys_user.nickname IS '昵称';
COMMENT ON COLUMN sys_user.email IS '邮箱';
COMMENT ON COLUMN sys_user.phone IS '手机号';
COMMENT ON COLUMN sys_user.avatar IS '头像URL';
COMMENT ON COLUMN sys_user.status IS '状态：1-启用，0-禁用';
COMMENT ON COLUMN sys_user.tenant_id IS '租户ID（多租户隔离，默认default）';
COMMENT ON COLUMN sys_user.create_time IS '创建时间';
COMMENT ON COLUMN sys_user.update_time IS '更新时间';
COMMENT ON COLUMN sys_user.deleted IS '逻辑删除标志：0-未删除，1-已删除';

-- sys_role table comments
COMMENT ON TABLE sys_role IS '角色表';
COMMENT ON COLUMN sys_role.id IS '角色ID';
COMMENT ON COLUMN sys_role.code IS '角色编码（唯一），如ADMIN、EDITOR、USER';
COMMENT ON COLUMN sys_role.name IS '角色名称';
COMMENT ON COLUMN sys_role.description IS '角色描述';
COMMENT ON COLUMN sys_role.tenant_id IS '租户ID';
COMMENT ON COLUMN sys_role.create_time IS '创建时间';
COMMENT ON COLUMN sys_role.update_time IS '更新时间';
COMMENT ON COLUMN sys_role.deleted IS '逻辑删除标志：0-未删除，1-已删除';

-- sys_user_role table comments
COMMENT ON TABLE sys_user_role IS '用户角色关联表';
COMMENT ON COLUMN sys_user_role.id IS '主键ID';
COMMENT ON COLUMN sys_user_role.user_id IS '用户ID';
COMMENT ON COLUMN sys_user_role.role_id IS '角色ID';
COMMENT ON COLUMN sys_user_role.tenant_id IS '租户ID';
COMMENT ON COLUMN sys_user_role.create_time IS '创建时间';

-- sys_permission table comments
COMMENT ON TABLE sys_permission IS '权限表';
COMMENT ON COLUMN sys_permission.id IS '权限ID';
COMMENT ON COLUMN sys_permission.code IS '权限编码（唯一），如dataset:read、dataset:create';
COMMENT ON COLUMN sys_permission.name IS '权限名称';
COMMENT ON COLUMN sys_permission.type IS '权限类型：menu、button、api';
COMMENT ON COLUMN sys_permission.description IS '权限描述';
COMMENT ON COLUMN sys_permission.tenant_id IS '租户ID';
COMMENT ON COLUMN sys_permission.create_time IS '创建时间';
COMMENT ON COLUMN sys_permission.deleted IS '逻辑删除标志：0-未删除，1-已删除';

-- sys_role_permission table comments
COMMENT ON TABLE sys_role_permission IS '角色权限关联表';
COMMENT ON COLUMN sys_role_permission.id IS '主键ID';
COMMENT ON COLUMN sys_role_permission.role_id IS '角色ID';
COMMENT ON COLUMN sys_role_permission.permission_id IS '权限ID';
COMMENT ON COLUMN sys_role_permission.tenant_id IS '租户ID';
COMMENT ON COLUMN sys_role_permission.create_time IS '创建时间';

-- ========================================
-- 2. Dataset Tables
-- ========================================

-- dataset table comments
COMMENT ON TABLE dataset IS '数据集表（矢量/影像）';
COMMENT ON COLUMN dataset.id IS '数据集ID';
COMMENT ON COLUMN dataset.name IS '数据集名称';
COMMENT ON COLUMN dataset.description IS '数据集描述';
COMMENT ON COLUMN dataset.type IS '数据类型：vector（矢量）、raster（影像）';
COMMENT ON COLUMN dataset.geometry_type IS '几何类型：Point、LineString、Polygon（矢量）';
COMMENT ON COLUMN dataset.srs IS '空间参考系统，如EPSG:4326';
COMMENT ON COLUMN dataset.storage_type IS '存储类型：postgis、minio';
COMMENT ON COLUMN dataset.table_name IS 'PostGIS表名（矢量数据）';
COMMENT ON COLUMN dataset.minio_key IS 'MinIO对象路径（影像数据）';
COMMENT ON COLUMN dataset.extent IS '空间范围，JSON格式：{minX,minY,maxX,maxY}';
COMMENT ON COLUMN dataset.feature_count IS '要素数量';
COMMENT ON COLUMN dataset.status IS '状态：draft（草稿）、published（已发布）';
COMMENT ON COLUMN dataset.version IS '版本号（乐观锁）';
COMMENT ON COLUMN dataset.workspace IS 'GeoServer工作区';
COMMENT ON COLUMN dataset.store_name IS 'GeoServer数据存储名称';
COMMENT ON COLUMN dataset.layer_name IS 'GeoServer图层名称';
COMMENT ON COLUMN dataset.tags IS '标签，JSON数组格式';
COMMENT ON COLUMN dataset.created_by IS '创建者用户ID';
COMMENT ON COLUMN dataset.tenant_id IS '租户ID';
COMMENT ON COLUMN dataset.create_time IS '创建时间';
COMMENT ON COLUMN dataset.update_time IS '更新时间';
COMMENT ON COLUMN dataset.deleted IS '逻辑删除标志：0-未删除，1-已删除';

-- dataset_version table comments
COMMENT ON TABLE dataset_version IS '数据集版本记录表';
COMMENT ON COLUMN dataset_version.id IS '版本记录ID';
COMMENT ON COLUMN dataset_version.dataset_id IS '数据集ID';
COMMENT ON COLUMN dataset_version.version IS '版本号';
COMMENT ON COLUMN dataset_version.snapshot_table IS '快照表名';
COMMENT ON COLUMN dataset_version.description IS '版本描述';
COMMENT ON COLUMN dataset_version.created_by IS '创建者用户ID';
COMMENT ON COLUMN dataset_version.create_time IS '创建时间';

-- dataset_permission table comments
COMMENT ON TABLE dataset_permission IS '数据集权限分配表';
COMMENT ON COLUMN dataset_permission.id IS '权限ID';
COMMENT ON COLUMN dataset_permission.dataset_id IS '数据集ID';
COMMENT ON COLUMN dataset_permission.role_id IS '角色ID';
COMMENT ON COLUMN dataset_permission.user_id IS '用户ID';
COMMENT ON COLUMN dataset_permission.can_access IS '是否可访问';
COMMENT ON COLUMN dataset_permission.can_edit IS '是否可编辑';
COMMENT ON COLUMN dataset_permission.can_download IS '是否可下载';
COMMENT ON COLUMN dataset_permission.can_publish IS '是否可发布';
COMMENT ON COLUMN dataset_permission.row_filter IS '行级权限过滤条件（如空间区域）';
COMMENT ON COLUMN dataset_permission.tenant_id IS '租户ID';
COMMENT ON COLUMN dataset_permission.create_time IS '创建时间';
COMMENT ON COLUMN dataset_permission.created_by IS '创建者用户ID';

-- ========================================
-- 3. Vector Features Table
-- ========================================

COMMENT ON TABLE vector_features_001 IS '矢量要素示例表';
COMMENT ON COLUMN vector_features_001.id IS '要素ID';
COMMENT ON COLUMN vector_features_001.dataset_id IS '所属数据集ID';
COMMENT ON COLUMN vector_features_001.geometry IS '空间几何字段（PostGIS GEOMETRY）';
COMMENT ON COLUMN vector_features_001.properties IS '属性JSON';
COMMENT ON COLUMN vector_features_001.version IS '版本号（乐观锁）';
COMMENT ON COLUMN vector_features_001.create_time IS '创建时间';
COMMENT ON COLUMN vector_features_001.update_time IS '更新时间';
COMMENT ON COLUMN vector_features_001.create_by IS '创建者用户ID';
COMMENT ON COLUMN vector_features_001.update_by IS '更新者用户ID';
COMMENT ON COLUMN vector_features_001.deleted IS '逻辑删除标志：0-未删除，1-已删除';

-- ========================================
-- 4. Raster Metadata Table
-- ========================================

COMMENT ON TABLE raster_metadata IS '影像元数据表';
COMMENT ON COLUMN raster_metadata.id IS '主键ID';
COMMENT ON COLUMN raster_metadata.dataset_id IS '关联数据集ID';
COMMENT ON COLUMN raster_metadata.file_name IS '原始文件名';
COMMENT ON COLUMN raster_metadata.file_size IS '文件大小（字节）';
COMMENT ON COLUMN raster_metadata.minio_bucket IS 'MinIO存储桶名称';
COMMENT ON COLUMN raster_metadata.minio_key IS 'MinIO对象路径';
COMMENT ON COLUMN raster_metadata.width IS '影像宽度（像素）';
COMMENT ON COLUMN raster_metadata.height IS '影像高度（像素）';
COMMENT ON COLUMN raster_metadata.bands IS '波段数';
COMMENT ON COLUMN raster_metadata.pixel_type IS '像素类型：Float32、UInt16、Byte';
COMMENT ON COLUMN raster_metadata.no_data_value IS '无效值';
COMMENT ON COLUMN raster_metadata.crs IS '坐标参考系统';
COMMENT ON COLUMN raster_metadata.transform IS 'GeoTIFF转换矩阵';
COMMENT ON COLUMN raster_metadata.overviews IS '金字塔信息';
COMMENT ON COLUMN raster_metadata.capture_time IS '拍摄时间';
COMMENT ON COLUMN raster_metadata.validation_status IS '校验状态：pending、valid、invalid';
COMMENT ON COLUMN raster_metadata.validation_message IS '校验消息';
COMMENT ON COLUMN raster_metadata.create_time IS '创建时间';
COMMENT ON COLUMN raster_metadata.update_time IS '更新时间';

-- ========================================
-- 5. Map Layer Tables
-- ========================================

COMMENT ON TABLE map_layer IS '地图图层配置表';
COMMENT ON COLUMN map_layer.id IS '图层ID';
COMMENT ON COLUMN map_layer.name IS '图层名称';
COMMENT ON COLUMN map_layer.type IS '图层类型：vector、raster、wms、wmts、base';
COMMENT ON COLUMN map_layer.dataset_id IS '关联数据集ID';
COMMENT ON COLUMN map_layer.source_type IS '数据源类型：geoserver、tile、xyz';
COMMENT ON COLUMN map_layer.source_url IS '数据源URL';
COMMENT ON COLUMN map_layer.style IS '样式配置（JSON）';
COMMENT ON COLUMN map_layer.visible IS '是否可见';
COMMENT ON COLUMN map_layer.opacity IS '透明度：0-1';
COMMENT ON COLUMN map_layer.z_index IS '图层顺序';
COMMENT ON COLUMN map_layer.min_zoom IS '最小显示级别';
COMMENT ON COLUMN map_layer.max_zoom IS '最大显示级别';
COMMENT ON COLUMN map_layer.public_access IS '是否公开访问';
COMMENT ON COLUMN map_layer.required_role IS '需要角色';
COMMENT ON COLUMN map_layer.tenant_id IS '租户ID';
COMMENT ON COLUMN map_layer.create_time IS '创建时间';
COMMENT ON COLUMN map_layer.update_time IS '更新时间';
COMMENT ON COLUMN map_layer.deleted IS '逻辑删除标志：0-未删除，1-已删除';

COMMENT ON TABLE map_layer_group IS '地图图层分组表';
COMMENT ON COLUMN map_layer_group.id IS '分组ID';
COMMENT ON COLUMN map_layer_group.name IS '分组名称';
COMMENT ON COLUMN map_layer_group.parent_id IS '父分组ID';
COMMENT ON COLUMN map_layer_group.order_index IS '排序索引';
COMMENT ON COLUMN map_layer_group.expanded IS '是否展开';
COMMENT ON COLUMN map_layer_group.tenant_id IS '租户ID';
COMMENT ON COLUMN map_layer_group.create_time IS '创建时间';

-- ========================================
-- 6. GeoServer Layer Mapping Table
-- ========================================

COMMENT ON TABLE gs_layer IS 'GeoServer图层映射表';
COMMENT ON COLUMN gs_layer.id IS '主键ID';
COMMENT ON COLUMN gs_layer.dataset_id IS '关联数据集ID';
COMMENT ON COLUMN gs_layer.workspace IS 'GeoServer工作区';
COMMENT ON COLUMN gs_layer.store_name IS '数据存储名称';
COMMENT ON COLUMN gs_layer.layer_name IS '图层名称';
COMMENT ON COLUMN gs_layer.service_type IS '服务类型：wms、wmts、vector';
COMMENT ON COLUMN gs_layer.tile_cache_dir IS '切片缓存目录';
COMMENT ON COLUMN gs_layer.tile_format IS '切片格式';
COMMENT ON COLUMN gs_layer.published IS '是否已发布';
COMMENT ON COLUMN gs_layer.last_tiled IS '上次切片时间';
COMMENT ON COLUMN gs_layer.tile_status IS '切片状态：none、pending、complete、failed';
COMMENT ON COLUMN gs_layer.create_time IS '创建时间';
COMMENT ON COLUMN gs_layer.update_time IS '更新时间';

-- ========================================
-- 7. System Configuration and Logging
-- ========================================

COMMENT ON TABLE sys_config IS '系统配置表';
COMMENT ON COLUMN sys_config.id IS '配置ID';
COMMENT ON COLUMN sys_config.config_key IS '配置键（如db.postgres.host）';
COMMENT ON COLUMN sys_config.config_value IS '配置值';
COMMENT ON COLUMN sys_config.value_type IS '值类型：string、json、number';
COMMENT ON COLUMN sys_config.description IS '配置描述';
COMMENT ON COLUMN sys_config.tenant_id IS '租户ID';
COMMENT ON COLUMN sys_config.update_time IS '更新时间';
COMMENT ON COLUMN sys_config.updated_by IS '更新者用户ID';

COMMENT ON TABLE sys_operation_log IS '操作日志表';
COMMENT ON COLUMN sys_operation_log.id IS '日志ID';
COMMENT ON COLUMN sys_operation_log.user_id IS '操作人用户ID';
COMMENT ON COLUMN sys_operation_log.username IS '操作人用户名';
COMMENT ON COLUMN sys_operation_log.module IS '模块：dataset、feature、raster、gs、license等';
COMMENT ON COLUMN sys_operation_log.operation IS '操作类型：create、update、delete、publish等';
COMMENT ON COLUMN sys_operation_log.target_type IS '目标资源类型';
COMMENT ON COLUMN sys_operation_log.target_id IS '目标资源ID';
COMMENT ON COLUMN sys_operation_log.before_snapshot IS '变更前快照（JSON）';
COMMENT ON COLUMN sys_operation_log.after_snapshot IS '变更后快照（JSON）';
COMMENT ON COLUMN sys_operation_log.method IS '请求方法+路径';
COMMENT ON COLUMN sys_operation_log.params IS '请求参数（JSON）';
COMMENT ON COLUMN sys_operation_log.result IS '执行结果/错误信息';
COMMENT ON COLUMN sys_operation_log.ip_address IS '客户端IP地址';
COMMENT ON COLUMN sys_operation_log.user_agent IS '浏览器信息';
COMMENT ON COLUMN sys_operation_log.create_time IS '操作时间';

-- ========================================
-- 8. License Management Tables
-- ========================================

COMMENT ON TABLE license IS '授权信息表';
COMMENT ON COLUMN license.id IS '授权ID';
COMMENT ON COLUMN license.license_key IS '授权码/激活码';
COMMENT ON COLUMN license.license_file IS '完整授权文件内容';
COMMENT ON COLUMN license.product IS '产品名称';
COMMENT ON COLUMN license.version IS '版本号';
COMMENT ON COLUMN license.edition IS '版本edition：professional、standard、basic';
COMMENT ON COLUMN license.issued_at IS '签发时间';
COMMENT ON COLUMN license.expires_at IS '过期时间（NULL表示永久）';
COMMENT ON COLUMN license.max_users IS '最大用户数';
COMMENT ON COLUMN license.modules IS '授权模块列表（JSON数组）';
COMMENT ON COLUMN license.features IS '特性开关（JSON对象）';
COMMENT ON COLUMN license.machine_fingerprint IS '机器指纹';
COMMENT ON COLUMN license.machine_info IS '机器信息摘要（JSON）';
COMMENT ON COLUMN license.status IS '状态：active、expired、revoked';
COMMENT ON COLUMN license.is_readonly_mode IS '是否只读模式';
COMMENT ON COLUMN license.last_check_time IS '上次校验时间';
COMMENT ON COLUMN license.check_interval_hours IS '校验间隔（小时）';
COMMENT ON COLUMN license.activation_time IS '激活时间';
COMMENT ON COLUMN license.activation_method IS '激活方式：online、offline';
COMMENT ON COLUMN license.activation_device IS '激活设备信息';
COMMENT ON COLUMN license.create_time IS '创建时间';
COMMENT ON COLUMN license.update_time IS '更新时间';

COMMENT ON TABLE license_machine IS '机器指纹历史表';
COMMENT ON COLUMN license_machine.id IS '记录ID';
COMMENT ON COLUMN license_machine.license_id IS '授权ID';
COMMENT ON COLUMN license_machine.fingerprint IS '机器指纹';
COMMENT ON COLUMN license_machine.mac_addresses IS 'MAC地址列表（JSON数组）';
COMMENT ON COLUMN license_machine.cpu_serial IS 'CPU序列号';
COMMENT ON COLUMN license_machine.motherboard_serial IS '主板序列号';
COMMENT ON COLUMN license_machine.disk_serial IS '磁盘序列号';
COMMENT ON COLUMN license_machine.hostname IS '主机名';
COMMENT ON COLUMN license_machine.os_version IS '操作系统版本';
COMMENT ON COLUMN license_machine.capture_time IS '采集时间';
COMMENT ON COLUMN license_machine.is_match IS '是否匹配';
COMMENT ON COLUMN license_machine.mismatch_reason IS '不匹配原因';

COMMENT ON TABLE license_log IS '授权变更日志表';
COMMENT ON COLUMN license_log.id IS '日志ID';
COMMENT ON COLUMN license_log.license_id IS '授权ID';
COMMENT ON COLUMN license_log.event_type IS '事件类型：activate、renew、expire、revoke、check';
COMMENT ON COLUMN license_log.event_time IS '事件时间';
COMMENT ON COLUMN license_log.detail IS '详情（JSON）';
COMMENT ON COLUMN license_log.ip_address IS 'IP地址';
COMMENT ON COLUMN license_log.operator IS '操作人';

-- ========================================
-- 9. Backup Management Tables
-- ========================================

COMMENT ON TABLE backup_record IS '备份记录表';
COMMENT ON COLUMN backup_record.id IS '记录ID';
COMMENT ON COLUMN backup_record.backup_type IS '备份类型：database、config、full';
COMMENT ON COLUMN backup_record.file_name IS '备份文件名';
COMMENT ON COLUMN backup_record.file_path IS '备份文件路径';
COMMENT ON COLUMN backup_record.file_size IS '文件大小（字节）';
COMMENT ON COLUMN backup_record.status IS '状态：pending、running、success、failed';
COMMENT ON COLUMN backup_record.progress IS '进度百分比';
COMMENT ON COLUMN backup_record.error_message IS '错误信息';
COMMENT ON COLUMN backup_record.create_time IS '创建时间';
COMMENT ON COLUMN backup_record.complete_time IS '完成时间';
COMMENT ON COLUMN backup_record.created_by IS '创建者用户ID';

COMMENT ON TABLE backup_settings IS '备份配置表';
COMMENT ON COLUMN backup_settings.id IS '配置ID';
COMMENT ON COLUMN backup_settings.backup_type IS '备份类型：database、minio、all';
COMMENT ON COLUMN backup_settings.enabled IS '是否启用';
COMMENT ON COLUMN backup_settings.schedule_cron IS 'Cron表达式';
COMMENT ON COLUMN backup_settings.retention_days IS '保留天数';
COMMENT ON COLUMN backup_settings.create_time IS '创建时间';
COMMENT ON COLUMN backup_settings.update_time IS '更新时间';

-- ========================================
-- Complete: All table and column comments added
-- ========================================
