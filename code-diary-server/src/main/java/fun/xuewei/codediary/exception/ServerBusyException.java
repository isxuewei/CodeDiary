package fun.xuewei.codediary.exception;

import java.io.Serializable;

/**
 * 服务器繁忙异常类
 *
 * @author 薛伟
 */
public class ServerBusyException extends BusinessException implements Serializable {

    private static final long serialVersionUID = -5653832166622573877L;

    public ServerBusyException() {
        super(ErrorCode.SERVICE_BUSY);
    }

    public ServerBusyException(Object userId) {
        super(ErrorCode.SERVICE_BUSY, "用户 ID：{}", userId);
    }
}
