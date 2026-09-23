package com.placement.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Student Dashboard - Placement Eligibility Portal
 *
 * Frontend-only implementation, intentionally coordinated with the existing
 * TPO/TPC dashboards:
 * - 1280 x 760 fixed window
 * - same green / off-white theme
 * - same sidebar proportions and typography
 * - CardLayout based navigation
 *
 * Student-specific resume/application actions are kept UI-local because the
 * supplied project did not include a completed student socket protocol for them.
 */
public class StudentDashboard extends JFrame {

    // ================= THEME (matches TPO/TPC) =================
    private static final Color PRIMARY_GREEN = new Color(27, 117, 61);
    private static final Color DARK_GREEN = new Color(20, 92, 48);
    private static final Color BG = new Color(247, 248, 245);
    private static final Color LIGHT_GREEN = new Color(232, 245, 236);
    private static final Color TEXT = new Color(38, 50, 43);
    private static final Color MUTED = new Color(105, 115, 108);
    private static final Color BORDER = new Color(214, 220, 215);
    private static final Color SUCCESS = new Color(31, 119, 65);
    private static final Color WARNING = new Color(154, 104, 18);
    private static final Color DANGER = new Color(168, 55, 55);

    private static final Font FONT_HEADING = new Font("SansSerif", Font.BOLD, 24);
    private static final Font FONT_SECTION = new Font("SansSerif", Font.BOLD, 15);
    private static final Font FONT_LABEL = new Font("SansSerif", Font.BOLD, 12);
    private static final Font FONT_BODY = new Font("SansSerif", Font.PLAIN, 13);

