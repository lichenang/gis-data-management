# Tasks: fix-add-geotiff-dependency

## Task 1: 添加 GeoTools 依赖

**File**: `backend/pom.xml`

已添加的依赖：
- `gt-coverage` - GridCoverage2D 支持
- `gt-api` - GeoTools API 支持
- `gt-geotiff` - GeoTIFF 读写支持
- `okhttp3` - MinIO HTTP 客户端

- [x] 已完成

## Task 2: 修复 GeoTiffParser 代码

**File**: `backend/src/main/java/com/gisplatform/util/GeoTiffParser.java`

由于 GeoTools 32.x API 变更较大，使用反射方式访问元数据，避免编译时依赖不存在的包路径。

- [x] 已完成

## Task 3: 验证编译

执行 Maven 编译：

```bash
cd backend
mvn compile
```

结果：BUILD SUCCESS

- [x] 已完成

## Task 4: 验证影像上传功能（可选）

如果后端服务可以启动，可以进一步测试：
1. 启动后端服务
2. 访问 Swagger UI: http://localhost:8088/swagger-ui.html
3. 调用 POST /api/v1/images/upload 接口
4. 上传一个 .tif 文件验证功能正常

- [ ] 待测试
