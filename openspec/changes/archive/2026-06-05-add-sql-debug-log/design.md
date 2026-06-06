## Context

`MultiFormatImportServiceImpl.insertFeature()` 方法在执行 INSERT SQL 时出现参数数量不匹配错误，需要添加调试日志来定位问题。

### 问题现象

- 错误: `"栏位索引超过许可范围：10，栏位数：9"`
- 堆栈: `MultiFormatImportServiceImpl.insertFeature(MultiFormatImportServiceImpl.java:323)`

### 添加的调试信息

1. SQL 生成后打印完整 SQL 语句
2. 统计占位符 `?` 的数量
3. 统计列名数量
4. 验证两者是否匹配
5. 如不匹配，抛出带详细信息的 `IllegalStateException`

## Goals / Non-Goals

**Goals:**
- 添加调试日志，帮助定位 SQL 占位符数量不匹配问题
- 添加验证逻辑，在问题发生时提供清晰的错误信息

**Non-Goals:**
- 不修改 SQL 构建逻辑
- 不修改业务功能

## 实现方案

在 `insertFeature` 方法中 SQL 生成后添加以下代码：

```java
// 调试日志
String finalSql = sql.toString();
long placeholderCount = finalSql.chars().filter(ch -> ch == '?').count();
logger.info("Generated SQL has {} columns and {} placeholders. SQL: {}",
        columnNames.size(), placeholderCount, finalSql);

// 验证
if (placeholderCount != columnNames.size()) {
    throw new IllegalStateException("SQL placeholder mismatch: expected " +
            columnNames.size() + " but found " + placeholderCount + " in SQL: " + finalSql);
}
```

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|---------|
| 调试代码仅在开发环境需要 | 可通过日志级别控制，生产环境降低日志级别 |
