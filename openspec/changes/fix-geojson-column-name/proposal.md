# Proposal: fix-geojson-column-name

## Summary

修复 GeoJSON 导出接口中的几何字段名错误问题。

## Problem Statement

根据诊断报告 `openspec/specs/debug-geojson-sql.md`：

1. 数据导入时建表使用字段名 `geometry`：
   ```sql
   CREATE TABLE ... (geometry GEOMETRY, ...)
   ```

2. GeoJSON 查询时错误使用了 `geom`：
   ```sql
   SELECT ST_AsGeoJSON(geom) ...  -- ❌ 错误！
   ```

3. 导致 SQL 执行失败：`ERROR: column "geom" does not exist`

## Root Cause

字段名不一致：
- 建表/插入：`geometry`
- 查询：`geom` (错误)

## Goals

1. 修改 DatasetServiceImpl.getDatasetAsGeoJSON() 中的 SQL
2. 将 `ST_AsGeoJSON(geom)` 改为 `ST_AsGeoJSON(geometry)`
3. 添加 schema 前缀确保跨 schema 查询正常
4. 检查项目中是否还有其他地方使用错误字段名

## Success Criteria

- GET /api/v1/datasets/{id}/geojson 正确返回 GeoJSON 数据
- 前端地图能正常渲染要素
