## Context

GeoTools 32.x 迁移问题：
- SimpleFeatureIterator 是接口，需要通过 DataStore.getFeatureSource().getFeatures().simpleIterator() 获取
- PostGIS 的 PGgeometry 类 API 有变化
- JTS WKT 解析使用 GeometryReader

## Goals / Non-Goals

Goals: 修复所有 API 迁移导致的编译错误

Non-Goals: 不修改业务逻辑

## Decisions

1. 使用 FeatureSource.getFeatures().simpleIterator() 替代直接实例化
2. 使用 JTS GeometryReader 解析 WKT
3. 使用 org.postgis.PGgeometry 正确方法

## Migration Plan

1. 检查编译错误具体位置
2. 逐一修复
3. 验证编译通过
