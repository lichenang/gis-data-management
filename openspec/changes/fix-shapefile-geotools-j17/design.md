## Context

当前系统使用 GeoTools 32.x 进行 Shapefile 解析，代码位于 `MultiFormatImportService.parseShapefile()` 方法。虽然已使用 `FileDataStore` API，但存在以下问题：1. **Java 17 模块兼容性问题**：运行时可能出现 `IllegalAccessError`，因为 GeoTools 内部使用了 Java 模块系统限制访问的内部 API
2. **API 混用问题**：代码同时使用 `org.opengis.*` 和 `org.geotools.*` 包，部分旧版 API 在 GeoTools 32.x 中已被移除或迁移3. **maven-compiler-plugin 缺少模块开放参数**：未配置 `--add-opens` / `--add-exports`，导致编译和运行时访问受限

## Goals / Non-Goals

**Goals:**
1. 解决 Java 17 下的模块访问异常问题
2. 统一使用 GeoTools 32.x 推荐的 ShapefileDataStore API
3. 确保 Shapefile 导入功能在 Java 17 环境下稳定运行

**Non-Goals:**
- 不实现 Shapefile 导出功能
- 不修改数据库 schema 或引入新的数据模型
- 不添加新的 REST API 端点

## Decisions

### 决策 1: 使用 ShapefileDataStore 替代 FileDataStore

**考虑选项：**
- 继续使用 `FileDataStore`（当前方案）：简单但可能有模块访问问题
- 改用 `ShapefileDataStore`：GeoTools 官方推荐的专用 API，提供更好的类型安全和错误处理

**选择：`ShapefileDataStore`**

**理由：**
1. `ShapefileDataStore` 是专门为 Shapefile 设计的 DataStore，API 更清晰
2. 可直接通过 `ShapefileDataStoreFinder.getDataStore()` 获取实例
3. 官方推荐用法，兼容性更有保障

### 决策 2: maven-compiler-plugin 模块参数配置

**考虑选项：**
- 仅添加 `--add-opens` 参数（仅运行时需要）
- 同时添加 `--add-opens` 和 `--add-exports` 参数

**选择：同时配置 `--add-opens` 和 `--add-exports`**

**理由：**
1. GeoTools 32.x 依赖的某些模块需要在编译期和运行时都开放访问权限
2. 需要开放的模块包括：`java.base`、`java.sql`、`java.xml`、`java.naming` 等 JDK 内部模块
3. 需要导出 GeoTools 相关模块供应用程序使用

### 决策 3: 统一 import 语句

**当前问题：**
```java
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
```

**修正方案：**
改用 GeoTools 32.x 提供的统一 API：
```java
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
```

**理由：** GeoTools 32.x 统一使用 `org.geotools.api.*` 包路径，废弃了部分 `org.opengis.*` 接口

## Risks / Trade-offs

- **风险 1**: GeoTools 版本兼容性变化
  - **缓解**: 当前项目使用 GeoTools 32.0，保持版本一致

- **风险 2**: 模块参数配置不完整导致运行时错误
  - **缓解**: 参考 GeoTools 官方文档配置完整的模块开放参数

- **风险 3**: 字符编码问题
  - **当前状态**: 已有 `FormatDetector.detectZipCharset()` 处理 ZIP 编码检测
  - **保持**: 继续使用现有编码检测逻辑

## Migration Plan

1. 修改 `backend/pom.xml` 的 maven-compiler-plugin 配置，添加模块开放参数
2. 修改 `MultiFormatImportService.java` 的 import 语句，统一使用 `org.geotools.api.*` 包
3. 将 `parseShapefile` 方法中的 `FileDataStore` 改为 `ShapefileDataStore`
4. 执行 Maven 编译验证：`cd backend && mvn compile`
5. 执行单元测试验证功能正常
