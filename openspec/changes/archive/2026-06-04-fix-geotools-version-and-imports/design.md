## Context

GeoTools 32.x 对包结构进行了重大重构，将原来的 `org.opengis.*` API 包迁移到 `org.geotools.api.*`。项目中新增的多格式导入功能使用了旧版 import 路径，导致编译错误。同时 pom.xml 中部分 GeoTools 依赖版本未显式指定。

## Goals / Non-Goals

**Goals:**
1. 统一 pom.xml 中所有 GeoTools 依赖版本为 32.0
2. 修复所有 Java 文件中的 org.opengis.* 引用为 org.geotools.api.*

**Non-Goals:**
- 不修改业务逻辑
- 不添加新功能

## Decisions

### 1. 包迁移策略

GeoTools 32.x 的包迁移对照：
- `org.opengis.feature` → `org.geotools.api.feature`
- `org.opengis.referencing` → `org.geotools.api.referencing`
- `org.opengis.geometry` → `org.geotools.api.geometry`

使用 IDE 重构功能或全局替换均可。

### 2. 依赖版本管理

在 pom.xml 的 dependencyManagement 中统一指定版本，确保传递依赖也使用一致版本。

## Risks / Trade-offs

**风险**: 某些旧版 GeoTools 代码可能使用已废弃 API

**缓解**: 编译验证后再提交

## Migration Plan

1. 修改 pom.xml 添加统一版本管理
2. 修复所有 Java 文件中的 import
3. 执行 mvn compile 验证
4. 如有编译错误继续修复
