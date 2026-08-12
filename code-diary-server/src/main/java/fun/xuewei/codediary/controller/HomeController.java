package fun.xuewei.codediary.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 首页控制器
 *
 * @author 薛伟
 */
@Controller
@RequestMapping("/")
public class HomeController {

    @GetMapping
    public String home(Model model) {
        model.addAttribute("title", "CodeDiary - AI 生成 Git 提交日志与工作日报");
        model.addAttribute("description", "CodeDiary 是一款 IntelliJ IDEA 插件，可先根据代码变更生成并回填 Git 提交日志，再根据 Git 提交记录生成工作日报。");
        return "index";
    }
}
