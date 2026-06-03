本次变更为缺陷修复，不涉及新能力或能力行为变更，故无新增/修改/删除的规范需求。

## 模块划分

- `backend/.../service/impl/TilePackageServiceImpl.java` — GWC 目录名构造中 workspace fallback
- `backend/.../common/GlobalExceptionHandler.java` — response 已提交保护

## 数据流设计

不变。仍为 Controller → Service(packageTiles) → 异常时 GlobalExceptionHandler 拦截。

## 接口列表

不变。仍为 `GET /api/v1/tiles/package/{datasetId}` 下载 ZIP。
