<div align="center">
  <img src="https://code-diary.xuewei.fun/images/codeDiary.svg" alt="CodeDiary Logo" width="120" height="120">
  
  # CodeDiary
  
  ### 让代码说话，让 AI 翻译
  
  <p>
    <img src="https://img.shields.io/badge/AI-驱动-blue?style=flat-square" alt="AI 驱动">
    <img src="https://img.shields.io/badge/IntelliJ-IDEA-orange?style=flat-square" alt="IntelliJ IDEA">
    <img src="https://img.shields.io/badge/Git-集成-green?style=flat-square" alt="Git 集成">
    <img src="https://img.shields.io/badge/开源-免费-brightgreen?style=flat-square" alt="免费开源">
  </p>
  
  CodeDiary 是一款基于 Git 提交记录的智能工作汇报插件，借助 AI 大模型的强大能力，<br>
  自动总结归纳你的代码提交，一键生成专业的工作报告。
  
  [插件官网](https://code-diary.xuewei.fun/) · [反馈问题](https://github.com/isxuewei/CodeDiary/issues)
  
</div>

---

## ✨ 功能特性

### 🎯 核心价值

- **90%+ 时间节省** - 从手写 10 分钟的日报，缩短到 1 分钟内完成。基于真实 Git 提交记录，确保内容准确无误。
- **技术语言转业务语言** - AI 自动将代码提交信息转化为通俗易懂的工作汇报，让非技术人员也能看懂你的工作内容。
- **零学习成本** - 三步操作即可完成：选择时间范围 → 筛选提交记录 → 生成报告。极简的交互设计，上手即用。
- **无缝融入工作流** - 直接集成在 IDEA 中，不需要切换工具，保持工作连贯性，让汇报成为顺手的事情。

### 🚀 主要功能

#### 📅 时间范围选择
灵活选择日报、周报、月报，一键切换时间范围。

#### ✅ 智能提交筛选
可视化提交列表，精准选择纳入报告的内容。

#### 🤖 AI 智能生成
通义千问大模型加持，自动生成专业工作报告。

#### ⚙️ 自定义配置
支持自定义报告格式，适应不同汇报规范。

---

## 📦 安装使用

### 方式一：IDEA 插件市场安装（推荐）

1. 打开 IntelliJ IDEA
2. 进入 `Settings/Preferences → Plugins`
3. 搜索 "CodeDiary"，作者认准 "薛伟同学"
4. 点击 `Install` 安装
5. 重启 IDE

### 方式二：手动安装

1. 访问 [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/29992-codediary) 下载最新版本
2. 进入 `Settings/Preferences → Plugins → ⚙️ → Install Plugin from Disk...`
3. 选择下载的文件并安装
4. 重启 IDE

### 快速开始

1. 点击工具栏 CodeDiary 图标
2. 选择时间范围和提交记录
3. 点击生成
4. 复制粘贴到汇报系统

---

## 🎬 使用场景

### 📝 日常工作日报
下班前快速生成今日工作总结，包含所有代码提交内容，支持一键复制到钉钉、企微等工具。

### 📊 周报、月报总结
选择相应时间范围，筛选重要工作内容，生成结构化的周报或月报，便于领导了解项目进度。

### 📈 技术成长复盘
定期回顾自己的代码提交，分析工作重心，思考技术成长路径，成为个人技术成长的镜子。

---

## 💡 设计理念

> 程序员的工作价值应该如何体现？传统的工作汇报方式，本质上是一种"二次创作"：先用代码表达一次（Git 提交），然后再用文字表达一次（日报/周报/月报）。

### 核心思想

- **代码即工作，提交即日志** - 每一次 Git 提交都包含了 What（做了什么）、When（什么时候做的）、How（怎么做的）、Why（为什么这么做）的完整信息。
- **AI 负责转换，人负责创造** - AI 擅长将技术语言转换为自然语言，人则专注于创造高质量的代码和提交信息。
- **融入工作流，无缝体验** - 直接集成在 IDEA 里，就在写代码的地方，不需要切换工具，保持工作连贯性。
- **消除负担，保留价值** - 让写报告从"每次要花 20 分钟"变成"2 分钟完成"，保留汇报的价值，消除形式主义的负担。

---

## 🛠 技术栈

- **后端框架**: Spring Boot
- **AI 模型**: 通义千问大模型
- **插件平台**: IntelliJ Platform SDK
- **版本控制**: Git

---

## 📝 开发计划

- [x] 基础功能实现
- [x] AI 智能生成
- [x] 时间范围选择
- [x] 提交记录筛选
- [ ] 多语言支持
- [ ] 更多 AI 模型接入
- [ ] 自定义模板
- [ ] 数据统计分析

---

## 🤝 贡献指南

欢迎贡献代码、报告问题或提出建议！

1. Fork 本仓库
2. 创建你的特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交你的改动 (`git commit -m '添加某个很棒的功能'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启一个 Pull Request

---

## 📄 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

---

## 👨‍💻 作者

**薛伟同学**

- GitHub: [@isxuewei](https://github.com/isxuewei)
- 项目主页: [CodeDiary](https://github.com/isxuewei/CodeDiary)

---

## 💖 支持项目

如果这个项目对你有帮助，欢迎：

- ⭐ 给项目点个 Star
- 🐛 提交 Issue 反馈问题
- 💡 提出新的功能建议
- 🔀 提交 Pull Request 贡献代码

---

<div align="center">
  
  **让 CodeDiary 帮你从繁琐的文字工作中解放出来**
  
  **把时间投入到更有价值的代码创造和技术提升上**
  
  Made with ❤️ by 薛伟同学
  
</div>
