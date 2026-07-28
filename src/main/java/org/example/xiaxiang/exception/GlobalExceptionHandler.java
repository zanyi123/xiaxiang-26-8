package org.example.xiaxiang.exception;

import lombok.extern.slf4j.Slf4j;
import org.example.xiaxiang.common.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

/**
 * 全局异常处理器
 *
 * 设计原则：
 * 1. 所有异常统一拦截，绝不将底层 SQL、COS SDK 原始报错抛给前端
 * 2. 业务异常返回友好提示；未知异常返回"系统繁忙"，并打印堆栈便于排查
 * 3. 记录完整请求路径和异常信息
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常（可预期的错误）
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("[业务异常] URI={} | msg={}", request.getRequestURI(), e.getMessage());
        return Result.fail(e.getMessage());
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("[参数异常] URI={} | msg={}", request.getRequestURI(), e.getMessage());
        return Result.fail("请求参数错误：" + e.getMessage());
    }

    /**
     * 兜底：处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("[系统异常] URI={} | error={}", request.getRequestURI(), e.getMessage(), e);
        return Result.fail("系统繁忙，请稍后重试");
    }
}
