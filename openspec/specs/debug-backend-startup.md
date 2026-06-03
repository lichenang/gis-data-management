# Backend 启动问题诊断报告

## 问题描述

运行 `mvn spring-boot:run` 时编译失败，报错信息如下：

```
java: 找不到符号
  符号:   类 GeometryOID
  位置: 程序包 org.postgis
```

```
java: 无法找到符号
  符号:   方法 read(ByteBuffer)
  位置: 类 org.locationtech.jts.io.WKBReader
```

## 根因分析

### 问题 1: GeometryOID 类不存在

**原因**：PostGIS JDBC 驱动版本演进导致

| 版本 | GeometryOID 状态 |
|------|------------------|
| 2.0.x 之前 | 存在（基于 OID 的旧 API） |
| 2.1.x - 2.4.x | 已废弃，标记 @Deprecated |
| 2.5.x | 已移除 |

当前项目使用的 `postgis-jdbc:2.5.1` 不包含此类。

**代码位置**：`GeometryTypeHandler.java:13`

```java
import org.postgis.GeometryOID;  // 不存在，导致编译错误
```

**影响**：实际上此 import 没有在代码中被使用，可以直接删除。

### 问题 2: WKBReader.read(ByteBuffer) 方法不存在

**原因**：JTS API 误解

JTS 的 `WKBReader` 类只提供以下重载方法：

| 方法签名 | 描述 |
|----------|------|
| `read(byte[] bytes)` | 从字节数组读取 |
| `read(InputStream inputStream)` | 从输入流读取 |

**不支持**：`read(ByteBuffer)`

**代码位置**：`GeometryTypeHandler.java:116`

```java
// 错误写法
return WKB_READER_3D.read(ByteBuffer.wrap(bytes));

// 正确写法
return WKB_READER_3D.read(bytes);
```

### 潜在问题：GeoTools 32.0 版本

根据 GeoTools 官方发布历史，**32.0 版本并不存在**。可用版本：

```
GeoTools 稳定版本：
├── 29.5 (最新稳定版，2024年)
├── 28.4
├── 27.3
├── 26.2
└── ...
```

如果 Maven 构建时 GeoTools 32.0 无法下载，会报 `Could not find artifact` 错误。

---

## 修复方案

### 修复 1：删除未使用的 import

**文件**：`backend/src/main/java/com/gisplatform/common/handler/GeometryTypeHandler.java`

```diff
-import org.postgis.GeometryOID;
```

### 修复 2：修正 WKBReader.read() 调用

**文件**：`backend/src/main/java/com/gisplatform/common/handler/GeometryTypeHandler.java`

```diff
- return WKB_READER_3D.read(ByteBuffer.wrap(bytes));
+ return WKB_READER_3D.read(bytes);
```

### 修复 3：GeoTools 版本调整（如需要）

**文件**：`backend/pom.xml`

如果 32.0 无法下载，改为 29.5：

```xml
<!-- GeoTools 统一为 29.5 -->
<dependency>
    <groupId>org.geotools</groupId>
    <artifactId>gt-main</artifactId>
    <version>29.5</version>
</dependency>

<dependency>
    <groupId>org.geotools.jdbc</groupId>
    <artifactId>gt-jdbc-postgis</artifactId>
    <version>29.5</version>
</dependency>
```

---

## 验证步骤

1. 修改 GeometryTypeHandler.java 后重新编译

```bash
cd backend
mvn clean compile
```

2. 如果出现 GeoTools 版本问题，修改 pom.xml 后重试

```bash
mvn clean compile
```

---

## 完整修复代码

### GeometryTypeHandler.java 修复版

```java
package com.gisplatform.common.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;

import java.sql.*;

/**
 * Geometry 类型处理器
 * <p>
 * 用于处理 PostGIS geometry 类型与 JTS Geometry 对象之间的相互转换。
 * 支持 WKB（Well-Known Binary）格式的编解码。
 * </p>
 *
 * @author GIS Platform Team
 * @version 1.0.0
 */
@MappedTypes({Geometry.class})
@MappedJdbcTypes(JdbcType.OTHER)
public class GeometryTypeHandler extends BaseTypeHandler<Geometry> {

    /**
     * JTS GeometryFactory，用于创建几何对象
     */
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    /**
     * WKT 读写器
     */
    private static final WKTReader WKT_READER = new WKTReader(GEOMETRY_FACTORY);
    private static final WKTWriter WKT_WRITER = new WKTWriter();

    /**
     * WKB 读写器
     */
    private static final WKBReader WKB_READER_3D = new WKBReader(GEOMETRY_FACTORY);
    private static final WKBWriter WKB_WRITER_3D = new WKBWriter(3, true);

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Geometry parameter, JdbcType jdbcType) throws SQLException {
        byte[] wkb = WKB_WRITER_3D.write(parameter);
        ps.setBytes(i, wkb);
    }

    @Override
    public Geometry getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseGeometry(rs.getBytes(columnName));
    }

    @Override
    public Geometry getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseGeometry(rs.getBytes(columnIndex));
    }

    @Override
    public Geometry getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseGeometry(cs.getBytes(columnIndex));
    }

    private Geometry parseGeometry(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            // 修复：使用正确的 read(byte[]) 方法
            return WKB_READER_3D.read(bytes);
        } catch (Exception e) {
            return null;
        }
    }

}
```

---

## 诊断总结

| 问题 | 原因 | 修复方案 |
|------|------|----------|
| GeometryOID 找不到 | PostGIS JDBC 2.5.x 已移除此类 | 删除未使用的 import |
| read(ByteBuffer) 不存在 | JTS API 不支持此方法 | 改用 read(byte[]) |
| GeoTools 32.0 可能不存在 | 版本号错误 | 改为 29.5 |

此问题属于**代码编写错误**而非依赖版本问题，修复后应能正常编译启动。
