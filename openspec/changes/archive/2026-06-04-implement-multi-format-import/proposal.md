## Why

当前系统的多格式矢量导入功能不完整：
1. MultiFormatImportService 对 Shapefile 返回"功能暂未实现"提示
2. FormatDetector 读取 ZIP 文件时抛出 MalformedInputException
3. 前端显示不一致的错误提示

## What Changes

1. **完成 Shapefile 导入逻辑**
   - 使用 JTS 直接解析 Shapefile 二进制格式
   - 支持 .shp 文件和 .zip 包上传
   - 实现 PostGIS 写入

2. **修复 FormatDetector**
   - 使用二进制模式读取 ZIP 文件
   - 正确处理文件名编码
   - 正确识别 Shapefile 和 KML 格式

3. **统一前端提示**
   - 移除"暂未实现"字样
   - 根据后端结果统一显示成功/失败

## Capabilities

基于 openspec/specs/multi-format-vector-support.md 规格实现。

## Impact

- backend/.../service/FormatDetector.java - 修复 ZIP 读取
- backend/.../service/impl/MultiFormatImportService.java - 完成 Shapefile/KML 导入
- frontend/.../views/datasets/index.vue - 前端提示优化

## Non-Goals

- 不改变 GeoJSON 导入现有逻辑
- 不添加新的导出格式
