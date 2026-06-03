## 1. Controller 层健壮性增强

- [x] 1.1 在 `ExportController.java` 中添加 `BusinessException` import
- [x] 1.2 `format` 参数为空时默认使用 "geojson"
- [x] 1.3 在 Controller 中增加 `table_name` 预检查，为空时返回 400
- [x] 1.4 修复 catch 块：区分 `BusinessException` 与普通异常，保持原始状态码

## 2. 编译验证

- [x] 2.1 执行 `mvn compile` 检查编译是否通过
