package fun.xuewei.codediary;

/**
 * CodeDiary 持久化状态，保存到用户本地（IDE 配置目录）。
 */
public class CodeDiaryState {

    private static final String LEGACY_DEFAULT_REPORT_FORMAT = String.join("\n",
        "项目名称：{项目名称}",
        "",
        "1. {模块名称}：{具体工作内容与当前进展} {完成进度}%",
        "2. {模块名称}：{具体工作内容与当前进展} {完成进度}%",
        "3. {模块名称}：{具体工作内容与当前进展} {完成进度}%",
        "4. {模块名称}：{具体工作内容与当前进展} {完成进度}%"
    );

    private static final String LEGACY_DEFAULT_OTHER_CONTENT = String.join("\n",
        "1. 生成内容中如果有英文的，我希望返回的格式是 AaaBbbCcc 的格式",
        "2. 中文和英文之间用空格分割"
    );

    public static final String DEFAULT_REPORT_FORMAT = String.join("\n",
        "项目名称：{项目名称}",
        "",
        "1. {业务模块}：{具体工作内容与当前进展} {完成进度}%",
        "2. {业务模块}：{具体工作内容与当前进展} {完成进度}%",
        "3. {业务模块}：{具体工作内容与当前进展} {完成进度}%",
        "4. {业务模块}：{具体工作内容与当前进展} {完成进度}%"
    );

    public static final String DEFAULT_OTHER_CONTENT = String.join("\n",
        "1. 生成内容中如果有英文的，我希望返回的格式是 AaaBbbCcc 的格式",
        "2. 中文和英文之间用空格分割",
        "3. 生成内容尽量简洁"
    );

    public static final String DEFAULT_GIT_COMMIT_LOG_FORMAT = String.join("\n",
        "<type>(<scope>): <简短描述>",
        "",
        "- <主要变更>"
    );

    /** 日报格式模板，供生成日报时由 AI 模仿 */
    public String reportFormat = DEFAULT_REPORT_FORMAT;

    /** 任何想要补充告诉 AI 的内容 */
    public String otherContent = DEFAULT_OTHER_CONTENT;

    /** 机器码，首次生成后持久化 */
    public String machineCode = "";

    /** Git 提交日志格式模板，供生成提交消息时由 AI 模仿 */
    public String gitCommitLogFormat = DEFAULT_GIT_COMMIT_LOG_FORMAT;

    public String getReportFormat() {
        return reportFormat;
    }

    public void setReportFormat(String reportFormat) {
        this.reportFormat = !hasText(reportFormat) || LEGACY_DEFAULT_REPORT_FORMAT.equals(reportFormat)
            ? DEFAULT_REPORT_FORMAT
            : reportFormat;
    }

    public String getOtherContent() {
        return otherContent;
    }

    public void setOtherContent(String otherContent) {
        this.otherContent = !hasText(otherContent) || LEGACY_DEFAULT_OTHER_CONTENT.equals(otherContent)
            ? DEFAULT_OTHER_CONTENT
            : otherContent;
    }

    public String getMachineCode() {
        return machineCode;
    }

    public void setMachineCode(String machineCode) {
        this.machineCode = machineCode != null ? machineCode : "";
    }

    public String getGitCommitLogFormat() {
        return gitCommitLogFormat;
    }

    public void setGitCommitLogFormat(String gitCommitLogFormat) {
        this.gitCommitLogFormat = hasText(gitCommitLogFormat)
            ? gitCommitLogFormat
            : DEFAULT_GIT_COMMIT_LOG_FORMAT;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
