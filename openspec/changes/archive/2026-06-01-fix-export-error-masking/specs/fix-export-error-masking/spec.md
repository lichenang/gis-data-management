## ADDED Requirements

### 模块划分

| 模块 | 位置 | 职责 |
|---|---|---|
| ExportController | `.../controller/ExportController.java` | 替换 sendError 为直接 JSON 写入 |
| SecurityConfig | `.../config/SecurityConfig.java` | 添加 `/error` 到 permitAll |

### 数据流设计

```
原始（问题）:
  Controller catch
    → response.sendError(500, msg)
    → Servlet forward to /error
    → Security 拦截（无认证）→ 403 ❌

修复后:
  Controller catch
    → response.setStatus(500)
    → response.getWriter().write({ code, message, data })
    → 客户端收到 500 + JSON body ✅
```

### Requirement: 导出错误不触发 Servlet 错误转发

当导出接口发生业务异常时，Controller SHALL 直接向响应体写入 JSON 格式的错误信息，MUST NOT 调用 `HttpServletResponse.sendError()`。

#### Scenario: 导出因数据集无关联表失败

- **WHEN** 导出数据集 ID 存在但 `table_name` 为 null
- **THEN** 服务层抛出 RuntimeException
- **AND** Controller catch 后设置 status=500
- **AND** 响应体为 `{"code":500,"message":"导出失败: 数据集无关联表","data":null}`
- **AND** 客户端收到 500（而非 403）

#### Scenario: 不支持的导出格式

- **WHEN** 请求参数 `format` 不在支持列表中（geojson/shapefile/kml/geotiff）
- **THEN** Controller 设置 status=400
- **AND** 响应体为 `{"code":400,"message":"不支持的导出格式: ...","data":null}`

#### Scenario: 数据集不存在

- **WHEN** 导出数据集 ID 不存在或被删除
- **THEN** Controller 设置 status=404
- **AND** 响应体为 `{"code":404,"message":"数据集不存在","data":null}`

### Requirement: /error 路径可匿名访问

Spring Security SHALL 放行 `/error` 路径，作为未捕获 `sendError` 的兜底处理。

#### Scenario: /error 不触发 403

- **WHEN** 用户未登录
- **AND** 访问 `GET /error`
- **THEN** 返回 404 或 Spring Boot 默认错误页面（而非 403）
