# Proposal: fix-add-epsg-dependency

## Summary

添加 gt-epsg-hsql 依赖，提供嵌入式 EPSG 坐标参考系统数据库支持，解决影像解析时 NoSuchAuthorityCodeException 错误。

## Problem Statement

影像上传模块在解析 GeoTIFF 元数据时，尝试获取坐标参考系统（CRS）信息（如 EPSG:4326），但由于缺少 EPSG 数据库依赖，抛出 NoSuchAuthorityCodeException 异常：

```
org.opengis.referencing.NoSuchAuthorityCodeException: No such authority is known [EPSG]
```

## Goals

1. 在 pom.xml 中添加 gt-epsg-hsql 依赖
2. 验证 Maven 编译通过

## Success Criteria

- pom.xml 包含 gt-epsg-hsql 依赖
- `mvn compile` 无错误

## Non-Goals

- 不修改 GeoTiffParser 代码（已使用反射方式兼容）
- 不修改其他 GeoTools 相关代码

## Affected Files

| 文件 | 操作 |
|------|------|
| backend/pom.xml | 添加 gt-epsg-hsql 依赖 |
