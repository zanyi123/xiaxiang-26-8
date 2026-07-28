package org.example.xiaxiang.common;

/**
 * 统一返回结果包装类
 * @param <T> 泛型，代表返回的具体数据类型
 */
public class Result<T> {

    private boolean success;
    private String message;
    private T data;

    private Result() {}

    /**
     * 快捷创建：成功结果
     */
    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.success = true;
        r.message = "操作成功";
        r.data = data;
        return r;
    }

    /**
     * 快捷创建：成功结果（自定义消息）
     */
    public static <T> Result<T> success(T data, String msg) {
        Result<T> r = new Result<>();
        r.success = true;
        r.message = msg;
        r.data = data;
        return r;
    }

    /**
     * 快捷创建：失败结果
     */
    public static <T> Result<T> fail(String msg) {
        Result<T> r = new Result<>();
        r.success = false;
        r.message = msg;
        r.data = null;
        return r;
    }

    // Getter / Setter

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
