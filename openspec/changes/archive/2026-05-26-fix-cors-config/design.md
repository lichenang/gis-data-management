# 设计: fix-cors-config

## 问题分析

当前 CORS 配置使用了 `setAllowedOriginPatterns(List.of("*"))`，这种方式在使用 `allowCredentials(true)` 时可能会被浏览器拒绝。

浏览器 CORS 规范要求：
- 当 `Access-Control-Allow-Credentials` 为 `true` 时
- `Access-Control-Allow-Origin` 不能使用通配符 `*`
- 必须明确指定允许的来源

## 解决方案

### 修改 SecurityConfig.java

修改 `corsConfigurationSource()` 方法：

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    // 明确允许的前端来源
    configuration.setAllowedOrigins(Arrays.asList(
        "http://localhost:5173",
        "http://127.0.0.1:5173"
    ));
    // 允许的方法
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    // 允许的头
    configuration.setAllowedHeaders(List.of("*"));
    // 允许凭证
    configuration.setAllowCredentials(true);
    // 暴露的头
    configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition"));
    // 预检请求缓存时间（秒）
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

### 主要修改点

| 修改项 | 原值 | 新值 |
|--------|------|------|
| allowedOrigins | `setAllowedOriginPatterns(List.of("*"))` | `setAllowedOrigins(Arrays.asList("http://localhost:5173", "http://127.0.0.1:5173"))` |
| maxAge | 无 | `3600L` (1小时) |

## 验证步骤

1. 重启后端服务
2. 使用前端发起 API 调用
3. 检查浏览器开发者工具 Network 面板：
   - 确认 Response Headers 包含 `Access-Control-Allow-Origin: http://localhost:5173`
   - 确认 OPTIONS 预检请求返回 200
4. 确认 API 调用成功，不再返回 403
