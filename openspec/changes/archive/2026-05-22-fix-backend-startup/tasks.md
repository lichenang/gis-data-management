## 1. 代码修复

- [x] 1.1 删除 GeometryTypeHandler.java 中无用的 import `org.postgis.GeometryOID`
- [x] 1.2 修正 parseGeometry 方法中的 `read(ByteBuffer.wrap(bytes))` 为 `read(bytes)`
- [x] 1.3 删除未使用的 `import java.nio.ByteBuffer`（如果需要）

## 2. 验证

- [ ] 2.1 验证 Maven 编译成功：`mvn clean compile`
- [ ] 2.2 验证应用启动成功：`mvn spring-boot:run`

## 3. 清理

- [ ] 3.1 确认修复完成后删除诊断报告 openspec/specs/debug-backend-startup.md（可选）
