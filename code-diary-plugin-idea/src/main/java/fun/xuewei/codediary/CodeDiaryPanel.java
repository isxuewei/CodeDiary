package fun.xuewei.codediary;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private static final int CUSTOM_RANGE_INDEX = 4;
    private static final float ACTION_ICON_SCALE = 1.25f;

    private final Project project;
    private final JPanel commitListPanel;
    private final JScrollPane listScrollPane;
    private final DateRangeSelector dateRangeSelector;
    private final JComboBox<String> repositoryCombo;
    private final Component repositoryGap;
    private final Component customDateGap;
    private final JButton refreshButton;
    private final JCheckBox selectAllCheckBox;
    private final JComboBox<String> rangeCombo;
    private final JButton generateButton;
    private JWindow hoverDetailWindow;
    private Timer hoverShowTimer;
    private Timer hoverHideTimer;
    private JPanel hoverRow;
    private List<GitRepositoryInfo> repositories = new ArrayList<>();
    private boolean repositoriesScanned;
    private long querySequence;

    public CodeDiaryPanel(Project project) {
        super(false, true);
        this.project = project;
        this.commitListPanel = new JPanel();
        this.listScrollPane = new JBScrollPane(commitListPanel);
        this.dateRangeSelector = new DateRangeSelector(LocalDate.now(), LocalDate.now());
        this.repositoryCombo = new JComboBox<>();
        this.repositoryGap = Box.createHorizontalStrut(JBUI.scale(4));
        this.customDateGap = Box.createHorizontalStrut(JBUI.scale(4));
        this.refreshButton = new JButton(AllIcons.Actions.Refresh);
        this.selectAllCheckBox = new JCheckBox();
        this.rangeCombo = new JComboBox<>(new String[]{"今天", "昨天", "近一周", "近一月", "自定义"});
        this.generateButton = new JButton("生成");
        initPanel();
    }

    private void initPanel() {
        JPanel content = new JPanel(new BorderLayout(0, JBUI.scale(8)));
        content.setBorder(JBUI.Borders.empty(10));

        // 顶部：时间范围
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, JBUI.scale(4)));
        repositoryCombo.setFocusable(false);
        repositoryCombo.setRenderer(createCenteredComboRenderer());
        repositoryCombo.setVisible(false);
        repositoryGap.setVisible(false);
        repositoryCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && repositoriesScanned) {
                loadCommitsAsync();
            }
        });
        top.add(repositoryCombo);
        top.add(repositoryGap);

        rangeCombo.setFocusable(false);
        rangeCombo.setRenderer(createCenteredComboRenderer());
        rangeCombo.setSelectedIndex(0);
        rangeCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                handleRangeSelection();
            }
        });
        top.add(rangeCombo);
        dateRangeSelector.matchStyle(rangeCombo);
        matchRangeComboHeight();
        customDateGap.setVisible(false);
        dateRangeSelector.setVisible(false);
        dateRangeSelector.addPropertyChangeListener(DateRangeSelector.RANGE_PROPERTY, e -> queryCustomRange());
        top.add(customDateGap);
        top.add(dateRangeSelector);
        top.add(Box.createHorizontalStrut(JBUI.scale(4)));
        refreshButton.setToolTipText("重新扫描仓库并刷新提交记录");
        refreshButton.setBorderPainted(false);
        disableFocusHighlight(refreshButton);
        refreshButton.setContentAreaFilled(false);
        refreshButton.getAccessibleContext().setAccessibleName("刷新提交记录");
        refreshButton.setIcon(IconUtil.scale(AllIcons.Actions.Refresh, refreshButton, ACTION_ICON_SCALE));
        matchIconButtonSize(refreshButton);
        refreshButton.addActionListener(e -> scanRepositoriesAsync());
        top.add(refreshButton);
        content.add(top, BorderLayout.NORTH);

        // 中间：带多选框的提交列表
        commitListPanel.setLayout(new BoxLayout(commitListPanel, BoxLayout.Y_AXIS));
        listScrollPane.setPreferredSize(new Dimension(0, 220));
        listScrollPane.setBorder(JBUI.Borders.empty());
        JPanel commitTablePanel = new JPanel(new BorderLayout(0, JBUI.scale(4)));
        commitTablePanel.add(createCommitHeader(), BorderLayout.NORTH);
        commitTablePanel.add(listScrollPane, BorderLayout.CENTER);
        content.add(commitTablePanel, BorderLayout.CENTER);

        // 底部：生成按钮 + 齿轮配置
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        disableFocusHighlight(generateButton);
        matchGenerateButtonHeight();
        generateButton.addActionListener(e -> onGenerate());
        bottom.add(generateButton);
        JButton settingsBtn = new JButton(AllIcons.General.Gear);
        disableFocusHighlight(settingsBtn);
        settingsBtn.setToolTipText("配置");
        settingsBtn.setBorderPainted(false);
        settingsBtn.setContentAreaFilled(false);
        settingsBtn.setIcon(IconUtil.scale(AllIcons.General.Gear, settingsBtn, ACTION_ICON_SCALE));
        matchIconButtonSize(settingsBtn);
        settingsBtn.addActionListener(e -> showConfigDialog());
        bottom.add(settingsBtn);
        content.add(bottom, BorderLayout.SOUTH);

        setContent(content);

        setDefaultDateRange();
        scanRepositoriesAsync();
    }

    private static DefaultListCellRenderer createCenteredComboRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus
            ) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                return label;
            }
        };
    }

    private void matchRangeComboHeight() {
        Dimension comboSize = rangeCombo.getPreferredSize();
        Insets comboInsets = rangeCombo.getInsets();
        int visibleHeight = dateRangeSelector.getPreferredSize().height;
        int comboHeight = visibleHeight + comboInsets.top + comboInsets.bottom;
        Dimension matchedSize = new Dimension(comboSize.width, comboHeight);
        rangeCombo.setPreferredSize(matchedSize);
        rangeCombo.setMinimumSize(matchedSize);
    }

    private void matchGenerateButtonHeight() {
        Dimension buttonSize = generateButton.getPreferredSize();
        Dimension comboSize = rangeCombo.getPreferredSize();
        Dimension matchedSize = new Dimension(buttonSize.width, comboSize.height);
        generateButton.setPreferredSize(matchedSize);
        generateButton.setMinimumSize(matchedSize);
    }

    private void matchIconButtonSize(AbstractButton button) {
        int size = rangeCombo.getPreferredSize().height;
        Dimension matchedSize = new Dimension(size, size);
        button.setPreferredSize(matchedSize);
        button.setMinimumSize(matchedSize);
    }

    private void scanRepositoriesAsync() {
        long currentScan = ++querySequence;
        repositoriesScanned = false;
        showCenteredStatus("正在扫描 Git 仓库…");
        refreshButton.setEnabled(false);
        generateButton.setEnabled(false);
        selectAllCheckBox.setSelected(false);
        selectAllCheckBox.setEnabled(false);
        updateSelectAllPresentation();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            List<GitRepositoryInfo> scannedRepositories;
            try {
                scannedRepositories = GitTodayCommitsLoader.scanRepositories(project);
            } catch (Throwable ignored) {
                scannedRepositories = new ArrayList<>();
            }
            List<GitRepositoryInfo> finalRepositories = scannedRepositories;
            SwingUtilities.invokeLater(() -> {
                if (currentScan != querySequence) return;
                repositories = finalRepositories;
                updateRepositorySelector();
                repositoriesScanned = true;
                generateButton.setEnabled(true);
                loadCommitsAsync();
            });
        });
    }

    private void updateRepositorySelector() {
        repositoryCombo.removeAllItems();
        repositoryCombo.addItem("全部仓库");
        for (GitRepositoryInfo repository : repositories) {
            repositoryCombo.addItem(repository.getDisplayName());
        }
        repositoryCombo.setSelectedIndex(0);

        boolean multipleRepositories = repositories.size() > 1;
        repositoryCombo.setVisible(multipleRepositories);
        repositoryGap.setVisible(multipleRepositories);
        if (multipleRepositories) {
            repositoryCombo.setPreferredSize(null);
            repositoryCombo.setMinimumSize(null);
            Dimension preferredSize = repositoryCombo.getPreferredSize();
            Dimension rangeSize = rangeCombo.getPreferredSize();
            Dimension matchedSize = new Dimension(preferredSize.width, rangeSize.height);
            repositoryCombo.setPreferredSize(matchedSize);
            repositoryCombo.setMinimumSize(matchedSize);
        }
        repositoryCombo.getParent().revalidate();
        repositoryCombo.getParent().repaint();
    }

    private List<GitRepositoryInfo> getSelectedRepositories() {
        int selectedIndex = repositoryCombo.getSelectedIndex();
        if (repositories.size() <= 1 || selectedIndex <= 0) {
            return new ArrayList<>(repositories);
        }
        int repositoryIndex = selectedIndex - 1;
        if (repositoryIndex >= repositories.size()) return new ArrayList<>();
        return Collections.singletonList(repositories.get(repositoryIndex));
    }

    private JPanel createCommitHeader() {
        JPanel header = new JPanel(new GridBagLayout());
        disableFocusHighlight(selectAllCheckBox);
        selectAllCheckBox.setToolTipText("全选");
        selectAllCheckBox.getAccessibleContext().setAccessibleName("全选");
        selectAllCheckBox.addActionListener(e -> setAllCommitsSelected(selectAllCheckBox.isSelected()));

        JLabel hashHeader = createHeaderLabel("Hash", COL_HASH_WIDTH);
        JLabel timeHeader = createHeaderLabel("提交时间", COL_TIME_WIDTH);
        JLabel messageHeader = createHeaderLabel("提交信息", 0);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(1, 0, 1, 4);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        header.add(selectAllCheckBox, gbc);
        gbc.gridx = 1;
        header.add(hashHeader, gbc);
        gbc.gridx = 2;
        header.add(timeHeader, gbc);
        gbc.gridx = 3;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        header.add(messageHeader, gbc);
        header.setBorder(JBUI.Borders.emptyBottom(2));
        return header;
    }

    private static JLabel createHeaderLabel(String text, int width) {
        JLabel label = new JLabel(text);
        label.setFont(JBFont.label().deriveFont(Collections.singletonMap(
            TextAttribute.WEIGHT,
            TextAttribute.WEIGHT_EXTRABOLD
        )));
        if (width > 0) {
            label.setPreferredSize(new Dimension(width, 22));
            label.setMinimumSize(new Dimension(width, 22));
        }
        return label;
    }

    private void setDefaultDateRange() {
        rangeCombo.setSelectedIndex(0);
        customDateGap.setVisible(false);
        dateRangeSelector.setVisible(false);
        setDateRangeFromCombo();
    }

    private void handleRangeSelection() {
        boolean customRange = isCustomRangeSelected();
        customDateGap.setVisible(customRange);
        dateRangeSelector.setVisible(customRange);
        dateRangeSelector.getParent().revalidate();
        dateRangeSelector.getParent().repaint();
        if (!customRange) {
            setDateRangeFromCombo();
        }
    }

    private boolean isCustomRangeSelected() {
        return rangeCombo.getSelectedIndex() == CUSTOM_RANGE_INDEX;
    }

    private void queryCustomRange() {
        if (isCustomRangeSelected() && repositoriesScanned) {
            loadCommitsAsync();
        }
    }

    private void setDateRangeFromCombo() {
        LocalDate today = LocalDate.now();
        LocalDate start;
        LocalDate end = today;
        switch (rangeCombo.getSelectedIndex()) {
            case 1:
                start = today.minusDays(1);
                end = start;
                break;
            case 2:
                start = today.minusDays(6);
                break;
            case 3:
                start = today.minusMonths(1);
                break;
            default:
                start = today;
                break;
        }
        dateRangeSelector.setRange(start, end);
        if (repositoriesScanned) {
            loadCommitsAsync();
        }
    }

    private void loadCommitsAsync() {
        long currentQuery = ++querySequence;
        List<GitRepositoryInfo> selectedRepositories = getSelectedRepositories();
        LocalDate start = dateRangeSelector.getStartDate();
        LocalDate end = dateRangeSelector.getEndDate();
        long startMillis = start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endMillis = endOfDay(end);

        // 立即显示「加载中」，让用户看到有反应
        showCenteredStatus("正在加载提交记录…");
        listScrollPane.revalidate();
        listScrollPane.repaint();
        refreshButton.setEnabled(false);
        selectAllCheckBox.setSelected(false);
        selectAllCheckBox.setEnabled(false);
        updateSelectAllPresentation();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            List<CommitInfo> commits;
            try {
                commits = GitTodayCommitsLoader.loadCommitsForCurrentUser(selectedRepositories, startMillis, endMillis);
            } catch (Throwable t) {
                commits = new ArrayList<>();
            }
            List<CommitInfo> finalCommits = commits;
            // 回到 EDT 更新列表，不传 ModalityState 避免在某些情况下不执行
            SwingUtilities.invokeLater(() -> {
                if (currentQuery == querySequence) {
                    applyCommitList(finalCommits);
                }
            });
        });
    }

    private void applyCommitList(List<CommitInfo> commits) {
        commitListPanel.removeAll();
        if (commits.isEmpty()) {
            commitListPanel.setLayout(new BorderLayout());
            commitListPanel.add(createEmptyState(), BorderLayout.CENTER);
        } else {
            commitListPanel.setLayout(new BoxLayout(commitListPanel, BoxLayout.Y_AXIS));
            SimpleDateFormat timeFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            boolean showRepositoryName = repositories.size() > 1 && repositoryCombo.getSelectedIndex() == 0;

            for (CommitInfo c : commits) {
                JCheckBox check = new JCheckBox();
                disableFocusHighlight(check);
                check.putClientProperty(KEY_COMMIT_INFO, c);
                check.setSelected(true);
                check.addItemListener(e -> updateSelectAllState());
                String subject = c.getSubject();
                String displayedSubject = showRepositoryName && c.getRepository() != null
                    ? "[" + c.getRepository().getDisplayName() + "] " + subject
                    : subject;

                JLabel hashLabel = new JLabel(c.getShortHash());
                hashLabel.setPreferredSize(new Dimension(COL_HASH_WIDTH, 22));
                hashLabel.setMinimumSize(new Dimension(COL_HASH_WIDTH, 22));

                JLabel timeLabel = new JLabel(timeFmt.format(new Date(c.getCommitTimeMillis())));
                timeLabel.setPreferredSize(new Dimension(COL_TIME_WIDTH, 22));
                timeLabel.setMinimumSize(new Dimension(COL_TIME_WIDTH, 22));

                JLabel msgLabel = new JLabel(displayedSubject != null ? displayedSubject : "");
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
        refreshButton.setEnabled(true);
        updateSelectAllState();
    }

    private JPanel createEmptyState() {
        Color secondaryTextColor = UIManager.getColor("Label.disabledForeground");
        if (secondaryTextColor == null) {
            secondaryTextColor = new Color(128, 128, 128);
        }

        Font textFont = JBFont.label();
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("暂无提交记录");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setFont(textFont.deriveFont(Font.BOLD));
        content.add(title);
        content.add(Box.createVerticalStrut(JBUI.scale(8)));

        JLabel description = new JLabel("请调整时间范围，或检查 Git 仓库与用户配置");
        description.setAlignmentX(Component.CENTER_ALIGNMENT);
        description.setFont(textFont);
        description.setForeground(secondaryTextColor);
        content.add(description);

        JPanel centered = new JPanel(new GridBagLayout());
        centered.add(content);
        return centered;
    }

    private void showCenteredStatus(String message) {
        commitListPanel.removeAll();
        commitListPanel.setLayout(new GridBagLayout());
        JLabel status = new JLabel(message);
        status.setFont(JBFont.label());
        Color secondaryTextColor = UIManager.getColor("Label.disabledForeground");
        if (secondaryTextColor != null) {
            status.setForeground(secondaryTextColor);
        }
        commitListPanel.add(status);
        commitListPanel.revalidate();
        commitListPanel.repaint();
    }

    private void setAllCommitsSelected(boolean selected) {
        for (Component component : commitListPanel.getComponents()) {
            if (!(component instanceof JPanel)) continue;
            for (Component rowComponent : ((JPanel) component).getComponents()) {
                if (rowComponent instanceof JCheckBox) {
                    ((JCheckBox) rowComponent).setSelected(selected);
                    break;
                }
            }
        }
        updateSelectAllState();
    }

    private void updateSelectAllState() {
        int totalCount = 0;
        int selectedCount = 0;
        for (Component component : commitListPanel.getComponents()) {
            if (!(component instanceof JPanel)) continue;
            for (Component rowComponent : ((JPanel) component).getComponents()) {
                if (rowComponent instanceof JCheckBox) {
                    totalCount++;
                    if (((JCheckBox) rowComponent).isSelected()) selectedCount++;
                    break;
                }
            }
        }
        boolean hasCommits = totalCount > 0;
        selectAllCheckBox.setSelected(hasCommits && selectedCount == totalCount);
        selectAllCheckBox.setEnabled(hasCommits);
        updateSelectAllPresentation();
    }

    private void updateSelectAllPresentation() {
        String actionText = selectAllCheckBox.isSelected() ? "取消全选" : "全选";
        selectAllCheckBox.setToolTipText(actionText);
        selectAllCheckBox.getAccessibleContext().setAccessibleName(actionText);
    }

    private static void disableFocusHighlight(AbstractButton button) {
        button.setFocusPainted(false);
        button.setFocusable(false);
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
            String detail = GitTodayCommitsLoader.getCommitDetail(c);
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

    private static long endOfDay(LocalDate date) {
        return date.atTime(23, 59, 59, 999_000_000).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private void showConfigDialog() {
        CodeDiarySettings settings = CodeDiarySettings.getInstance();
        JPanel configPanel = new JPanel(new BorderLayout(0, JBUI.scale(12)));
        configPanel.setBorder(JBUI.Borders.empty(12));

        JBTextArea formatArea = createConfigTextArea(
            settings.getReportFormat() != null ? settings.getReportFormat() : ""
        );
        JBTextArea commitLogFormatArea = createConfigTextArea(
            settings.getGitCommitLogFormat() != null ? settings.getGitCommitLogFormat() : ""
        );
        JBTextArea otherArea = createConfigTextArea(
            settings.getOtherContent() != null ? settings.getOtherContent() : ""
        );

        JTabbedPane tabs = new JTabbedPane(SwingConstants.TOP);
        tabs.addTab(
            "日报格式",
            createConfigTab(
                "AI 将参考此模板，把所选 Git 提交整理成日报；可以直接调整字段、序号和排版。",
                formatArea
            )
        );
        tabs.addTab(
            "Git 日志格式",
            createConfigTab(
                "点击 Commit 面板中的 CodeDiary 图标时，AI 将按此格式生成并回填提交消息。",
                commitLogFormatArea
            )
        );
        tabs.addTab(
            "其他补充内容",
            createConfigTab(
                "补充语言、命名、内容风格等个性化要求，生成日报时会一并发送给 AI。",
                otherArea
            )
        );
        configPanel.add(tabs, BorderLayout.CENTER);

        JButton saveBtn = new JButton("保存");
        JButton closeBtn = new JButton("关闭");
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, JBUI.scale(8), 0));
        south.setBorder(JBUI.Borders.emptyTop(2));
        south.add(saveBtn);
        south.add(closeBtn);
        configPanel.add(south, BorderLayout.SOUTH);

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "配置 CodeDiary", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.getContentPane().add(configPanel);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.getRootPane().setDefaultButton(saveBtn);
        dialog.setMinimumSize(new Dimension(JBUI.scale(640), JBUI.scale(400)));

        saveBtn.addActionListener(e -> {
            settings.setReportFormat(formatArea.getText().trim());
            settings.setGitCommitLogFormat(commitLogFormatArea.getText().trim());
            settings.setOtherContent(otherArea.getText().trim());
            dialog.dispose();
        });
        closeBtn.addActionListener(e -> dialog.dispose());

        dialog.pack();
        dialog.setSize(new Dimension(JBUI.scale(700), JBUI.scale(460)));
        dialog.setLocationRelativeTo(this);
        SwingUtilities.invokeLater(formatArea::requestFocusInWindow);
        dialog.setVisible(true);
    }

    private static JBTextArea createConfigTextArea(String text) {
        JBTextArea area = new JBTextArea(text, 12, 48);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setMargin(JBUI.insets(10, 12));
        area.getAccessibleContext().setAccessibleDescription("CodeDiary 配置内容编辑区");
        return area;
    }

    private static JPanel createConfigTab(String hint, JTextArea area) {
        JPanel tab = new JPanel(new BorderLayout(0, JBUI.scale(10)));
        tab.setBorder(JBUI.Borders.empty(14, 12, 12, 12));

        JLabel hintLabel = new JLabel(hint);
        Color hintColor = UIManager.getColor("Label.disabledForeground");
        hintLabel.setForeground(hintColor != null ? hintColor : new Color(128, 128, 128));
        tab.add(hintLabel, BorderLayout.NORTH);

        JBScrollPane scrollPane = new JBScrollPane(area);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        tab.add(scrollPane, BorderLayout.CENTER);
        return tab;
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
        Map<GitRepositoryInfo, List<CommitInfo>> commitsByRepository = groupCommitsByRepository(selected);
        if (commitsByRepository.isEmpty()) {
            JOptionPane.showMessageDialog(this, "无法确定所选提交所属的 Git 仓库。", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        CodeDiarySettings settings = CodeDiarySettings.getInstance();
        String machineCode = settings.getMachineCode();
        if (machineCode == null || machineCode.isEmpty()) {
            machineCode = java.util.UUID.randomUUID().toString();
            settings.setMachineCode(machineCode);
        }
        String finalMachineCode = machineCode;
        String reportFormat = settings.getReportFormat() != null ? settings.getReportFormat() : "";
        String otherContent = settings.getOtherContent() != null ? settings.getOtherContent() : "";

        generateButton.setEnabled(false);
        generateButton.setText("生成中...");
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            String result = generateReportsByRepository(
                commitsByRepository,
                finalMachineCode,
                reportFormat,
                otherContent
            );
            String finalResult = result;
            SwingUtilities.invokeLater(() -> {
                generateButton.setText("生成");
                generateButton.setEnabled(true);
                showGenerateResultDialog(finalResult);
            });
        });
    }

    private Map<GitRepositoryInfo, List<CommitInfo>> groupCommitsByRepository(List<CommitInfo> commits) {
        Map<GitRepositoryInfo, List<CommitInfo>> grouped = new LinkedHashMap<>();
        for (GitRepositoryInfo repository : repositories) {
            List<CommitInfo> repositoryCommits = new ArrayList<>();
            for (CommitInfo commit : commits) {
                if (repository.equals(commit.getRepository())) {
                    repositoryCommits.add(commit);
                }
            }
            if (!repositoryCommits.isEmpty()) {
                grouped.put(repository, repositoryCommits);
            }
        }
        return grouped;
    }

    private String generateReportsByRepository(
        Map<GitRepositoryInfo, List<CommitInfo>> commitsByRepository,
        String machineCode,
        String reportFormat,
        String otherContent
    ) {
        List<String> reports = new ArrayList<>();
        for (Map.Entry<GitRepositoryInfo, List<CommitInfo>> entry : commitsByRepository.entrySet()) {
            GitRepositoryInfo repository = entry.getKey();
            GenerateApiClient.GenerateRequest request = new GenerateApiClient.GenerateRequest();
            request.machineCode = machineCode;
            request.clientVersion = "26.01.30";
            request.projectName = repository.getName();
            request.commits = loadCommitDetails(entry.getValue());
            request.reportFormat = reportFormat;
            request.otherContent = otherContent;

            try {
                String report = GenerateApiClient.generate(request);
                reports.add(report != null ? report.trim() : "");
            } catch (Throwable t) {
                String message = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
                reports.add("项目名称：" + repository.getName() + "\n\n生成失败：" + message);
            }
        }
        return String.join("\n\n", reports);
    }

    private static List<String> loadCommitDetails(List<CommitInfo> commits) {
        List<String> details = new ArrayList<>();
        SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        for (CommitInfo commit : commits) {
            String detail = GitTodayCommitsLoader.getCommitDetail(commit);
            if (detail == null || detail.startsWith("读取失败:") || detail.startsWith("未找到提交:")) {
                detail = commit.getShortHash() + " "
                    + timeFormat.format(new Date(commit.getCommitTimeMillis())) + " "
                    + commit.getSubject();
            }
            details.add(detail);
        }
        return details;
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
        disableFocusHighlight(copyBtn);
        disableFocusHighlight(closeBtn);
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
