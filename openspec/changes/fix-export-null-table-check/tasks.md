## 1. Service 层异常替换

- [x] 1.1 在 `DatasetServiceImpl.java` 中添加 `BusinessException` import
- [x] 1.2 替换 `getDatasetAsGeoJSON` 中的 `RuntimeException` 为 `BusinessException`
- [x] 1.3 替换 `getDatasetAsKML` 中的 `RuntimeException` 为 `BusinessException`
- [x] 1.4 替换 `exportShapefileAsZip` 中的 `RuntimeException` 为 `BusinessException`

## 2. 编译验证

- [x] 2.1 执行 `mvn compile` 检查编译是否通过
