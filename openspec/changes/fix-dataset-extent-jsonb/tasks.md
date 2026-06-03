# Tasks: fix-dataset-extent-jsonb

## Task 1: 修改 Dataset 实体添加 typeHandler 注解 ✓

**文件**: `backend/src/main/java/com/gisplatform/entity/Dataset.java`

1. 添加 import:
```java
import com.gisplatform.common.handler.PGobjectJsonbTypeHandler;
```

2. 修改 extent 字段，添加 `@TableField(typeHandler = ...)` 注解:
```java
@TableField(typeHandler = PGobjectJsonbTypeHandler.class)
@Schema(description = "空间范围")
private String extent;
```

## Task 2: 验证编译 ✓

执行 Maven 编译：
```bash
cd backend
mvn compile
```

## Task 3: 测试发布接口（需要手动测试）✓

1. 重启应用
2. 调用 `POST /api/v1/images/{id}/publish`
3. 确认成功，不再出现 jsonb 类型错误
4. 查询数据库验证 extent 字段:
```sql
SELECT extent FROM dataset WHERE id = <id>;
```
