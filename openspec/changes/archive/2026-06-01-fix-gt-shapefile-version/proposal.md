## Why

pom.xml 中 `gt-shapefile` 在 `dependencies` 区域缺少 `<version>` 声明。虽然它在 `dependencyManagement` 中有版本锁定（32.0），但部分 IDE（如 IntelliJ IDEA）在解析直接依赖时会优先读取 `dependencies` 中的版本号。缺少显式版本导致 IDE 无法将 `gt-shapefile-32.0.jar` 加入模块编译路径，表现为 `Cannot resolve symbol` 错误。而 Maven 命令行（`mvn compile`）能通过 `dependencyManagement` 获取版本，所以不受影响。

## What Changes

1. 在 `pom.xml` 的 `dependencies` 区域中，为 `gt-shapefile` 添加 `<version>32.0</version>` 显式版本声明
2. 对其他已显式声明版本的 GeoTools 依赖不做改动

## 非目标

- 不改动 `dependencyManagement` 中的版本锁定
- 不修改其他 GeoTools 模块的依赖声明（`gt-main`、`gt-jdbc-postgis` 等通过 `dependencyManagement` 继承版本的不动）
- 不修改代码逻辑

## Capabilities

### New Capabilities
- `fix-gt-shapefile-version`: pom.xml 依赖版本显式声明

### Modified Capabilities

无

## Impact

- `backend/pom.xml`：仅 `dependencies` 区域中 `gt-shapefile` 增加一行 `<version>32.0</version>`
