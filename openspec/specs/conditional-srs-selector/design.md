# Design: Conditional SRS Selector

## Context

当前 Shapefile 上传时，前端始终显示"源坐标系"下拉框。但合理的交互应该是：
- 当 .prj 文件存在且 CRS 能被识别时 → 显示"已自动识别: EPSG:4490"，隐藏下拉框
- 当 .prj 文件缺失或 CRS 无法识别时 → 显示警告 + 源坐标系下拉框

## Current Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                    CURRENT PARSE FLOW                               │
└─────────────────────────────────────────────────────────────────────┘

  Frontend                        Backend
     │                               │
     │  POST /datasets/parse         │
     │ ────────────────────────────▶ │
     │                               │
     │                               │  parseFile()
     │                               │  ├─ formatDetector.detect()
     │                               │  └─ Returns:
     │                               │      { srs: "EPSG:4326",  ← 始终固定值
     │                               │        geometryType: "Unknown",
     │                               │        featureCount: 0 }
     │                               │
     │  ◀────────────────────────────│
     │  Response                     │
     │                               │
     │  Frontend always shows        │
     │  source SRS selector          │
```

## Proposed Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                PROPOSED: PARSE WITH CRS DETECTION                   │
└─────────────────────────────────────────────────────────────────────┘

  STEP 1: Parse File with CRS Detection
  ──────────────────────────────────────

  Frontend                        Backend
     │                               │
     │  POST /datasets/parse         │
     │ ────────────────────────────▶ │
     │                               │
     │                               │  parseFile()
     │                               │  ├─ formatDetector.detect()
     │                               │  ├─ createShapefileDataStore()  ← NEW
     │                               │  ├─ getFeatureSource().getSchema()
     │                               │  ├─ schema.getGeometryDescriptor().getCRS()
     │                               │  └─ CrsTransformUtil.getEpsgCode()
     │                               │
     │                               │  Returns GisDataParseResult:
     │                               │    - srs: "EPSG:4490" or null
     │                               │    - crsDetected: true/false    ← NEW
     │                               │
     │  ◀────────────────────────────│

  STEP 2: Frontend Conditional Display
  ──────────────────────────────────────

  if (result.crsDetected) {
    // .prj exists and CRS recognized
    // Show: "已自动识别: EPSG:4490 (CGCS2000)"
    // Hide: source SRS selector, warning
  } else {
    // .prj missing or unrecognized
    // Show: warning + source SRS selector
  }
```

## Data Model Changes

### GisDataParseResult (DTO)

```java
@Data
public class GisDataParseResult {
    private boolean success;
    private String message;
    private String geometryType;
    private String srs;              // null if CRS not detected
    private int featureCount;
    private double[] bounds;
    private String tableName;
    private String format;
    private List<String> properties;
    private boolean crsDetected;     // NEW: indicates if CRS was detected
}
```

## Implementation Plan

### Backend Changes

1. **GisDataParseResult.java**
   - Add `crsDetected: boolean` field

2. **MultiFormatImportServiceImpl.parseFile()**
   - For Shapefile format: create temp DataStore and extract CRS
   - Use `CrsTransformUtil.getEpsgCode()` to get EPSG code
   - Set `crsDetected` based on whether EPSG code > 0

### Frontend Changes

1. **api/dataset.ts - GisDataParseResult interface**
   - Add `crsDetected?: boolean` field

2. **views/datasets/index.vue**
   - Add computed: `isCrsDetected = computed(() => parseResult.value?.crsDetected !== false)`
   - Modify template:
     - If `isCrsDetected`: show "已自动识别: {srs}"
     - Else: show warning + source SRS selector

## API Response Examples

### CRS Detected

```json
{
  "success": true,
  "message": "文件格式: Shapefile",
  "format": "SHP",
  "geometryType": "Point",
  "srs": "EPSG:4490",
  "crsDetected": true,
  "featureCount": 1500,
  "bounds": [73.0, 18.0, 135.0, 54.0]
}
```

### CRS Not Detected

```json
{
  "success": true,
  "message": "文件格式: Shapefile",
  "format": "SHP",
  "geometryType": "Point",
  "srs": null,
  "crsDetected": false,
  "featureCount": 1500,
  "bounds": [73.0, 18.0, 135.0, 54.0]
}
```

## UI Mockup

### Case 1: CRS Detected

```
┌──────────────────────────────────────────────────────────────┐
│  📁 上传空间数据（可选）                                       │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌────────────────────────────────────────────────────────┐  │
│  │     拖拽文件到此处或 点击上传                            │  │
│  │                                                          │  │
│  │     支持格式：GeoJSON、Shapefile...                      │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                              │
│  ✓ 文件解析成功                                              │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  几何类型: Point        要素数量: 1500                   │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                              │
│  坐标系: 已自动识别 EPSG:4490 (CGCS2000)          ← 显示    │
│                                                              │
│  [取消]                           [仅创建]    [导入并创建]    │
└──────────────────────────────────────────────────────────────┘
```

### Case 2: CRS Not Detected

```
┌──────────────────────────────────────────────────────────────┐
│  📁 上传空间数据（可选）                                       │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌────────────────────────────────────────────────────────┐  │
│  │     拖拽文件到此处或 点击上传                            │  │
│  │                                                          │  │
│  │     支持格式：GeoJSON、Shapefile...                      │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                              │
│  ✓ 文件解析成功                                              │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  几何类型: Point        要素数量: 1500                   │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                              │
│  ⚠️ 请手动选择原始投影                              ← 显示   │
│                                                              │
│  目标坐标系: [EPSG:4326 - WGS84          ▼]                   │
│                                                              │
│  源坐标系:    [EPSG:4490 - CGCS2000     ▼]          ← 显示   │
│                                                              │
│  [取消]                           [仅创建]    [导入并创建]    │
└──────────────────────────────────────────────────────────────┘
```

## Non-Goals

- 不修改导入时的 CRS 转换逻辑（已在 fix-crs-missing-prj 中实现）
- 不在前端 parse 阶段验证用户选择的 sourceSrs 是否有效
