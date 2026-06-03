## Why

当 GWC 瓦片缓存目录为空（seed 任务失败或瓦片被删除）时，`POST /api/v1/images/{id}/tile-package` 仍返回 HTTP 200 + 空的 0 字节 ZIP 文件。用户下载后得到一个无任何瓦片的压缩包，无法判断是下载成功还是缓存缺失，造成困惑。

## What Changes

- 在 `TilePackageServiceImpl` 打包完成后检查实际写入的瓦片数 `totalWritten === 0`
- 若为 0，抛出 `RuntimeException`，返回明确错误提示"该影像尚未生成切片缓存，请先触发切片种子任务"

## Capabilities

### New Capabilities
- （无）

### Modified Capabilities
- （无）

## Non-goals

- 不修改前端的任何代码
- 不修改 DTO、Controller 或 Service 接口
- 不修改瓦片数量估算逻辑或任何校验流程

## Impact

| 文件 | 操作 | 说明 |
|------|------|------|
| `backend/.../service/impl/TilePackageServiceImpl.java` | 修改 | 在 ZIP 循环后增加 `totalWritten === 0` 检查 |
