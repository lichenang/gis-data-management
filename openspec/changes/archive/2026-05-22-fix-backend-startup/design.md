
## Context

在 init-backend 骨架中，GeometryTypeHandler.java 存在两处代码错误导致编译失败：
1. 使用了不存在的 import 语句
2. 调用了不存在的 JTS API 方法

经过 openspec/specs/debug-backend-startup.md 诊断分析，确认了问题根因和修复方案。

## Goals / Non-Goals

**Goals:**
- 修复 GeometryTypeHandler 编译错误
- 确保后端项目能够正常编译和启动

**Non-Goals:**
- 不修改任何依赖版本
- 不引入新功能
- 不修改其他代码文件

## Decisions

### 1. 删除无用 import
- **问题**：PostGIS JDBC 2.5.x 已移除 GeometryOID 类
- **解决**：删除 `import org.postgis.GeometryOID;`
- **代码位置**：第13行

### 2. 修正方法调用
- **问题**：JTS WKBReader.read() 不支持 ByteBuffer 参数
- **解决**：改用 read(byte[]) 方法
- **代码位置**：第116行

```java
// 错误
return WKB_READER_3D.read(ByteBuffer.wrap(bytes));

// 正确
return WKB_READER_3D.read(bytes);
```

## Risks / Trade-offs

代码修复简单直接，无明显风险。

---

## 修复代码

修改文件：`backend/src/main/java/com/gisplatform/common/handler/GeometryTypeHandler.java`

### 修改 1：删除 import

```diff
-import org.postgis.GeometryOID;
```

### 修改 2：修正 parseGeometry 方法

```diff
    private Geometry parseGeometry(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
-           return WKB_READER_3D.read(ByteBuffer.wrap(bytes));
+           return WKB_READER_3D.read(bytes);
        } catch (Exception e) {
            return null;
        }
    }
```

### 完整修改后的相关代码

```java
// 删除这行 import（不再需要）
// import org.postgis.GeometryOID;

// 删除这行 import（不再需要）
// import java.nio.ByteBuffer;

// parseGeometry 方法修改为：
private Geometry parseGeometry(byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
        return null;
    }
    try {
        // 修复：使用正确的 read(byte[]) 方法
        return WKB_READER_3D.read(bytes);
    } catch (Exception e) {
        return null;
    }
}
```

## 验证步骤

```bash
cd backend
mvn clean compile
```

编译成功后可启动应用：

```bash
mvn spring-boot:run
```
