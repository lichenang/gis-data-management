
## Why

在应用 init-backend 变更后，后端项目启动时发现 pom.xml 中存在多个依赖版本问题，包括 artifact 名称错误、版本不一致、版本不存在等问题，导致项目无法编译启动。需要修正这些依赖版本以确保项目正常运行。

## What Changes

1. **修正 Hutool 版本**：5.9.3 → 5.8.42（解决可能存在的兼容性问题）

2. **修正 Knife4j artifact 名称**：knife4j-openapi-starter-jakarta → knife4j-openapi3-jakarta-spring-boot-starter（Spring Boot 3.x 需要正确的 artifact）

3. **修正 MinIO SDK 版本**：8.6.2 → 8.6.0（小版本调整）

4. **添加 gt-jdbc-postgis 版本**：为 GeoTools JDBC PostgreSQL 模块显式指定版本 32.0（原来未声明版本）

5. **统一 GeoTools 版本**：gt-main 与 gt-jdbc-postgis 统一为 32.0（解决版本不一致问题）

## Capabilities

### New Capabilities

- 本次变更为修复类变更，不涉及新功能能力

### Modified Capabilities

- init-backend-project：修正 Maven 依赖版本配置

## Impact

- 修改文件：backend/pom.xml
- 影响范围：后端项目编译、依赖下载、启动

## Non-goals

- 不修改业务代码
- 不修改其他配置文件
- 不引入新的依赖

##受影响文件清单

- backend/pom.xml（依赖版本管理）
