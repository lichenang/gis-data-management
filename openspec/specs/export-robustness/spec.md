## 模块划分

此变更仅影响 `ExportController.java`，属于 Controller 层导出接口模块。

| 模块 | 说明 |
|------|------|
| Controller 层 | 处理 HTTP 请求，校验参数，分发到 Service 层方法 |

## 数据流设计

```
GET /api/v1/datasets/{id}/export?format=
  │
  ├── format 为空 → 默认 "geojson"
  │
  ├── dataset = getById(id)
  │     ├── null/deleted → 404 "数据集不存在"
  │     └── ok → 继续
  │
  ├── tableName == null/empty
  │     └── 400 "该数据集未导入空间数据，无法导出"
  │
  ├── try { service.exportXxx() }
  │     ├── BusinessException → writeErrorJson(response, e.getCode(), e.getMessage())
  │     └── Exception → writeErrorJson(response, 500, "导出失败: " + e.getMessage())
  │
  └── 成功 → 文件流输出
```

## 接口列表

| 接口 | 参数 | 修改前行为 | 修改后行为 |
|------|------|-----------|-----------|
| `GET /api/v1/datasets/{id}/export` | `format` 为空或缺失 | 400 "不支持的导出格式: " | 默认 GeoJSON |
| `GET /api/v1/datasets/{id}/export` | `format=geojson`，`table_name=null` | 500 "导出失败: ..." | 400 "该数据集未导入空间数据，无法导出" |

## ADDED Requirements

### Requirement: format 参数为空时默认使用 GeoJSON
当 `format` 请求参数为空字符串时，系统 SHALL 默认使用 `"geojson"` 进行导出。

#### Scenario: format 为空字符串
- **WHEN** 请求 `GET /api/v1/datasets/{id}/export?format=`
- **THEN** 系统视为 `format=geojson`，执行 GeoJSON 导出流程

### Requirement: Service 层异常保持原始状态码
Controller 的 catch 块 SHALL 区分 `BusinessException` 与普通异常，`BusinessException` 保持其原始 `code`（如 400）。

#### Scenario: Service 层抛出 BusinessException
- **WHEN** Service 层抛出 `BusinessException`（例如 `badRequest("该数据集未导入空间数据，无法导出")`）
- **THEN** Controller catch 块返回 `{"code": e.getCode(), "message": e.getMessage(), "data": null}`，而非统一 500

### Requirement: 导出前检查 table_name 是否为空
Controller 在调用 Service 导出方法前 MUST 检查 `dataset.getTableName()` 是否为空，若为空则直接返回 400。

#### Scenario: 数据集无关联表时控制器直接返回
- **WHEN** Controller 检查到 `dataset.getTableName()` 为 null 或空
- **THEN** 直接返回 `{"code": 400, "message": "该数据集未导入空间数据，无法导出", "data": null}`
