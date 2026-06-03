## Why

根据 `openspec/specs/debug-tile-seed-nan.md` 的诊断结果：

dataset.extent 字段存储的已经是 EPSG:3857 米制坐标（如 minY=4050247, maxY=4051356），但 `getImageExtentInWebMercator()` 方法错误地将其当作 EPSG:4326 度数，再次调用 `convertToWebMercator()` 做坐标转换。

Y 坐标 4050247 被当作度数处理时：`tan(90 + 4050247)°` → tan 值溢出 → Infinity → log(Infinity) → NaN

## What Changes

1. **修改 getImageExtentInWebMercator() 方法**
   - 添加坐标系检测逻辑
   - 检测规则：如果 |minY| > 10000 或 |maxY| > 10000，说明是 EPSG:3857 米制坐标
   - 若是米制坐标：直接返回 extent，不再调用 convertToWebMercator()
   - 若是度数坐标：仍然调用 convertToWebMercator() 进行转换

## Impact

- `backend/.../service/tiling/TileSeedService.java` — 修改 getImageExtentInWebMercator 方法

## Non-goals

- 不修改 dataset 表结构
- 不修改其他服务

## Affected Files

- `backend/src/main/java/com/gisplatform/service/tiling/TileSeedService.java`
