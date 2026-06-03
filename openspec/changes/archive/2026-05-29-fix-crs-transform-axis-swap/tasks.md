# 实现任务：修复 CrsTransformUtil 坐标轴交换

## 任务列表

- [x] ### 1. 修改 CrsTransformUtil.transformExtentToWgs84()

**文件**: `backend/src/main/java/com/gisplatform/util/CrsTransformUtil.java`
**位置**: 第 52-55 行

**修改内容**:

将错误的坐标赋值：
```java
result[0] = minResult[0];  // lat (错误)
result[1] = minResult[1];  // lon (错误)
result[2] = maxResult[0];  // lat (错误)
result[3] = maxResult[1];  // lon (错误)
```

修改为正确的坐标赋值：
```java
result[0] = minResult[1];  // lon (正确)
result[1] = minResult[0];  // lat (正确)
result[2] = maxResult[1];  // lon (正确)
result[3] = maxResult[0];  // lat (正确)
```

- [x] ### 2. 验证修改

1. 检查代码变更是否正确交换了 minResult/maxResult 的索引
2. 确认输出顺序为 [lon, lat, lon, lat]
3. 确认与 transformExtentSimple() 的输出顺序一致

- [x] ### 3. 测试

修改完成后，重新发布影像数据集，验证：
1. WMS 请求返回正确的影像
2. 地图能够跳转到正确的位置（西安）
3. 后端日志显示正确的 extent 转换结果
