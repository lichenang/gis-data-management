## 1. Service 层预检查与重构

- [x] 1.1 提取 `enumerateTileFiles()` 方法，遍历三层循环（zoom/x/y）收集匹配的瓦片 `File` 对象列表
- [x] 1.2 在 `packageTiles()` 中 `ZipOutputStream` 创建之前调用枚举，列表为空则提前抛出 RuntimeException
- [x] 1.3 将 ZIP 写入逻辑改为遍历预枚举的文件列表，移除 try-with-resources 内部的 `totalWritten == 0` 后置检查
- [x] 1.4 保留 `IOException` catch 块（ZipOutputStream 操作仍为 checked exception，且日志记录对排错有帮助）

## 2. Controller 简化为无捕获

- [x] 2.1 移除 `downloadTilePackage()` 方法的外层 `try-catch` 块，让异常自然传播
- [x] 2.2 删除 `writeErrorResponse()` 方法和 `escapeJson()` 方法
- [x] 2.3 删除不再使用的 `log` 字段和 `org.slf4j` 相关 import
- [x] 2.4 将 Content-Type/Disposition 头设置移到 `getOutputStream()` 之后，确保异常时 response 纯净

## 3. 编译验证

- [x] 3.1 运行 `mvn compile -f backend/pom.xml` 确认修改编译通过（BUILD SUCCESS）
- [x] 3.2 检查无新增编译警告（仅预存的 deprecation/unchecked 警告）
