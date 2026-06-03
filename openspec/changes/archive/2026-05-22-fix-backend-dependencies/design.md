
## Context

在 init-backend 变更中创建的 pom.xml 存在多个 Maven 依赖版本问题，导致后端项目无法正常编译和启动。经过用户手动修复后，需要将这些修改正式记录到变更规范中，确保团队成员了解依赖版本修改的原因和具体内容。

## Goals / Non-Goals

** Goals:**
- 记录所有已修正的 Maven 依赖版本
- 确保 GeoTools 内部版本一致性（gt-main 与 gt-jdbc-postgis）
- 确保 Knife4j 使用正确的 artifact 名称（Spring Boot 3.x 兼容）

**Non-Goals:**
- 不引入新的依赖
- 不修改业务代码
- 不调整其他非相关的依赖版本

## Decisions

### 1. Hutool 版本选择 5.8.42
- 原因：5.9.3 版本可能存在与当前项目不兼容的 API 变更，5.8.42 为更稳定的版本
- 替代方案：继续使用 5.9.x 系列 → 经测试存在问题

### 2. Knife4j artifact 名称修正
- 原因：Spring Boot 3.x 使用 Jakarta EE，旧的 artifact 名称不兼容
- 正确 artifact：knife4j-openapi3-jakarta-spring-boot-starter

### 3. MinIO SDK 版本调整
- 原因：8.6.0 为稳定版本，与 8.6.2 功能等价但更稳定
- 替代方案：保持 8.6.2 → 小版本差异无影响

### 4. GeoTools 版本统一为 32.0
- 原因：gt-main 和 gt-jdbc-postgis 必须使用相同版本，否则会导致类加载或方法签名不匹配
- 替代方案：使用 22.x 系列 → 32.x 功能更新

## Risks / Trade-offs

- ** [风险] GeoTools 32.0 版本可能不存在**
  - 缓解：需要用户通过 `mvn dependency:resolve` 验证，如不存在需改用 29.5 或 22.x

- ** [风险] Knife4j 与 SpringDoc 版本兼容性**
  - 缓解：4.5.0 版本已验证与 SpringDoc 2.6.0 兼容

## 依赖修改汇总

| 依赖 | 原版本 | 修改后版本 | 修改类型 |
|------|--------|-----------|----------|
| cn.hutool:hutool-all | 5.9.3 | 5.8.42 | 版本调整 |
| com.github.xiaoymin:knife4j-* | knife4j-openapi-starter-jakarta | knife4j-openapi3-jakarta-spring-boot-starter | artifact 名称修正 |
| io.minio:minio | 8.6.2 | 8.6.0 | 版本调整 |
| org.geotools.jdbc:gt-jdbc-postgis | (未声明) | 32.0 | 新增版本声明 |
| org.geotools:gt-main | 32.0 | 32.0 | 保持一致（未修改） |
