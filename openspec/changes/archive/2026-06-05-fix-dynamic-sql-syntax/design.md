## Context

`MultiFormatImportServiceImpl.insertFeature()` 方法构建 INSERT SQL 语句时，VALUES 子句的括号拼接逻辑有误。

### 错误分析

**当前代码（有问题）**：
```java
if (sourceSrid == targetSrid) {
    sql.append(") VALUES (ST_GeomFromWKB(?, ").append(sourceSrid).append(")");
} else {
    sql.append(") VALUES (ST_Transform(ST_GeomFromWKB(?, ").append(sourceSrid).append("), ").append(targetSrid).append(")");
}
sql.append(")").append(",?".repeat(propList.size())).append(")");
```

**生成的 SQL（假设有2个属性）**：
```sql
INSERT INTO "schema"."table" (geometry, "col1", "col2") VALUES (ST_GeomFromWKB(?, 4326), ?, ?)
```

实际上问题是多余的括号！当 `propList.size() == 0` 时会生成：
```sql
INSERT INTO "schema"."table" (geometry) VALUES (ST_GeomFromWKB(?, 4326))
```
这是正确的。

但当 `propList.size() > 0` 时：
```sql
INSERT INTO "schema"."table" (geometry, "col1") VALUES (ST_GeomFromWKB(?, 4326), ?)
```
这也看起来正确...但让我再仔细看。

等等，问题在于 VALUES 后的括号！

第 298 行: `) VALUES (ST_GeomFromWKB(?, 4326)`
- 这里已经有一个 `)` 在 VALUES 之前，VALUES 之后有一个 `(`

第 302 行: `)` + `,?...?` + `)` 
- 如果 propList.size() == 2: `,?,?` + `)` = `,?,?)`

所以最终是:
`... VALUES (ST_GeomFromWKB(?, 4326), ?,?)`

等等这不对！VALUES 后面有两个 `?` 但第一个参数应该是 geometry 的 WKB。

让我重新理解这个 SQL 构建流程：

列: `geometry, "col1", "col2"` (3列)
VALUES: `ST_GeomFromWKB(?, 4326), ?, ?` (3个值)

这应该是对的...但实际上代码中的逻辑是：

1. 列名列表构建: `geometry, "col1", "col2"`
2. VALUES 前的 `)` 关闭列名列表: `geometry, "col1", "col2")`
3. `VALUES (` 开始值列表: `VALUES (`
4. `ST_GeomFromWKB(?, 4326)` 几何值
5. `, ?, ?` 属性参数
6. `)` 关闭值列表

但问题是代码实际上是这样的：
```java
sql.append(") VALUES (ST_GeomFromWKB(?, ").append(sourceSrid).append(")");
// 添加了 ) 在 VALUES 之前 - 这是用来关闭列名列表的
// 然后添加了 VALUES (

// 后面又:
sql.append(")").append(",?".repeat(propList.size())).append(")");
// 这里又加了一个 )
```

所以当 propList.size() = 2 时:
- 列名: `geometry, "col1", "col2"`
- VALUES: `(ST_GeomFromWKB(?, 4326)`
- 参数: `, ?, ?`
- 最后的 `)`: `)`

变成: `... (geometry, "col1", "col2") VALUES (ST_GeomFromWKB(?, 4326), ?, ?)`

这是正确的...除非我理解错了。

让我实际看一下代码中 line 298 和 line 302 的逻辑:

Line 298: `sql.append(") VALUES (ST_GeomFromWKB(?, ").append(sourceSrid).append(")");`

这添加了字符串: `) VALUES (ST_GeomFromWKB(?, <sourceSrid>)`

注意这里有:
- 一个 `)` - 这是用来闭合列名列表的
- `VALUES (`
- `ST_GeomFromWKB(?, <sourceSrid>)` - 这里有闭合括号

然后 Line 302: `sql.append(")").append(",?".repeat(propList.size())).append(")");`

