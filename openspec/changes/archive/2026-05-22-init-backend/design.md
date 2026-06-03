## Context

当前 GIS 平台项目已完成技术规范设计，明确了使用 Spring Boot 3.5.x + MyBatis-Plus + PostgreSQL/PostGIS + GeoServer 技术栈。后端项目需要初始化基础结构，包括依赖管理、配置规范、包结构、全局组件（异常处理、空间类型转换器）等。

## Goals / Non-Goals

**Goals:**
1. 创建标准 Maven 项目结构，引入所有必需的依赖
2. 配置 application.yml，使用占位符读取外部配置，遵循 gis.datasource、minio、geoserver 命名约定
3. 创建标准包结构（entity、mapper、service、controller、config、security、common）
4. 实现全局统一返回格式和异常处理
5. 实现 GeometryTypeHandler 处理 PostGIS geometry 与 JTS 的映射
6. 配置 Spring Security + JWT 认证基础框架

**Non-Goals:**
- 不包含业务功能的数据库表结构
- 不包含具体的业务 Service 和 Controller 实现（用户管理、数据集管理等后续实现）
- 不包含前端项目

## Decisions

### 1. Spring Boot 版本选择 3.5.x
- **原因**：Spring Boot 3.5.x 是 3.x 系列的最新 LTS 版本，性能更好，支持 Java 21，同时 mybatis-plus-spring-boot3-starter 已完美兼容
- **替代方案**：Spring Boot 3.3.x 或 3.4.x - 已过时，不选择

### 2. MyBatis-Plus 替代 MyBatis 原生
- **原因**：提供强大的 CRUD 操作、Lambda 条件构造器、分页插件等，减少样板代码
- **替代方案**：原生 MyBatis - 需要手写 SQL，效率低

### 3. ConfigurationProperties 方式读取配置
- **原因**：类型安全、支持嵌套配置、支持数据校验
- **替代方案**：@Value 注解 - 不支持嵌套配置，不够优雅

### 4. JWT 认证使用 jjwt 库
- **原因**：jjwt 是 Java 界最成熟的 JWT 库，支持所有签名算法
- **替代方案**：其他库（Nimbus、Auth0）- 功能类似但不如 jjwt 流行

### 5. 使用 SpringDoc + Knife4j 生成 API 文档
- **原因**：SpringDoc 是 Spring Boot 3.x 原生支持，Knife4j 提供增强 UI
- **替代方案**：Springfox (Swagger 2) - 不支持 Spring Boot 3.x

## Risks / Trade-offs

- **[风险]** PostGIS geometry 类型与 Java 对象转换复杂
  - **缓解**：实现自定义 GeometryTypeHandler，使用 JTS WKT 格式作为中间转换

- **[风险]** GeoServer REST API 客户端兼容性
  - **封装适配层**：创建 GeoServerClient 统一封装不同版本 API 兼容处理

- **[风险]** JWT Token 刷新机制
  - **方案**：Access Token 15分钟过期，Refresh Token 7天过期，Refresh Token 存储在 Redis

