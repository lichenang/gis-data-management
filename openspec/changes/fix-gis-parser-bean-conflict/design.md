## Context

Spring Boot 会自动扫描实现同一接口的多个 Bean，需要通过 @Primary 或 @Qualifier 指定主实现。

## Goals / Non-Goals

Goals: 解决启动时的 Bean 冲突

Non-Goals: 不修改业务逻辑

## Decisions

方案1: 在 MultiFormatImportService 添加 @Primary (推荐)
- 保持原 GisDataParserServiceImpl 作为备用

方案2: 删除 GisDataParserServiceImpl
- 代码冗余

## Migration Plan

1. 检查 @Primary 是否已添加
2. 如已添加则只需验证
3. 如未添加则添加
4. 启动验证
