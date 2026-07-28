package org.example.xiaxiang.exception;

/**
 * 业务异常
 * 用于封装可预期的业务错误（如参数校验失败、资源不存在等）
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
