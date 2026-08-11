package fun.xuewei.codediary.exception;

import java.io.Serializable;

/**
 * 权限异常类
 *
 * @author 薛伟
 */
public class AuthorityException extends ClientException implements Serializable {

    private static final long serialVersionUID = -4211441659856381357L;

    public AuthorityException() {
        super(ErrorCode.AUTHORITY_ERROR, null);
    }

    public AuthorityException(String message) {
        super(ErrorCode.AUTHORITY_ERROR, message);
    }

    public AuthorityException(String message, Object... objects) {
        super(ErrorCode.AUTHORITY_ERROR, null);
        setDynamicMessage(message, objects);
    }

    public AuthorityException(ErrorCode errorCode) {
        super(errorCode, null);
    }

    public AuthorityException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public AuthorityException(ErrorCode errorCode, String message, Object... objects) {
        super(errorCode, message);
        setDynamicMessage(message, objects);
    }
}
