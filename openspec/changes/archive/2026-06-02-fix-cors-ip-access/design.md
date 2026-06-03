## Context

当前后端 CORS 配置仅允许 `localhost:3000` 访问。开发者在使用内网 IP `192.168.31.123:3000` 访问前端时，浏览器发送的登录请求被 CORS 策略拦截，登录失败。

当前可能的 CORS 配置位置：
1. `SecurityConfig.java` 中的 CORS 配置
2. 独立的 `CorsConfig.java`
3. `application.yml` 中的 Spring CORS 配置

## Goals / Non-Goals

**Goals:**
- 添加 `http://192.168.31.123:3000` 到允许的跨域源列表
- 支持通过环境变量配置 CORS 允许的来源
- 保持生产环境的安全性

**Non-Goals:**
- 生产环境仍然只允许已知的域名/ IP
- 不添加认证相关的功能变更

## Decisions

### Decision 1: 修改方式

**方案**: 在现有的 CorsConfig 或 SecurityConfig 中添加新的允许源。

**理由**:
- 最小化代码改动
- 保持现有配置结构

**备选**: 创建新的 CORS 配置类 — 但如果已存在配置，可能导致冲突。

### Decision 2: 配置方式

**方案**: 使用配置文件（application.yml）定义允许的 CORS 来源，通过 `@Value` 或 `@ConfigurationProperties` 注入。

**理由**:
- 便于不同环境切换
- 不需要重新编译即可修改配置
- 遵循配置管理原则

### Decision 3: 开发模式

**方案**: 添加 Spring Profile (`dev`) 下的特殊配置，开发模式下允许所有源或更宽松的配置。

**理由**:
- 方便开发调试
- 生产环境仍然安全

## Risks / Trade-offs

- **[低] 安全性** — 允许过多来源可能带来安全风险。Mitigation: 生产环境使用严格的来源限制，只在开发和测试环境放宽。
- **[低] 配置冲突** — 多个地方配置 CORS 可能导致冲突。Mitigation: 统一在一个地方配置。

## Migration Plan

1. 定位现有的 CORS 配置文件
2. 修改配置添加内网 IP
3. 测试跨域登录是否成功
4. 验证生产环境配置不变

无需数据库迁移。

## Open Questions

- CORS 配置目前在哪里？（需要确认后修改）
