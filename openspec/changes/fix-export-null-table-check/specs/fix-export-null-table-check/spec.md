## 模块划分

此变更影响 `DatasetServiceImpl` 类中的导出相关方法，属于数据集导出模块（`com.gisplatform.service.impl.DatasetServiceImpl`）。不涉及 Controller、前端、或基础设施层。

| 模块 | 说明 |
|------|------|
| Service 层 | 接收 controller 请求，检查数据集合法性，执行导出逻辑 |
| 全局异常处理 | `GlobalExceptionHandler` 已处理 `BusinessException` 无需修改 |

## 数据流设计

```
ExportController → DatasetServiceImpl.exportXxx(id, outputStream)
                    ├── Dataset dataset = this.getById(id)
                    ├── if (dataset == null) → BusinessException.notFound("数据集不存在") → GlobalExceptionHandler → JSON
                    ├── if (tableName == null) → BusinessException.badRequest("该数据集未导入空间数据，无法导出") → GlobalExceptionHandler → JSON
                    └── else → 正常导出流程
```

## 接口列表

| 接口方法 | 修改前错误处理 | 修改后错误处理 |
|----------|---------------|---------------|
| `getDatasetAsGeoJSON(Long id)` | `throw new RuntimeException("数据集不存在")`, `throw new RuntimeException("数据集无关联表")` | `throw BusinessException.notFound("数据集不存在")`, `throw BusinessException.badRequest("该数据集未导入空间数据，无法导出")` |
| `getDatasetAsKML(Long id, String formatName)` | 同上 | 同上 |
| `exportShapefileAsZip(Long id, OutputStream outputStream)` | 同上 | 同上 |

## ADDED Requirements

### Requirement: 导出时数据集无关联表返回清晰业务错误
当数据集(dataset)的`table_name`字段为null或空时，系统SHALL抛出`BusinessException`并附带清晰的错误提示。

#### Scenario: 导出GeoJSON时数据集无关联表
- **WHEN** 调用`getDatasetAsGeoJSON`且dataset.getTableName()为null或空
- **THEN** 系统抛出`BusinessException.badRequest("该数据集未导入空间数据，无法导出")`，全局异常处理器返回`{ "code": 400, "message": "该数据集未导入空间数据，无法导出", "data": null }`

#### Scenario: 导出KML时数据集无关联表
- **WHEN** 调用`getDatasetAsKML`且dataset.getTableName()为null或空
- **THEN** 系统抛出`BusinessException.badRequest("该数据集未导入空间数据，无法导出")`

#### Scenario: 导出Shapefile时数据集无关联表
- **WHEN** 调用`exportShapefileAsZip`且dataset.getTableName()为null或空
- **THEN** 系统抛出`BusinessException.badRequest("该数据集未导入空间数据，无法导出")`

#### Scenario: 导出时数据集不存在
- **WHEN** 调用任一导出方法且`this.getById(id)`返回null或deleted=1
- **THEN** 系统抛出`BusinessException.notFound("数据集不存在")`，全局异常处理器返回`{ "code": 404, "message": "数据集不存在", "data": null }`
