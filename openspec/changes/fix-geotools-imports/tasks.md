# Tasks: fix-geotools-imports

## Task 1: 修正 GeoTiffParser.java Import 路径

**File**: `backend/src/main/java/com/gisplatform/util/GeoTiffParser.java`

修正 import 列表：
- `org.geotools.coverage.grid.GridCoverage2D` → `org.geotools.coverage2.grid.GridCoverage2D`
- `org.opengis.coverage.SampleDimension` → `org.geotools.api.coverage.SampleDimension`
- `org.opengis.coverage.grid.GridGeometry` → `org.geotools.api.coverage.grid.GridGeometry`
- `org.opengis.referencing.crs.CoordinateReferenceSystem` → `org.geotools.api.referencing.crs.CoordinateReferenceSystem`
- 删除未使用的 import：CoverageFactoryFinder, AbstractGridCoverageWriter, AbstractGridFormat, GridFormatFinder, GeoTiffFormat, Envelope2D, CoordinateSystemAxis, DataBuffer

添加类级别和方法级别 Javadoc（中文）。

## Task 2: 为 ImageService.java 添加 Javadoc

**File**: `backend/src/main/java/com/gisplatform/service/ImageService.java`

添加：
- 类级别 Javadoc：描述接口职责
- 方法 Javadoc：uploadImage() 和 listImages()

## Task 3: 为 ImageServiceImpl.java 添加 Javadoc

**File**: `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java`

添加：
- 类级别 Javadoc：描述实现类职责
- 方法 Javadoc：uploadImage() 和 listImages()

## Task 4: 为 ImageController.java 添加方法级 Javadoc

**File**: `backend/src/main/java/com/gisplatform/controller/ImageController.java`

添加：
- upload() 方法的详细 Javadoc（包含 @Parameter 说明）
- list() 方法的详细 Javadoc
- getById() 方法的 Javadoc

## Task 5: 为 MinioConfig.java 添加 Javadoc

**File**: `backend/src/main/java/com/gisplatform/config/MinioConfig.java`

添加：
- 类级别 Javadoc：描述配置类职责
- 字段注释说明各配置项来源（application.yml）

## Task 6: 为 RasterMetadataMapper.java 添加 Javadoc

**File**: `backend/src/main/java/com/gisplatform/mapper/RasterMetadataMapper.java`

添加：
- 类级别 Javadoc：描述 Mapper 接口职责

## Task 7: 编译验证

执行 Maven 编译：
```bash
cd backend
mvn compile
```

确认：
- 无编译错误
- 无警告（可选）
- GeoTools 相关类正常加载
