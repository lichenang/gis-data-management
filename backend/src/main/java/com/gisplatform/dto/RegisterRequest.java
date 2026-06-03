package com.gisplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求DTO
 * <p>
 * 用于接收用户注册请求的用户信息。
 * </p>
 *
 * @author GIS Platform Team
 * @version 1.0.0
 */
@Data
@Schema(description = "注册请求")
public class RegisterRequest {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 64, message = "用户名长度必须在3-64个字符之间")
    @Schema(description = "用户名", required = true, example = "newuser")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 128, message = "密码长度必须在6-128个字符之间")
    @Schema(description = "密码", required = true, example = "password123")
    private String password;

    /**
     * 昵称
     */
    @Schema(description = "昵称", example = "新用户")
    private String nickname;

    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱", example = "user@example.com")
    private String email;

    /**
     * 手机号
     */
    @Schema(description = "手机号", example = "13800138000")
    private String phone;

}
