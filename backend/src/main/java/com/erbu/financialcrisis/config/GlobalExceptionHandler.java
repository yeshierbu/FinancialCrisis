package com.erbu.financialcrisis.config;

import com.erbu.financialcrisis.common.BusinessException;
import com.erbu.financialcrisis.common.Result;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 * 第一版先做轻量封装，后续可以继续增加日志、traceId 和错误码字典。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException ex) {
        return Result.fail(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getAllErrors().isEmpty()
                ? "参数校验失败"
                : ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return Result.fail(4001, message);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {
        return Result.fail(5003, ex.getMessage());
    }
}
