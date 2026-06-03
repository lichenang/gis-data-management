# Design: fix-add-epsg-dependency

## 解决方案

### 添加 Maven 依赖

在 `backend/pom.xml` 的 `<dependencies>` 中添加：

```xml
<!-- GeoTools EPSG 坐标参考系统数据库（HSQL 嵌入式） -->
<dependency>
    <groupId>org.geotools</groupId>
    <artifactId>gt-epsg-hsql</artifactId>
    <version>32.0</version>
</dependency>
```

### 依赖说明

gt-epsg-hsql 提供：
- 嵌入式 HSQL 数据库
- 内置 EPSG 坐标参考系统定义
- 支持坐标转换和 CRS 查询

### 添加位置

在 gt-geotiff 依赖之后：

```xml
<dependency>
    <groupId>org.geotools</groupId>
    <artifactId>gt-geotiff</artifactId>
    <version>32.0</version>
</dependency>

<!-- 新增：EPSG 坐标参考系统数据库 -->
<dependency>
    <groupId>org.geotools</groupId>
    <artifactId>gt-epsg-hsql</artifactId>
    <version>32.0</version>
</dependency>
```

## 验证方式

```bash
cd backend
mvn compile
```

预期结果：BUILD SUCCESS，无错误。
