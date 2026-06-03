-- Flyway Migration: V2__init_data.sql
-- GIS Platform Initial Data
-- Author: GIS Platform Team
-- Description: Insert initial roles, permissions, and test admin user

-- ========================================
-- 1. Insert Base Roles
-- ========================================

INSERT INTO sys_role (code, name, description, tenant_id) VALUES
('ADMIN', '管理员', '系统全部权限，包括数据源配置、用户管理', 'default'),
('EDITOR', '数据编辑员', '数据集管理、编辑、发布、切片管理', 'default'),
('USER', '普通用户', '只读访问已授权的数据集', 'default');

-- ========================================
-- 2. Insert Base Permissions (15 items)
-- ========================================

INSERT INTO sys_permission (code, name, type, description, tenant_id) VALUES
-- Dataset permissions
('dataset:read', '查看数据集', 'button', '查看数据集列表和详情', 'default'),
('dataset:create', '创建数据集', 'button', '创建新的数据集', 'default'),
('dataset:update', '编辑数据集', 'button', '编辑数据集元数据', 'default'),
('dataset:delete', '删除数据集', 'button', '删除数据集', 'default'),
('dataset:publish', '发布服务', 'button', '发布数据集为地图服务', 'default'),
-- Feature permissions
('feature:read', '查看要素', 'button', '查看要素属性和几何', 'default'),
('feature:create', '创建要素', 'button', '创建新要素', 'default'),
('feature:update', '编辑要素', 'button', '编辑要素', 'default'),
('feature:delete', '删除要素', 'button', '删除要素', 'default'),
-- Layer permissions
('layer:read', '查看图层', 'button', '查看地图图层', 'default'),
('layer:manage', '管理图层', 'button', '管理图层配置和样式', 'default'),
-- System permissions
('system:config', '系统配置', 'menu', '系统配置管理', 'default'),
('system:user', '用户管理', 'menu', '用户和角色管理', 'default'),
('system:log', '操作日志', 'menu', '查看操作日志', 'default'),
('system:license', '授权管理', 'menu', '系统授权管理', 'default'),
('system:backup', '备份管理', 'menu', '数据备份与恢复', 'default');

-- ========================================
-- 3. Role-Permission Associations
-- ========================================

-- ADMIN has all permissions
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p WHERE r.code = 'ADMIN';

-- EDITOR permissions (dataset, feature, layer related)
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'EDITOR' AND p.code IN (
    'dataset:read', 'dataset:create', 'dataset:update', 'dataset:delete', 'dataset:publish',
    'feature:read', 'feature:create', 'feature:update', 'feature:delete',
    'layer:read', 'layer:manage'
);

-- USER permissions (read-only)
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'USER' AND p.code IN (
    'dataset:read', 'feature:read', 'layer:read'
);

-- ========================================
-- 4. Insert Admin User
-- ========================================

-- Password: admin123 (BCrypt encrypted, verified 2026-05-25)
-- $2a$10$4exXS7dfb3HRJB416D79KOChjl2d4hmJOSm2Cycdo7mkE3onlysfO
INSERT INTO sys_user (username, password, nickname, email, status, tenant_id) VALUES
('admin', '$2a$10$4exXS7dfb3HRJB416D79KOChjl2d4hmJOSm2Cycdo7mkE3onlysfO', '系统管理员', 'admin@gisplatform.local', 1, 'default');

-- ========================================
-- 5. User-Role Associations
-- ========================================

-- Admin user -> ADMIN role
INSERT INTO sys_user_role (user_id, role_id, tenant_id)
SELECT u.id, r.id, 'default' FROM sys_user u, sys_role r
WHERE u.username = 'admin' AND r.code = 'ADMIN';

-- ========================================
-- 6. Insert Default System Configurations
-- ========================================

INSERT INTO sys_config (config_key, config_value, value_type, description, tenant_id) VALUES
-- Database connection (placeholders, actual values from environment variables)
('db.postgres.host', '${GIS_DB_HOST:localhost}', 'string', 'PostgreSQL 主机', 'default'),
('db.postgres.port', '${GIS_DB_PORT:5432}', 'string', 'PostgreSQL 端口', 'default'),
('db.postgres.database', '${GIS_DB_NAME:gisdb}', 'string', '数据库名称', 'default'),
('db.postgres.username', '${GIS_DB_USERNAME:postgres}', 'string', '数据库用户名', 'default'),
-- GeoServer configuration
('geoserver.url', '${GEOSERVER_URL:http://localhost:8080/geoserver}', 'string', 'GeoServer 地址', 'default'),
('geoserver.username', '${GEOSERVER_USERNAME:admin}', 'string', 'GeoServer 用户名', 'default'),
('geoserver.workspace', '${GEOSERVER_WORKSPACE:gisplatform}', 'string', '默认工作区', 'default'),
-- MinIO configuration
('minio.endpoint', '${MINIO_ENDPOINT:http://localhost:9000}', 'string', 'MinIO 端点', 'default'),
('minio.bucket', '${MINIO_BUCKET:gis-platform}', 'string', '默认存储桶', 'default'),
('minio.bucket.raster', '${MINIO_BUCKET_RASTER:gis-raster}', 'string', '影像存储桶', 'default'),
-- System configuration
('system.maxUploadSize', '524288000', 'number', '最大上传文件大小（字节）', 'default'),
('system.tileCachePath', '/data/tiles', 'string', '切片缓存目录', 'default');

-- ========================================
-- Complete: Initial data loaded
-- ========================================

-- Verify data
SELECT 'Roles inserted:' AS info, COUNT(*) AS count FROM sys_role
UNION ALL
SELECT 'Permissions inserted:', COUNT(*) FROM sys_permission
UNION ALL
SELECT 'Users inserted:', COUNT(*) FROM sys_user;
