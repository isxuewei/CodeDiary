package fun.xuewei.codediary.component;


import fun.xuewei.codediary.dto.response.RespError;
import fun.xuewei.codediary.dto.response.RespResult;
import fun.xuewei.codediary.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;

/**
 * 异常处理器
 *
 * @author 薛伟
 */
@Slf4j
@RestControllerAdvice
public class RestExceptionHandler {


    /**
     * 接口限流异常处理
     */
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    @ExceptionHandler(value = ServerBusyException.class)
    public RespResult<?> handleBusinessException(ServerBusyException e, HttpServletRequest request) {
        log.error("服务器繁忙，错误信息如下", e);
        RespError error = new RespError();
        error.setCode(e.getCode());
        error.setMessage(e.getMessage());
        error.setUrl(request.getRequestURL().toString());
        error.setException(e.getClass().getName());
        return error;
    }

    /**
     * 权限异常处理
     */
    @ExceptionHandler(value = AuthorityException.class)
    public RespResult<?> handleAuthorityException(AuthorityException e, HttpServletRequest request) {
        log.error("操作权限不足，错误信息如下", e);
        RespError error = new RespError();
        error.setCode(e.getCode());
        error.setMessage(e.getMessage());
        error.setUrl(request.getRequestURL().toString());
        error.setException(e.getClass().getName());
        return error;
    }

    /**
     * 业务异常
     */
    @ExceptionHandler(value = {BusinessException.class})
    public RespResult<?> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.error("业务异常，错误信息如下", e);
        RespError error = new RespError();
        error.setCode(e.getCode());
        error.setMessage(e.getMessage());
        error.setUrl(request.getRequestURL().toString());
        error.setException(e.getClass().getName());
        return error;
    }

    /**
     * 参数校验异常
     */
    @ExceptionHandler(value = {ParamValidException.class})
    public RespResult<?> handleParamValidException(ParamValidException e, HttpServletRequest request) {
        log.error("参数校验异常，错误信息如下", e);
        RespError error = new RespError();
        error.setCode(e.getCode());
        HashMap<String, Object> errorMap = new HashMap<>();
        errorMap.put(e.getParam(), e.getMessage());
        error.setMessage(e.getMessage());
        error.setData(errorMap);
        error.setUrl(request.getRequestURL().toString());
        error.setException(e.getClass().getName());
        return error;
    }

    /**
     * JSR303 校验异常
     */
    @ExceptionHandler(value = {BindException.class})
    public RespResult<?> handleBindException(BindException e, HttpServletRequest request) {
        log.error("参数校验异常，错误信息如下", e);
        RespError error = new RespError();
        ErrorCode errorCode = ErrorCode.PARAM_VALID;
        error.setCode(errorCode.getCode());
        // 绑定错误返回很多错误，是一个错误列表，只需要第一个错误
        BindingResult bindingResult = e.getBindingResult();
        HashMap<String, Object> errorMap = new HashMap<>();
        // 获取校验的错误结果
        StringBuilder errorMessage = new StringBuilder();
        bindingResult.getFieldErrors().forEach(item -> {
            // 获取错误消息
            String message = item.getDefaultMessage();
            // 获取错误的字段名
            String field = item.getField();
            errorMap.put(field, message);
            errorMessage.append(message).append(" ");
        });
        error.setMessage(errorMessage.toString().trim());
        error.setData(errorMap);
        error.setUrl(request.getRequestURL().toString());
        error.setException(e.getClass().getName());
        return error;
    }

    /**
     * 兜底异常
     */
    @ExceptionHandler(value = {Exception.class, RuntimeException.class})
    public RespResult<?> handleOtherException(Exception e, HttpServletRequest request) {
        log.error("其他异常，错误信息如下", e);
        RespError error = new RespError();
        ErrorCode errorCode = ErrorCode.SERVICE_ERROR;
        error.setCode(errorCode.getCode());
        error.setMessage(errorCode.getMessage());
        error.setData(e.getMessage());
        error.setUrl(request.getRequestURL().toString());
        error.setException(e.getClass().getName());
        if (error.getException().contains("HttpMessageNotReadableException")) {
            error.setMessage("请求参数不可为空");
        }
        return error;
    }
}
