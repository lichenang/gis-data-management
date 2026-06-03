## 1. 项目基础结构

- [x] 1.1 创建后端项目目录 backend/，使用 Maven 构建
- [x] 1.2 创建 pom.xml，引入所有必需依赖（Spring Boot 3.5.x、MyBatis-Plus、PostgreSQL、PostGIS、GeoTools、JTS、MinIO SDK、Spring Security、jjwt、SpringDoc、Knife4j、Actuator）
- [x] 1.3 创建应用主类 GisPlatformApplication.java
- [x] 1.4 创建 application.yml 配置文件，使用占位符定义 gis.datasource、minio、geoserver、jwt 等配置

## 2. 包结构创建

- [x] 2.1 创建 entity 包和示例实体类骨架
- [x] 2.2 创建 mapper 包和示例 Mapper 接口骨架
- [x] 2.3 创建 service 包和示例 Service 接口骨架
- [x] 2.4 创建 service.impl 包和示例 Service 实现类骨架
- [x] 2.5 创建 controller 包和示例 Controller 骨架
- [x] 2.6 创建 config 包用于配置类
- [x] 2.7 创建 security 包用于安全模块
- [x] 2.8 创建 common 包用于公共组件

## 3. 配置类开发

- [x] 3.1 创建 MybatisPlusConfig，注册 GeometryTypeHandler
- [x] 3.2 创建 DataSourceConfig 配置数据源（可选，使用自动配置）
- [x] 3.3 配置 SpringDoc OpenAPI + Knife4j
- [x] 3.4 配置 Spring Boot Actuator 端点

## 4. 安全模块开发

- [x] 4.1 创建 JwtTokenService 处理 JWT 生成和验证
- [x] 4.2 创建 JwtAuthenticationFilter
- [x] 4.3 创建 SecurityConfig 配置安全策略
- [x] 4.4 配置认证入口点和白名单

## 5. 公共组件开发

- [x] 5.1 创建统一返回格式类 R.java
- [x] 5.2 创建 GlobalExceptionHandler 全局异常处理器
- [x] 5.3 创建业务异常类 BusinessException

## 6. 文档与验证

- [x] 6.1 添加 .gitignore 文件
- [x] 6.2 创建 application-local.yml 示例配置
- [ ] 6.3 验证项目可以成功编译
- [ ] 6.4 验证应用可以正常启动（不连接数据库）
