# fix-insertfeature-sql-params

## MODIFIED Requirements

### Requirement: insertFeature SHALL generate SQL with correct placeholder count

The `insertFeature` method SHALL generate an INSERT SQL statement where the number of `?` placeholders exactly matches the number of columns in the VALUES clause.

**Original Behavior**: The VALUES clause was constructed with mismatched parentheses, causing the SQL parser to expect more parameters than were actually defined.

**New Behavior**: The VALUES clause is constructed as a single properly-formed expression: `VALUES (ST_GeomFromWKB(?, <srid>), ?, ?, ...)` with exactly N placeholders for N columns.

#### Scenario: VALUES clause has correct syntax when sourceSrid equals targetSrid
- **WHEN** `insertFeature` is called with `sourceSrid == targetSrid` and 2 property columns
- **THEN** the generated SQL SHALL be: `INSERT INTO "schema"."table" (geometry, "col1", "col2") VALUES (ST_GeomFromWKB(?, 4326), ?, ?)`
- **AND** there SHALL be exactly 3 placeholders

#### Scenario: VALUES clause has correct syntax when sourceSrid differs from targetSrid
- **WHEN** `insertFeature` is called with `sourceSrid != targetSrid` and 2 property columns
- **THEN** the generated SQL SHALL be: `INSERT INTO "schema"."table" (geometry, "col1", "col2") VALUES (ST_Transform(ST_GeomFromWKB(?, 4490), 4326), ?, ?)`
- **AND** there SHALL be exactly 3 placeholders

#### Scenario: No property columns
- **WHEN** `insertFeature` is called with 0 property columns
- **THEN** the generated SQL SHALL be: `INSERT INTO "schema"."table" (geometry) VALUES (ST_GeomFromWKB(?, 4326))`
- **AND** there SHALL be exactly 1 placeholder
