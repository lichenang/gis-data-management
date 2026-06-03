# 数据库初始化方案

## 1. 概述

本文档定义 GIS Platform 系统的数据库初始化完整方案，包括：
- 数据库版本管理策略
- 所有核心表的 DDL 脚本
- 初始数据（测试用户、基础角色）
- PostGIS 扩展与空间索引

## 2. 技术选型

### 2.1 数据库版本管理

| 方案 | 优点 | 缺点 | 推荐场景 |
|------|------|------|----------|
| Flyway | 版本追踪、冲突检测、幂等执行 | 需额外依赖 | 生产环境 |
| schema.sql + data.sql | 简单、无额外依赖 | 无版本控制 | 开发/快速启动 |

**决定采用：Flyway** - 理由：
- 支持版本演进和回滚
- 冲突检测机制
- 幂等执行，重复运行安全
- 与 Spring Boot 深度集成

### 2.2 目录结构

```
backend/src/main/resources/
├── db/migration/
│   └── V1__init_schema.sql        # 初始表结构
│   └── V2__init_data.sql          # 初始数据
├── db/backup/                      # 备份脚本（可选）
└── application.yml                 # 数据库配置
```

## 3. 初始化脚本

### 3.1 启用 PostGIS 扩展

```sql
-- V1__init_schema.sql

-- 启用 PostGIS 扩展
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 验证 PostGIS 版本
SELECT postgis_version();
```

### 3.2 核心表结构

#### 3.2.1 用户与角色表（RBAC）

```sql
-- 用户表
CREATE TABLE sys_user (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(64) NOT NULL UNIQUE,
    password        VARCHAR(128) NOT NULL,
    nickname        VARCHAR(64),
    email           VARCHAR(128),
    phone           VARCHAR(32),
    avatar          VARCHAR(512),
    status          SMALLINT NOT NULL DEFAULT 1,
    tenant_id       VARCHAR(64) NOT NULL DEFAULT 'default',
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP,
    deleted         SMALLINT NOT NULL DEFAULT 0
);

-- 用户名索引（唯一）
CREATE UNIQUE INDEX idx_sys_user_username ON sys_user(username);

-- 租户索引
CREATE INDEX idx_sys_user_tenant ON sys_user(tenant_id);

-- 角色表
CREATE TABLE sys_role (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(64) NOT NULL UNIQUE,
    name            VARCHAR(64) NOT NULL,
    description     VARCHAR(256),
    tenant_id       VARCHAR(64) NOT NULL DEFAULT 'default',
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP,
    deleted         SMALLINT NOT NULL DEFAULT 0
);

-- 角色编码索引
CREATE UNIQUE INDEX idx_sys_role_code ON sys_role(code);

-- 用户-角色关联表
CREATE TABLE sys_user_role (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    role_id     BIGINT NOT NULL,
    tenant_id   VARCHAR(64) NOT NULL DEFAULT 'default',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, role_id)
);

-- 用户-角色索引
CREATE INDEX idx_sys_user_role_user ON sys_user_role(user_id);
CREATE INDEX idx_sys_user_role_role ON sys_user_role(role_id);

-- 权限表
CREATE TABLE sys_permission (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(128) NOT NULL UNIQUE,
    name        VARCHAR(64) NOT NULL,
    type        VARCHAR(32) NOT NULL,
    description VARCHAR(256),
    tenant_id   VARCHAR(64) NOT NULL DEFAULT 'default',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT NOT NULL DEFAULT 0
);

-- 权限索引
CREATE UNIQUE INDEX idx_sys_permission_code ON sys_permission(code);

-- 角色-权限关联表
CREATE TABLE sys_role_permission (
    id              BIGSERIAL PRIMARY KEY,
    role_id         BIGINT NOT NULL,
    permission_id   BIGINT NOT NULL,
    tenant_id       VARCHAR(64) NOT NULL DEFAULT 'default',
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(role_id, permission_id)
);

-- 角色-权限索引
CREATE INDEX idx_sys_role_permission_role ON sys_role_permission(role_id);
CREATE INDEX idx_sys_role_permission_perm ON sys_role_permission(permission_id);
```

#### 3.2.2 数据集表

