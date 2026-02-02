package fun.xuewei.codediary;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * CodeDiary 工具窗口的主面板。
 * 顶部：时间范围选择；中间：带多选框的提交列表；底部：生成按钮。
 */
public class CodeDiaryPanel extends SimpleToolWindowPanel {

    private static final String KEY_COMMIT_INFO = "commitInfo";
    /** hash 列宽度，7 位短 hash */
    private static final int COL_HASH_WIDTH = 58;
    /** 时间列宽度，保证 yyyy-MM-dd HH:mm 完整显示 */
    private static final int COL_TIME_WIDTH = 120;
    private static final int HOVER_DELAY_MS = 400;
    private static final int HOVER_HIDE_DELAY_MS = 300;

    private final Project project;
    private final JPanel commitListPanel;
    private final JScrollPane listScrollPane;
    private final JSpinner startDateSpinner;
    private final JSpinner endDateSpinner;
    private final JButton queryButton;
    private final JComboBox<String> rangeCombo;
    private final JButton generateButton;
    private JWindow hoverDetailWindow;
    private Timer hoverShowTimer;
    private Timer hoverHideTimer;
    private JPanel hoverRow;

    public CodeDiaryPanel(Project project) {
        super(false, true);
        this.project = project;
        this.commitListPanel = new JPanel();
        this.listScrollPane = new JBScrollPane(commitListPanel);
        this.startDateSpinner = createDateSpinner();
        this.endDateSpinner = createDateSpinner();
        this.queryButton = new JButton("查询");
        this.rangeCombo = new JComboBox<>(new String[]{"今天", "最近3天", "最近7天", "最近30天", "最近90天"});
        this.generateButton = new JButton("生成");
        initPanel();
    }

