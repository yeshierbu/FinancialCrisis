package com.erbu.financialcrisis.common;

/**
 * 全局统一返回体。
 *
 * <p>这里不用 Lombok，是因为该类已经手写了静态工厂和构造器。保留显式代码可以避免
 * Lombok 构造器和手写构造器重复生成，也让统一响应结构在任何 JDK 下都更稳定。</p>
 */
public class Result<T> {

    private Integer code;
    private String message;
    private T data;

    public Result() {
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(0, "success", data);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
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
