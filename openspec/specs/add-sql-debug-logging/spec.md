# add-sql-debug-logging

## ADDED Requirements

### Requirement: insertFeature SHALL log generated SQL for debugging

The `insertFeature` method SHALL log the generated SQL statement and placeholder count when executing INSERT operations.

#### Scenario: Successful SQL generation
- **WHEN** `insertFeature` generates an INSERT SQL statement
- **THEN** the system SHALL log INFO level message containing:
  - Number of columns in the INSERT statement
  - Number of `?` placeholders in the SQL
  - The full SQL statement (truncated if too long)

#### Scenario: Placeholder count mismatch detection
- **WHEN** `insertFeature` generates an INSERT SQL statement
- **THEN** if the placeholder count does not match the column count, the system SHALL throw an `IllegalStateException` with a message containing:
  - Expected placeholder count (based on column count)
  - Actual placeholder count found in the SQL
  - The full SQL statement for inspection