```sql
-- 数据集表
CREATE TABLE dataset (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(128) NOT NULL,
    description         VARCHAR(512),
    type                VARCHAR(32) NOT NULL,
    geometry_type       VARCHAR(32),
    srs                 VARCHAR(64) NOT NULL DEFAULT 'EPSG:4326',
    
    -- 存储信息
    storage_type        VARCHAR(32) NOT NULL,
    table_name          VARCHAR(128),
    minio_key           VARCHAR(512),
    
    -- 范围
    extent              JSONB,
    feature_count       INTEGER,
    
    -- 状态
    status              VARCHAR(32) NOT NULL DEFAULT 'draft',
    version             INTEGER NOT NULL DEFAULT 1,
    
    -- GeoServer 关联
    workspace           VARCHAR(128),
    store_name          VARCHAR(128),
    layer_name          VARCHAR(128),
    
    -- 元数据
    tags                JSONB,
    created_by          BIGINT NOT NULL,
    
    tenant_id           VARCHAR(64) NOT NULL DEFAULT 'default',
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP,
    deleted             SMALLINT NOT NULL DEFAULT 0
);

-- 数据集索引
CREATE INDEX idx_dataset_name ON dataset(name);
CREATE INDEX idx_dataset_type ON dataset(type);
CREATE INDEX idx_dataset_status ON dataset(status);
CREATE INDEX idx_dataset_tenant ON dataset(tenant_id);
CREATE INDEX idx_dataset_created_by ON dataset(created_by);

-- 数据集版本记录
CREATE TABLE dataset_version (
    id              BIGSERIAL PRIMARY KEY,
    dataset_id      BIGINT NOT NULL,
    version         INTEGER NOT NULL,
    snapshot_table  VARCHAR(128),
    description     VARCHAR(256),
    created_by      BIGINT NOT NULL,
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 数据集版本索引
CREATE INDEX idx_dataset_version_dataset ON dataset_version(dataset_id);
CREATE INDEX idx_dataset_version_ver ON dataset_version(dataset_id, version);

-- 数据集权限分配
CREATE TABLE dataset_permission (
    id              BIGSERIAL PRIMARY KEY,
    dataset_id      BIGINT NOT NULL,
    role_id         BIGINT,
    user_id         BIGINT,
    
    can_access      BOOLEAN DEFAULT TRUE,
    can_edit        BOOLEAN DEFAULT FALSE,
    can_download    BOOLEAN DEFAULT FALSE,
    can_publish     BOOLEAN DEFAULT FALSE,
    
    row_filter      JSONB,
    
    tenant_id       VARCHAR(64) NOT NULL DEFAULT 'default',
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT NOT NULL
);

-- 数据集权限索引
CREATE INDEX idx_dataset_permission_dataset ON dataset_permission(dataset_id);
CREATE INDEX idx_dataset_permission_role ON dataset_permission(role_id);
CREATE INDEX idx_dataset_permission_user ON dataset_permission(user_id);
```

#### 3.2.3 矢量要素表示例

> 注意：矢量要素表是动态创建的，每个数据集对应一个独立的表。

```sql
-- 示例：dataset_001 的要素表
CREATE TABLE vector_features_001 (
    id                  BIGSERIAL PRIMARY KEY,
    dataset_id          BIGINT NOT NULL,
    
    -- 几何字段（PostGIS）
    geometry            GEOMETRY(GEOMETRY, 4326) NOT NULL,
    
    -- 属性字段
    properties          JSONB,
    
    -- 乐观锁
    version             INTEGER NOT NULL DEFAULT 1,
    
    -- 审计
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP,
    create_by           BIGINT,
    update_by           BIGINT,
    
    deleted             SMALLINT NOT NULL DEFAULT 0
);

-- GiST 空间索引
CREATE INDEX idx_vector_features_001_geom ON vector_features_001 USING GIST(geometry);
-- 数据集索引
CREATE INDEX idx_vector_features_001_dataset ON vector_features_001(dataset_id);
-- 删除标志索引（软删除优化）
CREATE INDEX idx_vector_features_001_deleted ON vector_features_001(deleted);
```

#### 3.2.4 影像元数据表

