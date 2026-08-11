package fun.xuewei.codediary.exception;

import java.io.Serializable;

/**
 * 客户端异常类
 *
 * @author 薛伟
 */
public class ClientException extends AbstractException implements Serializable {

    private static final long serialVersionUID = 2792869211011068994L;

    public ClientException() {
        super(ErrorCode.CLIENT_ERROR, null);
    }

    public ClientException(String message) {
        super(ErrorCode.CLIENT_ERROR, message);
    }

    public ClientException(String message, Object... objects) {
        super(ErrorCode.CLIENT_ERROR, null);
        setDynamicMessage(message, objects);
    }

    public ClientException(ErrorCode errorCode) {
        super(errorCode, null);
    }

    public ClientException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public ClientException(ErrorCode errorCode, String message, Object... objects) {
        super(errorCode, message);
        setDynamicMessage(message, objects);
    }
}
