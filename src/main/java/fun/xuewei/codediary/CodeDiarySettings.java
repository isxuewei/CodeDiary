package fun.xuewei.codediary;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * CodeDiary 应用级配置服务，状态保存到用户本地。
 */
@State(
    name = "CodeDiarySettings",
    storages = @Storage("codeDiary.xml")
)
public class CodeDiarySettings implements PersistentStateComponent<CodeDiaryState> {

    private final CodeDiaryState state = new CodeDiaryState();

    @Override
    public @Nullable CodeDiaryState getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull CodeDiaryState state) {
        this.state.setReportFormat(state.getReportFormat());
        this.state.setOtherContent(state.getOtherContent());
        this.state.setMachineCode(state.getMachineCode());
    }

    public String getReportFormat() {
        return state.getReportFormat();
    }

    public void setReportFormat(String reportFormat) {
        state.setReportFormat(reportFormat);
    }

    public String getOtherContent() {
        return state.getOtherContent();
    }

    public void setOtherContent(String otherContent) {
        state.setOtherContent(otherContent);
    }

    public String getMachineCode() {
        return state.getMachineCode();
    }

    public void setMachineCode(String machineCode) {
        state.setMachineCode(machineCode);
    }

    public static CodeDiarySettings getInstance() {
        return ServiceManager.getService(CodeDiarySettings.class);
    }
}
