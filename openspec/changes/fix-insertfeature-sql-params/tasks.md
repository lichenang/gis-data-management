## 1. 修复 insertFeature SQL 拼接

- [x] 1.1 重构 `insertFeature` 方法中 VALUES 子句的构建逻辑，使用 StringBuilder 统一构建，避免多次 append 导致的括号不匹配
