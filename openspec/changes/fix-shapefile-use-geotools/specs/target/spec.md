# Shapefile GeoTools 解析规格

## ADDED Requirements

### Requirement: 使用 GeoTools 解析 Shapefile
MultiFormatImportService SHALL use GeoTools ShapefileDataStore to parse .shp files instead of manual binary parsing.

#### Scenario: Parse shapefile from direct upload
- **WHEN** User uploads a .shp file directly
- **THEN** System SHALL use ShapefileDataStore to read and extract features

#### Scenario: Parse shapefile from ZIP
- **WHEN** User uploads a .zip file containing Shapefile
- **THEN** System SHALL extract .shp file to temp directory and use ShapefileDataStore

#### Scenario: Extract feature count from shapefile
- **WHEN** Parsing shapefile for preview (parseShapefile)
- **THEN** System SHALL return accurate feature count from data source

### Requirement: 正确处理各种几何类型
The system SHALL correctly parse Point, LineString, Polygon, and MultiPoint shapefiles.

#### Scenario: Import Point shapefile
- **WHEN** Import a Point type shapefile
- **THEN** All Point features SHALL be written to PostGIS

#### Scenario: Import Polygon shapefile
- **WHEN** Import a Polygon type shapefile
- **THEN** All Polygon features SHALL be written to PostGIS

#### Scenario: Handle Null geometry
- **WHEN** Shapefile contains features with NULL geometry
- **THEN** System SHALL skip those features and continue processing
