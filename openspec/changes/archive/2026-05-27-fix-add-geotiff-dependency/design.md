# Design: fix-add-geotiff-dependency

## 解决方案

### 添加 Maven 依赖

在 `backend/pom.xml` 的 `<dependencies>` 中添加：

```xml
<!-- GeoTools GeoTIFF 支持（必需） -->
<dependency>
    <groupId>org.geotools</groupId>
    <artifactId>gt-geotiff</artifactId>
    <version>32.0</version>
</dependency>
```

### 依赖位置

在 gt-referencing 依赖之后添加：

```xml
<dependency>
    <groupId>org.geotools</groupId>
    <artifactId>gt-referencing</artifactId>
    <version>32.0</version>
</dependency>

<!-- 新增：GeoTools GeoTIFF 支持 -->
<dependency>
    <groupId>org.geotools</groupId>
    <artifactId>gt-geotiff</artifactId>
    <version>32.0</version>
</dependency>
```

## 验证方式

```bash
cd backend
mvn compile
```

预期结果：
- BUILD SUCCESS
- 无编译错误
- 无警告（可选）

## 依赖说明

gt-geotiff 模块提供：
- GeoTiffReader：读取 GeoTIFF 文件
- GeoTiffWriter：写入 GeoTIFF 文件
- GeoTiffFormat：GeoTIFF 格式支持

这是 GeoTools 32.x 读写 GeoTIFF 格式的必需依赖。
