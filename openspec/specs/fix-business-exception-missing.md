# fix-business-exception-missing

## 问题描述

项目启动时报 `ClassNotFoundException: BusinessException`。

## 调查结论

**BusinessException 类存在于代码库中**，位置：
```
backend/src/main/java/com/gisplatform/common/exception/BusinessException.java
```

**GlobalExceptionHandler 正确引用了 BusinessException**：
```java
// GlobalExceptionHandler.java
package com.gisplatform.common.exception;  // 同包，无需 import

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusinessException(BusinessException e, ...) {
        // ...
    }
}
```

**结论**: 代码完整，不是缺失问题。

## 根本原因

这是 **Maven/IDE 编译缓存问题**，不是代码缺失。

```
┌─────────────────────────────────────────────────────────────────┐
│  编译流程问题                                                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. 编译 GlobalExceptionHandler.java 时                         │
│     → 编译器看到 @ExceptionHandler(BusinessException.class)     │
│     → 尝试解析 BusinessException                                │
│                                                                  │
│  2. 如果此时 BusinessException.class 不存在（未编译或缓存）      │
│     → 运行时抛出 ClassNotFoundException                         │
│                                                                  │
│  常见场景:                                                       │
│  - mvn compile 后直接运行，但 BusinessException 未重新编译       │
│  - IDE 增量编译，BusinessException.java 被修改但未同步编译       │
│  - target/classes 中的 class 文件损坏或过期                     │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## 解决方案

### 方案 1: Maven Clean Rebuild (推荐)

```bash
cd backend
mvn clean compile
mvn spring-boot:run
```

或者一次性执行：
```bash
mvn clean spring-boot:run
```

### 方案 2: IDE 强制刷新

**IntelliJ IDEA**:
1. `File` → `Invalidate Caches` → `Invalidate and Restart`
2. 或者 `Build` → `Rebuild Project`

**Eclipse**:
1. `Project` → `Clean...` → 选择项目
2. 勾选 `Build immediately`

### 方案 3: 手动删除 target 目录

```bash
rm -rf backend/target
mvn compile
```

## 验证方法

修复后启动项目，如果不再出现 `ClassNotFoundException: BusinessException`，则问题解决。

## 修改位置

无代码修改需求。这是构建环境问题，不是代码问题。
