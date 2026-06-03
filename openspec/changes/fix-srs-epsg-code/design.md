# Design: fix-srs-epsg-code

## 修复方案

### 修改 GeoTiffParser.extractCrs() 方法

**文件**: `backend/src/main/java/com/gisplatform/util/GeoTiffParser.java`

需要修改 imports 和 extractCrs 方法：

1. 添加 import:
```java
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
```

2. 重写 extractCrs 方法，使用 GeoTools API:

```java
private static String extractCrs(Object crsObj) {
    if (crsObj == null) {
        return null;
    }

    // 如果已经是 CoordinateReferenceSystem，直接处理
    if (crsObj instanceof CoordinateReferenceSystem) {
        CoordinateReferenceSystem crs = (CoordinateReferenceSystem) crsObj;
        
        // 方法1: 尝试从 getIdentifiers() 获取 EPSG 代码
        try {
            Set<Identifier> ids = crs.getIdentifiers();
            if (ids != null && !ids.isEmpty()) {
                for (Identifier id : ids) {
                    if (id != null) {
                        String code = id.getCode();
                        if (code != null && !code.isEmpty()) {
                            return "EPSG:" + code;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 忽略，继续尝试其他方法
        }

        // 方法2: 使用 CRS.lookupHorizontalCRS 查找
        try {
            CoordinateReferenceSystem horizontalCRS = CRS.lookupHorizontalCRS(crs);
            if (horizontalCRS != null) {
                Set<Identifier> ids = horizontalCRS.getIdentifiers();
                if (ids != null && !ids.isEmpty()) {
                    for (Identifier id : ids) {
                        if (id != null) {
                            String code = id.getCode();
                            if (code != null && !code.isEmpty()) {
                                return "EPSG:" + code;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 忽略
        }

        // 方法3: 尝试从 getName 获取并解析
        try {
            String name = crs.getName().getCode();
            if (name != null) {
                // 尝试从名称中提取 EPSG 代码
                java.util.regex.Pattern p = java.util.regex.Pattern.compile("EPSG[:\\s]*(\\d+)");
                java.util.regex.Matcher m = p.matcher(name);
                if (m.find()) {
                    return "EPSG:" + m.group(1);
                }
            }
        } catch (Exception e) {
            // 忽略
        }
    }

    // Fallback: 使用原有的反射逻辑（保持向后兼容）
    return extractCrsFallback(crsObj);
}

private static String extractCrsFallback(Object crsObj) {
    // 原有逻辑，保留作为最终 fallback
    String code = null;
    try {
        java.lang.reflect.Method getIds = crsObj.getClass().getMethod("getIdentifiers");
        Object identifiers = getIds.invoke(crsObj);
        if (identifiers != null) {
            java.lang.reflect.Method iterator = identifiers.getClass().getMethod("iterator");
            Object iter = iterator.invoke(identifiers);
            java.lang.reflect.Method hasNext = iter.getClass().getMethod("hasNext");
            java.lang.reflect.Method next = iter.getClass().getMethod("next");
            if ((Boolean) hasNext.invoke(iter)) {
                code = next.invoke(iter).toString();
            }
        }
    } catch (Exception e) {
        // 忽略异常
    }
    if (code == null || code.isEmpty()) {
        try {
            java.lang.reflect.Method getName = crsObj.getClass().getMethod("getName");
            Object name = getName.invoke(crsObj);
            if (name != null) {
                java.lang.reflect.Method toString = name.getClass().getMethod("toString");
                code = toString.invoke(name).toString();
            }
        } catch (Exception e) {
            code = "Unknown";
        }
    }
    return code;
}
```

## 数据流（修复后）

```
GeoTiffParser.parse()
│
├─ coverage.getCoordinateReferenceSystem()
│   └─ 返回 CoordinateReferenceSystem 对象
│
├─ extractCrs(crs)
│   ├─ getIdentifiers() → "3857"
│   └─ 返回 "EPSG:3857" ✓
│
└─ metadata.setCrs("EPSG:3857")
    └─ Dataset.srs = "EPSG:3857"
        └─ CrsTransformUtil.transformExtentToWgs84()
            └─ ✓ 正确解析和转换
```

## 验证步骤

1. Maven 编译: `mvn compile -f backend/pom.xml`
2. 重启应用
3. 上传 GeoTIFF 文件
4. 查询数据库验证 crs 字段:
```sql
SELECT srs FROM dataset WHERE id = <id>;
```
5. 调用发布接口测试 CRS 转换
