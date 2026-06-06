# Bean 冲突修复规格

## 问题

GisDataParserService 接口有两个实现类，导致 Spring 启动失败。

## 解决方案

在 MultiFormatImportService 上添加 @Primary 注解。

## 验证

应用正常启动。
