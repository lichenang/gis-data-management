# Proposal: fix-dataset-storage-type-default

## Summary

修复 DatasetServiceImpl.createDataset 中缺少 `storage_type` 默认值的问题，同时检查并补齐所有有 NOT NULL 约束但缺少默认值的字段。

## Problem Statement

数据库表 `dataset` 中 `storage_type` 字段定义为 `NOT NULL` 约束，但没有默认值。当通过 API 创建数据集时，如果前端没有传递 `storage_type` 字段，会导致插入数据库失败，返回"系统繁忙"错误。

## Background

根据 `V1__init_schema.sql` 数据库迁移文件，`dataset` 表中以下字段有 NOT NULL 约束：
- `name` - NOT NULL
- `type` - NOT NULL
- `srs` - NOT NULL DEFAULT 'EPSG:4326'
- `storage_type` - NOT NULL（**无默认值**）
- `status` - NOT NULL DEFAULT 'draft'
- `version` - NOT NULL DEFAULT 1
- `created_by` - NOT NULL
- `tenant_id` - NOT NULL DEFAULT 'default'
- `create_time` - NOT NULL DEFAULT CURRENT_TIMESTAMP
- `deleted` - NOT NULL DEFAULT 0

当前 `DatasetServiceImpl.createDataset` 方法已经处理了部分字段的默认值，但遗漏了 `storage_type`。

## Goals

1. 为 `storage_type` 字段添加默认值 "postgis"（矢量数据默认存储在 PostGIS）
2. 检查并确保所有有 NOT NULL 约束的字段都有合适的默认值处理

## Out of Scope

- 修改前端界面
- 修改数据库表结构
- 修改除 DatasetServiceImpl 之外的业务逻辑

## Success Criteria

- `POST /api/v1/datasets` 接口在未传递 `storage_type` 时能成功创建数据集
- 所有有 NOT NULL 约束的字段在 createDataset 方法中都得到正确处理
