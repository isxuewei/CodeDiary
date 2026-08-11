package fun.xuewei.codediary.exception;

import java.io.Serializable;

/**
 * 用户不存在异常
 *
 * @author 薛伟
 */
public class UserNotFoundException extends ClientException implements Serializable {

    private static final long serialVersionUID = -5653832166622573877L;

    public UserNotFoundException() {
        super(ErrorCode.USER_NOT_FOUND);
    }

    public UserNotFoundException(Object userId) {
        super(ErrorCode.USER_NOT_FOUND, "用户 ID：{}", userId);
    }
}
