## Why

GeoTools 32.x API 迁移导致代码中存在多处编译错误：
1. SimpleFeatureIterator 是接口无法直接实例化
2. PGgeometry.geomFromByteArray 方法不存在
3. WKT 解析相关 API 变更

## What Changes

1. 修复 MultiFormatImportService.java 中 SimpleFeatureIterator 使用错误
2. 修复几何转换逻辑，使用正确的 GeoTools 32.x API
3. 修复 PGgeometry 相关调用

## Capabilities

纯技术修复，无新功能需求。

## Impact

- MultiFormatImportService.java

## Non-Goals

- 不改变导入逻辑
- 不改变数据存储结构
