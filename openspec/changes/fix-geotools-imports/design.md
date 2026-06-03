# Design: fix-geotools-imports

## Import 修正方案

### GeoTools 32.x 路径映射表

```
旧路径 (错误)                          →  新路径 (正确)
─────────────────────────────────────────────────────────────
org.opengis.coverage.SampleDimension   →  org.geotools.api.coverage.SampleDimension
org.opengis.coverage.grid.GridGeometry →  org.geotools.api.coverage.grid.GridGeometry
org.opengis.referencing.crs.CoordinateReferenceSystem → org.geotools.api.referencing.crs.CoordinateReferenceSystem
org.opengis.referencing.cs.CoordinateSystemAxis → org.geotools.api.referencing.cs.CoordinateSystemAxis
org.geotools.coverage.grid.GridCoverage2D → org.geotools.coverage2.grid.GridCoverage2D
org.geotools.coverage.CoverageFactoryFinder → 删除（未使用）
org.geotools.coverage.grid.io.*        → 删除（未使用）
org.geotools.gce.geotiff.GeoTiffFormat → 删除（未使用）
org.geotools.geometry.Envelope2D       → org.geotools.api.geometry.Envelope
java.awt.image.DataBuffer              → 删除（未使用）
```

### 修正后 GeoTiffParser.java Import 列表

```java
import cn.hutool.json.JSONUtil;
import com.gisplatform.entity.RasterMetadata;
import org.geotools.coverage2.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.api.coverage.SampleDimension;
import org.geotools.api.coverage.grid.GridGeometry;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
```

## Javadoc 补充方案

### 注释规范（config.yaml 第 51-60 条）

- 所有 public 类、接口、方法必须有 Javadoc 注释
- 注释语言：中文
- Controller 方法注释必须包含：接口说明、请求参数、返回数据格式

### 各文件 Javadoc 内容

#### GeoTiffParser.java
- 类：`GeoTIFF 文件解析工具类，使用 GeoTools 32.x 读取元数据`
- parse()：`解析 GeoTIFF 文件并提取元数据。参数：file 上传文件，返回：RasterMetadata，抛出：Exception`
- getPixelType()：`获取影像像素类型`
- extractCrs()：`提取坐标参考系统标识符 EPSG 代码`

#### ImageService.java
- 类：`影像数据集服务接口，提供影像上传与查询功能`
- uploadImage()：`上传影像文件到 MinIO 并解析元数据`
- listImages()：`分页查询影像数据集列表`

#### ImageServiceImpl.java
- 类：`影像服务实现类，实现影像上传、存储、元数据解析`
- uploadImage()：详细描述流程（解析→上传→写库）
- listImages()：描述查询条件（type=raster）

#### ImageController.java
- upload()：`上传影像文件。@RequestParam file 文件，name 名称（可选），description 描述（可选）`
- list()：`获取影像列表，支持分页和名称模糊查询`

#### MinioConfig.java
- 类：`MinIO 配置类，提供 MinioClient 和桶名 Bean`
- 每个 @Value 字段说明其配置来源

#### RasterMetadataMapper.java
- 类：`影像元数据 Mapper 接口，继承 MyBatis-Plus BaseMapper`

## 验证方式

执行编译验证：
```bash
cd backend
mvn compile
```

预期结果：无编译错误、无警告
