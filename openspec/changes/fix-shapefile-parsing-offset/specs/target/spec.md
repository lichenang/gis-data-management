# Shapefile 解析修复规格

基于 openspec/specs/multi-format-vector-support.md

## ADDED Requirements

### Requirement: Shapefile 二进制记录正确解析
MultiFormatImportService SHALL correctly parse Shapefile binary records by skipping the record header before reading shape type.

#### Scenario: Parse Point shapefile
- **WHEN** Import a .shp file containing Point geometries
- **THEN** parseShpFile() SHALL extract all Point geometries correctly
- **AND** insertShapefileRecords() SHALL write all geometries to PostGIS

#### Scenario: Parse Polygon shapefile
- **WHEN** Import a .shp file containing Polygon geometries
- **THEN** parseShpFile() SHALL extract all Polygon geometries correctly
- **AND** The resulting table SHALL contain the expected number of records

### Requirement: 解析结果可观测
The system SHALL log the number of parsed geometries for debugging purposes.

#### Scenario: Successful parse with count
- **WHEN** Shapefile is parsed successfully
- **THEN** System SHALL log: "解析 Shapefile 完成，共 X 条几何记录"
