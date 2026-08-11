package fun.xuewei.codediary.exception;

import cn.hutool.core.util.ObjectUtil;
import lombok.Getter;

import java.io.Serializable;
import java.util.Optional;

/**
 * 异常基类
 *
 * @author 薛伟
 */
@Getter
public abstract class AbstractException extends RuntimeException implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    protected String code;

    /**
     * 错误信息
     */
    protected String message;

    public AbstractException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.message = Optional.ofNullable(message).orElse(errorCode.getMessage());
    }

    /**
     * 设置原因
     *
     * @param cause 原因
     */
    public AbstractException from(Throwable cause) {
        this.initCause(cause);
        return this;
    }

    /**
     * 设置动态消息
     *
     * @param message 动态消息
     * @param objects 动态参数
     */
    protected void setDynamicMessage(String message, Object... objects) {
        for (Object o : objects) {
            if (ObjectUtil.isNull(o)) {
                o = "";
            }
            message = message.replaceFirst("\\{\\}", o.toString());
        }
        this.message = message;
    }
}