```sql
-- 影像元数据表
CREATE TABLE raster_metadata (
    id                  BIGSERIAL PRIMARY KEY,
    dataset_id          BIGINT NOT NULL UNIQUE,
    
    -- 文件信息
    file_name           VARCHAR(256) NOT NULL,
    file_size           BIGINT,
    minio_bucket        VARCHAR(128),
    minio_key           VARCHAR(512),
    
    -- 影像属性
    width               INTEGER,
    height              INTEGER,
    bands               INTEGER,
    pixel_type          VARCHAR(32),
    no_data_value       DOUBLE PRECISION,
    
    -- 地理信息
    crs                 VARCHAR(64),
    transform           JSONB,
    
    -- 金字塔信息
    overviews           JSONB,
    
    -- 时间信息
    capture_time        TIMESTAMP,
    
    -- 校验状态
    validation_status   VARCHAR(32) DEFAULT 'pending',
    validation_message  VARCHAR(512),
    
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP
);

-- 影像索引
CREATE INDEX idx_raster_metadata_dataset ON raster_metadata(dataset_id);
CREATE INDEX idx_raster_metadata_capture ON raster_metadata(capture_time);
```

#### 3.2.5 地图图层配置

```sql
-- 地图图层表
CREATE TABLE map_layer (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(128) NOT NULL,
    type            VARCHAR(32) NOT NULL,
    
    -- 数据源
    dataset_id      BIGINT,
    source_type     VARCHAR(32),
    source_url      VARCHAR(512),
    
    -- 样式
    style           JSONB,
    
    -- 显示属性
    visible         BOOLEAN DEFAULT TRUE,
    opacity         DECIMAL(5,2) DEFAULT 1.0,
    z_index         INTEGER DEFAULT 0,
    
    -- 缩放级别
    min_zoom        INTEGER DEFAULT 0,
    max_zoom        INTEGER DEFAULT 18,
    
    -- 权限控制
    public_access   BOOLEAN DEFAULT TRUE,
    required_role   VARCHAR(64),
    
    tenant_id       VARCHAR(64) NOT NULL DEFAULT 'default',
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP,
    deleted         SMALLINT NOT NULL DEFAULT 0
);

-- 图层索引
CREATE INDEX idx_map_layer_name ON map_layer(name);
CREATE INDEX idx_map_layer_type ON map_layer(type);
CREATE INDEX idx_map_layer_dataset ON map_layer(dataset_id);
CREATE INDEX idx_map_layer_tenant ON map_layer(tenant_id);

-- 图层分组表
CREATE TABLE map_layer_group (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(128) NOT NULL,
    parent_id       BIGINT,
    order_index     INTEGER DEFAULT 0,
    expanded        BOOLEAN DEFAULT TRUE,
    tenant_id       VARCHAR(64) NOT NULL DEFAULT 'default',
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 分组索引
CREATE INDEX idx_map_layer_group_parent ON map_layer_group(parent_id);
CREATE INDEX idx_map_layer_group_tenant ON map_layer_group(tenant_id);
```

#### 3.2.6 GeoServer 映射表

```sql
-- GeoServer 图层映射表
CREATE TABLE gs_layer (
    id              BIGSERIAL PRIMARY KEY,
    dataset_id      BIGINT NOT NULL,
    workspace       VARCHAR(128) NOT NULL,
    store_name      VARCHAR(128) NOT NULL,
    layer_name      VARCHAR(128) NOT NULL,
    
    -- 服务类型
    service_type    VARCHAR(16) NOT NULL,
    
    -- 切片配置
    tile_cache_dir  VARCHAR(512),
    tile_format     VARCHAR(16),
    
    -- 状态
    published       BOOLEAN DEFAULT FALSE,
    last_tiled      TIMESTAMP,
    tile_status     VARCHAR(32),
    
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP,
    
    UNIQUE(workspace, store_name, layer_name)
);

-- GeoServer 图层索引
CREATE INDEX idx_gs_layer_dataset ON gs_layer(dataset_id);
CREATE INDEX idx_gs_layer_published ON gs_layer(published);
```

#### 3.2.7 系统配置与日志

