package com.gisplatform.controller;

import com.gisplatform.common.R;
import com.gisplatform.dto.AuthResponse;
import com.gisplatform.dto.LoginRequest;
import com.gisplatform.dto.RegisterRequest;
import com.gisplatform.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "认证管理", description = "用户登录、注册、Token刷新等接口")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "使用用户名密码登录，返回JWT令牌")
    public R<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        Object result = authService.login(request);
        return R.ok(convertToAuthResponse(result));
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册新用户，默认分配普通用户角色")
    public R<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        Object result = authService.register(request);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) result;
        return R.ok(map);
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新Token", description = "使用刷新令牌获取新的访问令牌")
    public R<AuthResponse> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        Object result = authService.refreshToken(refreshToken);
        return R.ok(convertToAuthResponse(result));
    }

    @GetMapping("/userinfo")
    @Operation(summary = "获取当前用户信息", description = "获取已登录用户的详细信息")
    public R<Map<String, Object>> getUserInfo() {
        Object result = authService.getUserInfo();
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) result;
        return R.ok(map);
    }

    private AuthResponse convertToAuthResponse(Object result) {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) result;
        AuthResponse response = new AuthResponse();
        response.setAccessToken((String) map.get("accessToken"));
        response.setRefreshToken((String) map.get("refreshToken"));
        response.setExpiresIn(map.get("expiresIn") != null ? ((Number) map.get("expiresIn")).longValue() : null);
        if (map.get("userId") != null) {
            response.setUserId(((Number) map.get("userId")).longValue());
        }
        if (map.get("username") != null) {
            response.setUsername((String) map.get("username"));
        }
        return response;
    }

}
