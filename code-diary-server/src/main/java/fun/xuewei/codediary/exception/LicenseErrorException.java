package fun.xuewei.codediary.exception;

import java.io.Serializable;

/**
 * 认证错误异常类
 *
 * @author 薛伟
 */
public class LicenseErrorException extends AbstractException implements Serializable {

    private static final long serialVersionUID = -4211441659856381357L;

    public LicenseErrorException() {
        super(ErrorCode.SERVICE_ERROR, null);
    }

    public LicenseErrorException(String message) {
        super(ErrorCode.SERVICE_ERROR, message);
    }

    public LicenseErrorException(String message, Object... objects) {
        super(ErrorCode.SERVICE_ERROR, null);
        setDynamicMessage(message, objects);
    }

    public LicenseErrorException(ErrorCode errorCode) {
        super(errorCode, null);
    }

    public LicenseErrorException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public LicenseErrorException(ErrorCode errorCode, String message, Object... objects) {
        super(errorCode, message);
        setDynamicMessage(message, objects);
    }
}
