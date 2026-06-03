## Why

在 init-backend 骨架生成后，运行 `mvn spring-boot:run` 时编译失败。错误原因：GeometryTypeHandler 中使用了不存在的 PostGIS 类（GeometryOID）和错误的 JTS API 调用方法。需要修复代码以使项目能够正常编译启动。

## What Changes

1. **删除未使用的 import**：移除 `import org.postgis.GeometryOID;`（该类在 PostGIS JDBC 2.5.x 中已移除）

2. **修正 WKBReader 方法调用**：将 `WKB_READER_3D.read(ByteBuffer.wrap(bytes))` 改为正确的 `WKB_READER_3D.read(bytes)`（JTS WKBReader 不支持 ByteBuffer 参数）

## Capabilities

### New Capabilities

- 本次变更为代码修复，不涉及新功能能力

### Modified Capabilities

- init-backend-project：修正 GeometryTypeHandler 代码错误

## Impact

- 修改文件：backend/src/main/java/com/gisplatform/common/handler/GeometryTypeHandler.java
- 影响范围：后端项目编译、启动

## Non-goals

- 不修改依赖版本
- 不修改其他代码文件
- 不引入新的功能

## 受影响文件清单

- backend/src/main/java/com/gisplatform/common/handler/GeometryTypeHandler.java
