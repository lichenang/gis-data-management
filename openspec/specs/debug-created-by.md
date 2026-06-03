# Debug: created_by NOT NULL Constraint Violation

## Error Summary

When importing a dataset via the file upload feature, the following error occurs:

```
null value in column "created_by" of relation "dataset" violates not-null constraint
```

## Root Cause Analysis

### Database Schema
The `dataset` table has `created_by` defined as NOT NULL:

```sql
-- V1__init_schema.sql, line 123
created_by          BIGINT NOT NULL,
```

### Issue Locations

There are **2 places** where `Dataset` is being saved but `created_by` is not set:

#### 1. GisDataParserServiceImpl.importToPostGIS() 
**File**: `backend/src/main/java/com/gisplatform/service/impl/GisDataParserServiceImpl.java`
**Lines**: 198-211

```java
Dataset dataset = new Dataset();
dataset.setName(datasetName);
dataset.setType("vector");
dataset.setStorageType("postgis");
dataset.setTableName(tableName);
dataset.setSrs(targetSrs);
dataset.setFeatureCount(importedCount);
dataset.setGeometryType("MultiPolygon");
dataset.setStatus("draft");
dataset.setVersion(1);
dataset.setTenantId("default");
dataset.setCreateTime(LocalDateTime.now());

// ❌ MISSING: dataset.setCreatedBy(??)

datasetMapper.insert(dataset);  // <-- FAILS HERE
```

#### 2. DatasetServiceImpl.createDataset()
**File**: `backend/src/main/java/com/gisplatform/service/impl/DatasetServiceImpl.java`
**Lines**: 52-66

```java
@Override
public boolean createDataset(Dataset dataset) {
    if (dataset.getStatus() == null) {
        dataset.setStatus("draft");
    }
    // ... other null checks ...
    dataset.setCreateTime(LocalDateTime.now());
    
    // ❌ MISSING: created_by not set anywhere
    
    return this.save(dataset);
}
```

---

## Solution Design

### How to Get Current User ID

Looking at `JwtAuthenticationFilter.java` (lines 84-85):

```java
UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(username, userId, authorities);
```

The JWT filter stores `userId` (Long) in the authentication's **credentials** field.

To get the current user ID from anywhere in the application:

```java
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

Authentication auth = SecurityContextHolder.getContext().getAuthentication();
Long currentUserId = (Long) auth.getCredentials();  // userId is stored in credentials
```

---

## Fix Plan

### Option A: Create a Utility Method (Recommended)

Create a helper class to get the current user ID:

```
src/main/java/com/gisplatform/security/CurrentUserUtils.java
```

```java
@Component
public class CurrentUserUtils {
    
    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return (Long) auth.getCredentials();
    }
    
    public String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        return auth.getPrincipal().toString();
    }
}
```

Then inject into both services:

1. **GisDataParserServiceImpl**
   - Add `@Autowired private CurrentUserUtils currentUserUtils;`
   - Add: `dataset.setCreatedBy(currentUserUtils.getCurrentUserId());`

2. **DatasetServiceImpl**
   - Add `@Autowired private CurrentUserUtils currentUserUtils;`
   - Add: `dataset.setCreatedBy(currentUserUtils.getCurrentUserId());`

---

### Option B: Pass userId through method parameters

Modify the service signatures to accept `Long userId`:

```java
// DatasetService
boolean createDataset(Dataset dataset, Long createdBy);

// DatasetController  
@PostMapping
public R<Boolean> create(@RequestBody Dataset dataset) {
    Long userId = getCurrentUserId(); // extract from security context
    boolean result = datasetService.createDataset(dataset, userId);
    return R.ok(result);
}

// GisDataParserService  
DatasetImportResult importToPostGIS(MultipartFile file, String fileName, String datasetName, String targetSrs, Long createdBy);
```

---

## Comparison

| Aspect | Option A (Utility) | Option B (Parameter) |
|--------|-------------------|---------------------|
| Changes needed | 2 service files | 2 services + 2 controllers |
| Non-invasive | ✅ Yes | ❌ Requires API changes |
| Consistency | Different approach than controller | Matches controller pattern |
| Error handling | Returns null if not authenticated | Caller must handle missing user |

---

## Recommended Fix

**Option A** - Create a utility class `CurrentUserUtils`:
- It's non-invasive to existing APIs
- Can be reused across the entire application
- Allows for easy future changes (e.g., switching to @AuthenticationPrincipal)

---

## Additional Notes

The current implementation stores userId in authentication credentials:
```java
new UsernamePasswordAuthenticationToken(username, userId, authorities);
//                                    ^^^^^^ this is stored in getCredentials()
```

Alternative pattern used elsewhere in enterprise apps - store in principal:
```java
// More common pattern:
new UsernamePasswordAuthenticationToken(
    new CustomUserDetails(userId, username),  // principal
    null,                                      // credentials  
    authorities
);
```

For future consideration, could align with this pattern via a CustomUserPrincipal class.
