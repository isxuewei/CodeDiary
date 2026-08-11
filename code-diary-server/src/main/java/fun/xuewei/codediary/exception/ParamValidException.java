package fun.xuewei.codediary.exception;

import lombok.Data;

import java.io.Serializable;

/**
 * 参数校验异常类
 *
 * @author 薛伟
 */
@Data
public class ParamValidException extends ClientException implements Serializable {

    private static final long serialVersionUID = -4211441659856381357L;

    /**
     * 参数名
     */
    private final String param;

    public ParamValidException(String param, String message) {
        super(ErrorCode.PARAM_VALID, message);
        this.param = param;
    }

    /**
     * 构建参数为空的异常
     *
     * @param param 参数名
     */
    public static ParamValidException empty(String param, String info) {
        return new ParamValidException(param, info + "参数不能为空");
    }
}
