# fix-srs-required

## MODIFIED Requirements

### Requirement: sourceSrs SHALL be required when CRS detection fails

When the parseFile response indicates CRS detection failure (crsDetected=false), the sourceSrs field SHALL be required before import submission.

#### Scenario: CRS detection failed - sourceSrs required
- **WHEN** parseFile response has `crsDetected: false`
- **THEN** the sourceSrs field SHALL be marked as required
- **AND** the import button SHALL be disabled until sourceSrs is selected

#### Scenario: CRS detected successfully - sourceSrs optional
- **WHEN** parseFile response has `crsDetected: true`
- **THEN** the sourceSrs field SHALL be hidden or optional
- **AND** the user SHALL be able to submit the import without selecting sourceSrs
