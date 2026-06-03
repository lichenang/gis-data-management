package com.gisplatform.common.exception;

import lombok.Getter;

/**
 * 业务异常类
 * <p>
 * 用于处理业务逻辑中的异常情况，可以指定错误码和错误消息。
 * </p>
 *
 * @author GIS Platform Team
 * @version 1.0.0
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final int code;

    /**
     * 错误消息
     */
    private final String message;

    /**
     * 构造函数
     *
     * @param message 错误消息
     */
    public BusinessException(String message) {
        super(message);
        this.code = 500;
        this.message = message;
    }

    /**
     * 构造函数
     *
     * @param code    错误码
     * @param message 错误消息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * 构造函数（带原因）
     *
     * @param message 错误消息
     * @param cause   异常原因
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
        this.message = message;
    }

    /**
     * 常用错误码
     */
    public static final int NOT_FOUND = 404;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int BAD_REQUEST = 400;
    public static final int CONFLICT = 409;
    public static final int INTERNAL_ERROR = 500;

    /**
     * 创建资源不存在异常
     *
     * @param message 错误消息
     * @return BusinessException 实例
     */
    public static BusinessException notFound(String message) {
        return new BusinessException(NOT_FOUND, message);
    }

    /**
     * 创建未授权异常
     *
     * @param message 错误消息
     * @return BusinessException 实例
     */
    public static BusinessException unauthorized(String message) {
        return new BusinessException(UNAUTHORIZED, message);
    }

    /**
     * 创建禁止访问异常
     *
     * @param message 错误消息
     * @return BusinessException 实例
     */
    public static BusinessException forbidden(String message) {
        return new BusinessException(FORBIDDEN, message);
    }

    /**
     * 创建参数错误异常
     *
     * @param message 错误消息
     * @return BusinessException 实例
     */
    public static BusinessException badRequest(String message) {
        return new BusinessException(BAD_REQUEST, message);
    }

    /**
     * 创建冲突异常（用于并发操作）
     *
     * @param message 错误消息
     * @return BusinessException 实例
     */
    public static BusinessException conflict(String message) {
        return new BusinessException(CONFLICT, message);
    }

}
