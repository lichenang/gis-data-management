# Diagnostic: GeoServer Coverage Store Creation Failure

## Error Observed

```
Failed to create coverage store: raster_27
```

- Workspace creation: **成功**
- Coverage store creation: **失败**

## Root Cause Analysis

### 1. Wrong REST API Endpoint (Primary Issue)

**Current Code (GeoServerCoverageStoreService.java:39):**
```java
client.post("/rest/workspaces/" + workspace + "/coveragestores", xml.toString(), String.class);
```

**Problem:** This endpoint creates an **empty ImageMosaic coverage store**, not one that references an external file.

**Correct Endpoint for External File:**
```
POST /rest/workspaces/{workspace}/coveragestores/{storeName}/external.imageMosaic
```

Or for a single GeoTIFF:
```
POST /rest/workspaces/{workspace}/coveragestores/{storeName}/external.geotiff
```

**Reference:** GeoServer REST API docs:
- `POST /rest/workspaces/{workspace}/coveragestores/{store}/external.imageMosaic` - create ImageMosaic referencing external files
- `POST /rest/workspaces/{workspace}/coveragestores/{store}/external.geotiff` - create single GeoTIFF coverage store

---

### 2. S3 URL Format May Not Be Compatible

**Current URL:**
```java
String fileUrl = "s3://minio/" + minioKey;  // e.g., "s3://minio/images/uuid.tif"
```

**Problems:**
1. GeoServer requires the **S3 support plugin** to be installed and configured
2. The credentials (access key / secret key) must be configured in GeoServer's S3 coverage store settings
3. The URL format may need to match exactly what GeoServer expects (e.g., `s3://bucket/key`)

**Alternative Approaches:**

| Option | Pros | Cons |
|--------|------|------|
| Use `file://` URL | Simple if GeoServer can access the filesystem | File must be on GeoServer's filesystem |
| Use GeoServer's S3 plugin | Works with MinIO/S3 | Requires plugin + credentials config |
| Configure GeoServer to access MinIO via HTTP | Works if MinIO is network-accessible | Requires additional GeoServer config |
| Copy file locally then publish | Works | Extra storage, file duplication |

**Recommendation:** If MinIO is accessible via HTTP (like `http://minio:9000/bucket/key`), use that URL directly:
```java
String fileUrl = "http://minio:9000/" + minioKey;
// or with bucket
String fileUrl = "http://minio:9000/" + rasterBucket + "/" + minioKey;
```

---

### 3. Missing Error Response Details

**Current Code (GeoServerClient.java:38-40):**
```java
} catch (Exception e) {
    throw new RuntimeException("GeoServer API call failed: " + url + ", error: " + e.getMessage(), e);
}
```

**Problem:** RestTemplate throws `HttpStatusCodeException` which contains the **response body** from GeoServer with detailed error information:
```xml
<internalError>
  <message>Could not open input file for coverage: s3://minio/...</message>
  <trace>...</trace>
</internalError>
```

**Fix:**
```java
public <T> T exchange(String path, HttpMethod method, Object body, Class<T> responseType) {
    // ...
    try {
        ResponseEntity<T> response = restTemplate.exchange(url, method, request, responseType);
        return response.getBody();
    } catch (org.springframework.web.client.HttpStatusCodeException e) {
        String errorBody = e.getResponseBodyAsString();
        throw new RuntimeException("GeoServer API call failed: " + url +
            ", status: " + e.getStatusCode() +
            ", response: " + errorBody, e);
    } catch (Exception e) {
        throw new RuntimeException("GeoServer API call failed: " + url + ", error: " + e.getMessage(), e);
    }
}
```

---

### 4. Empty Password in Configuration

**application.yml line 97:**
```yaml
password: ${GEOSERVER_PASSWORD:}
```

**Problem:** If `GEOSERVER_PASSWORD` env var is not set, the password defaults to empty string, causing all GeoServer API calls to fail with 401 Unauthorized.

**Fix:** Set the env var or provide a default:
```yaml
password: ${GEOSERVER_PASSWORD:geoserver}
```

---

## Corrected Implementation

### Fixed GeoServerCoverageStoreService

