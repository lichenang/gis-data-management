package com.gisplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类
 * <p>
 * 对应数据库表 sys_user，存储系统用户信息。
 * </p>
 *
 * @author GIS Platform Team
 * @version 1.0.0
 */
@Data
@TableName("sys_user")
@Schema(description = "用户信息")
public class User {

    /**
     * 用户ID
     */
    @TableId(type = IdType.AUTO)
    @Schema(description = "用户ID")
    private Long id;

    /**
     * 用户名（登录账号），唯一
     */
    @Schema(description = "用户名")
    private String username;

    /**
     * 密码（加密存储）
     */
    private String password;

    /**
     * 昵称
     */
    @Schema(description = "昵称")
    private String nickname;

    /**
     * 邮箱
     */
    @Schema(description = "邮箱")
    private String email;

    /**
     * 手机号
     */
    @Schema(description = "手机号")
    private String phone;

    /**
     * 头像URL
     */
    @Schema(description = "头像URL")
    private String avatar;

    /**
     * 状态：1-启用，0-禁用
     */
    @Schema(description = "状态：1-启用，0-禁用")
    private Integer status;

    /**
     * 租户ID（多租户隔离）
     */
    @Schema(description = "租户ID")
    private String tenantId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标志：0-未删除，1-已删除
     */
    @TableLogic
    @Schema(description = "逻辑删除标志")
    private Integer deleted;

}
