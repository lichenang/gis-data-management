# Proposal: fix-add-geotiff-dependency

## Summary

在 pom.xml 中添加缺失的 gt-geotiff 依赖，解决影像上传模块编译失败问题。

## Problem Statement

影像上传模块（GeoTiffParser.java）使用 GeoTiffReader 读取 GeoTIFF 文件，但 pom.xml 中缺少 `gt-geotiff` 模块依赖，导致运行时 NoClassDefFoundError。

错误信息：
```
java.lang.NoClassDefFoundError: org/geotools/gce/geotiff/GeoTiffReader
```

## Goals

1. 在 pom.xml 中添加 gt-geotiff 依赖
2. 验证 Maven 编译通过

## Success Criteria

- pom.xml 包含 gt-geotiff 依赖
- `mvn compile` 无错误

## Non-Goals

- 不修改其他代码（GeoTiffParser.java 的 import 已正确）
- 不修改其他 GeoTools 相关代码

## Affected Files

| 文件 | 操作 |
|------|------|
| backend/pom.xml | 添加 gt-geotiff 依赖 |
