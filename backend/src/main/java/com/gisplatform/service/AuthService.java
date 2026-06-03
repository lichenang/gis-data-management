package com.gisplatform.service;

import com.gisplatform.dto.LoginRequest;
import com.gisplatform.dto.RegisterRequest;

/**
 * 认证服务接口
 * <p>
 * 定义用户认证相关的业务方法，包括登录、注册、Token刷新等。
 * </p>
 *
 * @author GIS Platform Team
 * @version 1.0.0
 */
public interface AuthService {

    /**
     * 用户登录
     * <p>
     * 根据用户名和密码进行验证，验证成功后生成JWT Token。
     * </p>
     *
     * @param loginRequest 登录请求（包含用户名和密码）
     * @return 登录结果（包含accessToken、refreshToken和过期时间）
     */
    Object login(LoginRequest loginRequest);

    /**
     * 用户注册
     * <p>
     * 根据注册信息创建新用户，默认分配普通用户角色。
     * </p>
     *
     * @param registerRequest 注册请求（包含用户名、密码、昵称、邮箱等）
     * @return 注册结果
     */
    Object register(RegisterRequest registerRequest);

    /**
     * 刷新Token
     * <p>
     * 使用refreshToken获取新的accessToken。
     * </p>
     *
     * @param refreshToken 刷新Token
     * @return 新的登录结果
     */
    Object refreshToken(String refreshToken);

    /**
     * 获取当前用户信息
     * <p>
     * 从SecurityContext中获取当前登录用户的信息。
     * </p>
     *
     * @return 当前用户信息
     */
    Object getUserInfo();

}
