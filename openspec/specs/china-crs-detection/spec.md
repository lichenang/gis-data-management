# china-crs-detection

## 模块划分

| 模块 | 职责 | 边界 |
|------|------|------|
| `CrsTransformUtil` | 坐标系识别和 EPSG 代码转换 | 仅负责 CRS 识别，不处理几何转换 |
| `MultiFormatImportServiceImpl` | Shapefile 导入时的坐标系处理 | 仅负责矢量数据导入的坐标系逻辑 |

## 数据流设计

```
Shapefile ZIP 上传
        ↓
VectorDataStoreFactory 解压并创建 ShapefileDataStore
        ↓
MultiFormatImportServiceImpl.importUsingDataStore()
        ↓
读取 schema.getGeometryDescriptor().getCRS() 获取原生 CRS
        ↓
CrsTransformUtil.getEpsgCode(crs) 尝试识别 EPSG 代码
        │
        ├── 成功获取 nativeSrid > 0
        │       ↓
        │   使用 nativeSrid 作为源 SRS，目标为 targetSrs (默认 4326)
        │       ↓
        │   ST_Transform(geometry, nativeSrid, targetSrid)
        │
        └── 获取失败 nativeSrid == 0
                ↓
            检查 targetSrs 是否有效
                │
                ├── targetSrs 有效 → 记录警告，使用 targetSrs 作为源
                │       ↓
                │   ST_GeomFromWKB(wkb, targetSrid)  // 无转换
                │
                └── targetSrs 无效 → 抛出 IllegalStateException
                        ↓
                    明确提示用户坐标系无法识别
```

## 接口列表

### CrsTransformUtil

| 方法 | 输入 | 输出 | 说明 |
|------|------|------|------|
| `getEpsgCode(CoordinateReferenceSystem crs)` | GeoTools CRS 对象 | int (EPSG 代码，0 表示无法识别) | 获取 CRS 对应的 EPSG 代码，支持中国坐标系识别 |
| `findInChinaMapping(String crsName)` | CRS 名称字符串 | Integer (EPSG 代码) | 在中国坐标系映射表中查找匹配 |

### MultiFormatImportServiceImpl

| 方法 | 修改 | 说明 |
|------|------|------|
| `importUsingDataStore(...)` | 新增 targetSrs 参数 | 根据 targetSrs 确定转换策略 |
| `insertFeature(...)` | 新增 sourceSrid, targetSrid 参数 | 当 sourceSrid == targetSrid 时跳过转换 |
| `parseTargetSrs(String targetSrs)` | 新增方法 | 解析 targetSrs 字符串获取 EPSG 代码 |

## ADDED Requirements

### Requirement: CrsTransformUtil SHALL recognize China common CRS by name mapping

The system SHALL provide a static mapping table that maps China common coordinate reference system names to their EPSG codes. The mapping SHALL include at least the following CRS:

- GCS_China_Geodetic_Coordinate_System_2000 (CGCS2000) → EPSG:4490
- CGCS2000 → EPSG:4490
- Beijing_1954 → EPSG:4214
- Xian_1980 → EPSG:4610
- GCS_WGS_1984 → EPSG:4326

#### Scenario: CGCS2000 CRS is correctly recognized
- **WHEN** `getEpsgCode()` is called with a CRS whose name is "GCS_China_Geodetic_Coordinate_System_2000"
- **THEN** the method SHALL return 4490

#### Scenario: Beijing 1954 CRS is correctly recognized
- **WHEN** `getEpsgCode()` is called with a CRS whose name is "Beijing_1954"
- **THEN** the method SHALL return 4214

#### Scenario: WGS 1984 CRS is correctly recognized
- **WHEN** `getEpsgCode()` is called with a CRS whose name is "GCS_WGS_1984"
- **THEN** the method SHALL return 4326

#### Scenario: Unknown CRS returns zero
- **WHEN** `getEpsgCode()` is called with a CRS that is not in the mapping table and has no EPSG identifier
- **THEN** the method SHALL return 0 and log a warning message

### Requirement: CrsTransformUtil SHALL use multi-level identification strategy

The system SHALL attempt to identify the EPSG code in the following priority order:

1. Extract from `crs.getIdentifiers()` if the identifier code is numeric
2. Match against the China CRS mapping table (exact match first, then contains match)
3. Extract "EPSG:XXX" pattern from the CRS name string

#### Scenario: Identifier with numeric code takes priority
- **WHEN** `getEpsgCode()` is called with a CRS that has both an identifier with code "4490" and name containing "CGCS2000"
- **THEN** the method SHALL return 4490 from the identifier, not from the mapping table

#### Scenario: China mapping takes priority over EPSG string extraction
- **WHEN** `getEpsgCode()` is called with a CRS whose name is "GCS_China_Geodetic_Coordinate_System_2000" (no EPSG: prefix)
- **THEN** the method SHALL match against the China mapping table and return 4490

### Requirement: MultiFormatImportServiceImpl SHALL handle unrecognized CRS gracefully

When the native CRS cannot be identified (returns 0):

- **IF** the user provided a valid target SRS: the system SHALL use the target SRS and log a warning
- **IF** the user did not provide a valid target SRS: the system SHALL throw an `IllegalStateException` with a clear message

#### Scenario: Unrecognized CRS with valid target SRS
- **WHEN** native CRS cannot be identified and user specified "EPSG:4326" as target SRS
- **THEN** the system SHALL log a warning message and proceed with target SRS as the source
- **AND** data SHALL be stored without coordinate transformation

#### Scenario: Unrecognized CRS without valid target SRS
- **WHEN** native CRS cannot be identified and user did not specify a valid target SRS
- **THEN** the system SHALL throw an `IllegalStateException`
- **AND** the exception message SHALL indicate both the native CRS name and the invalid target SRS

### Requirement: MultiFormatImportServiceImpl SHALL skip transformation when source equals target

When `sourceSrid` equals `targetSrid`, the system SHALL NOT perform any coordinate transformation.

#### Scenario: No transformation needed when source equals target
- **WHEN** importing data with nativeSrid=4326 and targetSrs="EPSG:4326"
- **THEN** the SQL SHALL use `ST_GeomFromWKB(?, 4326)` instead of `ST_Transform(...)`
- **AND** no coordinate transformation SHALL occur

### Requirement: Import process SHALL provide detailed logging

The system SHALL log the following information during the import process:

- Native CRS name and identified EPSG code (or 0 if unrecognized)
- Target EPSG code
- Warning when falling back to user-specified SRS
- Exception with clear message when CRS cannot be determined

#### Scenario: Successful import with CGCS2000 data
- **WHEN** importing a Shapefile with CGCS2000 CRS
- **THEN** the log SHALL contain "Identified native CRS: GCS_China_Geodetic_Coordinate_System_2000 (EPSG:4490), will transform to EPSG:4326"
