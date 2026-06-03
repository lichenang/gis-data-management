# Design: fix-created-by-null

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         APPLICATION LAYER                           │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  Controller Layer                                                  │
│  ┌─────────────────┐    ┌─────────────────┐                        │
│  │DatasetController│    │DatasetController│                        │
│  │   (import API)  │    │  (create API)   │                        │
│  └────────┬────────┘    └────────┬────────┘                        │
│           │                      │                                  │
│           ▼                      ▼                                  │
│  ┌─────────────────┐    ┌─────────────────┐                        │
│  │                 │    │                 │                        │
│  │GisDataParser    │    │DatasetService   │                        │
│  │ServiceImpl      │    │Impl             │                        │
│  │                 │    │                 │                        │
│  └────────┬────────┘    └────────┬────────┘                        │
│           │                      │                                  │
│           │   ┌───────────────┐  │                                  │
│           └──►│CurrentUserUtils│◄─┘                                  │
│               │ (NEW)         │                                     │
│               └───────┬───────┘                                     │
│                       │                                              │
│                       ▼                                              │
│         ┌─────────────────────────┐                                 │
│         │SecurityContextHolder    │                                 │
│         │(Spring Security)        │                                 │
│         └─────────────────────────┘                                 │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

## Implementation Details

### 1. Create CurrentUserUtils

```java
package com.gisplatform.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

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

**File**: `backend/src/main/java/com/gisplatform/security/CurrentUserUtils.java`

### 2. Update GisDataParserServiceImpl

Add field injection and set created_by:

```java
@Autowired
private CurrentUserUtils currentUserUtils;

// In importToPostGIS method:
dataset.setCreatedBy(currentUserUtils.getCurrentUserId());
```

**File**: `backend/src/main/java/com/gisplatform/service/impl/GisDataParserServiceImpl.java`

### 3. Update DatasetServiceImpl

Add field injection and set created_by in createDataset method:

```java
@Autowired
private CurrentUserUtils currentUserUtils;

// In createDataset method:
dataset.setCreatedBy(currentUserUtils.getCurrentUserId());
```

**File**: `backend/src/main/java/com/gisplatform/service/impl/DatasetServiceImpl.java`

## How User ID is Retrieved

From `JwtAuthenticationFilter.java`:

```java
Long userId = jwtTokenService.getUserIdFromToken(jwt);

UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(username, userId, authorities);
//                                                   ^^^^^^ stored in credentials
```

So we retrieve it via:

```java
Long userId = (Long) authentication.getCredentials();
```

## Testing Considerations

- Unit test CurrentUserUtils to verify security context retrieval
- Integration test dataset creation flow
- Verify null handling for unauthenticated requests (should not reach service layer due to security config)
