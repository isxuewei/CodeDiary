package fun.xuewei.codediary.dto.response;

import cn.hutool.core.util.ObjectUtil;
import fun.xuewei.codediary.exception.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口响应结果
 *
 * @author 薛伟
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "接口响应结果")
public class RespResult<T> {

    /**
     * 响应编码
     */
    @Schema(description = "响应编码", example = "OK")
    protected String code;

    /**
     * 响应信息
     */
    @Schema(description = "响应信息", example = "操作成功")
    protected String message;

    /**
     * 响应数据
     */
    @Schema(description = "响应数据")
    protected T data;

    /**
     * 构建响应结果
     *
     * @param errorCode 错误码
     */
    public static <T> RespResult<T> build(ErrorCode errorCode) {
        return build(errorCode, errorCode.getMessage());
    }

    /**
     * 构建响应结果
     *
     * @param errorCode 错误码
     * @param message   错误信息
     */
    public static <T> RespResult<T> build(ErrorCode errorCode, String message) {
        return build(errorCode, message, null);
    }

    /**
     * 构建响应结果
     *
     * @param errorCode 错误码
     * @param data      响应数据
     */
    public static <T> RespResult<T> build(ErrorCode errorCode, String message, T data) {
        RespResult<T> result = new RespResult<>();
        result.setCode(errorCode.getCode());
        result.setMessage(ObjectUtil.defaultIfEmpty(message, errorCode.getMessage()));
        result.setData(data);
        return result;
    }

    /**
     * 请求成功
     */
    public static <T> RespResult<T> success() {
        return build(ErrorCode.SUCCESS);
    }

    /**
     * 请求成功
     */
    public static <T> RespResult<T> success(String message) {
        return build(ErrorCode.SUCCESS, message);
    }

    /**
     * 请求成功
     */
    public static <T> RespResult<T> success(String message, T data) {
        return build(ErrorCode.SUCCESS, message, data);
    }

    /**
     * 资源不存在
     */
    public static <T> RespResult<T> notFound() {
        return build(ErrorCode.RESOURCE_NOT_FOUND);
    }

    /**
     * 资源不存在
     */
    public static <T> RespResult<T> notFound(String message) {
        return build(ErrorCode.RESOURCE_NOT_FOUND, message);
    }

    /**
     * 资源不存在
     */
    public static <T> RespResult<T> notFound(String message, T data) {
        return build(ErrorCode.RESOURCE_NOT_FOUND, message, data);
    }

    /**
     * 请求是否成功
     */
    public boolean yesSuccess() {
        return ErrorCode.SUCCESS.getCode().equals(code);
    }

    /**
     * 请求是否成功并有数据返回
     */
    public boolean yesSuccessWithDateResp() {
        return yesSuccess() && ObjectUtil.isNotEmpty(data);
    }

    /**
     * 请求是否成功
     */
    public boolean notSuccess() {
        return !yesSuccess();
    }
}
