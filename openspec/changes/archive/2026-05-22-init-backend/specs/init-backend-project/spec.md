## ADDED Requirements

### Requirement: 后端项目基础结构初始化
后端项目 SHALL 具备标准的 Maven 项目结构、完整的依赖配置、统一的代码包结构和全局通用组件，以确保后续业务开发遵循统一标准。

#### Scenario: 创建标准 Maven 项目
- **WHEN** 执行项目初始化
- **THEN** 系统 SHALL 生成符合以下条件的 Maven 项目：
  - Java 版本为 17
  - Spring Boot 版本为 3.5.x
  - 包含所有必需的依赖（Spring Web、MyBatis-Plus、PostgreSQL 驱动、PostGIS、GeoTools、JTS、MinIO SDK、Spring Security、jjwt、SpringDoc OpenAPI、Knife4j、Spring Boot Actuator）

#### Scenario: 配置文件正确加载
- **WHEN** 应用启动时
- **THEN** 系统 SHALL 通过 application.yml 中定义的占位符正确读取以下配置：
  - 数据库连接（gis.datasource.*）
  - MinIO 配置（minio.*）
  - GeoServer 配置（geoserver.*）
  - JWT 配置（jwt.*）

#### Scenario: 包结构符合规范
- **WHEN** 查看项目源码目录
- **THEN** 系统 SHALL 包含以下包结构：
  - com.gisplatform.entity（实体类）
  - com.gisplatform.mapper（Mapper 接口）
  - com.gisplatform.service（Service 接口）
  - com.gisplatform.service.impl（Service 实现）
  - com.gisplatform.controller（控制器）
  - com.gisplatform.config（配置类）
  - com.gisplatform.security（安全模块）
  - com.gisplatform.common（公共组件）

#### Scenario: 全局异常统一处理
- **WHEN** 业务代码抛出异常时
- **THEN** 系统 SHALL 通过 GlobalExceptionHandler 统一捕获并返回以下格式：
  ```json
  { "code": number, "message": string, "data": null }
  ```

#### Scenario: 空间数据类型正确映射
- **WHEN** MyBatis-Plus 执行空间数据存取时
- **THEN** 系统 SHALL 通过 GeometryTypeHandler 将 PostGIS geometry 类型与 JTS geometry 对象正确转换

#### Scenario: API 文档可访问
- **WHEN** 应用运行在开发环境时
- **THEN** 系统 SHALL 通过 SpringDoc + Knife4j 生成 API 文档，可通过 /doc.html 或 /swagger-ui.html 访问
