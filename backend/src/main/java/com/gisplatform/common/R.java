package com.gisplatform.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回结果类
 * <p>
 * 所有 REST API 统一返回此格式：{ code, message, data }
 * </p>
 *
 * @author GIS Platform Team
 * @version 1.0.0
 * @param <T> 数据类型
 */
@Data
public class R<T> implements Serializable {

    /**
     * 序列化版本UID
     */
    private static final long serialVersionUID = 1L;

    /**
     * 响应码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 成功响应码
     */
    public static final int SUCCESS_CODE = 200;

    /**
     * 失败响应码
     */
    public static final int ERROR_CODE = 500;

    /**
     * 未授权响应码
     */
    public static final int UNAUTHORIZED_CODE = 401;

    /**
     * 禁止访问响应码
     */
    public static final int FORBIDDEN_CODE = 403;

    /**
     * 资源不存在响应码
     */
    public static final int NOT_FOUND_CODE = 404;

    /**
     * 私有构造函数
     */
    private R() {
    }

    /**
     * 创建成功响应（无数据）
     *
     * @param <T> 泛型类型
     * @return R 实例
     */
    public static <T> R<T> ok() {
        return restResult(null, SUCCESS_CODE, "操作成功");
    }

    /**
     * 创建成功响应（带数据）
     *
     * @param data 响应数据
     * @param <T> 泛型类型
     * @return R 实例
     */
    public static <T> R<T> ok(T data) {
        return restResult(data, SUCCESS_CODE, "操作成功");
    }

    /**
     * 创建成功响应（带自定义消息）
     *
     * @param data    响应数据
     * @param message 响应消息
     * @param <T>     泛型类型
     * @return R 实例
     */
    public static <T> R<T> ok(T data, String message) {
        return restResult(data, SUCCESS_CODE, message);
    }

    /**
     * 创建失败响应
     *
     * @param message 错误消息
     * @param <T>     泛型类型
     * @return R 实例
     */
    public static <T> R<T> fail(String message) {
        return restResult(null, ERROR_CODE, message);
    }

    /**
     * 创建失败响应（带自定义状态码）
     *
     * @param code    响应码
     * @param message 错误消息
     * @param <T>     泛型类型
     * @return R 实例
     */
    public static <T> R<T> fail(int code, String message) {
        return restResult(null, code, message);
    }

    /**
     * 创建未授权响应
     *
     * @param message 错误消息
     * @param <T>     泛型类型
     * @return R 实例
     */
    public static <T> R<T> unauthorized(String message) {
        return restResult(null, UNAUTHORIZED_CODE, message);
    }

    /**
     * 创建禁止访问响应
     *
     * @param message 错误消息
     * @param <T>     泛型类型
     * @return R 实例
     */
    public static <T> R<T> forbidden(String message) {
        return restResult(null, FORBIDDEN_CODE, message);
    }

    /**
     * 创建资源不存在响应
     *
     * @param message 错误消息
     * @param <T>     泛型类型
     * @return R 实例
     */
    public static <T> R<T> notFound(String message) {
        return restResult(null, NOT_FOUND_CODE, message);
    }

    /**
     * 内部方法：构建响应结果
     *
     * @param data    响应数据
     * @param code    响应码
     * @param message 响应消息
     * @param <T>     泛型类型
     * @return R 实例
     */
    private static <T> R<T> restResult(T data, int code, String message) {
        R<T> apiResult = new R<>();
        apiResult.setCode(code);
        apiResult.setData(data);
        apiResult.setMessage(message);
        return apiResult;
    }

    /**
     * 判断是否成功
     *
     * @return 成功返回 true
     */
    public boolean isSuccess() {
        return SUCCESS_CODE == this.code;
    }

}
