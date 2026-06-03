package com.gisplatform.common.exception;

import com.gisplatform.common.R;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>
 * 统一捕获和处理系统中的各类异常，返回统一格式的响应。
 * 所有异常均返回 { code, message, data } 格式。
 * </p>
 *
 * @author GIS Platform Team
 * @version 1.0.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 日志记录器
     */
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务异常
     *
     * @param e   业务异常
     * @param request HTTP 请求对象
     * @return 统一响应结果
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public R<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        logger.warn("业务异常: {} - {}", request.getRequestURI(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常
     *
     * @param e   参数校验异常
     * @param request HTTP 请求对象
     * @return 统一响应结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        logger.warn("参数校验失败: {} - {}", request.getRequestURI(), message);
        return R.fail(400, message);
    }

    /**
     * 处理绑定异常
     *
     * @param e   绑定异常
     * @param request HTTP 请求对象
     * @return 统一响应结果
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleBindException(BindException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        logger.warn("参数绑定失败: {} - {}", request.getRequestURI(), message);
        return R.fail(400, message);
    }

    /**
     * 处理认证异常
     *
     * @param e   认证异常
     * @param request HTTP 请求对象
     * @return 统一响应结果
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public R<Void> handleAuthenticationException(AuthenticationException e, HttpServletRequest request) {
        logger.warn("认证失败: {} - {}", request.getRequestURI(), e.getMessage());
        return R.unauthorized("认证失败: " + e.getMessage());
    }

    /**
     * 处理凭证错误异常
     *
     * @param e   凭证错误异常
     * @param request HTTP 请求对象
     * @return 统一响应结果
     */
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public R<Void> handleBadCredentialsException(BadCredentialsException e, HttpServletRequest request) {
        logger.warn("凭证错误: {} - {}", request.getRequestURI(), e.getMessage());
        return R.unauthorized("用户名或密码错误");
    }

    /**
     * 处理访问被拒绝异常
     *
     * @param e   访问被拒绝异常
     * @param request HTTP 请求对象
     * @return 统一响应结果
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<Void> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        logger.warn("访问被拒绝: {} - {}", request.getRequestURI(), e.getMessage());
        return R.forbidden("没有访问权限");
    }

    /**
     * 处理 JWT 过期异常
     *
     * @param e   JWT 过期异常
     * @param request HTTP 请求对象
     * @return 统一响应结果
     */
    @ExceptionHandler(ExpiredJwtException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public R<Void> handleExpiredJwtException(ExpiredJwtException e, HttpServletRequest request) {
        logger.warn("Token 已过期: {}", request.getRequestURI());
        return R.unauthorized("登录已过期，请重新登录");
    }

    /**
     * 处理 JWT 异常
     *
     * @param e   JWT 异常
     * @param request HTTP 请求对象
     * @return 统一响应结果
     */
    @ExceptionHandler(JwtException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public R<Void> handleJwtException(JwtException e, HttpServletRequest request) {
        logger.warn("Token 无效: {} - {}", request.getRequestURI(), e.getMessage());
        return R.unauthorized("无效的 Token");
    }

    /**
     * 处理请求方法不支持异常
     *
     * @param e   请求方法不支持异常
     * @param request HTTP 请求对象
     * @return 统一响应结果
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public R<Void> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        logger.warn("请求方法不支持: {} - {}", request.getRequestURI(), e.getMessage());
        return R.fail(405, "请求方法不支持");
    }

    /**
     * 处理 404 异常
     *
     * @param e   404 异常
     * @param request HTTP 请求对象
     * @return 统一响应结果
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R<Void> handleNoHandlerFoundException(NoHandlerFoundException e, HttpServletRequest request) {
        logger.warn("资源不存在: {}", request.getRequestURI());
        return R.notFound("请求的资源不存在");
    }

    /**
     * 处理运行时异常
     *
     * @param e   运行时异常
     * @param request HTTP 请求对象
     * @return 统一响应结果
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleRuntimeException(RuntimeException e, HttpServletRequest request, HttpServletResponse response) {
        logger.error("系统异常: {} - {}", request.getRequestURI(), e.getMessage(), e);
        if (response.isCommitted()) {
            logger.warn("Response already committed, skipping error response: {} - {}", request.getRequestURI(), e.getMessage());
            return null;
        }
        try {
            response.reset();
            response.setContentType("application/json;charset=UTF-8");
        } catch (Exception ex) {
            logger.warn("Failed to reset response: {}", ex.getMessage());
        }
        return R.fail("系统繁忙，请稍后再试");
    }

    /**
     * 处理所有未捕获的异常
     *
     * @param e   异常对象
     * @param request HTTP 请求对象
     * @return 统一响应结果
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleException(Exception e, HttpServletRequest request, HttpServletResponse response) {
        logger.error("未处理的异常: {} - {}", request.getRequestURI(), e.getMessage(), e);
        if (response.isCommitted()) {
            logger.warn("Response already committed, skipping error response: {} - {}", request.getRequestURI(), e.getMessage());
            return null;
        }
        try {
            response.reset();
            response.setContentType("application/json;charset=UTF-8");
        } catch (Exception ex) {
            logger.warn("Failed to reset response: {}", ex.getMessage());
        }
        return R.fail("系统繁忙，请稍后再试");
    }

}
