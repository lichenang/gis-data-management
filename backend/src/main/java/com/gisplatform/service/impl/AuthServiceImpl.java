package com.gisplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gisplatform.dto.LoginRequest;
import com.gisplatform.dto.RegisterRequest;
import com.gisplatform.entity.User;
import com.gisplatform.mapper.RoleMapper;
import com.gisplatform.mapper.UserMapper;
import com.gisplatform.security.CustomUserDetailsService;
import com.gisplatform.security.JwtTokenService;
import com.gisplatform.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证服务实现类
 * <p>
 * 实现AuthService接口，封装用户登录、注册、Token刷新等逻辑。
 * </p>
 *
 * @author GIS Platform Team
 * @version 1.0.0
 */
@Service
public class AuthServiceImpl implements AuthService {

    /**
     * 认证管理器
     */
    @Autowired
    private AuthenticationManager authenticationManager;

    /**
     * JWT Token 服务
     */
    @Autowired
    private JwtTokenService jwtTokenService;

    /**
     * 用户详情服务
     */
    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    /**
     * 用户 Mapper
     */
    @Autowired
    private UserMapper userMapper;

    /**
     * 角色 Mapper
     */
    @Autowired
    private RoleMapper roleMapper;

    /**
     * 密码编码器
     */
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 用户登录
     * <p>
     * 验证用户名密码，生成JWT Token并返回。
     * </p>
     *
     * @param loginRequest 登录请求
     * @return 登录结果（包含Token信息）
     */
    @Override
    public Object login(LoginRequest loginRequest) {
        // 使用AuthenticationManager进行认证
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        // 获取认证后的用户信息
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, loginRequest.getUsername())
                        .eq(User::getDeleted, 0)
        );

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 检查用户状态
        if (user.getStatus() == 0) {
            throw new RuntimeException("用户已被禁用");
        }

        // 查询用户角色
        List<String> roleCodes = roleMapper.selectRoleCodesByUserId(user.getId());
        if (roleCodes == null || roleCodes.isEmpty()) {
            roleCodes = List.of("USER");
        }
        String[] roles = roleCodes.toArray(new String[0]);

        // 生成Token
        String accessToken = jwtTokenService.generateAccessToken(
                user.getUsername(),
                user.getId(),
                roles
        );
        String refreshToken = jwtTokenService.generateRefreshToken(
                user.getUsername(),
                user.getId()
        );

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshToken);
        result.put("expiresIn", 900);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());

        return result;
    }

    /**
     * 用户注册
     * <p>
     * 创建新用户，默认角色为普通用户。
     * </p>
     *
     * @param registerRequest 注册请求
     * @return 注册结果
     */
    @Override
    public Object register(RegisterRequest registerRequest) {
        // 检查用户名是否已存在
        User existUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, registerRequest.getUsername())
        );
        if (existUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 创建新用户
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setNickname(registerRequest.getNickname());
        user.setEmail(registerRequest.getEmail());
        user.setPhone(registerRequest.getPhone());
        user.setStatus(1);
        user.setTenantId("default");

        userMapper.insert(user);

        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("username", user.getUsername());

        return result;
    }

    /**
     * 刷新Token
     * <p>
     * 使用refreshToken生成新的accessToken。
     * </p>
     *
     * @param refreshToken 刷新Token
     * @return 新的登录结果
     */
    @Override
    public Object refreshToken(String refreshToken) {
        // 验证refreshToken
        if (!jwtTokenService.validateToken(refreshToken)) {
            throw new RuntimeException("refreshToken无效");
        }

        // 检查Token类型
        String tokenType = jwtTokenService.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new RuntimeException("无效的Token类型");
        }

        // 获取用户信息
        String username = jwtTokenService.getUsernameFromToken(refreshToken);
        Long userId = jwtTokenService.getUserIdFromToken(refreshToken);

        // 查询用户角色
        List<String> userRoles = roleMapper.selectRoleCodesByUserId(userId);
        if (userRoles == null || userRoles.isEmpty()) {
            userRoles = List.of("USER");
        }
        String[] roles = userRoles.toArray(new String[0]);

        // 生成新的Token
        String newAccessToken = jwtTokenService.generateAccessToken(username, userId, roles);
        String newRefreshToken = jwtTokenService.generateRefreshToken(username, userId);

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", newAccessToken);
        result.put("refreshToken", newRefreshToken);
        result.put("expiresIn", 900);

        return result;
    }

    /**
     * 获取当前用户信息
     * <p>
     * 从SecurityContext中获取当前登录用户的信息。
     * </p>
     *
     * @return 用户信息
     */
    @Override
    public Object getUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = (String) authentication.getPrincipal();

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
                        .eq(User::getDeleted, 0)
        );

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 查询用户角色
        List<String> userRoles = roleMapper.selectRoleCodesByUserId(user.getId());
        if (userRoles == null || userRoles.isEmpty()) {
            userRoles = List.of("USER");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        result.put("email", user.getEmail());
        result.put("phone", user.getPhone());
        result.put("avatar", user.getAvatar());
        result.put("roles", userRoles);

        return result;
    }

}
