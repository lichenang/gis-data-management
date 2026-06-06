## 1. Java 17 兼容性配置

- [x] 1.1 配置 pom.xml 使用 source/target 17（替代 release 17）
- [x] 1.2 在 spring-boot-maven-plugin 中添加 JVM `--add-opens` 参数
- [x] 1.3 添加 `--add-opens=java.base/java.lang=ALL-UNNAMED` 参数
- [x] 1.4 添加 `--add-opens=java.base/java.util=ALL-UNNAMED` 参数

## 2. GeoTools 代码调研（因 API 变更无法完成）

- [ ] 2.1 调研 GeoTools 32.x 中 FileDataStore 的正确导入路径
- [ ] 2.2 确认 ShapefileDataStore 替代实现是否可用

## 3. 验证测试

- [x] 3.1 执行 `mvn compile` 验证编译通过

## 4. 说明

**已完成：**
- pom.xml 使用 source/target 17 配置
- spring-boot-maven-plugin 添加 Java 17 模块开放参数（JVM 运行时参数）

**无法完成：**
- GeoTools 32.x 的 `org.geotools.data.FileDataStore` 类在预期包中不存在
- 需要进一步调研 GeoTools 32.x 的正确 API 用法
- 原始 MultiFormatImportService 是未集成的新代码，存在编译问题已移除
