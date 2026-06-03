# Design: fix-dataset-extent-jsonb

## 修复方案

### 1. 修改 Dataset 实体

**文件**: `backend/src/main/java/com/gisplatform/entity/Dataset.java`

需要修改:

1. 添加 import:
```java
import com.gisplatform.common.handler.PGobjectJsonbTypeHandler;
```

2. 给 extent 字段添加注解:
```java
@TableField(typeHandler = PGobjectJsonbTypeHandler.class)
@Schema(description = "空间范围")
private String extent;
```

修改后 Dataset.java 相关部分:
```java
import com.baomidou.mybatisplus.annotation.*;
import com.gisplatform.common.handler.PGobjectJsonbTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

// ...

@Schema(description = "空间范围")
@TableField(typeHandler = PGobjectJsonbTypeHandler.class)
private String extent;
```

## 数据流（修复后）

```
ImageServiceImpl.publishImageDataset()
│
├─ 创建 JSON 字符串:
│   String extentJson = String.format("{\"minX\":%s,...}", ...);
│
├─ dataset.setExtent(extentJson)
│   └─ extent 是 String 类型
│
├─ this.updateById(dataset)
│   └─ MyBatis-Plus 检测到 @TableField(typeHandler = ...)
│
└─ PGobjectJsonbTypeHandler.setNonNullParameter()
    └─ 创建 PGobject(jsonb, extentJson)
       └─ ps.setObject(i, pgObject)
          └─ PostgreSQL JSONB ✓ 正确写入
```

## 验证步骤

1. Maven 编译: `mvn compile -pl backend`
2. 重启应用
3. 调用 `POST /api/v1/images/{id}/publish`
4. 确认不再出现 jsonb 类型错误
5. 查询数据库验证 extent 字段正确存储为 JSONB
