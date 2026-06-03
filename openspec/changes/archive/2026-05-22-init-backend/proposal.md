## Why

当前 GIS 平台项目已完成技术规范设计（openspec/specs/gis-platform.md），明确了整体架构和技术选型。考虑到项目将采用 Spring Boot 3.5.x 框架、PostgreSQL + PostGIS 数据库、GeoServer 地图服务等技术栈，后端项目需要标准化初始化，包括统一依赖管理、配置规范、包结构、全局组件（异常处理、空间类型转换器）等，确保后续开发遵循统一标准。

## What Changes

1. **创建 Maven 项目结构**
   - 创建标准 Java 17 + Maven 项目
   - 配置 pom.xml，引入 Spring Boot 3.5.x 及相关依赖

2. **创建应用主类和配置文件**
   - 创建主程序入口类 GisPlatformApplication
   - 创建 application.yml，使用占位符读取外部配置

3. **创建标准包结构**
   - 创建 entity、mapper、service、service.impl、controller、config、security、common 包

4. **创建全局通用组件**
   - 全局统一返回格式 R.java
   - 全局异常处理器 GlobalExceptionHandler
   - 自定义 GeometryTypeHandler 注册到 MybatisPlusConfig

5. **配置安全模块**
   - 创建 SecurityConfig 安全配置
   - 创建 JwtTokenService 用于 JWT 操作

## Capabilities

### New Capabilities

- **init-backend-project**: 创建 GIS 平台后端项目的基础结构，包括依赖管理、配置、通用组件、安全模块等。

### Modified Capabilities

- 无（变更不涉及需求变更）

## Impact

- 新增文件：pom.xml、application.yml、Application 主类、实体类、Mapper 接口、Service 接口及实现、Controller 类、配置类、安全组件、全局异常处理器等。

## Non-goals

- 不包含具体的业务功能实现（用户管理、数据集管理等）
- 不包含数据库表结构的初始化脚本
- 不包含前端的创建
