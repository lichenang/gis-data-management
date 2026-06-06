# conditional-srs-selector

## MODIFIED Requirements

### Requirement: parseFile SHALL return CRS detection status

The parseFile API SHALL return information indicating whether the coordinate reference system was successfully detected from the file.

#### Scenario: Shapefile with valid .prj file
- **WHEN** user uploads a Shapefile that contains a valid .prj file
- **AND** the CRS can be identified from the .prj file
- **THEN** the system SHALL return `srs` with the detected EPSG code (e.g., "EPSG:4490")
- **AND** the system SHALL return `crsDetected: true`

#### Scenario: Shapefile without .prj file
- **WHEN** user uploads a Shapefile that lacks a .prj file
- **THEN** the system SHALL return `srs: null`
- **AND** the system SHALL return `crsDetected: false`

#### Scenario: Shapefile with unrecognized CRS
- **WHEN** user uploads a Shapefile with a .prj file containing an unrecognized CRS
- **THEN** the system SHALL return `srs: null`
- **AND** the system SHALL return `crsDetected: false`

### Requirement: Frontend SHALL conditionally display source SRS selector

The upload dialog SHALL only display the source SRS selector when CRS detection fails.

#### Scenario: CRS detected successfully
- **WHEN** parseFile response has `crsDetected: true`
- **THEN** the system SHALL display a read-only CRS label showing the detected value
- **AND** the system SHALL NOT display the source SRS dropdown
- **AND** the system SHALL NOT display the warning message

#### Scenario: CRS detection failed
- **WHEN** parseFile response has `crsDetected: false`
- **THEN** the system SHALL display a warning message: "请手动选择原始投影"
- **AND** the system SHALL display the source SRS dropdown
- **AND** the dropdown SHALL contain common China CRS options (CGCS2000, WGS84, etc.)
