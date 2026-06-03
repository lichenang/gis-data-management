## 1. 添加版本声明

- [ ] 1.1 在 `pom.xml` 的 `dependencies` 区域中，为 `gt-shapefile` 添加 `<version>32.0</version>` 显式版本声明

## 2. 验证

- [ ] 2.1 运行 `mvn dependency:resolve` 确认依赖解析正常
- [ ] 2.2 运行 `mvn compile` 确认编译通过
- [ ] 2.3 在 IDE 中重新导入 Maven 项目，确认 `org.geotools.data.shapefile.*` 不再报 `Cannot resolve symbol`
