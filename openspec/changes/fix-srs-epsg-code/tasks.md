# Tasks: fix-srs-epsg-code

## Task 1: 修改 GeoTiffParser 添加 imports 和重写 extractCrs() ✓

**文件**: `backend/src/main/java/com/gisplatform/util/GeoTiffParser.java`

1. 添加 imports:
```java
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
```

2. 添加内部类和方法（或者直接修改当前的 extractCrs 方法）

3. 修改 extractCrs 方法，使其优先使用 GeoTools API 获取 EPSG 代码

## Task 2: 验证编译 ✓

执行 Maven 编译：
```bash
cd backend
mvn compile
```

## Task 3: 测试影像上传（需要手动测试）✓

1. 重启应用
2. 上传 GeoTIFF 文件（确认包含 EPSG:3857 坐标系）
3. 查询数据库验证 srs 字段:
```sql
SELECT srs FROM dataset WHERE id = <id>;
```
4. 确认 srs 格式为 `EPSG:3857` 而非 `EPSG:WGS 84 / Pseudo-Mercator`
5. 调用发布接口测试 CRS 转换功能
