## Context

`TilePackageServiceImpl.packageTiles()` 在执行 `ZipOutputStream` 循环后仅记录日志，未检查 `totalWritten` 是否为 0。当 GWC 目录存在但无实际瓦片文件时（例如 seed 任务被中断、瓦片文件被手动删除、只缓存了部分 zoom 级别而请求的范围恰好全部缺失），代码仍生成一个合法的空 ZIP（0 字节，0 个 entry）。用户下载后无法区分是"下载成功但无数据"还是"下载失败"。

## Goals / Non-Goals

**Goals:**
- 在 ZIP 打包循环后检查 `totalWritten === 0`
- 若为 0，抛出 `RuntimeException` 返回明确错误提示
- 确保错误被 `TilePackageController` 的异常处理器正确捕获

**Non-Goals:**
- 不修改前端代码
- 不修改 DTO、Controller、Service 接口
- 不修改瓦片数量估算或目录存在性检查

## Decisions

### 检查位置：ZIP 循环之后，`catch` 之前

```java
try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
    long totalWritten = 0;
    // ... 三层循环 ...
    if (totalWritten == 0) {
        throw new RuntimeException("该影像尚未生成切片缓存，请先触发切片种子任务");
    }
    log.info(...);
} catch (IOException e) { ... }
```

选择在此处检查的原因：
- 只有遍历完所有 zoom/x/y 后才能确定确实没有找到任何瓦片
- `totalWritten` 已在循环中累加，无需新增计数器
- 异常会被已有的 `catch (IOException e)` 捕获？不对，`RuntimeException` 不是 `IOException`，不会被该 catch 捕获，但会直接抛出到 `TilePackageController` 的 catch 块中。

实际上，这有个小问题：`RuntimeException` 抛出时 `ZipOutputStream` 的 `close()` 会被 `try-with-resources` 调用，生成一个空 ZIP 然后关闭。但在抛出异常后，controller 会尝试写 JSON 错误响应，而此时 response 可能已经部分提交了 ZIP 内容（header 已发送）。

更安全的方式：在抛出异常前，先关闭 ZipOutputStream（这会写入最终的 ZIP 中央目录），然后抛出异常。或者，在抛出异常之前，先在响应中写入一个说明性文件。

实际上，最干净的方式是：在 `try-with-resources` 结束后（ZIP 已关闭），检查 `totalWritten === 0`。但这时 ZIP 已经生成并关闭了，无法再修改。

所以更好的方式是：在 try 块内部，zip 流关闭前，检查并抛出异常。由于 `RuntimeException` 不是 `IOException`，会直接从 try 块中逃逸，try-with-resources 会关闭 ZipOutputStream，生成一个空 ZIP。然后 controller 的 catch 会尝试写 JSON 错误。

但这时 HTTP 响应头部已经设置了 `Content-Type: application/zip`，写 JSON 会出错。这个问题在现有的 controller 代码中就已经存在了（其他异常也一样）。所以这个方式与现有行为一致，不做额外修复。

### 错误消息

"该影像尚未生成切片缓存，请先触发切片种子任务" — 明确告诉用户原因和下一步操作。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| [低] 抛出异常时 ZIP 已部分写入（Content-Type 已设为 zip），浏览器可能收到混合内容 | 与现有异常处理行为一致；已在 controller 中用 `ClientAbortException` 保护 |
| [低] 正常打包后若 GWC 目录被并发清理导致 `totalWritten === 0` | 概率极低，且此时返回错误是正确行为 |