```sql
-- 系统配置表
CREATE TABLE sys_config (
    id              BIGSERIAL PRIMARY KEY,
    config_key      VARCHAR(128) NOT NULL UNIQUE,
    config_value    TEXT,
    value_type      VARCHAR(32) DEFAULT 'string',
    description     VARCHAR(256),
    tenant_id       VARCHAR(64) NOT NULL DEFAULT 'default',
    update_time     TIMESTAMP,
    updated_by      BIGINT
);

-- 配置索引
CREATE UNIQUE INDEX idx_sys_config_key ON sys_config(config_key);
CREATE INDEX idx_sys_config_tenant ON sys_config(tenant_id);

-- 操作日志表
CREATE TABLE sys_operation_log (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT,
    username        VARCHAR(64),
    module          VARCHAR(64),
    operation       VARCHAR(128),
    target_type     VARCHAR(32),
    target_id       BIGINT,
    before_snapshot JSONB,
    after_snapshot  JSONB,
    method          VARCHAR(256),
    params          JSONB,
    result          TEXT,
    ip_address      VARCHAR(64),
    user_agent      VARCHAR(512),
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 操作日志索引
CREATE INDEX idx_sys_operation_log_user ON sys_operation_log(user_id);
CREATE INDEX idx_sys_operation_log_module ON sys_operation_log(module);
CREATE INDEX idx_sys_operation_log_operation ON sys_operation_log(operation);
CREATE INDEX idx_sys_operation_log_time ON sys_operation_log(create_time);
CREATE INDEX idx_sys_operation_log_target ON sys_operation_log(target_type, target_id);
```

#### 3.2.8 授权管理表

```sql
-- 授权信息表
CREATE TABLE license (
    id                  BIGSERIAL PRIMARY KEY,
    license_key         VARCHAR(512),
    license_file        TEXT,
    
    product             VARCHAR(64) NOT NULL,
    version             VARCHAR(32) NOT NULL,
    edition             VARCHAR(32),
    issued_at           TIMESTAMP NOT NULL,
    expires_at          TIMESTAMP,
    max_users           INTEGER NOT NULL,
    modules             JSONB,
    features            JSONB,
    
    machine_fingerprint VARCHAR(128) NOT NULL,
    machine_info        JSONB,
    
    status              VARCHAR(32) NOT NULL DEFAULT 'active',
    is_readonly_mode    BOOLEAN DEFAULT FALSE,
    
    last_check_time     TIMESTAMP,
    check_interval_hours INTEGER DEFAULT 24,
    
    activation_time     TIMESTAMP,
    activation_method   VARCHAR(16),
    activation_device   VARCHAR(256),
    
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP
);

-- 授权索引
CREATE INDEX idx_license_status ON license(status);
CREATE INDEX idx_license_fingerprint ON license(machine_fingerprint);
CREATE INDEX idx_license_expires ON license(expires_at);

-- 机器指纹历史表
CREATE TABLE license_machine (
    id                  BIGSERIAL PRIMARY KEY,
    license_id          BIGINT NOT NULL,
    fingerprint         VARCHAR(128) NOT NULL,
    mac_addresses       JSONB,
    cpu_serial          VARCHAR(128),
    motherboard_serial  VARCHAR(128),
    disk_serial         VARCHAR(128),
    hostname            VARCHAR(256),
    os_version          VARCHAR(128),
    capture_time        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_match            BOOLEAN DEFAULT TRUE,
    mismatch_reason     VARCHAR(256)
);

-- 机器指纹索引
CREATE INDEX idx_license_machine_license ON license_machine(license_id);
CREATE INDEX idx_license_machine_fingerprint ON license_machine(fingerprint);

-- 授权变更日志表
CREATE TABLE license_log (
    id                  BIGSERIAL PRIMARY KEY,
    license_id          BIGINT,
    event_type          VARCHAR(32) NOT NULL,
    event_time          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    detail              JSONB,
    ip_address          VARCHAR(64),
    operator            VARCHAR(64)
);

-- 授权日志索引
CREATE INDEX idx_license_log_license ON license_log(license_id);
CREATE INDEX idx_license_log_event ON license_log(event_type);
CREATE INDEX idx_license_log_time ON license_log(event_time);
```

#### 3.2.9 备份管理表

```sql
-- 备份记录表
CREATE TABLE backup_record (
    id                  BIGSERIAL PRIMARY KEY,
    backup_type         VARCHAR(32) NOT NULL,
    file_name           VARCHAR(256),
    file_path           VARCHAR(512),
    file_size           BIGINT,
    status              VARCHAR(32) NOT NULL,
    progress            INTEGER DEFAULT 0,
    error_message       TEXT,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    complete_time       TIMESTAMP,
    created_by          BIGINT
);

-- 备份记录索引
CREATE INDEX idx_backup_record_type ON backup_record(backup_type);
CREATE INDEX idx_backup_record_status ON backup_record(status);
CREATE INDEX idx_backup_record_time ON backup_record(create_time);

-- 备份配置表
CREATE TABLE backup_settings (
    id                  BIGSERIAL PRIMARY KEY,
    backup_type         VARCHAR(32) NOT NULL,
    enabled             BOOLEAN DEFAULT TRUE,
    schedule_cron       VARCHAR(64),
    retention_days      INTEGER DEFAULT 30,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP
);
```