    private final String studentName;
    private final String prn;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);

    private SolidMenuButton dashboardButton;
    private SolidMenuButton drivesButton;
    private SolidMenuButton applicationsButton;
    private SolidMenuButton notificationsButton;
    private SolidMenuButton resumeButton;
    private SolidMenuButton profileButton;
    private SolidMenuButton statusButton;
    private SolidMenuButton selectedButton;

    // Dashboard values / labels that can later be bound to server data
    private JLabel eligibleValue;
    private JLabel appliedValue;
    private JLabel shortlistedValue;
    private JLabel deadlineValue;

    private final List<Drive> drives = new ArrayList<>();
    private final List<Application> applications = new ArrayList<>();
    private final List<Notice> notices = new ArrayList<>();

    public StudentDashboard(String studentName, String prn) {
        this.studentName = (studentName == null || studentName.isBlank()) ? "Student" : studentName;
        this.prn = (prn == null || prn.isBlank()) ? "PRN000" : prn;

        setTitle("Placement Eligibility Portal - Student Dashboard");
        setSize(1280, 760);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        loadMockData();
        initUI();
    }

    // Convenience constructor for standalone testing.
    public StudentDashboard() {
        this("Gayatri Dethe", "B24CE1021");
    }

    private void initUI() {
        JPanel main = new JPanel(new BorderLayout());
        main.add(buildSidebar(), BorderLayout.WEST);

        contentPanel.setBackground(BG);
        contentPanel.add(buildDashboardPage(), "DASHBOARD");
        contentPanel.add(buildDrivesPage(), "DRIVES");
        contentPanel.add(buildApplicationsPage(), "APPLICATIONS");
        contentPanel.add(buildNotificationsPage(), "NOTIFICATIONS");
        contentPanel.add(buildResumePage(), "RESUME");
        contentPanel.add(buildProfilePage(), "PROFILE");
        contentPanel.add(buildStatusPage(), "STATUS");

        main.add(buildMainArea(), BorderLayout.CENTER);
        add(main);

        showPage("DASHBOARD", dashboardButton, "Student Dashboard");
    }

    // ================= SIDEBAR =================

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(245, 760));
        sidebar.setBackground(DARK_GREEN);
        sidebar.setBorder(new EmptyBorder(30, 18, 20, 18));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        JLabel logo = new JLabel("PLACEMENT");
        logo.setFont(new Font("SansSerif", Font.BOLD, 23));
        logo.setForeground(Color.WHITE);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(logo);

        JLabel portal = new JLabel("ELIGIBILITY PORTAL");
        portal.setFont(new Font("SansSerif", Font.BOLD, 11));
        portal.setForeground(new Color(220, 245, 225));
        portal.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(portal);
        sidebar.add(Box.createVerticalStrut(35));

        dashboardButton = menu(sidebar, "Dashboard", e -> showPage("DASHBOARD", dashboardButton, "Student Dashboard"));
        drivesButton = menu(sidebar, "Placement Drives", e -> showPage("DRIVES", drivesButton, "Placement Drives"));
        applicationsButton = menu(sidebar, "My Applications", e -> showPage("APPLICATIONS", applicationsButton, "My Applications"));
        notificationsButton = menu(sidebar, "Notifications", e -> showPage("NOTIFICATIONS", notificationsButton, "Notifications"));
        resumeButton = menu(sidebar, "My Resume", e -> showPage("RESUME", resumeButton, "My Resume"));
        profileButton = menu(sidebar, "My Profile", e -> showPage("PROFILE", profileButton, "My Profile"));
        statusButton = menu(sidebar, "Placement Status", e -> showPage("STATUS", statusButton, "Placement Status"));

        sidebar.add(Box.createVerticalGlue());

        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(78, 135, 91));
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(separator);
        sidebar.add(Box.createVerticalStrut(15));

        JLabel user = new JLabel("<html><b>STUDENT</b><br>" + escapeHtml(studentName)
                + "<br>" + escapeHtml(prn) + "</html>");
        user.setForeground(Color.WHITE);
        user.setFont(new Font("SansSerif", Font.PLAIN, 12));
        user.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(user);
        sidebar.add(Box.createVerticalStrut(15));

        SolidMenuButton logout = new SolidMenuButton("Logout");
        logout.setPreferredSize(new Dimension(209, 38));
        logout.setMinimumSize(new Dimension(209, 38));
        logout.setMaximumSize(new Dimension(209, 38));
        logout.setAlignmentX(Component.LEFT_ALIGNMENT);
        logout.setHorizontalAlignment(SwingConstants.LEFT);
        logout.setBorderColor(new Color(100, 175, 120));
        logout.addActionListener(e -> handleLogout());
        sidebar.add(logout);

        return sidebar;
    }

    private SolidMenuButton menu(JPanel sidebar, String text, ActionListener action) {
        SolidMenuButton button = new SolidMenuButton(text);
        button.setPreferredSize(new Dimension(209, 43));
        button.setMinimumSize(new Dimension(209, 43));
        button.setMaximumSize(new Dimension(209, 43));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.addActionListener(e -> action.actionPerformed(e));
        sidebar.add(button);
        sidebar.add(Box.createVerticalStrut(4));
        return button;
    }

    private void handleLogout() {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to logout?",
                "Logout",
                JOptionPane.YES_NO_OPTION
        );
        if (choice == JOptionPane.YES_OPTION) {
            dispose();
            // Login screen can be reopened by the application controller.
            // Keep this dashboard independent from the login implementation.
        }
    }

    private static class SolidMenuButton extends JButton {
        private boolean selectedVisual;
        private Color borderColor = DARK_GREEN;

        SolidMenuButton(String text) {
            super(text);
            setFont(new Font("SansSerif", Font.BOLD, 12));
            setForeground(Color.WHITE);
            setBackground(DARK_GREEN);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setRolloverEnabled(false);
            setMargin(new Insets(0, 14, 0, 8));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        void setSelectedVisual(boolean value) {
            selectedVisual = value;
            repaint();
        }

        void setBorderColor(Color color) {
            borderColor = color;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(DARK_GREEN);
            g2.fillRect(0, 0, getWidth(), getHeight());

            if (selectedVisual) {
                g2.setColor(PRIMARY_GREEN);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }

            if (borderColor != DARK_GREEN || selectedVisual) {
                g2.setColor(selectedVisual ? PRIMARY_GREEN : borderColor);
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            }

            g2.setColor(Color.WHITE);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int x = 14;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(getText(), x, y);
            g2.dispose();
        }
    }

    // ================= MAIN AREA =================

    private JPanel buildMainArea() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(20, 25, 18, 25));

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        pageTitle = new JLabel("Student Dashboard");
        pageTitle.setFont(FONT_HEADING);
        pageTitle.setForeground(TEXT);

        JLabel subtitle = new JLabel("Placement & Recruitment");
        subtitle.setForeground(MUTED);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));

        titlePanel.add(pageTitle);
        titlePanel.add(Box.createVerticalStrut(3));
        titlePanel.add(subtitle);

        header.add(titlePanel, BorderLayout.WEST);

        JLabel profile = new JLabel("<html><b>" + escapeHtml(studentName)
                + "</b><br><font color='#69736C'>" + escapeHtml(prn) + "</font></html>");
        profile.setHorizontalAlignment(SwingConstants.RIGHT);
        header.add(profile, BorderLayout.EAST);

        wrapper.add(header, BorderLayout.NORTH);

        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.setBackground(BG);
        contentWrapper.setBorder(new EmptyBorder(20, 25, 25, 25));
        contentWrapper.add(contentPanel, BorderLayout.CENTER);
        wrapper.add(contentWrapper, BorderLayout.CENTER);

        return wrapper;
    }

    private JLabel pageTitle;

    private void showPage(String card, JButton button, String title) {
        if (selectedButton instanceof SolidMenuButton) {
            ((SolidMenuButton) selectedButton).setSelectedVisual(false);
        }
        selectedButton = (SolidMenuButton) button;
        selectedButton.setSelectedVisual(true);
        pageTitle.setText(title);
        cardLayout.show(contentPanel, card);
    }

    // ================= DASHBOARD =================

    private JPanel buildDashboardPage() {
        JPanel root = new JPanel(new BorderLayout(0, 20));
        root.setBackground(BG);

        JPanel top = new JPanel(new BorderLayout(0, 15));
        top.setOpaque(false);

        JPanel welcome = new JPanel();
        welcome.setOpaque(false);
        welcome.setLayout(new BoxLayout(welcome, BoxLayout.Y_AXIS));

        JLabel greet = new JLabel("Welcome back, " + studentName);
        greet.setFont(new Font("SansSerif", Font.BOLD, 19));
        greet.setForeground(TEXT);
        welcome.add(greet);

        JLabel desc = new JLabel("Track your placement opportunities, applications and upcoming deadlines.");
        desc.setFont(FONT_BODY);
        desc.setForeground(MUTED);
        welcome.add(Box.createVerticalStrut(4));
        welcome.add(desc);
        top.add(welcome, BorderLayout.NORTH);

        JPanel stats = new JPanel(new GridLayout(1, 4, 15, 0));
        stats.setOpaque(false);

        eligibleValue = statCard(stats, "Eligible Drives", "6", "Available to apply");
        appliedValue = statCard(stats, "Applied", "4", "Active applications");
        shortlistedValue = statCard(stats, "Shortlisted", "2", "Current shortlist");
        deadlineValue = statCard(stats, "Upcoming Deadlines", "3", "Within next 10 days");

        top.add(stats, BorderLayout.CENTER);
        root.add(top, BorderLayout.NORTH);

        JPanel lower = new JPanel(new GridLayout(1, 2, 20, 0));
        lower.setOpaque(false);
        lower.add(buildEligibleDrivesCard());
        lower.add(buildUpcomingCard());
        root.add(lower, BorderLayout.CENTER);

        return root;
    }

    private JLabel statCard(JPanel parent, String name, String value, String note) {
        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(16, 18, 16, 18)
        ));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel label = new JLabel(name);
        label.setFont(FONT_LABEL);
        label.setForeground(MUTED);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        valueLabel.setForeground(PRIMARY_GREEN);
        valueLabel.setBorder(new EmptyBorder(7, 0, 2, 0));

        JLabel desc = new JLabel(note);
        desc.setFont(new Font("SansSerif", Font.PLAIN, 11));
        desc.setForeground(MUTED);

        card.add(label);
        card.add(valueLabel);
        card.add(desc);
        parent.add(card);
        return valueLabel;
    }

    private JPanel buildEligibleDrivesCard() {
        JPanel card = whiteSection("Eligible Placement Drives");

        String[] columns = {"Company", "Role", "Package", "Deadline", "Action"};
        Object[][] rows = {
                {"Infosys", "Software Engineer", "6.5 LPA", "12 Sep", "Apply"},
                {"TCS", "Graduate Engineer", "7.2 LPA", "15 Sep", "Apply"},
                {"Deloitte", "Analyst", "8.0 LPA", "18 Sep", "Apply"},
                {"Accenture", "Associate", "6.0 LPA", "20 Sep", "Apply"}
        };

        JTable table = createTable(columns, rows);
        table.getColumnModel().getColumn(4).setCellRenderer(new ActionTextRenderer());
        card.add(new JScrollPane(table), BorderLayout.CENTER);
        return card;
    }

    private JPanel buildUpcomingCard() {
        JPanel card = whiteSection("Upcoming Deadlines");

        Object[][] rows = {
                {"Infosys", "Registration", "12 Sep 2026", "2 days"},
                {"TCS", "Application", "15 Sep 2026", "5 days"},
                {"Deloitte", "Registration", "18 Sep 2026", "8 days"},
                {"Amazon", "Application", "20 Sep 2026", "10 days"}
        };
        String[] columns = {"Company", "Type", "Deadline", "Remaining"};

        JTable table = createTable(columns, rows);
        card.add(new JScrollPane(table), BorderLayout.CENTER);
        return card;
    }

    // ================= PLACEMENT DRIVES =================

    private JPanel buildDrivesPage() {
        JPanel root = pageContainer();

        JPanel header = simpleHeader(
                "Placement Drives",
                "Placement and internship opportunities shared through the placement portal."
        );
        root.add(header, BorderLayout.NORTH);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchPanel.setOpaque(false);

        JTextField search = new JTextField(22);
        search.setPreferredSize(new Dimension(220, 30));
        search.setToolTipText("Search company or role");

        JComboBox<String> type = new JComboBox<>(new String[]{"All Types", "Placement", "Internship"});
        type.setPreferredSize(new Dimension(125, 30));

        JCheckBox eligibleOnly = new JCheckBox("Eligible Only");
        eligibleOnly.setOpaque(false);
        eligibleOnly.setFont(FONT_BODY);

        JButton searchButton = primaryButton("Search");
        searchPanel.add(search);
        searchPanel.add(type);
        searchPanel.add(eligibleOnly);
        searchPanel.add(searchButton);

        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(header, BorderLayout.NORTH);
        north.add(searchPanel, BorderLayout.SOUTH);
        root.add(north, BorderLayout.NORTH);

        String[] columns = {"Company", "Role", "Type", "Package", "Min CGPA", "Backlogs", "Deadline", "Eligibility"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (Drive d : drives) {
            model.addRow(new Object[]{d.company, d.role, d.type, d.packageText,
                    d.minCgpa, d.maxBacklogs, d.deadline, d.eligible ? "Eligible" : "Not Eligible"});
        }

        JTable table = new JTable(model);
        styleTable(table);
        table.getColumnModel().getColumn(7).setCellRenderer(new EligibilityRenderer());

        JButton viewButton = primaryButton("View / Apply");
        viewButton.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Select a placement drive first.", "No drive selected", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String company = String.valueOf(table.getValueAt(row, 0));
            String role = String.valueOf(table.getValueAt(row, 1));
            showDriveDialog(company, role);
        });

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 10));
        actionRow.setOpaque(false);
        actionRow.add(viewButton);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        center.add(actionRow, BorderLayout.SOUTH);
        root.add(center, BorderLayout.CENTER);

        return root;
    }

    private void showDriveDialog(String company, String role) {
        String message = "<html><b>" + company + " — " + role + "</b><br><br>"
                + "Eligibility is based on the criteria received from the placement team.<br>"
                + "Your profile, CGPA, branch and backlog status are checked before application.<br><br>"
                + "Selection process: Aptitude → GD → Interview<br>"
                + "Resume: Available</html>";

        int choice = JOptionPane.showConfirmDialog(
                this,
                message,
                "Placement Drive Details",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.INFORMATION_MESSAGE
        );

        if (choice == JOptionPane.OK_OPTION) {
            JOptionPane.showMessageDialog(this,
                    "Application submitted successfully (demo frontend).",
                    "Application Submitted",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // ================= APPLICATIONS =================

    private JPanel buildApplicationsPage() {
        JPanel root = pageContainer();
        root.add(simpleHeader("My Applications", "Track the current stage and status of every application."), BorderLayout.NORTH);

        String[] columns = {"Company", "Role", "Applied On", "Current Stage", "Status"};
        Object[][] rows = new Object[applications.size()][5];
        for (int i = 0; i < applications.size(); i++) {
            Application a = applications.get(i);
            rows[i] = new Object[]{a.company, a.role, a.appliedOn, a.stage, a.status};
        }

        JTable table = createTable(columns, rows);
        table.getColumnModel().getColumn(4).setCellRenderer(new StatusRenderer());

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(new JScrollPane(table), BorderLayout.CENTER);

        JButton details = primaryButton("View Application Details");
        details.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Select an application first.", "No application selected", JOptionPane.WARNING_MESSAGE);
                return;
            }
            JOptionPane.showMessageDialog(this,
                    "Application Timeline\n\nApplied ✓\nEligibility Verified ✓\n"
                            + "Current Stage: " + table.getValueAt(row, 3)
                            + "\nStatus: " + table.getValueAt(row, 4),
                    "Application Details",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        JPanel action = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 10));
        action.setOpaque(false);
        action.add(details);
        center.add(action, BorderLayout.SOUTH);
        root.add(center, BorderLayout.CENTER);
        return root;
    }

    // ================= NOTIFICATIONS =================

    private JPanel buildNotificationsPage() {
        JPanel root = pageContainer();
        root.add(simpleHeader("Notifications", "Eligibility alerts, placement updates and deadline reminders."), BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setBackground(Color.WHITE);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(new EmptyBorder(6, 8, 6, 8));

        for (Notice n : notices) {
            addNotice(list, n);
        }

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        root.add(scroll, BorderLayout.CENTER);
        return root;
    }

    private void addNotice(JPanel parent, Notice n) {
        JPanel item = new JPanel(new BorderLayout());
        item.setBackground(Color.WHITE);
        item.setBorder(new CompoundBorder2());
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(n.title);
        title.setFont(FONT_LABEL);
        title.setForeground(TEXT);

        JLabel body = new JLabel(n.body);
        body.setFont(FONT_BODY);
        body.setForeground(MUTED);

        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(body);

        JLabel date = new JLabel(n.date);
        date.setFont(new Font("SansSerif", Font.PLAIN, 11));
        date.setForeground(MUTED);

        item.add(left, BorderLayout.CENTER);
        item.add(date, BorderLayout.EAST);
        parent.add(item);
        parent.add(Box.createVerticalStrut(4));
    }

    // ================= RESUME =================

    private JPanel buildResumePage() {
        JPanel root = pageContainer();
        root.add(simpleHeader("My Resume", "Keep your latest resume ready before applying to placement drives."), BorderLayout.NORTH);

        JPanel card = new JPanel(new BorderLayout(20, 20));
        card.setBackground(Color.WHITE);
        card.setBorder(new EmptyBorder(25, 25, 25, 25));

        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));

        JLabel file = new JLabel("resume_gayatri.pdf");
        file.setFont(new Font("SansSerif", Font.BOLD, 17));
        file.setForeground(TEXT);

        JLabel uploaded = new JLabel("Uploaded: 08 Sep 2026");
        uploaded.setForeground(MUTED);
        uploaded.setFont(FONT_BODY);

        JLabel ready = new JLabel("Ready for applications");
        ready.setForeground(SUCCESS);
        ready.setFont(FONT_LABEL);

        details.add(file);
        details.add(Box.createVerticalStrut(7));
        details.add(uploaded);
        details.add(Box.createVerticalStrut(12));
        details.add(ready);

        card.add(details, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton view = primaryButton("View Resume");
        JButton update = primaryButton("Update Resume");
        actions.add(view);
        actions.add(update);
        card.add(actions, BorderLayout.SOUTH);

        view.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Resume preview would open here (frontend placeholder).",
                "Resume",
                JOptionPane.INFORMATION_MESSAGE));
        update.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "File chooser / upload API will be connected here.",
                "Update Resume",
                JOptionPane.INFORMATION_MESSAGE));

        root.add(card, BorderLayout.CENTER);
        return root;
    }

    // ================= PROFILE =================

    private JPanel buildProfilePage() {
        JPanel root = pageContainer();
        root.add(simpleHeader("My Profile", "Your academic and placement profile used for eligibility checks."), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(4, 2, 18, 18));
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(25, 25, 25, 25));

        addField(form, "Name", studentName);
        addField(form, "PRN", prn);
        addField(form, "Course", "B.E. Computer Engineering");
        addField(form, "Department", "Computer Engineering");
        addField(form, "CGPA", "8.72");
        addField(form, "10th Percentage", "92%");
        addField(form, "12th Percentage", "88%");
        addField(form, "Backlogs", "0");

        root.add(form, BorderLayout.CENTER);

        JPanel skills = new JPanel(new BorderLayout());
        skills.setBackground(Color.WHITE);
        skills.setBorder(new EmptyBorder(0, 25, 25, 25));
        JLabel skillLabel = new JLabel("Skills");
        skillLabel.setFont(FONT_LABEL);
        skillLabel.setForeground(MUTED);
        JLabel skillValue = new JLabel("C++, Java, HTML/CSS/JS, SQL, React");
        skillValue.setFont(FONT_BODY);
        skillValue.setForeground(TEXT);
        skills.add(skillLabel, BorderLayout.NORTH);
        skills.add(skillValue, BorderLayout.CENTER);
        root.add(skills, BorderLayout.SOUTH);

        return root;
    }

    private void addField(JPanel parent, String label, String value) {
        JPanel field = new JPanel();
        field.setOpaque(false);
        field.setLayout(new BoxLayout(field, BoxLayout.Y_AXIS));

        JLabel l = new JLabel(label);
        l.setFont(FONT_LABEL);
        l.setForeground(MUTED);
        JLabel v = new JLabel(value);
        v.setFont(new Font("SansSerif", Font.BOLD, 14));
        v.setForeground(TEXT);

        field.add(l);
        field.add(Box.createVerticalStrut(5));
        field.add(v);
        parent.add(field);
    }

    // ================= PLACEMENT STATUS =================

    private JPanel buildStatusPage() {
        JPanel root = pageContainer();
        root.add(simpleHeader("Placement Status", "Your overall placement journey and current outcome."), BorderLayout.NORTH);

        JPanel card = new JPanel(new BorderLayout(0, 20));
        card.setBackground(Color.WHITE);
        card.setBorder(new EmptyBorder(25, 25, 25, 25));

        JPanel summary = new JPanel(new GridLayout(1, 3, 15, 0));
        summary.setOpaque(false);
        summary.add(miniStatus("Applications", "4", "Submitted"));
        summary.add(miniStatus("Shortlisted", "2", "Current"));
        summary.add(miniStatus("Interviews", "1", "Upcoming"));
        card.add(summary, BorderLayout.NORTH);

        JPanel timeline = new JPanel();
        timeline.setOpaque(false);
        timeline.setLayout(new BoxLayout(timeline, BoxLayout.Y_AXIS));
        addTimelineStep(timeline, "Profile completed", true);
        addTimelineStep(timeline, "Resume uploaded", true);
        addTimelineStep(timeline, "Applications submitted", true);
        addTimelineStep(timeline, "Shortlisted for interviews", true);
        addTimelineStep(timeline, "Final placement result", false);
        card.add(timeline, BorderLayout.CENTER);

        root.add(card, BorderLayout.CENTER);
        return root;
    }

    private JPanel miniStatus(String title, String value, String note) {
        JPanel p = new JPanel();
        p.setBackground(LIGHT_GREEN);
        p.setBorder(new EmptyBorder(14, 14, 14, 14));
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel t = new JLabel(title);
        t.setFont(FONT_LABEL);
        t.setForeground(MUTED);
        JLabel v = new JLabel(value);
        v.setFont(new Font("SansSerif", Font.BOLD, 25));
        v.setForeground(PRIMARY_GREEN);
        JLabel n = new JLabel(note);
        n.setFont(new Font("SansSerif", Font.PLAIN, 11));
        n.setForeground(MUTED);

        p.add(t);
        p.add(Box.createVerticalStrut(5));
        p.add(v);
        p.add(n);
        return p;
    }

    private void addTimelineStep(JPanel parent, String text, boolean complete) {
        JLabel step = new JLabel((complete ? "✓  " : "○  ") + text);
        step.setFont(new Font("SansSerif", complete ? Font.BOLD : Font.PLAIN, 14));
        step.setForeground(complete ? SUCCESS : MUTED);
        step.setBorder(new EmptyBorder(8, 5, 8, 5));
        parent.add(step);
    }

    // ================= COMMON UI =================

    private JPanel pageContainer() {
        JPanel p = new JPanel(new BorderLayout(0, 15));
        p.setBackground(BG);
        return p;
    }

    private JPanel simpleHeader(String title, String description) {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel h = new JLabel(title);
        h.setFont(FONT_HEADING);
        h.setForeground(TEXT);

        JLabel d = new JLabel(description);
        d.setFont(FONT_BODY);
        d.setForeground(MUTED);
        d.setBorder(new EmptyBorder(4, 0, 0, 0));

        header.add(h);
        header.add(d);
        return header;
    }

    private JPanel whiteSection(String title) {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(15, 15, 15, 15)
        ));

        JLabel heading = new JLabel(title);
        heading.setFont(FONT_SECTION);
        heading.setForeground(TEXT);
        card.add(heading, BorderLayout.NORTH);
        return card;
    }

    private JTable createTable(String[] columns, Object[][] rows) {
        DefaultTableModel model = new DefaultTableModel(rows, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        styleTable(table);
        return table;
    }

    private void styleTable(JTable table) {
        table.setRowHeight(29);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setForeground(TEXT);
        table.setGridColor(new Color(232, 235, 232));
        table.setSelectionBackground(LIGHT_GREEN);
        table.setSelectionForeground(TEXT);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(245, 247, 245));
        table.getTableHeader().setForeground(TEXT);
    }

    private JButton primaryButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(DARK_GREEN);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(8, 15, 8, 15));
        return b;
    }

    // ================= RENDERERS =================

    private static class EligibilityRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                        boolean focused, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(new Font("SansSerif", Font.BOLD, 12));
            if ("Eligible".equals(value)) {
                label.setForeground(SUCCESS);
            } else {
                label.setForeground(DANGER);
            }
            return label;
        }
    }

    private static class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                        boolean focused, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            label.setFont(new Font("SansSerif", Font.BOLD, 12));
            String status = String.valueOf(value);
            if (status.contains("Shortlisted")) {
                label.setForeground(SUCCESS);
            } else if (status.contains("Pending")) {
                label.setForeground(WARNING);
            } else if (status.contains("Rejected")) {
                label.setForeground(DANGER);
            } else {
                label.setForeground(TEXT);
            }
            return label;
        }
    }

    private static class ActionTextRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                        boolean focused, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            label.setForeground(PRIMARY_GREEN);
            label.setFont(new Font("SansSerif", Font.BOLD, 12));
            label.setHorizontalAlignment(SwingConstants.CENTER);
            return label;
        }
    }

    private static class CompoundBorder2 implements javax.swing.border.Border {
        private final Border line = BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER);
        private final Border pad = new EmptyBorder(10, 6, 10, 6);

        @Override public Insets getBorderInsets(Component c) {
            Insets a = line.getBorderInsets(c);
            Insets b = pad.getBorderInsets(c);
            return new Insets(a.top + b.top, a.left + b.left, a.bottom + b.bottom, a.right + b.right);
        }
        @Override public boolean isBorderOpaque() { return true; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            pad.paintBorder(c, g, x, y, width, height);
            line.paintBorder(c, g, x, y, width, height);
        }
    }

    // ================= MOCK DATA =================
    // =========================================================
    //
    // ---------------------------------------------------------
    // This method contains sample frontend data used only for
    // testing the Student Dashboard UI.
    // Replace this method with backend/server calls when the
    // database and APIs are integrated.
    //
    // DO NOT add real student/job data here.
    // =========================================================

    private void loadMockData() {
        drives.add(new Drive("Infosys", "Software Engineer", "Placement", "6.5 LPA", 7.50, 0, "12 Sep 2026", true));
        drives.add(new Drive("TCS", "Graduate Engineer", "Placement", "7.2 LPA", 7.00, 1, "15 Sep 2026", true));
        drives.add(new Drive("Deloitte", "Analyst", "Placement", "8.0 LPA", 8.50, 0, "18 Sep 2026", false));
        drives.add(new Drive("Accenture", "Associate", "Placement", "6.0 LPA", 7.20, 0, "20 Sep 2026", true));
        drives.add(new Drive("Capgemini", "Intern", "Internship", "25K / month", 7.00, 1, "23 Sep 2026", true));

        applications.add(new Application("Infosys", "Software Engineer", "08 Sep 2026", "GD", "Shortlisted"));
        applications.add(new Application("Deloitte", "Analyst", "06 Sep 2026", "Interview", "Shortlisted"));
        applications.add(new Application("TCS", "Graduate Engineer", "02 Sep 2026", "Final Result", "Pending"));
        applications.add(new Application("Wipro", "Project Engineer", "30 Aug 2026", "Technical Interview", "Pending"));

        notices.add(new Notice("Eligibility Update", "You are eligible for Infosys Software Engineer.", "Today"));
        notices.add(new Notice("Deadline Reminder", "TCS application deadline is in 5 days.", "Today"));
        notices.add(new Notice("Shortlisted", "You have been shortlisted for Deloitte interview.", "08 Sep"));
        notices.add(new Notice("Resume", "Keep your latest resume ready before applying.", "07 Sep"));
    }

    private static class Drive {
        String company, role, type, packageText, deadline;
        double minCgpa;
        int maxBacklogs;
        boolean eligible;

        Drive(String company, String role, String type, String packageText,
              double minCgpa, int maxBacklogs, String deadline, boolean eligible) {
            this.company = company;
            this.role = role;
            this.type = type;
            this.packageText = packageText;
            this.minCgpa = minCgpa;
            this.maxBacklogs = maxBacklogs;
            this.deadline = deadline;
            this.eligible = eligible;
        }
    }

    private static class Application {
        String company, role, appliedOn, stage, status;

        Application(String company, String role, String appliedOn, String stage, String status) {
            this.company = company;
            this.role = role;
            this.appliedOn = appliedOn;
            this.stage = stage;
            this.status = status;
        }
    }

    private static class Notice {
        String title, body, date;

        Notice(String title, String body, String date) {
            this.title = title;
            this.body = body;
            this.date = date;
        }
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StudentDashboard().setVisible(true));
    }
}
