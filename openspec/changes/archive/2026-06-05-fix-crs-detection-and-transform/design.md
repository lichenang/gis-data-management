## Context

当前 `CrsTransformUtil.getEpsgCode()` 方法只能识别显式包含 "EPSG:" 标识的坐标系，无法识别中国常用坐标系（CGCS2000、Beijing 1954、Xian 1980 等）。这导致 Shapefile 导入时坐标系识别失败，源 SRS 被错误地硬编码为 EPSG:4326，投影坐标直接作为经纬度存储，造成巨大的定位偏差。

### 问题链分析

```
GeoTools 读取 .prj 文件 → nativeCrs = "GCS_China_Geodetic_Coordinate_System_2000"
        ↓
CrsTransformUtil.getEpsgCode() → 0 (无法识别)
        ↓
sourceSrid = 0 > 0 ? nativeSrid : 4326  → 4326 (错误的默认值)
        ↓
ST_Transform(ST_GeomFromWKB(?, 4326), 4326) → 无转换发生
        ↓
PostGIS 直接存储原始坐标，但被当作 WGS84 解释
```

## Goals / Non-Goals

**Goals:**
- 建立中国常用坐标系名称到 EPSG 代码的映射表
- 增强 `CrsTransformUtil.getEpsgCode()` 方法的识别能力
- 改进 `MultiFormatImportServiceImpl` 的错误处理和回退逻辑
- 添加明确的日志记录和异常信息

**Non-Goals:**
- 不修改现有的 extent 转换逻辑
- 不修改 GeoJSON 导入逻辑（已有正确的坐标系处理）
- 不修改影像数据的坐标系处理
- 不添加新的 API 接口

## Decisions

### Decision 1: 建立静态映射表而非运行时查询

**方案**：在 `CrsTransformUtil` 中添加静态 `Map<String, Integer>` 映射表

**理由**：
- 中国坐标系种类有限，映射表可以穷举
- 静态表查询 O(1)，性能最优
- 不引入外部依赖，保持简单
- 保留原有识别逻辑作为后备

### Decision 2: 多级识别策略

**方案**：按优先级尝试以下方法获取 EPSG 代码：

1. **Identifier 提取**（最高优先级）：从 `crs.getIdentifiers()` 提取数字代码
2. **映射表完整匹配**（次高优先级）：在映射表中查找完整名称
3. **映射表包含匹配**（中等优先级）：检查 CRS 名称是否包含映射表中的关键词
4. **EPSG 字符串提取**（最后后备）：从 `crs.getName()` 提取 "EPSG:" 模式

### Decision 3: 识别失败时的分层处理策略

**方案**：在 `MultiFormatImportServiceImpl` 中实现三层处理：

| 情况 | 处理策略 |
|------|---------|
| `nativeSrid > 0` | 正常用于 ST_Transform |
| `nativeSrid == 0 && 用户指定了有效 targetSrs` | 使用 targetSrs，记录警告日志 |
| `nativeSrid == 0 && 无有效 targetSrs` | 抛出 `IllegalStateException`，明确说明坐标系无法识别 |

### Decision 4: 使用 Slf4J 日志框架

**理由**：
- 项目已在 `CrsTransformUtil` 上使用 `@Slf4j` (Lombok)
- `MultiFormatImportServiceImpl` 也使用 Slf4J
- 保持一致性

## 中国常用坐标系映射表

### 地理坐标系

| CRS 名称 | EPSG 代码 | 说明 |
|---------|----------|------|
| GCS_China_Geodetic_Coordinate_System_2000 | 4490 | CGCS2000 |
| CGCS2000 | 4490 | CGCS2000 简称 |
| China_2000 | 4490 | CGCS2000 别名 |
| GCS_Beijing_1954 | 4214 | 北京 1954 |
| GCS_Xian_1980 | 4610 | 西安 1980 |
| GCS_WGS_1984 | 4326 | WGS 84 |

### 投影坐标系

| CRS 名称 | EPSG 代码 | 说明 |
|---------|----------|------|
| CGCS2000 / 3-degree Gauss-Kruger zone 37 | 4491 | 山西东部 |
| CGCS2000 / 3-degree Gauss-Kruger zone 38 | 4492 | 山西中部 |
| CGCS2000 / 3-degree Gauss-Kruger zone 39 | 4493 | 山西西部 |
| CGCS2000 / 3-degree Gauss-Kruger zone 40 | 4494 | 山西与陕西交界 |
| Beijing 1954 / 3-degree Gauss-Kruger CM 117E | 2433 | - |
| Beijing 1954 / 3-degree Gauss-Kruger CM 123E | 2434 | - |

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|---------|
| 新映射表不完整 | 保留原有识别逻辑作为后备，添加警告日志提示用户 |
| 某些 CRS 正确定义了 identifier 但代码非数字 | 跳过非数字 identifier，继续尝试其他方法 |
| 映射表匹配过于宽松 | 优先完整匹配，其次才用包含匹配 |
| 特殊格式的 prj 文件 | 在异常信息中提示用户检查 prj 文件或手动指定 SRS |
