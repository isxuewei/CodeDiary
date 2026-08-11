package fun.xuewei.codediary.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 错误信息
 *
 * @author 薛伟
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "错误信息")
public class RespError extends RespResult {

    /**
     * 请求地址（发生异常时返回）
     */
    @Schema(description = "请求地址", example = "http://127.0.0.1:8080/TParam/saveData")
    private String url;

    /**
     * 异常类（发生异常时返回）
     */
    @Schema(description = "异常类", example = "java.lang.NullPointerException")
    private String exception;
}
