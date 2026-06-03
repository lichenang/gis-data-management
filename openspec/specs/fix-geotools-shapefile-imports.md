# GeoTools 32.x Shapefile 导入诊断报告

## 1. 问题现象

在 `DatasetServiceImpl.exportShapefileAsZip()` 中引用以下类时出现编译错误：

```
import org.geotools.shapefile.ShapefileDataStore;          // ❌ 包不存在
import org.geotools.shapefile.ShapefileDataStoreFactory;   // ❌ 包不存在
```

## 2. 环境版本

| 组件 | 版本 |
|---|---|
| GeoTools | **32.0** |
| gt-shapefile | **32.0** (新增依赖) |
| Java | 17 |
| Spring Boot | 3.5.0 |

## 3. 依赖分析

### 3.1 pom.xml 配置

已确认 `gt-shapefile` 同时配置在 `dependencyManagement` 和 `dependencies` 中：

**dependencyManagement（版本锁定）：**

```xml
<dependency>
    <groupId>org.geotools</groupId>
    <artifactId>gt-shapefile</artifactId>
    <version>32.0</version>
</dependency>
```

**dependencies（实际引用）：**

```xml
<dependency>
    <groupId>org.geotools</groupId>
    <artifactId>gt-shapefile</artifactId>
</dependency>
```

依赖已正确配置。JAR 文件存在于本地 Maven 仓库：
```
~/.m2/repository/org/geotools/gt-shapefile/32.0/gt-shapefile-32.0.jar (245 KB)
```

### 3.2 其他已存在的 GeoTools 依赖

```
gt-main         32.0   → 核心：DataStore, FeatureWriter, Transaction
gt-jdbc-postgis 32.0   → PostGIS 数据存储
gt-api          32.0   → SimpleFeature, SimpleFeatureType 接口
gt-shapefile    32.0   → ShapefileDataStore, ShapefileDataStoreFactory
gt-referencing  32.0   → CRS 转换
gt-coverage     32.0   → 栅格覆盖
gt-geotiff      32.0   → GeoTIFF 读写
gt-epsg-hsql    32.0   → EPSG 坐标系数据库
```

## 4. 根本原因：三个层面的接口变更

### 4.1 层面一：包路径迁移（vs GeoTools 28.x）

GeoTools 28.x 及更早版本中，Shapefile 类位于 `org.geotools.shapefile.*`：

```
org.geotools.shapefile.ShapefileDataStore          // ← 旧包路径（≤28.x）
org.geotools.shapefile.ShapefileDataStoreFactory   // ← 旧包路径（≤28.x）
```

GeoTools 32.x 中已重构为 `org.geotools.data.shapefile.*`：

```
org.geotools.data.shapefile.ShapefileDataStore          // ← ✅ 正确包路径
org.geotools.data.shapefile.ShapefileDataStoreFactory   // ← ✅ 正确包路径
```

### 4.2 层面二：接口迁移（`org.geotools` → `org.geotools.api`）

GeoTools 31+ 开始将数据访问接口从 `org.geotools.data.*` 和 `org.opengis.*` 迁移到 `org.geotools.api.*` 命名空间。这是 GeoTools 向 Jakarta EE / Eclipse Foundation 规范对齐的一部分。

```
旧路径（≤28.x）                     →  新路径（32.x）
─────────────────────────────────────────────────────────────────
org.geotools.data.DataStore          →  org.geotools.api.data.DataStore
org.geotools.data.DataStoreFinder    →  org.geotools.api.data.DataStoreFinder
org.geotools.data.FeatureWriter      →  org.geotools.api.data.FeatureWriter
org.geotools.data.Transaction        →  org.geotools.api.data.Transaction
org.geotools.data.simple.SimpleFeatureSource
                                     →  org.geotools.api.data.SimpleFeatureSource
org.opengis.feature.simple.SimpleFeature
                                     →  org.geotools.api.feature.simple.SimpleFeature
org.opengis.feature.simple.SimpleFeatureType
                                     →  org.geotools.api.feature.simple.SimpleFeatureType
```

**保持不变的实现类：**

```
org.geotools.data.DefaultTransaction  ← 无变化（实现类）
org.geotools.data.simple.SimpleFeatureIterator  ← 无变化
```

### 4.3 层面三：API 方法移除

`ShapefileDataStore` 在 GeoTools 32.x 中不再直接暴露以下方法：