## 4. 初始数据

### 4.1 基础角色

```sql
-- V2__init_data.sql

-- 插入基础角色
INSERT INTO sys_role (code, name, description, tenant_id) VALUES
('ADMIN', '管理员', '系统全部权限，包括数据源配置、用户管理', 'default'),
('EDITOR', '数据编辑员', '数据集管理、编辑、发布、切片管理', 'default'),
('USER', '普通用户', '只读访问已授权的数据集', 'default');

-- 基础权限
INSERT INTO sys_permission (code, name, type, description, tenant_id) VALUES
-- 数据集权限
('dataset:read', '查看数据集', 'button', '查看数据集列表和详情', 'default'),
('dataset:create', '创建数据集', 'button', '创建新的数据集', 'default'),
('dataset:update', '编辑数据集', 'button', '编辑数据集元数据', 'default'),
('dataset:delete', '删除数据集', 'button', '删除数据集', 'default'),
('dataset:publish', '发布服务', 'button', '发布数据集为地图服务', 'default'),
-- 要素权限
('feature:read', '查看要素', 'button', '查看要素属性和几何', 'default'),
('feature:create', '创建要素', 'button', '创建新要素', 'default'),
('feature:update', '编辑要素', 'button', '编辑要素', 'default'),
('feature:delete', '删除要素', 'button', '删除要素', 'default'),
-- 图层权限
('layer:read', '查看图层', 'button', '查看地图图层', 'default'),
('layer:manage', '管理图层', 'button', '管理图层配置和样式', 'default'),
-- 系统权限
('system:config', '系统配置', 'menu', '系统配置管理', 'default'),
('system:user', '用户管理', 'menu', '用户和角色管理', 'default'),
('system:log', '操作日志', 'menu', '查看操作日志', 'default'),
('system:license', '授权管理', 'menu', '系统授权管理', 'default'),
('system:backup', '备份管理', 'menu', '数据备份与恢复', 'default');

-- 角色-权限关联（管理员拥有所有权限）
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p WHERE r.code = 'ADMIN';

-- 数据编辑员权限（数据集、要素、图层相关）
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p 
WHERE r.code = 'EDITOR' AND p.code IN (
    'dataset:read', 'dataset:create', 'dataset:update', 'dataset:delete', 'dataset:publish',
    'feature:read', 'feature:create', 'feature:update', 'feature:delete',
    'layer:read', 'layer:manage'
);

-- 普通用户权限（只读）
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p 
WHERE r.code = 'USER' AND p.code IN (
    'dataset:read', 'feature:read', 'layer:read'
);
```

### 4.2 测试管理员用户

```sql
-- BCrypt 加密密码示例：密码 "admin123" 的 BCrypt hash
-- $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5E

-- 插入管理员用户
INSERT INTO sys_user (username, password, nickname, email, status, tenant_id) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5E', '系统管理员', 'admin@gisplatform.local', 1, 'default');

-- 关联管理员角色
INSERT INTO sys_user_role (user_id, role_id, tenant_id)
SELECT u.id, r.id, 'default' FROM sys_user u, sys_role r 
WHERE u.username = 'admin' AND r.code = 'ADMIN';
```

> **重要**：上述密码的明文是 `admin123`，仅用于开发和测试环境。生产环境请使用随机生成的强密码并重新生成 BCrypt hash。

### 4.3 默认系统配置

