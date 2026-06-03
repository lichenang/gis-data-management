# Proposal: fix-geotools-imports

## Summary

修正 GeoTools 32.x Import 路径错误并补全所有新文件的 Javadoc 注释，确保代码编译通过并符合注释规范。

## Problem Statement

GeoTools 从 29.x 升级到 32.x 后，包结构发生重大变更：
- `org.opengis.*` 包已移至 `org.geotools.api.*`
- `org.geotools.coverage.*` 移至 `org.geotools.coverage2.*`

当前 `add-image-upload` 变更创建的 `GeoTiffParser.java` 使用了过时的 import 路径，导致编译失败。同时，根据 config.yaml 的注释规范（强制），多个新建的 Java 文件缺少 Javadoc 注释。

## Goals

1. 修正 GeoTiffParser.java 中的所有错误 import 路径
2. 为 6 个 Java 文件补全中文 Javadoc 注释（类级别 + 方法级别）

## Success Criteria

- Maven 编译通过：`mvn compile` 无错误
- 所有 public 类、方法都有中文 Javadoc 注释
- 代码符合 config.yaml 中的注释规范

## Non-Goals

- 不修改现有已正确工作的代码（如 GisDataParserServiceImpl.java）
- 不修改 RasterMetadata 实体类（已有 @Schema 注解）

## Affected Files

| 文件 | 操作 |
|------|------|
| GeoTiffParser.java | 修正 import + 添加 Javadoc |
| ImageService.java | 添加 Javadoc |
| ImageServiceImpl.java | 添加 Javadoc |
| ImageController.java | 添加方法级 Javadoc |
| MinioConfig.java | 添加 Javadoc |
| RasterMetadataMapper.java | 添加 Javadoc |
