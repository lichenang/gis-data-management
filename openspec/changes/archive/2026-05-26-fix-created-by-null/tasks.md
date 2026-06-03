# Tasks: fix-created-by-null

## Task List

### Task 1: Create CurrentUserUtils utility class
- [x] **ID**: create-current-user-utils
- **Description**: Create a utility class to get current authenticated user from Spring Security context
- **File**: `backend/src/main/java/com/gisplatform/security/CurrentUserUtils.java`
- **Dependencies**: None

**Implementation**:
1. Create new Java class with `@Component` annotation
2. Add `getCurrentUserId()` method that retrieves user ID from SecurityContextHolder
3. Add `getCurrentUsername()` method for convenience

---

### Task 2: Update GisDataParserServiceImpl  
- [x] **ID**: update-gis-data-parser-service
- **Description**: Inject CurrentUserUtils and set created_by when importing dataset
- **File**: `backend/src/main/java/com/gisplatform/service/impl/GisDataParserServiceImpl.java`
- **Dependencies**: create-current-user-utils

**Implementation**:
1. Add `@Autowired private CurrentUserUtils currentUserUtils;` field
2. In `importToPostGIS()` method, before `datasetMapper.insert(dataset);`, add:
   ```java
   dataset.setCreatedBy(currentUserUtils.getCurrentUserId());
   ```

---

### Task 3: Update DatasetServiceImpl
- [x] **ID**: update-dataset-service-impl
- **Description**: Inject CurrentUserUtils and set created_by when creating dataset
- **File**: `backend/src/main/java/com/gisplatform/service/impl/DatasetServiceImpl.java`
- **Dependencies**: create-current-user-utils

**Implementation**:
1. Add `@Autowired private CurrentUserUtils currentUserUtils;` field
2. In `createDataset()` method, before `return this.save(dataset);`, add:
   ```java
   dataset.setCreatedBy(currentUserUtils.getCurrentUserId());
   ```

---

### Task 4: Verify and test
- [x] **ID**: verify-fix
- **Description**: Compile and verify the fix works correctly
- **Dependencies**: update-gis-data-parser-service, update-dataset-service-impl

**Implementation**:
1. Run `mvn compile` to verify no compilation errors
2. Start the application
3. Test dataset creation via API/UI
4. Verify created_by is properly set in database

---

## Execution Order

```
Task 1 (create-current-user-utils)
    │
    ├──────────────────┬──────────────────┐
    ▼                  ▼
Task 2            Task 3
(GisDataParser)   (DatasetService)
    │                  │
    └────────┬─────────┘
             ▼
       Task 4 (Verify)
```

All tasks completed and verified.
