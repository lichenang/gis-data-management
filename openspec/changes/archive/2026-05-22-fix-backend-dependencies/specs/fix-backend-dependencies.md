## MODIFIED Requirements

### Requirement: 后端项目基础结构初始化
后端项目 SHALL 具备标准的 Maven 项目结构、完整的依赖配置、统一的代码包结构和全局通用组件，以确保后续业务开发遵循统一标准。

#### Scenario: Maven 依赖版本正确
- **WHEN** 执行 `mvn clean compile` 命令
- **THEN** 系统 SHALL 成功解析并下载所有 Maven 依赖，无版本冲突或找不到 artifact 的错误

#### Scenario: GeoTools 依赖版本一致
- **WHEN** 检查 gt-main 和 gt-jdbc-postgis 版本
- **THEN** 系统 SHALL 确保两个依赖使用相同的主版本号（如 32.0 或 22.x），避免运行时类加载错误

#### Scenario: Knife4j 与 Spring Boot 3.x 兼容
- **WHEN** 启动应用并访问 /doc.html 或 /swagger-ui.html
- **THEN** 系统 SHALL 使用正确的 Knife4j artifact 名称（knife4j-openapi3-jakarta-spring-boot-starter），确保 API 文档正常显示

#### Scenario: MinIO SDK 版本可用
- **WHEN** MinIO 客户端代码尝试连接对象存储服务
- **THEN** 系统 SHALL 使用版本 8.6.0 的 minio SDK，确保 API 调用正常

#### Scenario: Hutool 工具库版本兼容
- **WHEN** 项目代码使用 Hutool 工具类
- **THEN** 系统 SHALL 使用版本 5.8.42，避免因版本差异导致的 API 不兼容问题
