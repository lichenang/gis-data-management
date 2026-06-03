本次变更为缺陷修复，不涉及规范层面的变更。

## 模块划分

- `backend/.../service/impl/TilePackageServiceImpl.java` — enumerateTileFiles 和 getTileRange 方法修改

## 数据流设计

不变。仍为 Controller → Service(packageTiles) → ZIP 打包 → Response。

## 接口列表

不变。仍为 `GET /api/v1/tiles/package/{datasetId}` 下载 ZIP。

## ADDED Requirements

（本次变更不引入新能力）
