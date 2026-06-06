# Shapefile GeoTools 解析规格

## ADDED Requirements

### Requirement: 使用 GeoTools 解析 Shapefile
MultiFormatImportService SHALL use GeoTools ShapefileDataStore to parse .shp files.

#### Scenario: Parse direct upload
- **WHEN** User uploads .shp file directly
- **THEN** System SHALL use ShapefileDataStore to read features

#### Scenario: Parse from ZIP
- **WHEN** User uploads .zip containing Shapefile
- **THEN** System SHALL extract to temp directory and parse with ShapefileDataStore

### Requirement: Import Point and Polygon shapefiles
The system SHALL correctly import both Point and Polygon type shapefiles.

#### Scenario: Import Point shapefile
- **WHEN** Import Point type shapefile
- **THEN** All features written to PostGIS

#### Scenario: Import Polygon shapefile  
- **WHEN** Import Polygon type shapefile
- **THEN** All features written to PostGIS
