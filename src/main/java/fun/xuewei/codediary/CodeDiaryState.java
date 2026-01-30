package fun.xuewei.codediary;

/**
 * CodeDiary 持久化状态，保存到用户本地（IDE 配置目录）。
 */
public class CodeDiaryState {

    /** 日报格式模板，供生成日报时由 AI 模仿 */
    public String reportFormat = "";

    /** 任何想要补充告诉 AI 的内容 */
    public String otherContent = "";

    /** 机器码，首次生成后持久化 */
    public String machineCode = "";

    public String getReportFormat() {
        return reportFormat;
    }

    public void setReportFormat(String reportFormat) {
        this.reportFormat = reportFormat != null ? reportFormat : "";
    }

    public String getOtherContent() {
        return otherContent;
    }

    public void setOtherContent(String otherContent) {
        this.otherContent = otherContent != null ? otherContent : "";
    }

    public String getMachineCode() {
        return machineCode;
    }

    public void setMachineCode(String machineCode) {
        this.machineCode = machineCode != null ? machineCode : "";
    }
}
