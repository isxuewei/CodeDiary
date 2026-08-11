package fun.xuewei.codediary.exception;

/**
 * 系统内置错误码，返回错误信息必须要有对应的错误码。
 * <p>错误码是一个包含 5 个字符的字符串，它分为两部分：错误来源标识（1个字符）和错误编号（4个数字）。</p>
 * <p>本系统暂且将错误码分为 ABC 三类：</p>
 * <ul>
 * <li>A 类：客户端错误，由客户端（如浏览器）发起的请求，如参数错误、用户不存在等。</li>
 * <li>B 类：系统内部错误，由系统内部模块（如服务层）发起的请求，如系统超时、执行超时等。</li>
 * <li>C 类：第三方接口调用错误，由第三方接口（如 RPC 调用）发起的请求，如第三方服务异常等。</li>
 * </ul>
 *
 * @author 薛伟
 */
public enum ErrorCode {

    SUCCESS("OK", "操作成功"),

    CLIENT_ERROR("A0001", "客户端错误"),
    USER_NOT_FOUND("A0002", "用户不存在"),
    USER_ALREADY_EXISTS("A0003", "用户已存在"),
    USERNAME_PASSWORD_INCORRECT("A0004", "用户名或密码错误"),
    VERIFICATION_CODE_EXPIRED("A0005", "验证码已过期"),
    BAD_CREDENTIALS_EXPIRED("A0006", "用户认证异常"),
    AUTHORITY_ERROR("A0007", "权限异常"),
    PARAM_EMPTY("A0008", "参数为空"),
    PARAM_VALID("A0009", "参数校验失败"),
    FIELD_NOT_EXIST("A0010", "字段不存在"),
    SQL_ERROR("A0011", "SQL 语法错误"),
    USER_NOT_LOGIN("A0012", "用户未登录"),

    SERVICE_ERROR("B0001", "系统内部错误"),
    SERVICE_TIMEOUT("B0002", "系统执行超时"),
    JWT_PARSE_ERROR("B0003", "JWT 解析异常"),
    SERVICE_BUSY("B0004", "系统繁忙"),
    RESOURCE_NOT_FOUND("B0005", "资源不存在"),

    REMOTE_ERROR("C0001", "远程服务网络异常"),
    FEIGN_ERROR("C0002", "微服务调用异常"),
    IDEMPOTENT_TOKEN_ERROR("C0003", "幂等 TOKEN 参数为空"),
    IDEMPOTENT_TOKEN_INVALID("C0004", "幂等 Token 已被使用或失效"),
    IDEMPOTENT_ERROR("C0005", "幂等操作异常");

    /**
     * 错误码
     */
    private final String code;

    /**
     * 错误信息
     */
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