    private static JSpinner createDateSpinner() {
        JSpinner spinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, "yyyy-MM-dd");
        spinner.setEditor(editor);
        return spinner;
    }

    private void initPanel() {
        JPanel content = new JPanel(new BorderLayout(0, JBUI.scale(8)));
        content.setBorder(JBUI.Borders.empty(10));

        // 顶部：时间范围
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(4), JBUI.scale(4)));
        top.add(new JLabel("开始:"));
        top.add(startDateSpinner);
        top.add(new JLabel("结束:"));
        top.add(endDateSpinner);
        queryButton.addActionListener(e -> loadCommitsAsync());
        top.add(queryButton);
        rangeCombo.setSelectedIndex(0);
        rangeCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                setDateRangeFromCombo();
            }
        });
        top.add(rangeCombo);
        content.add(top, BorderLayout.NORTH);

        // 中间：带多选框的提交列表
        commitListPanel.setLayout(new BoxLayout(commitListPanel, BoxLayout.Y_AXIS));
        listScrollPane.setPreferredSize(new Dimension(0, 220));
        content.add(listScrollPane, BorderLayout.CENTER);

        // 底部：生成按钮 + 齿轮配置
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        generateButton.addActionListener(e -> onGenerate());
        bottom.add(generateButton);
        JButton settingsBtn = new JButton(AllIcons.General.Gear);
        settingsBtn.setToolTipText("配置");
        settingsBtn.setBorderPainted(false);
        settingsBtn.setFocusPainted(false);
        settingsBtn.setContentAreaFilled(false);
        int iconSize = JBUI.scale(22);
        settingsBtn.setPreferredSize(new Dimension(iconSize, iconSize));
        settingsBtn.setMinimumSize(new Dimension(iconSize, iconSize));
        settingsBtn.addActionListener(e -> showConfigDialog());
        bottom.add(settingsBtn);
        content.add(bottom, BorderLayout.SOUTH);

        setContent(content);

        setDefaultDateRange();
    }

    private void setDefaultDateRange() {
        rangeCombo.setSelectedIndex(0);
        setDateRangeFromCombo();
    }

    private void setDateRangeFromCombo() {
        int days = getDaysFromRangeCombo();
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(days);
        startDateSpinner.setValue(Date.from(start.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        endDateSpinner.setValue(Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        loadCommitsAsync();
    }

    /** 根据下拉选项索引返回天数：今天=0, 最近3天=3, 最近7天=7, 最近30天=30, 最近90天=90 */
    private int getDaysFromRangeCombo() {
        switch (rangeCombo.getSelectedIndex()) {
            case 1: return 3;
            case 2: return 7;
            case 3: return 30;
            case 4: return 90;
            default: return 0;
        }
    }

    private void loadCommitsAsync() {
        Date start = (Date) startDateSpinner.getValue();
        Date end = (Date) endDateSpinner.getValue();
        if (start.after(end)) {
            JOptionPane.showMessageDialog(this, "开始日期不能晚于结束日期。", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        long startMillis = start.getTime();
        long endMillis = endOfDay(end);

        // 立即显示「加载中」，让用户看到有反应
        commitListPanel.removeAll();
        commitListPanel.add(new JLabel("加载中..."));
        commitListPanel.revalidate();
        commitListPanel.repaint();
        listScrollPane.revalidate();
        listScrollPane.repaint();
        queryButton.setEnabled(false);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            List<CommitInfo> commits;
            try {
                commits = GitTodayCommitsLoader.loadCommitsForCurrentUser(project, startMillis, endMillis);
            } catch (Throwable t) {
                commits = new ArrayList<>();
            }
            List<CommitInfo> finalCommits = commits;
            // 回到 EDT 更新列表，不传 ModalityState 避免在某些情况下不执行
            SwingUtilities.invokeLater(() -> applyCommitList(finalCommits));
        });
    }

    private void applyCommitList(List<CommitInfo> commits) {
        commitListPanel.removeAll();
        if (commits.isEmpty()) {
            JLabel emptyHint = new JLabel("该时间范围内暂无提交记录");
            emptyHint.setForeground(new Color(128, 128, 128));
            commitListPanel.add(emptyHint);
            commitListPanel.add(Box.createVerticalStrut(JBUI.scale(8)));
            JLabel hintTitle = new JLabel("请确认：");
            hintTitle.setForeground(new Color(128, 128, 128));
            hintTitle.setFont(hintTitle.getFont().deriveFont(11f));
            commitListPanel.add(hintTitle);
            int leftGap = JBUI.scale(12);
            String[] hints = {
                "项目根目录为 Git 仓库",
                "已配置 git user.name / user.email",
                "所选日期内有您的提交"
            };
            for (String h : hints) {
                JLabel line = new JLabel("  • " + h);
                line.setForeground(new Color(128, 128, 128));
                line.setFont(line.getFont().deriveFont(11f));
                line.setBorder(BorderFactory.createEmptyBorder(0, leftGap, 0, 0));
                commitListPanel.add(line);
            }
        } else {
            SimpleDateFormat timeFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            // 获取今天的日期范围（用于判断是否为今天的提交）
            LocalDate today = LocalDate.now();
            long todayStartMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long todayEndMillis = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1;
            
            for (CommitInfo c : commits) {
                JCheckBox check = new JCheckBox();
                check.putClientProperty(KEY_COMMIT_INFO, c);
                // 默认选中今天的提交
                boolean isToday = c.getCommitTimeMillis() >= todayStartMillis && c.getCommitTimeMillis() <= todayEndMillis;
                check.setSelected(isToday);
                String subject = c.getSubject();

                JLabel hashLabel = new JLabel(c.getShortHash());
                hashLabel.setPreferredSize(new Dimension(COL_HASH_WIDTH, 22));
                hashLabel.setMinimumSize(new Dimension(COL_HASH_WIDTH, 22));

                JLabel timeLabel = new JLabel(timeFmt.format(new Date(c.getCommitTimeMillis())));
                timeLabel.setPreferredSize(new Dimension(COL_TIME_WIDTH, 22));
                timeLabel.setMinimumSize(new Dimension(COL_TIME_WIDTH, 22));

                JLabel msgLabel = new JLabel(subject != null ? subject : "");
                msgLabel.setToolTipText(subject);

                JPanel row = new JPanel(new GridBagLayout());
                row.putClientProperty(KEY_COMMIT_INFO, c);
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.anchor = GridBagConstraints.WEST;
                gbc.insets = new Insets(1, 0, 1, 4);
                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.weightx = 0;
                row.add(check, gbc);
                gbc.gridx = 1;
                row.add(hashLabel, gbc);
                gbc.gridx = 2;
                row.add(timeLabel, gbc);
                gbc.gridx = 3;
                gbc.weightx = 1;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                row.add(msgLabel, gbc);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
                addRowHoverListener(row);
                commitListPanel.add(row);
            }
        }
        commitListPanel.revalidate();
        commitListPanel.repaint();
        listScrollPane.revalidate();
        listScrollPane.repaint();
        queryButton.setEnabled(true);
    }

    /** 为整行及行内子组件添加悬浮监听（hash、时间上不显示详情）、点击行联动多选 */
    private void addRowHoverListener(JPanel row) {
        MouseAdapter hoverAdapter = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                cancelHoverHide();
                hoverRow = row;
                if (hoverShowTimer != null) hoverShowTimer.stop();
                hoverShowTimer = new Timer(HOVER_DELAY_MS, ev -> {
                    hoverShowTimer.stop();
                    Object info = row.getClientProperty(KEY_COMMIT_INFO);
                    if (info instanceof CommitInfo) showHoverDetail(row, (CommitInfo) info);
                });
                hoverShowTimer.setRepeats(false);
                hoverShowTimer.start();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (hoverShowTimer != null) { hoverShowTimer.stop(); hoverShowTimer = null; }
                hoverRow = null;
                scheduleHoverHide();
            }
        };
        MouseAdapter clickAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getSource() instanceof JCheckBox) return;
                for (Component comp : row.getComponents()) {
                    if (comp instanceof JCheckBox) {
                        JCheckBox check = (JCheckBox) comp;
                        check.setSelected(!check.isSelected());
                        break;
                    }
                }
            }
        };
        row.addMouseListener(hoverAdapter);
        row.addMouseListener(clickAdapter);
        Component[] comps = row.getComponents();
        for (int i = 0; i < comps.length; i++) {
            comps[i].addMouseListener(clickAdapter);
            if (i == 3) comps[i].addMouseListener(hoverAdapter);
        }
    }

    private void cancelHoverHide() {
        if (hoverHideTimer != null) {
            hoverHideTimer.stop();
            hoverHideTimer = null;
        }
    }

    private void scheduleHoverHide() {
        cancelHoverHide();
        hoverHideTimer = new Timer(HOVER_HIDE_DELAY_MS, ev -> {
            hoverHideTimer.stop();
            hideHoverDetail();
        });
        hoverHideTimer.setRepeats(false);
        hoverHideTimer.start();
    }

    private void showHoverDetail(JPanel row, CommitInfo c) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            String detail = GitTodayCommitsLoader.getCommitDetail(project, c.getHash());
            SwingUtilities.invokeLater(() -> {
                if (hoverRow != row) return;
                hideHoverDetail();
                JTextArea area = new JTextArea(detail);
                area.setEditable(false);
                area.setLineWrap(true);
                area.setWrapStyleWord(true);
                area.setRows(10);
                area.setMargin(new Insets(6, 8, 6, 8));
                area.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) { cancelHoverHide(); }
                    @Override
                    public void mouseExited(MouseEvent e) { scheduleHoverHide(); }
                });
                JScrollPane scroll = new JScrollPane(area);
                scroll.setPreferredSize(new Dimension(420, 220));
                scroll.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) { cancelHoverHide(); }
                    @Override
                    public void mouseExited(MouseEvent e) { scheduleHoverHide(); }
                });
                hoverDetailWindow = new JWindow(SwingUtilities.getWindowAncestor(CodeDiaryPanel.this));
                hoverDetailWindow.getContentPane().add(scroll);
                hoverDetailWindow.pack();
                Point rowOnScreen = row.getLocationOnScreen();
                hoverDetailWindow.setLocation(rowOnScreen.x, rowOnScreen.y + row.getHeight() + 2);
                hoverDetailWindow.setVisible(true);
            });
        });
    }

    private void hideHoverDetail() {
        if (hoverDetailWindow != null) {
            hoverDetailWindow.setVisible(false);
            hoverDetailWindow.dispose();
            hoverDetailWindow = null;
        }
    }

    private static long endOfDay(Date date) {
        LocalDate d = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return d.atTime(23, 59, 59, 999_000_000).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private void showConfigDialog() {
        CodeDiarySettings settings = CodeDiarySettings.getInstance();
        JPanel configPanel = new JPanel(new BorderLayout(JBUI.scale(8), JBUI.scale(8)));
        configPanel.setBorder(JBUI.Borders.empty(16));

        // 日报格式
        JLabel formatLabel = new JLabel("日报格式");
        JLabel formatHint = new JLabel("可配置生成日报的格式模板，供 AI 模仿。");
        formatHint.setForeground(new Color(128, 128, 128));
        formatHint.setFont(formatHint.getFont().deriveFont(11f));
        JTextArea formatArea = new JTextArea(6, 40);
        formatArea.setLineWrap(true);
        formatArea.setWrapStyleWord(true);
        formatArea.setMargin(new Insets(6, 8, 6, 8));
        formatArea.setText(settings.getReportFormat() != null ? settings.getReportFormat() : "");

        // 其他内容
        JLabel otherLabel = new JLabel("其他内容");
        JLabel otherHint = new JLabel("任何想要补充告诉 AI 的内容。");
        otherHint.setForeground(new Color(128, 128, 128));
        otherHint.setFont(otherHint.getFont().deriveFont(11f));
        JTextArea otherArea = new JTextArea(4, 40);
        otherArea.setLineWrap(true);
        otherArea.setWrapStyleWord(true);
        otherArea.setMargin(new Insets(6, 8, 6, 8));
        otherArea.setText(settings.getOtherContent() != null ? settings.getOtherContent() : "");

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        int gap = JBUI.scale(12);
        JPanel formatBlock = new JPanel(new BorderLayout(0, JBUI.scale(4)));
        formatBlock.add(formatLabel, BorderLayout.NORTH);
        formatBlock.add(formatHint, BorderLayout.CENTER);
        content.add(formatBlock);
        content.add(Box.createVerticalStrut(JBUI.scale(4)));
        content.add(new JBScrollPane(formatArea));
        content.add(Box.createVerticalStrut(gap));
        JPanel otherBlock = new JPanel(new BorderLayout(0, JBUI.scale(4)));
        otherBlock.add(otherLabel, BorderLayout.NORTH);
        otherBlock.add(otherHint, BorderLayout.CENTER);
        content.add(otherBlock);
        content.add(Box.createVerticalStrut(JBUI.scale(4)));
        content.add(new JBScrollPane(otherArea));

        JScrollPane contentScroll = new JBScrollPane(content);
        contentScroll.setBorder(JBUI.Borders.empty());
        configPanel.add(contentScroll, BorderLayout.CENTER);

        JButton saveBtn = new JButton("保存");
        JButton closeBtn = new JButton("关闭");
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, JBUI.scale(8), 0));
        south.add(saveBtn);
        south.add(closeBtn);
        configPanel.add(south, BorderLayout.SOUTH);

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "配置 CodeDiary", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.getContentPane().add(configPanel);
        dialog.setMinimumSize(new Dimension(420, 420));

        saveBtn.addActionListener(e -> {
            settings.setReportFormat(formatArea.getText().trim());
            settings.setOtherContent(otherArea.getText().trim());
            dialog.dispose();
        });
        closeBtn.addActionListener(e -> dialog.dispose());

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void onGenerate() {
        List<CommitInfo> selected = new ArrayList<>();
        for (Component comp : commitListPanel.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel row = (JPanel) comp;
                for (Component c : row.getComponents()) {
                    if (c instanceof JCheckBox) {
                        JCheckBox check = (JCheckBox) c;
                        if (check.isSelected()) {
                            Object info = check.getClientProperty(KEY_COMMIT_INFO);
                            if (info instanceof CommitInfo) selected.add((CommitInfo) info);
                        }
                        break;
                    }
                }
            }
        }
        if (selected.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请至少勾选一条提交记录。", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        CodeDiarySettings settings = CodeDiarySettings.getInstance();
        String machineCode = settings.getMachineCode();
        if (machineCode == null || machineCode.isEmpty()) {
            machineCode = java.util.UUID.randomUUID().toString();
            settings.setMachineCode(machineCode);
        }
        List<String> commits = new ArrayList<>();
        SimpleDateFormat timeFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        for (CommitInfo c : selected) {
            String detail = GitTodayCommitsLoader.getCommitDetail(project, c.getHash());
            commits.add(detail != null ? detail : (c.getShortHash() + " " + timeFmt.format(new Date(c.getCommitTimeMillis())) + " " + (c.getSubject() != null ? c.getSubject() : "")));
        }
        GenerateApiClient.GenerateRequest req = new GenerateApiClient.GenerateRequest();
        req.machineCode = machineCode;
        req.clientVersion = "26.01.30";
        req.projectName = project.getName() != null ? project.getName() : "";
        req.commits = commits;
        req.reportFormat = settings.getReportFormat() != null ? settings.getReportFormat() : "";
        req.otherContent = settings.getOtherContent() != null ? settings.getOtherContent() : "";

        generateButton.setEnabled(false);
        generateButton.setText("生成中...");
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            String result;
            try {
                result = GenerateApiClient.generate(req);
            } catch (Throwable t) {
                result = "生成失败: " + (t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName());
            }
            String finalResult = result;
            SwingUtilities.invokeLater(() -> {
                generateButton.setText("生成");
                generateButton.setEnabled(true);
                showGenerateResultDialog(finalResult);
            });
        });
    }

    /** 弹出对话框展示生成结果或错误信息 */
    private void showGenerateResultDialog(String fullText) {
        JTextArea area = new JTextArea(fullText);
        area.setEditable(true);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setMargin(new Insets(8, 10, 8, 10));
        JScrollPane scroll = new JBScrollPane(area);
        scroll.setPreferredSize(new Dimension(520, 360));
        JButton copyBtn = new JButton("复制");
        JButton closeBtn = new JButton("关闭");
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, JBUI.scale(8), 0));
        south.add(copyBtn);
        south.add(closeBtn);
        JPanel content = new JPanel(new BorderLayout(JBUI.scale(8), JBUI.scale(8)));
        content.setBorder(JBUI.Borders.empty(12));
        content.add(scroll, BorderLayout.CENTER);
        content.add(south, BorderLayout.SOUTH);
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "生成内容", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.getContentPane().add(content);
        copyBtn.addActionListener(e -> {
            try {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(area.getText()), null);
                JOptionPane.showMessageDialog(dialog, "复制成功。", "提示", JOptionPane.PLAIN_MESSAGE);
            } catch (Exception ignored) { }
        });
        closeBtn.addActionListener(e -> dialog.dispose());
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
}
