# GeoTools 32.x import 路径修正对照表

## 问题描述

当前代码中存在 GeoTools 类导入错误。GeoTools 32.x 经历了重大包重构：
- 核心 API 接口从 `org.opengis.*` 迁移到 `org.geotools.api.*`
- 部分类仍使用旧路径 `org.geotools.*`

## GeoTools 32.x 模块依赖

pom.xml 中已有以下模块：
- `gt-api` (32.0) - 核心 API 接口
- `gt-main` (32.0) - 主要实现类
- `gt-referencing` (32.0) - 坐标系处理
- `gt-coverage` (32.0) - 栅格数据
- `gt-shapefile` (32.0) - Shapefile 支持
- `gt-jdbc-postgis` (32.0) - PostGIS 支持
- `gt-geotiff` (32.0) - GeoTIFF 支持
- `gt-epsg-hsql` (32.0) - EPSG 坐标数据库

## 完整修正对照表

### 数据访问 (gt-main)
| 错误导入 | 正确导入 | 所属模块 |
|----------|----------|----------|
| `org.geotools.api.data.DataStore` | `org.geotools.data.DataStore` | gt-main |
| `org.geotools.api.data.DataStoreFinder` | `org.geotools.data.DataStoreFinder` | gt-main |
| `org.geotools.api.data.SimpleFeatureSource` | `org.geotools.data.SimpleFeatureSource` | gt-main |
| `org.geotools.api.data.SimpleFeatureStore` | `org.geotools.data.SimpleFeatureStore` | gt-main |
| `org.geotools.data.*.*.*DataStore` | `org.geotools.data.*.*DataStore` | 见下表 |

### Feature API (gt-api, 推荐使用)
| 错误导入 | 正确导入 | 所属模块 |
|----------|----------|----------|
| 当前代码 | 正确 | 说明 |
| `org.geotools.api.feature.simple.SimpleFeature` | 直接使用 | gt-api |
| `org.geotools.api.feature.simple.SimpleFeatureType` | 直接使用 | gt-api |
| `org.geotools.api.feature.AttributeDescriptor` | 直接使用 | gt-api |

### CSV DataStore (需额外模块)
需要添加依赖：
```xml
<dependency>
    <groupId>org.geotools</groupId>
    <artifactId>gt-csv</artifactId>
    <version>32.0</version>
</dependency>
```
| 正确导入 | 说明 |
|----------|------|
| `org.geotools.data.csv.CSVDataStore` | gt-csv |
| `org.geotools.data.csv.CSVDataStoreFactory` | gt-csv |

### XML 配置 (gt-xml)
GeoTools 32.x 将 KML/GML 支持移到了单独的模块：
```xml
<dependency>
    <groupId>org.geotools</groupId>
    <artifactId>gt-kml</artifactId>
    <version>32.0</version>
</dependency>
<dependency>
    <groupId>org.geotools</groupId>
    <artifactId>gt-xsd</artifactId>
    <version>32.0</version>
</dependency>
```
| 正确导入 | 说明 |
|----------|------|
| `org.geotools.kml.KMLConfiguration` | gt-kml |
| `org.geotools.gml3.GMLConfiguration` | gt-xsd |
| `org.geotools.xsd.Configuration` | gt-xsd |

### 栅格数据 (gt-coverage, gt-geotiff)
| 正确导入 | 所属模块 |
|----------|----------|
| `org.geotools.coverage.grid.GridCoverage2D` | gt-coverage |
| `org.geotools.gce.geotiff.GeoTiffReader` | gt-geotiff |

### 坐标系 (gt-referencing)
| 正确导入 | 所属模块 |
|----------|----------|
| `org.geotools.referencing.CRS` | gt-referencing |
| `org.geotools.api.referencing.crs.CoordinateReferenceSystem` | gt-api |
| `org.geotools.api.referencing.operation.MathTransform` | gt-api |

---

## 代码修正示例

### MultiFormatImportService.java

**修正前** (错误):
```java
import org.geotools.api.feature.AttributeDescriptor;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.DataStore;  // 注意：这个路径是正确的
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.referencing.CRS;
```

**修正后** (正确):
```java
// feature API - 使用 gt-api 模块
import org.geotools.api.feature.AttributeDescriptor;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

// data - 使用 gt-main 模块
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.FeatureReader;

// referencing
import org.geotools.referencing.CRS;
```

### VectorDataStoreFactory.java

**修正前**:
```java
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
```

**修正后**:
```java
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
```

注意：上面两个是正确的！但 `org.geotools.feature.simple.SimpleFeatureTypeBuilder` 需要改为：
```java
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
```

### DatasetServiceImpl.java

**当前导入** (正确):
```java
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.DataUtilities;
```

**需要修正为**:
```java
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.DataUtilities;
```

---

## 需添加到 pom.xml 的依赖

```xml
<!-- CSV 支持 -->
<dependency>
    <groupId>org.geotools</groupId>
    <artifactId>gt-csv</artifactId>
    <version>32.0</version>
</dependency>

<!-- KML 支持 -->
<dependency>
    <groupId>org.geotools</groupId>
    <artifactId>gt-kml</artifactId>
    <version>32.0</version>
</dependency>

<!-- XML 配置 -->
<dependency>
    <groupId>org.geotools</groupId>
    <artifactId>gt-xsd</artifactId>
    <version>32.0</version>
</dependency>
```

---

## 总结

GeoTools 32.x 包结构规则：

1. **所有 `org.opengis.*` 迁移到 `org.geotools.api.*`**
   - 适用于: feature, referencing, geometry 等核心 API 接口

2. **数据访问类 (`DataStore`, `FeatureReader` 等) 保持在 `org.geotools.data.*`**
   - 来自 gt-main 模块

3. **实现类 (`ShapefileDataStore`, `GeoTiffReader` 等) 保持在 `org.geotools.*`**
   - 各功能模块自己的包

4. **需要明确添加的模块依赖:**
   - gt-csv: CSV 支持
   - gt-kml: KML 支持
   - gt-xsd: XML 配置
