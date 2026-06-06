## 1. 修复 VectorDataStoreFactory

- [x] 1.1 删除 switch-case 中多余的 `case KMZ:` 语句
- [x] 1.2 更新 GeoTools import: `org.geotools.data.DataStore` → `org.geotools.api.data.DataStore`
- [x] 1.3 更新 GeoTools import: `org.geotools.data.DataStoreFinder` → `org.geotools.api.data.DataStoreFinder`

## 2. 验证

- [x] 2.1 执行 `mvn compile` 验证编译通过
