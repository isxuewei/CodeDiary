package fun.xuewei.codediary.exception;

import java.io.Serializable;

/**
 * 内部服务异常类
 *
 * @author 薛伟
 */
public class BusinessException extends AbstractException implements Serializable {

    private static final long serialVersionUID = -4211441659856381357L;

    public BusinessException() {
        super(ErrorCode.SERVICE_ERROR, null);
    }

    public BusinessException(String message) {
        super(ErrorCode.SERVICE_ERROR, message);
    }

    public BusinessException(String message, Object... objects) {
        super(ErrorCode.SERVICE_ERROR, null);
        setDynamicMessage(message, objects);
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode, null);
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public BusinessException(ErrorCode errorCode, String message, Object... objects) {
        super(errorCode, null);
        setDynamicMessage(message, objects);
    }
}
