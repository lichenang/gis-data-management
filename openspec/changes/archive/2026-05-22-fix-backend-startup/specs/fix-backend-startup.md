## MODIFIED Requirements

### Requirement: 后端项目基础结构初始化
后端项目 SHALL 具备标准的 Maven 项目结构、完整的依赖配置、统一的代码包结构和全局通用组件，以确保后续业务开发遵循统一标准。

#### Scenario: GeometryTypeHandler 编译成功
- **WHEN** 执行 `mvn clean compile` 命令
- **THEN** 系统 SHALL 成功编译 GeometryTypeHandler.java，无 "找不到符号" 或 "Cannot resolve method" 错误

#### Scenario: 后端应用启动成功
- **WHEN** 执行 `mvn spring-boot:run` 命令
- **THEN** 系统 SHALL 成功启动应用，无任何运行时错误

#### Scenario: GeometryTypeHandler 正确处理空间数据
- **WHEN** 代码中使用 GeometryTypeHandler 进行几何数据存取
- **THEN** 系统 SHALL 正确地将 JTS Geometry 对象转换为 WKB 字节数组并写入数据库，以及从数据库读取 WKB 数据并转换为 JTS Geometry 对象
