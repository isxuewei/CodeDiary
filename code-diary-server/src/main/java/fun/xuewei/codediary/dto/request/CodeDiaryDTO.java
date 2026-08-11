package fun.xuewei.codediary.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 代码日记（CodeDiary）请求体
 *
 * @author 薛伟
 */
@Data
@AllArgsConstructor
@Schema(description = "代码日记（CodeDiary）请求体")
public class CodeDiaryDTO {

    @Data
    public static class GenerateDTO {

        @Schema(description = "项目名")
        @NotEmpty(message = "项目名不能为空")
        private String projectName;

        @Schema(description = "机器码")
        @NotEmpty(message = "机器码不能为空")
        private String machineCode;

        @Schema(description = "客户端版本")
        @NotEmpty(message = "客户端版本不能为空")
        private String clientVersion;

        @Schema(description = "提交列表")
        @NotEmpty(message = "提交列表不能为空")
        private List<String> commits;

        @Schema(description = "日报格式示例")
        private String reportFormat;

        @Schema(description = "其他内容")
        private String otherContent;
    }

    @Data
    public static class GitCommitLogDTO {

        @Schema(description = "项目名")
        @NotEmpty(message = "项目名不能为空")
        private String projectName;

        @Schema(description = "机器码")
        @NotEmpty(message = "机器码不能为空")
        private String machineCode;

        @Schema(description = "客户端版本")
        @NotEmpty(message = "客户端版本不能为空")
        private String clientVersion;

        @Schema(description = "最近的 Git 提交记录，用于参考语言和格式")
        @Size(max = 20, message = "最近提交记录不能超过 20 条")
        private List<String> recentCommits;

        @Schema(description = "当前选中代码变更的文件清单与 diff")
        @NotEmpty(message = "代码变更不能为空")
        @Size(max = 50, message = "代码变更不能超过 50 段")
        private List<String> changes;

        @Schema(description = "Git 提交日志格式示例")
        @Size(max = 500, message = "Git 提交日志格式不能超过 500 个字符")
        private String commitLogFormat;
    }
}
