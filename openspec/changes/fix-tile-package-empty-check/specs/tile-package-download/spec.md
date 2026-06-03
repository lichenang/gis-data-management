## ADDED Requirements

### Requirement: 空缓存空结果检测
系统在打包切片完成后 SHALL 检查实际写入 ZIP 的瓦片数量，若为 0 则返回明确错误提示而非空 ZIP。

#### Scenario: GWC 目录无瓦片时返回错误
- **WHEN** 打包循环结束后 `totalWritten === 0`
- **THEN** 后端抛出错误，消息为"该影像尚未生成切片缓存，请先触发切片种子任务"

#### Scenario: 存在瓦片时正常返回 ZIP
- **WHEN** 打包循环结束后 `totalWritten > 0`
- **THEN** 后端返回正常 ZIP 文件，行为不变