| 方法 | 说明 |
|---|---|
| `setTransaction(Transaction)` | 不再作为 DataStore 直接方法 |
| `addFeatures(FeatureCollection)` | 不再作为 DataStore 直接方法 |
| `getFeatureStore(String)` | ContentDataStore 不暴露此方法 |

**✅ 正确的调用方式**：通过 `getFeatureSource()` 返回的 `SimpleFeatureStore` 进行写入

```java
// ❌ 错误方法
shpStore.setTransaction(t);
shpStore.addFeatures(collection);

// ✅ 正确方法
ShapefileDataStore shpStore = (ShapefileDataStore) shpFactory.createNewDataStore(shpParams);
shpStore.createSchema(schema);

List<SimpleFeature> features = DataUtilities.list(source.getFeatures());
String typeName = shpStore.getTypeNames()[0];
((SimpleFeatureStore) shpStore.getFeatureSource(typeName))
        .addFeatures(DataUtilities.collection(features));
```

`getFeatureSource()` 返回的是 `ShapefileFeatureStore`（位于 `org.geotools.data.shapefile` 包中），它实现了 `SimpleFeatureStore` 接口，可以直接调用 `addFeatures()`。

## 5. 完整调用链路

```
                    ┌──────────────────────────────────────────┐
                    │   PostgisNGDataStoreFactory              │
                    │   ┌──────────────┐                       │
                    │   │  DataSource  │ (通过 jdbcTemplate)   │
                    │   │  schema=public                       │
                    │   └──────┬───────┘                       │
                    └──────────┼───────────────────────────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │  org.geotools.api.   │
                    │  data.DataStore      │  ← 注意是 api 包
                    └──────────┬───────────┘
                               │ getFeatureSource(tableName)
                               ▼
                    ┌──────────────────────┐
                    │  org.geotools.api.   │
                    │  data.SimpleFeatureSource
                    └──────────┬───────────┘
                               │ getSchema()
                               ▼
                    ┌──────────────────────┐
                    │  org.geotools.api.   │
                    │  feature.simple.     │
                    │  SimpleFeatureType   │
                    └──────────────────────┘
                               │
                               ▼
              ┌─────────────────────────────────────┐
              │  ShapefileDataStoreFactory          │
              │  → createNewDataStore(params)       │
              └──────────────┬──────────────────────┘
                             │
                             ▼
              ┌─────────────────────────────────────┐
              │  org.geotools.data.shapefile.       │
              │  ShapefileDataStore                 │  ← 注意是 data.shapefile 包
              │  → createSchema(SimpleFeatureType)  │
              │  → getTypeNames()[0]                │
              │  → getFeatureSource(typeName)       │
              │    → 返回 ShapefileFeatureStore     │
              │      (implements SimpleFeatureStore) │
              └──────────────┬──────────────────────┘
                             │
                             ▼
              ┌─────────────────────────────────────┐
              │  org.geotools.api.data.             │
              │  SimpleFeatureStore                 │
              │  → addFeatures(FeatureCollection)   │
              └─────────────────────────────────────┘
```

## 6. 验证结论

```bash
mvn compile
# BUILD SUCCESS
```

当前代码已编译通过。关键修复总结：

| 问题 | 修复 |
|---|---|
| 缺少 gt-shapefile 依赖 | 在 pom.xml 的 dependencyManagement 和 dependencies 中分别添加 |
| gt-shapefile JAR 未下载 | `mvn dependency:copy-dependencies` 强制下载 |
| 包名 `org.geotools.shapefile.*` 不存在 | 改为 `org.geotools.data.shapefile.*` |
| 接口 `Transaction` 不存在 | 使用 `org.geotools.api.data.Transaction` |
| `setTransaction`/`addFeatures` 不存在 | 使用 `getFeatureSource()` → `SimpleFeatureStore.addFeatures()` |

## 7. 参考资料

- 当前工作代码：`DatasetServiceImpl.java:230-281`
- Shapefile 创建工厂：`ShapefileDataStoreFactory` (位于 `org.geotools.data.shapefile` 包)
- Shapefile 数据存储：`ShapefileDataStore` (继承自 `ContentDataStore`, via `getFeatureSource()` 返回 `ShapefileFeatureStore`)
- 要素写入 API：`SimpleFeatureStore.addFeatures(FeatureCollection)` (位于 `org.geotools.api.data` 包)
