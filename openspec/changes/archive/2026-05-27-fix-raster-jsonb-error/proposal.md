# Proposal: fix-raster-jsonb-error

## Summary

修复 RasterMetadata 实体中 JSONB 字段的类型转换错误，通过全局注册 JacksonTypeHandler 实现 Map 与 JSONB 的自动转换。

## Problem Statement

raster_metadata 表的 transform 和 overviews 字段定义为 JSONB 类型。当前存在两种处理方式的手动转换问题：

1. 当前方式：在 GeoTiffParser 中手动使用 JSONUtil.toJsonStr() 转换为 String
2. 问题：代码冗余，需要在每处手动转换

解决方案：全局注册 JacksonTypeHandler，让 MyBatis-Plus 自动处理 Map → JSONB 的转换。

## Goals

1. 在 RasterMetadata 实体中将 transform 和 overviews 改为 Map 类型
2. 在 MybatisPlusConfig 中全局注册 JacksonTypeHandler
3. 添加 @TableField 注解指定 typeHandler
4. 删除 GeoTiffParser 中手动 JSON 字符串转换的代码

## Success Criteria

- Maven 编译通过
- 影像上传时 transform 和 overviews 字段自动存储为 JSONB

## Non-Goals

- 不修改其他已存在的 JSONB 字段处理逻辑

## Affected Files

| 文件 | 操作 |
|------|------|
| backend/src/main/java/com/gisplatform/entity/RasterMetadata.java | 字段类型改为 Map，添加 typeHandler |
| backend/src/main/java/com/gisplatform/config/MybatisPlusConfig.java | 全局注册 JacksonTypeHandler |
| backend/src/main/java/com/gisplatform/util/GeoTiffParser.java | 删除手动 JSON 转换代码 |
