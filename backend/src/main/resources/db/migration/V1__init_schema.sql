-- Flyway Migration: V1__init_schema.sql
-- GIS Platform Database Schema
-- Author: GIS Platform Team
-- Description: Create all core tables for the GIS Platform

-- Enable PostgreSQL extensions
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Verify PostGIS installation
SELECT postgis_version();

-- ========================================
-- 1. User and Role Tables (RBAC)
-- ========================================

-- User table
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

-- User indexes
CREATE UNIQUE INDEX idx_sys_user_username ON sys_user(username);
CREATE INDEX idx_sys_user_tenant ON sys_user(tenant_id);
CREATE INDEX idx_sys_user_email ON sys_user(email);

-- Role table
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

-- Role indexes
CREATE UNIQUE INDEX idx_sys_role_code ON sys_role(code);
CREATE INDEX idx_sys_role_tenant ON sys_role(tenant_id);

-- User-Role association table
CREATE TABLE sys_user_role (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    role_id     BIGINT NOT NULL,
    tenant_id   VARCHAR(64) NOT NULL DEFAULT 'default',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, role_id)
);

-- User-Role indexes
CREATE INDEX idx_sys_user_role_user ON sys_user_role(user_id);
CREATE INDEX idx_sys_user_role_role ON sys_user_role(role_id);
CREATE INDEX idx_sys_user_role_tenant ON sys_user_role(tenant_id);

-- Permission table
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

-- Permission indexes
CREATE UNIQUE INDEX idx_sys_permission_code ON sys_permission(code);
CREATE INDEX idx_sys_permission_tenant ON sys_permission(tenant_id);

-- Role-Permission association table
CREATE TABLE sys_role_permission (
    id              BIGSERIAL PRIMARY KEY,
    role_id         BIGINT NOT NULL,
    permission_id   BIGINT NOT NULL,
    tenant_id       VARCHAR(64) NOT NULL DEFAULT 'default',
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(role_id, permission_id)
);

-- Role-Permission indexes
CREATE INDEX idx_sys_role_permission_role ON sys_role_permission(role_id);
CREATE INDEX idx_sys_role_permission_perm ON sys_role_permission(permission_id);
CREATE INDEX idx_sys_role_permission_tenant ON sys_role_permission(tenant_id);

-- ========================================
-- 2. Dataset Tables
-- ========================================

-- Dataset table
CREATE TABLE dataset (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(128) NOT NULL,
    description         VARCHAR(512),
    type                VARCHAR(32) NOT NULL,
    geometry_type       VARCHAR(32),
    srs                 VARCHAR(64) NOT NULL DEFAULT 'EPSG:4326',
    storage_type        VARCHAR(32) NOT NULL,
    table_name          VARCHAR(128),
    minio_key           VARCHAR(512),
    extent              JSONB,
    feature_count       INTEGER,
    status              VARCHAR(32) NOT NULL DEFAULT 'draft',
    version             INTEGER NOT NULL DEFAULT 1,
    workspace           VARCHAR(128),
    store_name          VARCHAR(128),
    layer_name          VARCHAR(128),
    tags                JSONB,
    created_by          BIGINT NOT NULL,
    tenant_id           VARCHAR(64) NOT NULL DEFAULT 'default',
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP,
    deleted             SMALLINT NOT NULL DEFAULT 0
);

-- Dataset indexes
CREATE INDEX idx_dataset_name ON dataset(name);
CREATE INDEX idx_dataset_type ON dataset(type);
CREATE INDEX idx_dataset_status ON dataset(status);
CREATE INDEX idx_dataset_tenant ON dataset(tenant_id);
CREATE INDEX idx_dataset_created_by ON dataset(created_by);

