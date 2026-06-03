# 设计: fix-cors-origin-patterns

## 问题定位

```
前端环境:
  - Vite 开发服务器端口: 3000
  - 请求 Origin: http://localhost:3000

后端 CORS 配置 (错误):
  - allowedOrigins: ["http://localhost:5173", "http://127.0.0.1:5173"]
                              ↑
                         不匹配!

浏览器行为:
  1. 发送 OPTIONS 预检请求
  2. 后端检查 Origin: http://localhost:3000
  3. 不在允许列表 → 返回 403
  4. 浏览器阻止后续请求
```

## 解决方案

修改 `CorsConfigurationSource` 方法，使用通配符模式：

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    // 使用通配符模式，允许所有本地端口
    configuration.setAllowedOriginPatterns(Arrays.asList(
        "http://localhost:*",
        "http://127.0.0.1:*"
    ));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);
    configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition"));
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

## 修改说明

| 修改项 | 原值 | 新值 |
|--------|------|------|
| 配置方法 | `setAllowedOrigins()` | `setAllowedOriginPatterns()` |
| 允许的值 | 固定端口列表 | 通配符模式 |

使用 `allowedOriginPatterns` 的优势：
- 适配任何本地开发端口（3000、5173、8080 等）
- 避免每次改端口都要修改后端配置

## 验证步骤

1. 重启后端服务
2. 启动前端服务（端口 3000）
3. 访问登录页面，输入 admin/admin123
4. 检查 Network 面板：
   - OPTIONS 请求 → 200 OK
   - POST 请求 → 200 + Token
5. 确认登录成功跳转首页
