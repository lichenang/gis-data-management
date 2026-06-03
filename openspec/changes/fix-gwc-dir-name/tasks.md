## 1. 确认 GWC 目录路径

- [x] 1.1 确认当前代码 `workspace + "_" + layerName` 格式与实际 GWC 磁盘目录名一致（已确认，代码在第 82-83 行使用 `workspace + "_" + layerName`）
- [x] 1.2 代码已正确，无需修改（`{dataDir}/gwc/{workspace}_{layerName}` 格式）

## 2. 编译验证

- [x] 2.1 运行 `mvn compile -f backend/pom.xml` 确认修改编译通过（BUILD SUCCESS）
- [x] 2.2 检查无新增编译警告（仅预存的 deprecation/unchecked 警告）