-- Dataset version table
CREATE TABLE dataset_version (
    id              BIGSERIAL PRIMARY KEY,
    dataset_id      BIGINT NOT NULL,
    version         INTEGER NOT NULL,
    snapshot_table  VARCHAR(128),
    description     VARCHAR(256),
    created_by      BIGINT NOT NULL,
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Dataset version indexes
CREATE INDEX idx_dataset_version_dataset ON dataset_version(dataset_id);
CREATE INDEX idx_dataset_version_ver ON dataset_version(dataset_id, version);

-- Dataset permission table
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

-- Dataset permission indexes
CREATE INDEX idx_dataset_permission_dataset ON dataset_permission(dataset_id);
CREATE INDEX idx_dataset_permission_role ON dataset_permission(role_id);
CREATE INDEX idx_dataset_permission_user ON dataset_permission(user_id);
CREATE INDEX idx_dataset_permission_tenant ON dataset_permission(tenant_id);

-- ========================================
-- 3. Vector Feature Table (Example)
-- ========================================

-- Example vector features table
CREATE TABLE vector_features_001 (
    id                  BIGSERIAL PRIMARY KEY,
    dataset_id          BIGINT NOT NULL,
    geometry            GEOMETRY(GEOMETRY, 4326) NOT NULL,
    properties          JSONB,
    version             INTEGER NOT NULL DEFAULT 1,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP,
    create_by           BIGINT,
    update_by           BIGINT,
    deleted             SMALLINT NOT NULL DEFAULT 0
);

-- GiST Spatial Index
CREATE INDEX idx_vector_features_001_geom ON vector_features_001 USING GIST(geometry);
-- Dataset index
CREATE INDEX idx_vector_features_001_dataset ON vector_features_001(dataset_id);
-- Soft delete index
CREATE INDEX idx_vector_features_001_deleted ON vector_features_001(deleted);

-- ========================================
-- 4. Raster Metadata Table
-- ========================================

CREATE TABLE raster_metadata (
    id                  BIGSERIAL PRIMARY KEY,
    dataset_id          BIGINT NOT NULL UNIQUE,
    file_name           VARCHAR(256) NOT NULL,
    file_size           BIGINT,
    minio_bucket        VARCHAR(128),
    minio_key           VARCHAR(512),
    width               INTEGER,
    height              INTEGER,
    bands               INTEGER,
    pixel_type          VARCHAR(32),
    no_data_value       DOUBLE PRECISION,
    crs                 VARCHAR(64),
    transform           JSONB,
    overviews           JSONB,
    capture_time        TIMESTAMP,
    validation_status   VARCHAR(32) DEFAULT 'pending',
    validation_message  VARCHAR(512),
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP
);

-- Raster metadata indexes
CREATE INDEX idx_raster_metadata_dataset ON raster_metadata(dataset_id);
CREATE INDEX idx_raster_metadata_capture ON raster_metadata(capture_time);

-- ========================================
-- 5. Map Layer Tables
-- ========================================

CREATE TABLE map_layer (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(128) NOT NULL,
    type            VARCHAR(32) NOT NULL,
    dataset_id      BIGINT,
    source_type     VARCHAR(32),
    source_url      VARCHAR(512),
    style           JSONB,
    visible         BOOLEAN DEFAULT TRUE,
    opacity         DECIMAL(5,2) DEFAULT 1.0,
    z_index         INTEGER DEFAULT 0,
    min_zoom        INTEGER DEFAULT 0,
    max_zoom        INTEGER DEFAULT 18,
    public_access   BOOLEAN DEFAULT TRUE,
    required_role   VARCHAR(64),
    tenant_id       VARCHAR(64) NOT NULL DEFAULT 'default',
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP,
    deleted         SMALLINT NOT NULL DEFAULT 0
);

-- Map layer indexes
CREATE INDEX idx_map_layer_name ON map_layer(name);
CREATE INDEX idx_map_layer_type ON map_layer(type);
CREATE INDEX idx_map_layer_dataset ON map_layer(dataset_id);
CREATE INDEX idx_map_layer_tenant ON map_layer(tenant_id);

CREATE TABLE map_layer_group (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(128) NOT NULL,
    parent_id       BIGINT,
    order_index     INTEGER DEFAULT 0,
    expanded        BOOLEAN DEFAULT TRUE,
    tenant_id       VARCHAR(64) NOT NULL DEFAULT 'default',
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Map layer group indexes
CREATE INDEX idx_map_layer_group_parent ON map_layer_group(parent_id);
CREATE INDEX idx_map_layer_group_tenant ON map_layer_group(tenant_id);

-- ========================================
-- 6. GeoServer Layer Mapping Table
-- ========================================

CREATE TABLE gs_layer (
    id              BIGSERIAL PRIMARY KEY,
    dataset_id      BIGINT NOT NULL,
    workspace       VARCHAR(128) NOT NULL,
    store_name      VARCHAR(128) NOT NULL,
    layer_name      VARCHAR(128) NOT NULL,
    service_type    VARCHAR(16) NOT NULL,
    tile_cache_dir  VARCHAR(512),
    tile_format     VARCHAR(16),
    published       BOOLEAN DEFAULT FALSE,
    last_tiled      TIMESTAMP,
    tile_status     VARCHAR(32),
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP,
    UNIQUE(workspace, store_name, layer_name)
);

-- GeoServer layer indexes
CREATE INDEX idx_gs_layer_dataset ON gs_layer(dataset_id);
CREATE INDEX idx_gs_layer_published ON gs_layer(published);

-- ========================================
-- 7. System Configuration and Logging
-- ========================================

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

-- Config indexes
CREATE UNIQUE INDEX idx_sys_config_key ON sys_config(config_key);
CREATE INDEX idx_sys_config_tenant ON sys_config(tenant_id);

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

-- Operation log indexes
CREATE INDEX idx_sys_operation_log_user ON sys_operation_log(user_id);
CREATE INDEX idx_sys_operation_log_module ON sys_operation_log(module);
CREATE INDEX idx_sys_operation_log_operation ON sys_operation_log(operation);
CREATE INDEX idx_sys_operation_log_time ON sys_operation_log(create_time);
CREATE INDEX idx_sys_operation_log_target ON sys_operation_log(target_type, target_id);

-- ========================================
-- 8. License Management Tables
-- ========================================

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

-- License indexes
CREATE INDEX idx_license_status ON license(status);
CREATE INDEX idx_license_fingerprint ON license(machine_fingerprint);
CREATE INDEX idx_license_expires ON license(expires_at);

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

-- License machine indexes
CREATE INDEX idx_license_machine_license ON license_machine(license_id);
CREATE INDEX idx_license_machine_fingerprint ON license_machine(fingerprint);

CREATE TABLE license_log (
    id                  BIGSERIAL PRIMARY KEY,
    license_id          BIGINT,
    event_type          VARCHAR(32) NOT NULL,
    event_time          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    detail              JSONB,
    ip_address          VARCHAR(64),
    operator            VARCHAR(64)
);

-- License log indexes
CREATE INDEX idx_license_log_license ON license_log(license_id);
CREATE INDEX idx_license_log_event ON license_log(event_type);
CREATE INDEX idx_license_log_time ON license_log(event_time);

-- ========================================
-- 9. Backup Management Tables
-- ========================================

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

-- Backup record indexes
CREATE INDEX idx_backup_record_type ON backup_record(backup_type);
CREATE INDEX idx_backup_record_status ON backup_record(status);
CREATE INDEX idx_backup_record_time ON backup_record(create_time);

CREATE TABLE backup_settings (
    id                  BIGSERIAL PRIMARY KEY,
    backup_type         VARCHAR(32) NOT NULL,
    enabled             BOOLEAN DEFAULT TRUE,
    schedule_cron       VARCHAR(64),
    retention_days      INTEGER DEFAULT 30,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP
);

-- ========================================
-- Complete: Schema initialization done
-- ========================================
