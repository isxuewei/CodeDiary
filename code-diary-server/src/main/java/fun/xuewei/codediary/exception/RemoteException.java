package fun.xuewei.codediary.exception;

import java.io.Serializable;

/**
 * 远程调用异常
 *
 * @author 薛伟
 */
public class RemoteException extends AbstractException implements Serializable {

    private static final long serialVersionUID = -3095639653937569308L;

    private final String url;

    public RemoteException(String url, String message, Object... objects) {
        super(ErrorCode.REMOTE_ERROR, null);
        this.url = url;
        setDynamicMessage(message, objects);
    }

    public RemoteException(ErrorCode errorCode, String url, String message, Object... objects) {
        super(errorCode, null);
        this.url = url;
        setDynamicMessage(message, objects);
    }

    public String getUrl() {
        return url;
    }
}
