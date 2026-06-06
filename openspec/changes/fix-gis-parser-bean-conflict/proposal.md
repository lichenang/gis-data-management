## Why

当前项目中有两个实现 GisDataParserService 接口的类：
1. GisDataParserServiceImpl (原有)
2. MultiFormatImportService (新增多格式导入)

Spring 无法自动选择使用哪个 Bean，导致启动失败。

## What Changes

1. 在 MultiFormatImportService 类上添加 @Primary 注解
2. 删除或禁用 GisDataParserServiceImpl 避免重复

## Capabilities

纯技术修复，无新功能需求。

## Impact

- backend/.../service/impl/MultiFormatImportService.java
- backend/.../service/impl/GisDataParserServiceImpl.java

## Non-Goals

- 不改变导入功能逻辑
