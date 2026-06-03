package com.gisplatform.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JWT 认证过滤器
 * <p>
 * 拦截每个请求，从请求头中提取 JWT Token 并验证，
 * 验证通过后将用户信息设置到 SecurityContext 中。
 * </p>
 *
 * @author GIS Platform Team
 * @version 1.0.0
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * JWT Token 服务
     */
    @Autowired
    private JwtTokenService jwtTokenService;

    /**
     * 请求头中的 Token 名称
     */
    private static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * Token 前缀
     */
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * 执行过滤逻辑
     *
     * @param request     HTTP 请求对象
     * @param response    HTTP 响应对象
     * @param filterChain 过滤器链
     * @throws ServletException Servlet 异常
     * @throws IOException      IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // 从请求头中获取 JWT Token
            String jwt = getJwtFromRequest(request);

            // 验证 Token 是否有效
            if (StringUtils.hasText(jwt) && jwtTokenService.validateToken(jwt)) {
                // 判断 Token 类型（只处理 Access Token）
                String tokenType = jwtTokenService.getTokenType(jwt);
                if (!"access".equals(tokenType)) {
                    filterChain.doFilter(request, response);
                    return;
                }

                // 从 Token 中获取用户名和用户ID
                String username = jwtTokenService.getUsernameFromToken(jwt);
                Long userId = jwtTokenService.getUserIdFromToken(jwt);

                // 创建认证对象
                if (StringUtils.hasText(username)) {
                    // 获取用户角色（实际应从数据库查询）
                    List<SimpleGrantedAuthority> authorities = getAuthorities(jwt);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(username, userId, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 设置到 SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    // 将 userId 存储到 UserContextHolder（ThreadLocal）
                    UserContextHolder.setUserId(userId);
                }
            }
        } catch (Exception ex) {
            // 记录异常日志
            logger.error("Could not set user authentication in security context", ex);
        } finally {
            // 清理 UserContextHolder，避免内存泄漏
            UserContextHolder.clear();
        }

        // 继续执行过滤器链
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中获取 JWT Token
     *
     * @param request HTTP 请求对象
     * @return JWT Token 字符串，不存在返回 null
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * 从 Token 中获取用户权限
     * <p>
     * 实际项目中应从数据库查询用户的角色和权限。
     * </p>
     *
     * @param token JWT Token 字符串
     * @return 权限列表
     */
    @SuppressWarnings("unchecked")
    private List<SimpleGrantedAuthority> getAuthorities(String token) {
        List<String> roles = (List<String>) jwtTokenService.parseToken(token).get("roles", List.class);
        if (roles == null || roles.isEmpty()) {
            return new ArrayList<>();
        }
        return roles.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

}