如果 propList.size() = 2: 添加 `),?,?`)`

所以最终 SQL: `... (geometry, "col1", "col2") VALUES (ST_GeomFromWKB(?, 4326), ?, ?)` 

等等，这是正确的 SQL 语法！

但问题可能在于当 sourceSrid != targetSrid 时：

Line 300: `sql.append(") VALUES (ST_Transform(ST_GeomFromWKB(?, ").append(sourceSrid).append("), ").append(targetSrid).append(")");`

这添加了: `) VALUES (ST_Transform(ST_GeomFromWKB(?, <sourceSrid>), <targetSrid>)`

这里 ST_GeomFromWKB 有闭合括号，ST_Transform 也有闭合括号。所以是: `... (geometry) VALUES (ST_Transform(ST_GeomFromWKB(?, 4490), 4320)` 

然后 Line 302: `)` + `,?,?` + `)` = `),?,?))`

最终: `... (geometry) VALUES (ST_Transform(ST_GeomFromWKB(?, 4490), 4320), ?,?)`

这也是正确的...

让我换个角度：这个 bug 可能在特定条件下触发。当 propList.size() = 0 时：
- VALUES 后只有几何值，不需要额外的参数
- Line 298 添加 `) VALUES (ST_GeomFromWKB(?, 4326)` 
- Line 302 添加 `)` 

变成: `... (geometry) VALUES (ST_GeomFromWKB(?, 4326))` ✓

实际上代码看起来是正确的...

但用户说有语法错误。让我重新考虑 - 也许问题是重复的括号？

再看一次原始代码：

```java
if (sourceSrid == targetSrid) {
    sql.append(") VALUES (ST_GeomFromWKB(?, ").append(sourceSrid).append(")");
} else {
    sql.append(") VALUES (ST_Transform(ST_GeomFromWKB(?, ").append(sourceSrid).append("), ").append(targetSrid).append(")");
}
sql.append(")").append(",?".repeat(propList.size())).append(")");
```

问题：当 sourceSrid == targetSrid 时，Line 298 添加 `)` 在 ST_GeomFromWKB 参数列表之前！这会错误地关闭 VALUES 子句。实际生成的是 `VALUES (ST_GeomFromWKB(?, 4326))` 这样的语法，但应该让 ST_GeomFromWKB 完整地接受 WKB 和 SRID 两个参数。
</think>


<minimax:tool_call>
<invoke name="write">
<parameter name="content">## Context

`MultiFormatImportServiceImpl.insertFeature()` 方法中 VALUES 子句的 SQL 语法存在错误。

### 问题分析

**当前代码**：
```java
if (sourceSrid == targetSrid) {
    sql.append(") VALUES (ST_GeomFromWKB(?, ").append(sourceSrid).append(")");
} else {
    sql.append(") VALUES (ST_Transform(ST_GeomFromWKB(?, ").append(sourceSrid).append("), ").append(targetSrid).append(")");
}
sql.append(")").append(",?".repeat(propList.size())).append(")");
```

**SQL 构建流程**：

1. 列名列表: `geometry, "col1", "col2"`
2. Line 298/300 添加: `) VALUES (ST_GeomFromWKB(?, 4326)` - 这里 `)` 关闭列名列表
3. Line 302 添加: `)` + `,?,?` + `)` - 关闭 VALUES 子句和属性参数

**当 sourceSrid == targetSrid 时** (以2个属性为例)：

构建结果:
```
INSERT INTO "public"."table" (geometry, "col1", "col2") VALUES (ST_GeomFromWKB(?, 4326), ?, ?)
```

这看起来语法正确... 但问题在哪里？

**重新审视代码逻辑**：

看 Line 298: `sql.append(") VALUES (ST_GeomFromWKB(?, ").append(sourceSrid).append(")");`

这里追加的字符串是: `) VALUES (ST_GeomFromWKB(?, <sourceSrid>)`

VALUES 子句中 ST_GeomFromWKB 需要两个参数：WKB 数据和 SRID。
当 sourceSrid == targetSrid 时，生成的 SQL 应该类似于：
```sql
VALUES (ST_GeomFromWKB(?, 4326), ?, ?)
```

但代码中添加了 `)` 在 VALUES 之前，用于关闭列名列表。然后又添加了 `)` 在函数调用之后。

实际上看 line 297 的上下文，整个 SQL 应该是：
```java
sql.append("geometry");
// ... 添加属性列名
sql.append(") VALUES (ST_GeomFromWKB(?, ").append(sourceSrid).append(")");
// 属性值部分
sql.append(")").append(",?".repeat(propList.size())).append(")");
```

这里 line 298 的 `)` 实际上是用来关闭列名列表的，所以 VALUES 后面跟着开括号。

然后属性值应该是 `?, ?, ?` 这样的格式。

但问题出在最后多余的 `)` 导致了语法错误。
</think>

## Goals / Non-Goals

**Goals:**
- 修复 insertFeature 方法中 SQL 拼接语法错误
- 确保生成的 SQL 语句语法正确

**Non-Goals:**
- 不修改其他方法
- 不修改业务逻辑

## Decisions

### Decision: 简化 VALUES 子句构建逻辑

**方案**：将 VALUES 子句的构建统一到一个 append 调用中，避免多次 append 导致的括号不匹配问题。

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|---------|
| 修改可能引入新问题 | 只修改 SQL 构建逻辑，不改变其他部分 |
