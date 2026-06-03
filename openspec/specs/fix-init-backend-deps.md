# init-backend 依赖修复建议

## 概述

本文档分析 `init-backend` 变更后 pom.xml 中的依赖问题，并提供修复建议。

## 问题分析

### 1. 用户已修正的依赖（4项）✅

| 依赖 | 原问题 | 修正方案 | 状态 |
|------|--------|----------|------|
| Hutool | 可能存在兼容性问题 | 5.8.42 | ✅ 已修正 |
| Knife4j | 错误的 artifact 名称 | knife4j-openapi3-jakarta-spring-boot-starter 4.5.0 | ✅ 已修正 |
| MinIO | 小版本号差异 | 8.6.0 | ✅ 已修正 |
| gt-jdbc-postgis | 未显式声明版本 | 22.2 | ✅ 已修正 |

### 2. 新发现的版本不一致问题 🔴

**核心问题：GeoTools 主模块与 JDBC 模块版本不一致**

```
当前配置：
├── gt-main: 32.0          ❌ 版本过高
└── gt-jdbc-postgis: 22.2  ✅ 用户新添加的
```

**问题说明**：
- `gt-main 32.0` 和 `gt-jdbc-postgis 22.2` 属于不同的 GeoTools 大版本
- 版本号必须保持一致（如全部用 22.x 或全部用 32.x）
- GeoTools 32.x 需要配置 OSGeo 仓库，且某些模块可能不在 Maven Central

**影响**：
- 可能导致运行时类找不到或方法签名不匹配
- 编译时可能拉取失败

## 修复方案

### 方案 A：降级到 GeoTools 22.x（推荐）✅

统一使用 GeoTools 22.x 系列，版本稳定且仓库可正常访问：

```xml
<!-- 统一版本为 22.2 -->
<dependency>
    <groupId>org.geotools</groupId>
    <artifactId>gt-main</artifactId>
    <version>22.2</version>
</dependency>

<dependency>
    <groupId>org.geotools.jdbc</groupId>
    <artifactId>gt-jdbc-postgis</artifactId>
    <version>22.2</version>
</dependency>
```

### 方案 B：升级到 GeoTools 32.x（需要额外仓库配置）

如果需要使用 GeoTools 32.x，需要同时修改两个依赖并确保仓库配置正确：

```xml
<!-- 统一版本为 32.0 -->
<dependency>
    <groupId>org.geotools</groupId>
    <artifactId>gt-main</artifactId>
    <version>32.0</version>
</dependency>

<dependency>
    <groupId>org.geotools.jdbc</groupId>
    <artifactId>gt-jdbc-postgis</artifactId>
    <version>32.0</version>
</dependency>
```

**注意**：GeoTools 32.x 可能存在模块拆分问题，可能还需要添加额外依赖，如：
```xml
<dependency>
    <groupId>org.geotools</groupId>
    <artifactId>gt-referencing</artifactId>
    <version>32.0</version>
</dependency>
```

## 完整修复后的 pom.xml 关键变更

### 依赖版本管理（dependencyManagement）

```xml
<!-- GeoTools 统一为 22.2 -->
<dependency>
    <groupId>org.geotools</groupId>
    <artifactId>gt-main</artifactId>
    <version>22.2</version>
</dependency>

<dependency>
    <groupId>org.geotools.jdbc</groupId>
    <artifactId>gt-jdbc-postgis</artifactId>
    <version>22.2</version>
</dependency>
```

### 或使用 32.x（如果需要新特性）

```xml
<!-- GeoTools 统一为 32.0 -->
<dependency>
    <groupId>org.geotools</groupId>
    <artifactId>gt-main</artifactId>
    <version>32.0</version>
</dependency>

<dependency>
    <groupId>org.geotools.jdbc</groupId>
    <artifactId>gt-jdbc-postgis</artifactId>
    <version>32.0</version>
</dependency>
```

## 验证步骤

修复后请执行以下验证：

```bash
# 1. 清理 Maven 缓存
mvn clean

# 2. 验证依赖解析
mvn dependency:resolve

# 3. 编译项目
mvn compile

# 4. 启动应用（可选）
mvn spring-boot:run
```

## 其他依赖检查结果

以下依赖经检查无问题：

| 依赖 | 版本 | 状态 |
|------|------|------|
| Spring Boot | 3.5.0 | ✅ |
| MyBatis-Plus | 3.5.7 | ✅ |
| PostgreSQL 驱动 | 42.7.3 | ✅ |
| PostGIS JDBC | 2.5.1 | ✅ |
| JTS | 1.19.0 | ✅ |
| JWT (jjwt) | 0.12.5 | ✅ |
| SpringDoc OpenAPI | 2.6.0 | ✅ |
| Lombok | 1.18.34 | ✅ |
| Hutool | 5.8.42 | ✅ |
| MinIO | 8.6.0 | ✅ |
| Knife4j | 4.5.0 | ✅ |

## 总结

- ✅ 用户已正确修正了 4 个依赖问题
- 🔴 需要修复 gt-main 与 gt-jdbc-postgis 版本不一致问题
- 建议使用**方案 A（GeoTools 22.x）**，稳定性更好
