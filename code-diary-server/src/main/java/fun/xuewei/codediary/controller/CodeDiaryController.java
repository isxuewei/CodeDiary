package fun.xuewei.codediary.controller;

import fun.xuewei.codediary.dto.request.CodeDiaryDTO;
import fun.xuewei.codediary.dto.response.RespResult;
import fun.xuewei.codediary.service.CodeDiaryAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 代码日记（CodeDiary） 接口层
 *
 * @author 薛伟
 */
@RestController
@Tag(name = "代码日记")
@RequestMapping("/CodeDiary")
public class CodeDiaryController {

    @Autowired
    private CodeDiaryAppService service;

    @PostMapping("/generate")
    @Operation(summary = "生成代码日记")
    public RespResult<String> generate(@Valid @RequestBody CodeDiaryDTO.GenerateDTO generateDTO) {
        String machineCode = generateDTO.getMachineCode();
        String clientVersion = generateDTO.getClientVersion();
        List<String> commits = generateDTO.getCommits();
        String reportFormat = generateDTO.getReportFormat();
        String otherContent = generateDTO.getOtherContent();
        String projectName = generateDTO.getProjectName();
        String result = service.generateDiary(machineCode, clientVersion, projectName, commits, reportFormat, otherContent);
        return RespResult.success("登录成功", result);
    }

    @PostMapping("/generateGitCommitLog")
    @Operation(summary = "根据所选代码变更生成 Git 提交日志")
    public RespResult<String> generateGitCommitLog(
            @Valid @RequestBody CodeDiaryDTO.GitCommitLogDTO generateDTO
    ) {
        String result = service.generateGitCommitLog(
                generateDTO.getMachineCode(),
                generateDTO.getClientVersion(),
                generateDTO.getProjectName(),
                generateDTO.getRecentCommits(),
                generateDTO.getChanges(),
                generateDTO.getCommitLogFormat()
        );
        return RespResult.success("生成成功", result);
    }
}
