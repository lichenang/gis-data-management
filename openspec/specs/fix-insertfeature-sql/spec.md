# fix-insertfeature-sql

## ADDED Requirements

### Requirement: insertFeature SHALL generate syntactically correct INSERT SQL

The `insertFeature` method SHALL generate a syntactically correct INSERT SQL statement with properly balanced parentheses in the VALUES clause.

#### Scenario: VALUES clause has correct syntax when sourceSrid equals targetSrid
- **WHEN** `insertFeature` is called with `sourceSrid == targetSrid`
- **THEN** the generated SQL SHALL be: `INSERT INTO "schema"."table" (geometry, "col1", ...) VALUES (ST_GeomFromWKB(?, <srid>), ?, ?, ...)`

#### Scenario: VALUES clause has correct syntax when sourceSrid differs from targetSrid
- **WHEN** `insertFeature` is called with `sourceSrid != targetSrid`
- **THEN** the generated SQL SHALL be: `INSERT INTO "schema"."table" (geometry, "col1", ...) VALUES (ST_Transform(ST_GeomFromWKB(?, <sourceSrid>), <targetSrid>), ?, ?, ...)`
