# Proposal: fix-created-by-null

## Problem Statement

When importing a dataset via the file upload feature, the following database error occurs:

```
null value in column "created_by" of relation "dataset" violates not-null constraint
```

The `dataset` table has a NOT NULL constraint on `created_by`, but the application does not set this field when creating Dataset records.

## Why This Matters

- **Data Integrity**: The NOT NULL constraint exists to track data ownership and audit trails
- **Feature Blocked**: Users cannot import datasets through the UI
- **System Incomplete**: Other create operations for Dataset may also be affected

## Affected Operations

1. **File Upload Import**: `GisDataParserServiceImpl.importToPostGIS()`
   - When user uploads GeoJSON file to create dataset

2. **Basic Create**: `DatasetServiceImpl.createDataset()`
   - When creating dataset via the simple form

## Proposed Solution

Create a utility class `CurrentUserUtils` to get the current authenticated user from Spring Security context, then inject it into both services to set `created_by` before saving.

## Scope

- Backend only (no frontend changes needed)
- Fix 2 service classes
- Add 1 new utility class
- No API contract changes

## Risk Assessment

- **Risk Level**: Low
- **Impact**: Localized to dataset creation operations
- **Rollback**: Easy - remove utility class usage

## Dependencies

- Requires Spring Security configuration (already present)
- Uses existing JWT authentication flow
