## Why

Shapefile 导入功能在解析二进制记录时存在偏移量错误，导致所有几何记录被跳过，最终写入 PostGIS 的数据为空。这是由于自定义 Shapefile 解析器未正确跳过记录头（记录号 4 字节 + 内容长度 4 字节），直接读取了错误的位置作为 shape type。

## What Changes

1. **修复 parseShpFile 记录头偏移**
   - 修改 `MultiFormatImportService.parseShpFile()` 方法
   - 在读取每条记录的 shape type 之前，先跳过 8 字节的记录头
   - 确保 recordShapeType 读到的是实际的几何类型，不是记录号或内容长度

2. **添加解析调试日志**
   - 打印实际解析到的几何数量，便于排查问题
   - 打印每条记录的 shape type 值，验证解析正确性

3. **改进错误处理**
   - 统计失败记录数，失败率高时给出警告
   - 区分解析异常和正常情况

## Capabilities

### New Capabilities
- `shapefile-binary-parsing`: 正确解析 Shapefile 二进制格式

### Modified Capabilities
- `multi-format-vector-import`: 修复现有 Shapefile 导入功能

## Impact

- `backend/src/main/java/com/gisplatform/service/impl/MultiFormatImportService.java` - 修复 parseShpFile() 方法
- 无前端影响

## Non-Goals

- 不改变现有的 API 接口契约
- 不添加新的导入格式
- 暂不切换到 GeoTools ShapefileDataStore（当前手动解析已足够）
