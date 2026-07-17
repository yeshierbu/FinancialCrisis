package com.erbu.financialcrisis.common;

/**
 * 业务异常。
 * 用来表达“系统能预期到，但当前流程不允许继续”的错误，例如申请单不存在、状态不合法等。
 */
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
