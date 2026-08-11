package fun.xuewei.codediary;

import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/** IntelliJ 风格的日期范围选择器。 */
final class DateRangeSelector extends JComponent {

    static final String RANGE_PROPERTY = "dateRange";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int HORIZONTAL_PADDING = 12;

    private LocalDate startDate;
    private LocalDate endDate;
    private String displayText;
    private int preferredHeight;

    DateRangeSelector(LocalDate startDate, LocalDate endDate) {
        setFocusable(false);
        setOpaque(false);
        setToolTipText("选择日期范围");
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    showCalendarPopup();
                }
            }
        });
        setRange(startDate, endDate);
    }

    void matchStyle(JComboBox<?> comboBox) {
        Objects.requireNonNull(comboBox);
        preferredHeight = comboBox.getPreferredSize().height;
        setFont(comboBox.getFont());
        setForeground(comboBox.getForeground());
        setBackground(comboBox.getBackground());
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics metrics = getFontMetrics(getFont());
        String text = displayText != null ? displayText : "0000-00-00  至  0000-00-00";
        int width = metrics.stringWidth(text) + JBUI.scale(HORIZONTAL_PADDING * 2);
        int height = preferredHeight > 0
            ? preferredHeight
            : new JComboBox<>().getPreferredSize().height;
        return new Dimension(width, height);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            float borderInset = JBUI.scale(1);
            int arc = UIManager.getInt("Component.arc");
            if (arc <= 0) arc = JBUI.scale(5);

            Color background = JBColor.namedColor(
                "ComboBox.nonEditableBackground",
                JBColor.namedColor("ComboBox.darcula.nonEditableBackground", new JBColor(0xfcfcfc, 0x3c3f41))
            );
            Color border = JBColor.namedColor("Component.borderColor", new JBColor(0xbfbfbf, 0x5e6060));
            RoundRectangle2D shape = new RoundRectangle2D.Float(
                borderInset,
                borderInset,
                width - borderInset * 2,
                height - borderInset * 2,
                arc,
                arc
            );

            g.setColor(background);
            g.fill(shape);
            g.setColor(border);
            g.setStroke(new BasicStroke(JBUI.scale(1)));
            g.draw(shape);

            g.setFont(getFont());
            g.setColor(getForeground());
            FontMetrics metrics = g.getFontMetrics();
            int textX = (width - metrics.stringWidth(displayText)) / 2;
            int textY = (height - metrics.getHeight()) / 2 + metrics.getAscent();
            g.drawString(displayText, textX, textY);
        } finally {
            g.dispose();
        }
    }

    LocalDate getStartDate() {
        return startDate;
    }

    LocalDate getEndDate() {
        return endDate;
    }

    void setRange(LocalDate newStartDate, LocalDate newEndDate) {
        Objects.requireNonNull(newStartDate);
        Objects.requireNonNull(newEndDate);
        if (newStartDate.isAfter(newEndDate)) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期");
        }

        String oldRange = formatRange(startDate, endDate);
        startDate = newStartDate;
        endDate = newEndDate;
        String newRange = formatRange(startDate, endDate);
        displayText = newRange;
        revalidate();
        repaint();
        if (oldRange != null && !oldRange.equals(newRange)) {
            firePropertyChange(RANGE_PROPERTY, oldRange, newRange);
        }
    }

    private static String formatRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) return null;
        return DATE_FORMATTER.format(startDate) + "  至  " + DATE_FORMATTER.format(endDate);
    }

    private void showCalendarPopup() {
        JPopupMenu popup = new JPopupMenu();
        popup.setLayout(new BorderLayout());
        popup.add(new RangeCalendarPanel(startDate, endDate, (selectedStart, selectedEnd) -> {
            setRange(selectedStart, selectedEnd);
            popup.setVisible(false);
        }), BorderLayout.CENTER);
        popup.show(this, 0, getHeight());
    }

    private interface RangeSelectionHandler {
        void rangeSelected(LocalDate startDate, LocalDate endDate);
    }

    private static final class RangeCalendarPanel extends JPanel {

        private static final String[] WEEK_DAYS = {"一", "二", "三", "四", "五", "六", "日"};

        private final LocalDate committedStartDate;
        private final LocalDate committedEndDate;
        private final RangeSelectionHandler selectionHandler;
        private final JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
        private final JLabel selectionHint = new JLabel("请选择开始日期", SwingConstants.CENTER);
        private final JPanel daysPanel = new JPanel(new GridLayout(0, 7, JBUI.scale(2), JBUI.scale(2)));
        private YearMonth visibleMonth;
        private LocalDate draftStartDate;
        private boolean selectingEndDate;

        RangeCalendarPanel(LocalDate startDate, LocalDate endDate, RangeSelectionHandler selectionHandler) {
            super(new BorderLayout(0, JBUI.scale(6)));
            this.committedStartDate = startDate;
            this.committedEndDate = endDate;
            this.selectionHandler = selectionHandler;
            this.visibleMonth = YearMonth.from(startDate);
            setBorder(JBUI.Borders.empty(8));
            add(createHeader(), BorderLayout.NORTH);
            add(daysPanel, BorderLayout.CENTER);
            selectionHint.setForeground(UIManager.getColor("Label.disabledForeground"));
            add(selectionHint, BorderLayout.SOUTH);
            rebuildDays();
        }

        private JComponent createHeader() {
            JPanel header = new JPanel(new BorderLayout());
            JButton previous = createIconButton(AllIcons.General.ChevronLeft, "上个月");
            JButton next = createIconButton(AllIcons.General.ChevronRight, "下个月");
            previous.addActionListener(e -> changeMonth(-1));
            next.addActionListener(e -> changeMonth(1));
            monthLabel.setFont(monthLabel.getFont().deriveFont(Font.BOLD));
            header.add(previous, BorderLayout.WEST);
            header.add(monthLabel, BorderLayout.CENTER);
            header.add(next, BorderLayout.EAST);
            return header;
        }

        private void changeMonth(int months) {
            visibleMonth = visibleMonth.plusMonths(months);
            rebuildDays();
        }

        private void handleDateClick(LocalDate clickedDate) {
            if (!selectingEndDate) {
                draftStartDate = clickedDate;
                selectingEndDate = true;
                selectionHint.setText("请选择结束日期");
                rebuildDays();
                return;
            }

            if (clickedDate.isBefore(draftStartDate)) {
                draftStartDate = clickedDate;
                selectionHint.setText("已重新选择开始日期，请选择结束日期");
                rebuildDays();
                return;
            }

            selectionHandler.rangeSelected(draftStartDate, clickedDate);
        }

        private void rebuildDays() {
            daysPanel.removeAll();
            monthLabel.setText(visibleMonth.getYear() + " 年 " + visibleMonth.getMonthValue() + " 月");

            for (String weekDay : WEEK_DAYS) {
                JLabel label = new JLabel(weekDay, SwingConstants.CENTER);
                label.setForeground(UIManager.getColor("Label.disabledForeground"));
                daysPanel.add(label);
            }

            int leadingBlankDays = visibleMonth.atDay(1).getDayOfWeek().getValue() - 1;
            for (int i = 0; i < leadingBlankDays; i++) {
                daysPanel.add(Box.createGlue());
            }

            for (int day = 1; day <= visibleMonth.lengthOfMonth(); day++) {
                LocalDate value = visibleMonth.atDay(day);
                JButton dayButton = new JButton(String.valueOf(day));
                configureDayButton(dayButton, value);
                dayButton.addActionListener(e -> handleDateClick(value));
                daysPanel.add(dayButton);
            }

            daysPanel.revalidate();
            daysPanel.repaint();
        }

        private void configureDayButton(JButton button, LocalDate value) {
            int size = JBUI.scale(28);
            button.setPreferredSize(new Dimension(size, size));
            button.setMargin(JBUI.emptyInsets());
            button.setFocusPainted(false);
            button.setFocusable(false);
            button.setBorderPainted(false);

            LocalDate displayedStart = selectingEndDate ? draftStartDate : committedStartDate;
            LocalDate displayedEnd = selectingEndDate ? null : committedEndDate;
            boolean isEndpoint = value.equals(displayedStart) || value.equals(displayedEnd);
            boolean isWithinRange = displayedEnd != null
                && value.isAfter(displayedStart)
                && value.isBefore(displayedEnd);

            if (isEndpoint) {
                applySelectionColors(button, 1.0f);
            } else if (isWithinRange) {
                applySelectionColors(button, 0.35f);
            } else {
                button.setContentAreaFilled(false);
                if (value.equals(LocalDate.now())) {
                    button.setFont(button.getFont().deriveFont(Font.BOLD));
                }
            }
        }

        private static void applySelectionColors(JButton button, float selectionRatio) {
            Color selection = UIManager.getColor("List.selectionBackground");
            Color background = UIManager.getColor("Panel.background");
            if (selection == null) selection = button.getBackground();
            if (background == null) background = button.getBackground();
            button.setOpaque(true);
            button.setContentAreaFilled(true);
            button.setBackground(blend(background, selection, selectionRatio));
            if (selectionRatio >= 1.0f) {
                Color foreground = UIManager.getColor("List.selectionForeground");
                if (foreground != null) button.setForeground(foreground);
            }
        }

        private static Color blend(Color base, Color overlay, float overlayRatio) {
            float baseRatio = 1.0f - overlayRatio;
            return new Color(
                Math.round(base.getRed() * baseRatio + overlay.getRed() * overlayRatio),
                Math.round(base.getGreen() * baseRatio + overlay.getGreen() * overlayRatio),
                Math.round(base.getBlue() * baseRatio + overlay.getBlue() * overlayRatio)
            );
        }

        private static JButton createIconButton(Icon icon, String tooltip) {
            JButton button = new JButton(icon);
            button.setToolTipText(tooltip);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.setFocusable(false);
            button.setContentAreaFilled(false);
            int size = JBUI.scale(24);
            button.setPreferredSize(new Dimension(size, size));
            return button;
        }
    }
}
