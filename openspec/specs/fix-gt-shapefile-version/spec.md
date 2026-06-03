## ADDED Requirements

### 模块划分

| 模块 | 位置 | 职责 |
|---|---|---|
| pom.xml | `backend/pom.xml` | 在 `dependencies` 区域为 `gt-shapefile` 添加显式版本声明 |

### Requirement: gt-shapefile 依赖版本号显式声明

pom.xml 的 `dependencies` 区域中 `gt-shapefile` 依赖 SHALL 包含 `<version>32.0</version>` 显式版本声明。

#### Scenario: IDE 能解析 gt-shapefile 中的类

- **WHEN** IDE 重新导入 Maven 项目
- **THEN** `org.geotools.data.shapefile.ShapefileDataStore` 可被 IDE 解析
- **AND** `org.geotools.data.shapefile.ShapefileDataStoreFactory` 可被 IDE 解析
- **AND** IDE 不再报 `Cannot resolve symbol 'shapefile'`

#### Scenario: Maven 命令行编译正常

- **WHEN** 执行 `mvn compile`
- **THEN** 编译成功（BUILD SUCCESS）
- **AND** 无新增警告或错误
