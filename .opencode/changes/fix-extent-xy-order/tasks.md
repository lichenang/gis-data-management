# 实现任务：修复 extent 坐标轴顺序

## 任务列表

- [x] ### 1. 修改 extent 解析顺序

**文件**: `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java`
**位置**: 第 396-399 行

**修改内容**:

将错误的 extent 解析代码：
```java
extent[0] = ((Number) extentMap.get("minY")).doubleValue();
extent[1] = ((Number) extentMap.get("minX")).doubleValue();
extent[2] = ((Number) extentMap.get("maxY")).doubleValue();
extent[3] = ((Number) extentMap.get("maxX")).doubleValue();
```

修改为正确的解析顺序：
```java
extent[0] = ((Number) extentMap.get("minX")).doubleValue();
extent[1] = ((Number) extentMap.get("minY")).doubleValue();
extent[2] = ((Number) extentMap.get("maxX")).doubleValue();
extent[3] = ((Number) extentMap.get("maxY")).doubleValue();
```

✅ 已实现 - 第 396-399 行已修复

- [x] ### 2. 验证修改

1. 检查代码变更是否正确
2. 确认 extent 数组顺序为 [minX, minY, maxX, maxY]
3. 确认与 CrsTransformUtil.transformExtentToWgs84() 的预期输入格式一致

- [x] ### 3. 测试

修改完成后，重新发布影像数据集，验证：
1. WMS 请求返回正确的影像
2. 地图能够跳转到正确的位置（西安）
3. 后端日志显示正确的 extent 转换结果
