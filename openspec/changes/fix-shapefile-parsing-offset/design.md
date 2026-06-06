## Context

当前系统的 Shapefile 导入功能使用自定义二进制解析器读取 .shp 文件。诊断发现 parseShpFile() 方法在解析记录时偏移量错误：

Shapefile 文件结构:
```
[主文件头 100 字节]
  - 位置 0-15: 文件代码、长度
  - 位置 16-19: Shape Type
  - 位置 20-99: 边界框

[记录 1]
  - 位置 100-103: 记录号 (4 字节)
  - 位置 104-107: 内容长度 (4 字节，以 16-bit words 为单位)
  - 位置 108+: 实际几何数据 ← Shape Type 在这里!
```

当前代码从位置 100 直接读取 shape type，错误地读到了记录号(值为1)，而不是实际的几何类型。

## Goals / Non-Goals

**Goals:**
1. 修复记录头偏移问题，确保正确读取 shape type
2. 添加调试日志便于问题排查
3. 改进错误处理机制

**Non-Goals:**
- 不改变 API 接口
- 不切换到 GeoTools ShapefileDataStore（保持当前手动解析）
- 不添加新功能

## Decisions

### 修复方案选择

**选项 A: 跳过记录头 (推荐)**
- 在读取 recordShapeType 之前，先额外读取/跳过 4 字节
- 最小改动，风险最低

**选项 B: 使用内容长度精确跳转**
- 读取内容长度，使用 `100 + 8 + contentLength - 4` 精确跳到下一条记录
- 更准确但改动略大

**选择**: 选项 A，因为改动最小且能立即解决问题

### 日志级别

**决策**: 使用 info 级别打印解析结果，便于确认工作正常
- 打印: 解析到的几何数量、shape type 值
- 不使用 debug，避免排查时信息不足

## Risks / Trade-offs

- **风险**: 如果有 Null Shape (shape type = 0) 误判为无效 → 需确保 parseShpRecord 正确处理
- **风险**: Polygon 可能有多个 ring，当前解析只处理第一个 ring 的外边界 → 简化处理，后续根据需要扩展
