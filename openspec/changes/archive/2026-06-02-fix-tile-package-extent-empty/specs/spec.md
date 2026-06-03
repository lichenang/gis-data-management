本次变更为缺陷修复，不涉及规范层面的变更。

## 模块划分

- `backend/.../service/impl/TilePackageServiceImpl.java` — extent 获取逻辑增强、瓦片枚举 fallback

## 数据流设计

不变。仍为 Controller → Service(packageTiles) → 异常时 GlobalExceptionHandler 拦截。

## 接口列表

不变。仍为 `GET /api/v1/tiles/package/{datasetId}` 下载 ZIP。

## ADDED Requirements

（本次变更不引入新能力）
