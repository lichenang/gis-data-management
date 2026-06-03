# Tasks: fix-add-epsg-dependency

## Task 1: 添加 gt-epsg-hsql 依赖

**File**: `backend/pom.xml`

已添加的依赖：
- `gt-epsg-hsql` - EPSG 坐标参考系统数据库（HSQL 嵌入式）

- [x] 已完成

## Task 2: 验证编译

执行 Maven 编译：

```bash
cd backend
mvn compile
```

结果：BUILD SUCCESS

- [x] 已完成