```java
public void createImageMosaicStore(String workspace, String storeName, String minioKey, int minZoom, int maxZoom) {
    if (storeExists(workspace, storeName)) {
        log.info("CoverageStore {} already exists", storeName);
        return;
    }

    // Determine file URL based on storage type
    // Option 1: HTTP URL to MinIO (assumes MinIO is network accessible)
    String fileUrl = "http://minio:9000/" + minioKey;

    // Option 2: Local file (if GeoServer is on same machine)
    // String fileUrl = "file:///data/geotiff/" + minioKey;

    // Option 3: S3 URL (requires GeoServer S3 plugin)
    // String fileUrl = "s3://bucket/" + minioKey;

    StringBuilder xml = new StringBuilder();
    xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    xml.append("<coverageStore>");
    xml.append("<name>").append(storeName).append("</name>");
    xml.append("<type>ImageMosaic</type>");
    xml.append("<url>").append(fileUrl).append("</url>");
    xml.append("</coverageStore>");

    try {
        // Use external.imageMosaic endpoint for external files
        client.post("/rest/workspaces/" + workspace + "/coveragestores/" + storeName + "/external.imageMosaic",
                    xml.toString(), String.class);
        log.info("Created ImageMosaic store: {} for file: {}", storeName, fileUrl);
    } catch (Exception e) {
        throw new RuntimeException("Failed to create coverage store: " + storeName +
            ". GeoServer error: " + extractGeoServerError(e), e);
    }
}
```

### Fixed GeoServerClient with Better Error Handling

```java
public <T> T exchange(String path, HttpMethod method, Object body, Class<T> responseType) {
    String url = props.getUrl() + path;

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_XML);
    String auth = props.getUsername() + ":" + props.getPassword();
    String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
    headers.set("Authorization", "Basic " + encodedAuth);

    HttpEntity<Object> request = new HttpEntity<>(body, headers);

    try {
        ResponseEntity<T> response = restTemplate.exchange(url, method, request, responseType);
        return response.getBody();
    } catch (org.springframework.web.client.HttpStatusCodeException e) {
        String errorBody = e.getResponseBodyAsString();
        log.error("GeoServer error response: {}", errorBody);
        throw new RuntimeException("GeoServer API call failed: " + url +
            ", status: " + e.getStatusCode() +
            ", response: " + errorBody, e);
    } catch (Exception e) {
        throw new RuntimeException("GeoServer API call failed: " + url + ", error: " + e.getMessage(), e);
    }
}
```

---

## GeoServer 2.26.x API Compatibility

GeoServer 2.26.x REST API for coverage stores:

| Endpoint | Purpose |
|----------|---------|
| `POST /rest/workspaces/{ws}/coveragestores` | Create empty store |
| `POST /rest/workspaces/{ws}/coveragestores/{store}/external.imageMosaic` | Create ImageMosaic from external files |
| `POST /rest/workspaces/{ws}/coveragestores/{store}/external.geotiff` | Create GeoTIFF coverage from external file |
| `GET /rest/workspaces/{ws}/coveragestores/{store}` | Check if store exists |

For single GeoTIFF files, `external.geotiff` is simpler than ImageMosaic.

---

## Recommended Fixes (Priority Order)

1. **Fix endpoint** — Change to `/rest/workspaces/{ws}/coveragestores/{store}/external.imageMosaic`
2. **Fix URL** — Use HTTP URL to MinIO or local file path, not S3 URL (unless S3 plugin is configured)
3. **Add error capture** — Extract GeoServer error response body for debugging
4. **Fix password config** — Ensure GEOSERVER_PASSWORD env var is set

---

## Debugging Steps

1. **Check GeoServer logs** at `$GEOSERVER_DATA_DIR/logs/geoserver.log` for detailed error
2. **Test API directly**:
   ```bash
   curl -u admin:geoserver -X POST \
     -H "Content-Type: application/xml" \
     -d '<coverageStore><name>test</name><type>ImageMosaic</type><url>file:///tmp/test.tif</url></coverageStore>' \
     http://localhost:8080/geoserver/rest/workspaces/gisplatform/coveragestores/test/external.imageMosaic
   ```
3. **Check S3 plugin** is installed: `http://localhost:8080/geoserver/rest/about/system-status` or UI → Server Status → Modules
4. **Verify credentials**: Ensure GEOSERVER_PASSWORD is set correctly in environment
