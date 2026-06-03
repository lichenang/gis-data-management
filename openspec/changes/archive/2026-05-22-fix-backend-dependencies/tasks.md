## 1. 验证依赖修复

- [ ] 1.1 验证 Maven 依赖解析成功：`mvn dependency:resolve`
- [ ] 1.2 验证项目编译成功：`mvn clean compile`
- [ ] 1.3 验证应用启动成功：`mvn spring-boot:run`（不连接数据库）

## 2. 功能验证

- [ ] 2.1 验证 SpringDoc / Knife4j API 文档可访问（访问 /doc.html）
- [ ] 2.2 验证 GeoTools 类可以正常加载（检查空间数据处理功能）

## 3. 文档更新

- [ ] 3.1 更新 openspec/specs/fix-init-backend-deps.md 修复建议文档状态为"已修复"
