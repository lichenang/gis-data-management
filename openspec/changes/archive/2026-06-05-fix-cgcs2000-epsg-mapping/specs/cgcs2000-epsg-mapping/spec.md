# cgcs2000-epsg-mapping

## ADDED Requirements

### Requirement: CrsTransformUtil SHALL correctly map CGCS2000 3-degree Gauss-Kruger zones to EPSG codes

The system SHALL provide accurate EPSG code mappings for CGCS2000 3-degree Gauss-Kruger projection coordinate systems.

#### Scenario: Zone 38 maps to EPSG:4527
- **WHEN** CRS name contains "CGCS2000_3_Degree_GK_Zone_38" or "CGCS2000 / 3-degree Gauss-Kruger zone 38"
- **THEN** the system SHALL return EPSG code 4527

#### Scenario: Zone 35 maps to EPSG:4524
- **WHEN** CRS name contains "CGCS2000_3_Degree_GK_Zone_35" or "CGCS2000 / 3-degree Gauss-Kruger zone 35"
- **THEN** the system SHALL return EPSG code 4524

#### Scenario: Zone 36 maps to EPSG:4525
- **WHEN** CRS name contains "CGCS2000_3_Degree_GK_Zone_36" or "CGCS2000 / 3-degree Gauss-Kruger zone 36"
- **THEN** the system SHALL return EPSG code 4525

#### Scenario: Zone 37 maps to EPSG:4526
- **WHEN** CRS name contains "CGCS2000_3_Degree_GK_Zone_37" or "CGCS2000 / 3-degree Gauss-Kruger zone 37"
- **THEN** the system SHALL return EPSG code 4526

#### Scenario: Zone 39 maps to EPSG:4528
- **WHEN** CRS name contains "CGCS2000_3_Degree_GK_Zone_39" or "CGCS2000 / 3-degree Gauss-Kruger zone 39"
- **THEN** the system SHALL return EPSG code 4528

#### Scenario: Zone 40 maps to EPSG:4529
- **WHEN** CRS name contains "CGCS2000_3_Degree_GK_Zone_40" or "CGCS2000 / 3-degree Gauss-Kruger zone 40"
- **THEN** the system SHALL return EPSG code 4529

#### Scenario: Underscore format is supported
- **WHEN** CRS name uses underscore format like "CGCS2000_3_Degree_GK_Zone_38"
- **THEN** the system SHALL correctly match and return the corresponding EPSG code

#### Scenario: Slash format is supported
- **WHEN** CRS name uses slash format like "CGCS2000 / 3-degree Gauss-Kruger zone 38"
- **THEN** the system SHALL correctly match and return the corresponding EPSG code
