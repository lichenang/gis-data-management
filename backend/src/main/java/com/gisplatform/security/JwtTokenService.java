package com.gisplatform.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Token 服务类
 * <p>
 * 提供 JWT Token 的生成、解析和验证功能。
 * </p>
 *
 * @author GIS Platform Team
 * @version 1.0.0
 */
@Component
public class JwtTokenService {

    /**
     * JWT 密钥
     */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * Access Token 过期时间（分钟）
     */
    @Value("${jwt.access-token-expire:15}")
    private long accessTokenExpire;

    /**
     * Refresh Token 过期时间（天）
     */
    @Value("${jwt.refresh-token-expire:7}")
    private long refreshTokenExpire;

    /**
     * 获取 SecretKey 对象
     *
     * @return SecretKey 对象
     */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 Access Token
     *
     * @param username 用户名
     * @param userId   用户ID
     * @param roles    角色列表
     * @return JWT Token 字符串
     */
    public String generateAccessToken(String username, Long userId, String[] roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("roles", roles);
        claims.put("type", "access");

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpire * 60 * 1000);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSecretKey())
                .compact();
    }

    /**
     * 生成 Refresh Token
     *
     * @param username 用户名
     * @param userId   用户ID
     * @return JWT Token 字符串
     */
    public String generateRefreshToken(String username, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "refresh");

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpire * 24 * 60 * 60 * 1000);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSecretKey())
                .compact();
    }

    /**
     * 解析 Token
     *
     * @param token JWT Token 字符串
     * @return Claims 对象，包含载荷信息
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 验证 Token 是否有效
     *
     * @param token JWT Token 字符串
     * @return 有效返回 true，无效返回 false
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 从 Token 中获取用户名
     *
     * @param token JWT Token 字符串
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 从 Token 中获取用户ID
     *
     * @param token JWT Token 字符串
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        return parseToken(token).get("userId", Long.class);
    }

    /**
     * 从 Token 中获取 Token 类型
     *
     * @param token JWT Token 字符串
     * @return Token 类型（access/refresh）
     */
    public String getTokenType(String token) {
        return parseToken(token).get("type", String.class);
    }

    /**
     * 判断 Token 是否过期
     *
     * @param token JWT Token 字符串
     * @return 过期返回 true，未过期返回 false
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = parseToken(token).getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

}