```sql
-- 插入默认系统配置
INSERT INTO sys_config (config_key, config_value, value_type, description, tenant_id) VALUES
-- 数据库连接（占位符，实际从环境变量读取）
('db.postgres.host', '${GIS_DB_HOST:localhost}', 'string', 'PostgreSQL 主机', 'default'),
('db.postgres.port', '${GIS_DB_PORT:5432}', 'string', 'PostgreSQL 端口', 'default'),
('db.postgres.database', '${GIS_DB_NAME:gisdb}', 'string', '数据库名称', 'default'),
('db.postgres.username', '${GIS_DB_USERNAME:postgres}', 'string', '数据库用户名', 'default'),
-- GeoServer 配置
('geoserver.url', '${GEOSERVER_URL:http://localhost:8080/geoserver}', 'string', 'GeoServer 地址', 'default'),
('geoserver.username', '${GEOSERVER_USERNAME:admin}', 'string', 'GeoServer 用户名', 'default'),
('geoserver.workspace', '${GEOSERVER_WORKSPACE:gisplatform}', 'string', '默认工作区', 'default'),
-- MinIO 配置
('minio.endpoint', '${MINIO_ENDPOINT:http://localhost:9000}', 'string', 'MinIO 端点', 'default'),
('minio.bucket', '${MINIO_BUCKET:gis-platform}', 'string', '默认存储桶', 'default'),
('minio.bucket.raster', '${MINIO_BUCKET_RASTER:gis-raster}', 'string', '影像存储桶', 'default'),
-- 系统配置
('system.maxUploadSize', '524288000', 'number', '最大上传文件大小（字节）', 'default'),
('system.tileCachePath', '/data/tiles', 'string', '切片缓存目录', 'default');
```

## 5. Spring Boot 配置

### 5.1 application.yml 配置

```yaml
spring:
  # Flyway 配置
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
    sql-migration-prefix: V
    sql-migration-separator: __
    sql-migration-suffixes: .sql
    validate-on-migrate: true
    
  # 数据源配置
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://${GIS_DB_HOST:localhost}:${GIS_DB_PORT:5432}/${GIS_DB_NAME:gisdb}
    username: ${GIS_DB_USERNAME:postgres}
    password: ${GIS_DB_PASSWORD:}
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      idle-timeout: 300000
      max-lifetime: 1800000
      connection-timeout: 30000

# MyBatis-Plus 配置
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.gisplatform.entity
  configuration:
    map-underscore-to-camel-case: true
```

### 5.2 目录结构

```
backend/src/main/resources/
├── application.yml
├── db/
│   └── migration/
│       └── V1__init_schema.sql
│       └── V2__init_data.sql
└── mapper/
    └── (MyBatis XML mappers)
```

## 6. PostGIS 验证脚本

```sql
-- 验证 PostGIS 扩展
SELECT postgis_version();
-- 预期输出: "3.4 USE_GEOS" 或类似

-- 验证空间参考系统
SELECT * FROM spatial_ref_sys WHERE srid = 4326;

-- 验证几何类型函数
SELECT ST_GeomFromText('POINT(116.4 39.9)', 4326);
```

## 7. 初始化检查清单

| 序号 | 检查项 | 验证方法 |
|------|--------|----------|
| 1 | PostGIS 扩展已安装 | `SELECT postgis_version();` |
| 2 | 用户表已创建且包含 tenant_id | `\d sys_user` |
| 3 | 管理员用户已创建 | `SELECT * FROM sys_user WHERE username='admin';` |
| 4 | 角色已插入 | `SELECT * FROM sys_role;` |
| 5 | 角色关联正确 | `SELECT * FROM sys_user_role;` |
| 6 | 权限已插入 | `SELECT COUNT(*) FROM sys_permission;` > 0 |
| 7 | GiST 索引存在（矢量表） | `\d vector_features_001` |
| 8 | 登录测试 | 使用 admin/admin123 登录 |

## 8. 密码生成工具

如需生成新的 BCrypt 密码 hash，可使用以下方法：

### 8.1 通过 Spring Shell

```java
@Bean
public CommandLineRunner passwordEncoder() {
    return args -> {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println(encoder.encode("your-password"));
    };
}
```

### 8.2 手动生成（在线工具）

访问 BCrypt 在线生成工具，输入明文密码后获取 hash。

> **安全警告**：禁止将生产环境的明文密码提交到代码仓库或文档中。

---

## 9. 后续迭代

本文档覆盖核心表结构。后续迭代可添加：

- **V3__add_dataset_metadata.sql** - 数据集元数据表（ISO 19115）
- **V4__add_quality_rules.sql** - 质检规则表
- **V5__add_layer_styles.sql** - 图层样式表
- **V6__add_export_share.sql** - 导出与分享表
