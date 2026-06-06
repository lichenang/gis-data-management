# handle-missing-prj

## MODIFIED Requirements

### Requirement: importUsingDataStore SHALL use user-specified sourceSrs when native CRS cannot be identified

When the native CRS from a Shapefile cannot be identified (nativeSrid == 0) and the user has provided a sourceSrs parameter, the system SHALL use the user-specified sourceSrs as the source coordinate system for ST_Transform.

**Original Behavior**: When native CRS was unknown, the system would use targetSrs as the source, which is incorrect because targetSrs represents the target coordinate system.

**New Behavior**: When native CRS is unknown, the system checks if a user-specified sourceSrs is available. If so, it uses sourceSrs as the source coordinate system.

#### Scenario: Native CRS identified successfully
- **WHEN** native CRS is identified from .prj file with EPSG code 4490
- **AND** targetSrs is EPSG:4326
- **THEN** the system SHALL transform from EPSG:4490 to EPSG:4326

#### Scenario: Native CRS cannot be identified, user provides sourceSrs
- **WHEN** native CRS cannot be identified (nativeSrid == 0)
- **AND** user provides sourceSrs = "EPSG:4490"
- **AND** targetSrs is EPSG:4326
- **THEN** the system SHALL transform from EPSG:4490 to EPSG:4326
- **AND** the system SHALL log a warning about using user-specified source SRS

#### Scenario: Native CRS cannot be identified, no sourceSrs provided
- **WHEN** native CRS cannot be identified (nativeSrid == 0)
- **AND** user does not provide sourceSrs
- **THEN** the system SHALL throw an IllegalStateException with a clear message indicating that source CRS cannot be determined

### Requirement: importUsingDataStore SHALL skip ST_Transform when source equals target

When sourceSrid equals targetSrid, the system SHALL NOT perform any coordinate transformation.

#### Scenario: Source and target are the same
- **WHEN** sourceSrid equals targetSrid (e.g., both are EPSG:4326)
- **THEN** the SQL SHALL use ST_GeomFromWKB(?, sourceSrid) without ST_Transform
- **AND** no coordinate transformation SHALL occur
