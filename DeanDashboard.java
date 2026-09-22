package com.placement.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

/**
 * Socket-connected Dean Dashboard.
 *
 * Uses only data exposed by the existing placement backend:
 * GET_DASHBOARD_STATS, GET_JOBS, GET_STUDENTS,
 * GET_ELIGIBLE_STUDENTS and GET_NOTIFICATIONS.
 *
 * This dashboard intentionally does not display "Placed Students" or
 * "Placement Rate" because the current backend has no placement-status field.
 */
public class DeanDashboard extends JFrame {

    private static final Color SIDEBAR_GREEN = new Color(20, 83, 45);
    private static final Color BG_GRAY = new Color(243, 244, 246);
    private static final Color LIGHT_GREEN = new Color(230, 240, 233);
    private static final Color DARK_TEXT = new Color(31, 41, 35);
    private static final Color GREY_TEXT = new Color(107, 114, 110);
    private static final Color BORDER = new Color(220, 225, 222);

    private final String displayName;
    private final String sessionToken;
    private final SocketClient socketClient;

    private JPanel contentPanel;

    private JLabel totalStudentsValue;
    private JLabel activeJobsValue;
    private JLabel pendingEligibleValue;
    private JLabel notificationsValue;

    private DefaultTableModel studentsModel;
    private JTable studentsTable;
    private TableRowSorter<DefaultTableModel> studentsSorter;
    private JTextField studentSearchField;

    private DefaultTableModel jobsModel;
    private JTable jobsTable;
    private TableRowSorter<DefaultTableModel> jobsSorter;
    private JTextField jobSearchField;

    private DefaultTableModel notificationsModel;
    private JTable notificationsTable;

    private final Map<String, Integer> companyJobCounts = new LinkedHashMap<>();
    private int reportJobs;
    private int reportPending;
    private int reportNotifications;
    private int reportStudents;

    public DeanDashboard(String displayName, String sessionToken) {
        this.displayName = displayName == null || displayName.isBlank()
                ? "Dean" : displayName;
        this.sessionToken = sessionToken;
        this.socketClient = new SocketClient();

        setTitle("Placement Eligibility Portal - Dean Dashboard");
        setSize(1120, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        initializeUI();
        loadDashboardData();
    }

    public DeanDashboard() {
        this("Dean", null);
    }

    private void initializeUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_GRAY);

        JPanel sidebar = createSidebar();
        root.add(sidebar, BorderLayout.WEST);

        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(BG_GRAY);
        right.add(createHeader(), BorderLayout.NORTH);

        contentPanel = new JPanel(new CardLayout());
        contentPanel.setBackground(BG_GRAY);
        contentPanel.setBorder(new EmptyBorder(18, 22, 22, 22));

        contentPanel.add(createDashboardPanel(), "DASHBOARD");
        contentPanel.add(createPlacementOverviewPanel(), "OVERVIEW");
        contentPanel.add(createStudentsPanel(), "STUDENTS");
        contentPanel.add(createCompaniesPanel(), "COMPANIES");
        contentPanel.add(createReportsPanel(), "REPORTS");
        contentPanel.add(createAnnouncementsPanel(), "ANNOUNCEMENTS");

        JScrollPane scroll = new JScrollPane(contentPanel);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(15);
        right.add(scroll, BorderLayout.CENTER);

        root.add(right, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(SIDEBAR_GREEN);
        sidebar.setPreferredSize(new Dimension(220, getHeight()));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(30, 20, 25, 20));

        JLabel title = new JLabel("PLACEMENT");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));

        JLabel subtitle = new JLabel("ELIGIBILITY PORTAL");
        subtitle.setForeground(new Color(200, 220, 210));
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 11));

        sidebar.add(title);
        sidebar.add(subtitle);
        sidebar.add(Box.createVerticalStrut(30));

        JButton dashboard = createSidebarButton("Dashboard");
        JButton overview = createSidebarButton("Placement Overview");
        JButton students = createSidebarButton("Students");
        JButton companies = createSidebarButton("Companies");
        JButton reports = createSidebarButton("Reports");
        JButton announcements = createSidebarButton("Announcements");

        sidebar.add(dashboard);
        sidebar.add(overview);
        sidebar.add(students);
        sidebar.add(companies);
        sidebar.add(reports);
        sidebar.add(announcements);
        sidebar.add(Box.createVerticalGlue());

        JButton logout = createSidebarButton("Logout");
        sidebar.add(logout);

        dashboard.addActionListener(e -> showCard("DASHBOARD"));
        overview.addActionListener(e -> showCard("OVERVIEW"));
        students.addActionListener(e -> showCard("STUDENTS"));
        companies.addActionListener(e -> showCard("COMPANIES"));
        reports.addActionListener(e -> showCard("REPORTS"));
        announcements.addActionListener(e -> {
            showCard("ANNOUNCEMENTS");
            loadNotifications();
        });
        logout.addActionListener(e -> logout());

        return sidebar;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(17, 24, 17, 24));

        JPanel greetingPanel = new JPanel();
        greetingPanel.setBackground(Color.WHITE);
        greetingPanel.setLayout(new BoxLayout(greetingPanel, BoxLayout.Y_AXIS));

        JLabel greeting = new JLabel("Good morning, " + displayName);
        greeting.setFont(new Font("SansSerif", Font.BOLD, 21));
        greeting.setForeground(DARK_TEXT);

        JLabel description = new JLabel("Placement Management Overview");
        description.setFont(new Font("SansSerif", Font.PLAIN, 12));
        description.setForeground(GREY_TEXT);

        greetingPanel.add(greeting);
        greetingPanel.add(Box.createVerticalStrut(4));
        greetingPanel.add(description);

        JPanel profile = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        profile.setBackground(Color.WHITE);

        JLabel initials = new JLabel(getInitials(displayName));
        initials.setPreferredSize(new Dimension(38, 38));
        initials.setHorizontalAlignment(SwingConstants.CENTER);
        initials.setOpaque(true);
        initials.setBackground(LIGHT_GREEN);
        initials.setForeground(SIDEBAR_GREEN);
        initials.setFont(new Font("SansSerif", Font.BOLD, 13));

        JLabel dean = new JLabel("Dean");
        dean.setFont(new Font("SansSerif", Font.BOLD, 13));
        dean.setForeground(DARK_TEXT);

        profile.add(initials);
        profile.add(dean);

        header.add(greetingPanel, BorderLayout.WEST);
        header.add(profile, BorderLayout.EAST);
        return header;
    }

    private JPanel createDashboardPanel() {
        JPanel page = pagePanel();

        JPanel cards = new JPanel(new GridLayout(1, 4, 12, 0));
        cards.setBackground(BG_GRAY);
        cards.setPreferredSize(new Dimension(850, 105));

        totalStudentsValue = new JLabel("--");
        activeJobsValue = new JLabel("--");
        pendingEligibleValue = new JLabel("--");
        notificationsValue = new JLabel("--");

        cards.add(createCard("Total Students", totalStudentsValue, "Registered students"));
        cards.add(createCard("Active Jobs", activeJobsValue, "Current job postings"));
        cards.add(createCard("Pending Eligible", pendingEligibleValue, "Eligible students awaiting notification"));
        cards.add(createCard("Notifications Sent", notificationsValue, "Notification history"));

        page.add(cards, BorderLayout.NORTH);
        page.add(Box.createVerticalStrut(16), BorderLayout.CENTER);

        JPanel lower = new JPanel(new GridLayout(1, 2, 12, 0));
        lower.setBackground(BG_GRAY);
        lower.add(createSystemSummaryPanel());
        lower.add(createDataNotePanel());

        page.add(lower, BorderLayout.SOUTH);
        return page;
    }

    private JPanel createSystemSummaryPanel() {
        JPanel panel = createWhitePanel();
        panel.setLayout(new BorderLayout());
        JLabel title = sectionTitle("System Summary");
        panel.add(title, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setBackground(Color.WHITE);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        addSummaryLine(body, "Students registered", totalStudentsValue);
        addSummaryLine(body, "Active job postings", activeJobsValue);
        addSummaryLine(body, "Pending eligible records", pendingEligibleValue);
        addSummaryLine(body, "Notifications sent", notificationsValue);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createDataNotePanel() {
        JPanel panel = createWhitePanel();
        panel.setLayout(new BorderLayout());
        panel.add(sectionTitle("Data Availability"), BorderLayout.NORTH);

        JTextArea note = new JTextArea(
                "The current backend does not store a placement-status field.\n\n"
                        + "Therefore this dashboard reports students, active jobs, eligibility, "
                        + "and notifications instead of inventing placed-student or placement-rate figures.\n\n"
                        + "Company information is derived from active job postings."
        );
        note.setEditable(false);
        note.setLineWrap(true);
        note.setWrapStyleWord(true);
        note.setBackground(Color.WHITE);
        note.setForeground(DARK_TEXT);
        note.setFont(new Font("SansSerif", Font.PLAIN, 12));
        note.setBorder(new EmptyBorder(8, 0, 0, 0));
        panel.add(note, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createPlacementOverviewPanel() {
        JPanel page = pagePanel();
        JPanel panel = createWhitePanel();
        panel.setLayout(new BorderLayout(0, 12));
        panel.add(sectionTitle("Eligibility & Recruitment Overview"), BorderLayout.NORTH);

        JPanel body = new JPanel(new GridLayout(2, 2, 12, 12));
        body.setBackground(Color.WHITE);
        body.add(infoBox("Total Students", () -> value(totalStudentsValue)));
        body.add(infoBox("Active Jobs", () -> value(activeJobsValue)));
        body.add(infoBox("Pending Eligible", () -> value(pendingEligibleValue)));
        body.add(infoBox("Notifications Sent", () -> value(notificationsValue)));

        panel.add(body, BorderLayout.CENTER);
        page.add(panel, BorderLayout.CENTER);
        return page;
    }

    private JPanel createStudentsPanel() {
        JPanel page = pagePanel();
        JPanel panel = createWhitePanel();
        panel.setLayout(new BorderLayout(0, 10));

        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setBackground(Color.WHITE);
        top.add(sectionTitle("Students"), BorderLayout.WEST);

        studentSearchField = new JTextField();
        studentSearchField.setPreferredSize(new Dimension(260, 32));
        studentSearchField.putClientProperty("JTextField.placeholderText", "Search students...");
        studentSearchField.getDocument().addDocumentListener(new SimpleDocumentListener() {
            public void update() { applyStudentFilter(); }
        });
        top.add(studentSearchField, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);

        studentsModel = new DefaultTableModel(
                new Object[]{"PRN", "Name", "Email", "Department", "CGPA", "Year", "Backlogs", "Semester", "Skills"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
            public Class<?> getColumnClass(int c) {
                if (c == 4) return Double.class;
                if (c == 6 || c == 7) return Integer.class;
                return String.class;
            }
        };

        studentsTable = new JTable(studentsModel);
        studentsTable.setRowHeight(28);
        studentsTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        studentsTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        studentsTable.getTableHeader().setBackground(LIGHT_GREEN);
        studentsTable.getTableHeader().setForeground(DARK_TEXT);
        studentsTable.setGridColor(BORDER);
        studentsTable.setShowVerticalLines(false);
        studentsTable.setAutoCreateRowSorter(true);
        studentsSorter = (TableRowSorter<DefaultTableModel>) studentsTable.getRowSorter();
        centerColumns(studentsTable, 4, 5, 6, 7);

        JScrollPane scroll = new JScrollPane(studentsTable);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        panel.add(scroll, BorderLayout.CENTER);
        page.add(panel, BorderLayout.CENTER);
        return page;
    }

    private JPanel createCompaniesPanel() {
        JPanel page = pagePanel();
        JPanel panel = createWhitePanel();
        panel.setLayout(new BorderLayout(0, 10));

        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setBackground(Color.WHITE);
        top.add(sectionTitle("Companies from Active Job Postings"), BorderLayout.WEST);

        jobSearchField = new JTextField();
        jobSearchField.setPreferredSize(new Dimension(260, 32));
        jobSearchField.putClientProperty("JTextField.placeholderText", "Search company / role...");
        jobSearchField.getDocument().addDocumentListener(new SimpleDocumentListener() {
            public void update() { applyJobFilter(); }
        });
        top.add(jobSearchField, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);

        jobsModel = new DefaultTableModel(
                new Object[]{"Job ID", "Company", "Role", "Package", "Min CGPA", "Branches", "Max Backlogs", "Passing Year", "Deadline"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
            public Class<?> getColumnClass(int c) {
                if (c == 4) return Double.class;
                if (c == 6 || c == 7) return Integer.class;
                return String.class;
            }
        };

        jobsTable = new JTable(jobsModel);
        jobsTable.setRowHeight(28);
        jobsTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        jobsTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        jobsTable.getTableHeader().setBackground(LIGHT_GREEN);
        jobsTable.getTableHeader().setForeground(DARK_TEXT);
        jobsTable.setGridColor(BORDER);
        jobsTable.setShowVerticalLines(false);
        jobsTable.setAutoCreateRowSorter(true);
        jobsSorter = (TableRowSorter<DefaultTableModel>) jobsTable.getRowSorter();
        centerColumns(jobsTable, 4, 6, 7);

        JScrollPane scroll = new JScrollPane(jobsTable);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        panel.add(scroll, BorderLayout.CENTER);
        page.add(panel, BorderLayout.CENTER);
        return page;
    }

    private JPanel createReportsPanel() {
        JPanel page = pagePanel();
        JPanel report = createWhitePanel();
        report.setLayout(new BorderLayout(0, 12));
        report.add(sectionTitle("Reports & System Metrics"), BorderLayout.NORTH);

        JPanel charts = new JPanel(new GridLayout(1, 2, 15, 0));
        charts.setBackground(Color.WHITE);
        charts.add(new MetricBarChart());
        charts.add(new MetricDonutChart());
        report.add(charts, BorderLayout.CENTER);

        JLabel note = new JLabel(
                "Reports use only backend-provided counts: students, active jobs, pending eligible records, and notifications sent.");
        note.setForeground(GREY_TEXT);
        note.setFont(new Font("SansSerif", Font.PLAIN, 11));
        report.add(note, BorderLayout.SOUTH);

        page.add(report, BorderLayout.CENTER);
        return page;
    }

    private JPanel createAnnouncementsPanel() {
        JPanel page = pagePanel();
        JPanel panel = createWhitePanel();
        panel.setLayout(new BorderLayout(0, 10));
        panel.add(sectionTitle("Notification / Announcement History"), BorderLayout.NORTH);

        notificationsModel = new DefaultTableModel(
                new Object[]{"ID", "Job ID", "Subject", "Recipient Group", "Recipients", "Created At", "Message"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
            public Class<?> getColumnClass(int c) {
                if (c == 0 || c == 4) return Long.class;
                return String.class;
            }
        };

        notificationsTable = new JTable(notificationsModel);
        notificationsTable.setRowHeight(28);
        notificationsTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        notificationsTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        notificationsTable.getTableHeader().setBackground(LIGHT_GREEN);
        notificationsTable.getTableHeader().setForeground(DARK_TEXT);
        notificationsTable.setGridColor(BORDER);
        notificationsTable.setShowVerticalLines(false);
        notificationsTable.setAutoCreateRowSorter(true);
        centerColumns(notificationsTable, 0, 4);

        JScrollPane scroll = new JScrollPane(notificationsTable);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        panel.add(scroll, BorderLayout.CENTER);
        page.add(panel, BorderLayout.CENTER);
        return page;
    }

    private void loadDashboardData() {
        loadDashboardStats();
        loadJobs();
        loadStudents();
        loadNotifications();
    }

    private void loadDashboardStats() {
        if (!hasSession()) {
            showError("No active session. Please log in again.");
            return;
        }

        runRequest(() -> socketClient.sendRequest("GET_DASHBOARD_STATS", sessionToken), response -> {
            if (!response.success) {
                showError(response.payload);
                return;
            }
            String[] parts = response.parts();
            if (parts.length < 4) {
                showError("Invalid dashboard statistics received.");
                return;
            }
            try {
                reportJobs = Integer.parseInt(parts[0]);
                reportPending = Integer.parseInt(parts[1]);
                reportNotifications = Integer.parseInt(parts[2]);
                reportStudents = Integer.parseInt(parts[3]);

                activeJobsValue.setText(String.valueOf(reportJobs));
                pendingEligibleValue.setText(String.valueOf(reportPending));
                notificationsValue.setText(String.valueOf(reportNotifications));
                totalStudentsValue.setText(String.valueOf(reportStudents));
                contentPanel.revalidate();
                contentPanel.repaint();
            } catch (NumberFormatException ex) {
                showError("Invalid numeric dashboard data.");
            }
        });
    }

    private void loadStudents() {
        if (!hasSession()) return;
        runListRequest(() -> socketClient.sendListRequest("GET_STUDENTS", sessionToken), response -> {
            if (!response.success) {
                showError(response.errorMessage);
                return;
            }
            studentsModel.setRowCount(0);
            for (String line : response.lines) {
                StudentRow s = parseStudent(line);
                if (s == null) continue;
                studentsModel.addRow(new Object[]{s.prn, s.name, s.email, s.department,
                        s.cgpa, s.passingYear, s.backlogs, s.semester, s.skills});
            }
            applyStudentFilter();
        });
    }

    private void loadJobs() {
        runListRequest(() -> socketClient.sendListRequest("GET_JOBS"), response -> {
            if (!response.success) {
                showError(response.errorMessage);
                return;
            }
            jobsModel.setRowCount(0);
            companyJobCounts.clear();
            for (String line : response.lines) {
                JobRow j = parseJob(line);
                if (j == null) continue;
                jobsModel.addRow(new Object[]{j.id, j.company, j.role, j.minPackage, j.minCgpa,
                        j.branches, j.maxBacklogs, j.passingYear, j.deadline});
                companyJobCounts.merge(j.company, 1, Integer::sum);
            }
            applyJobFilter();
        });
    }

    private void loadNotifications() {
        if (!hasSession() || notificationsModel == null) return;
        runListRequest(() -> socketClient.sendListRequest("GET_NOTIFICATIONS", sessionToken), response -> {
            if (!response.success) {
                showError(response.errorMessage);
                return;
            }
            notificationsModel.setRowCount(0);
            for (String line : response.lines) {
                NotificationRow n = parseNotification(line);
                if (n == null) continue;
                notificationsModel.addRow(new Object[]{n.id, n.jobId, n.subject,
                        n.recipientGroup, n.recipientCount, n.createdAt, n.message});
            }
        });
    }

    private void applyStudentFilter() {
        if (studentsSorter == null) return;
        String text = studentSearchField == null ? "" : studentSearchField.getText().trim();
        if (text.isEmpty()) {
            studentsSorter.setRowFilter(null);
            return;
        }
        try {
            studentsSorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text)));
        } catch (PatternSyntaxException ignored) {
            studentsSorter.setRowFilter(null);
        }
    }

    private void applyJobFilter() {
        if (jobsSorter == null) return;
        String text = jobSearchField == null ? "" : jobSearchField.getText().trim();
        if (text.isEmpty()) {
            jobsSorter.setRowFilter(null);
            return;
        }
        try {
            jobsSorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text)));
        } catch (PatternSyntaxException ignored) {
            jobsSorter.setRowFilter(null);
        }
    }

    private void runRequest(RequestAction action, ResponseHandler handler) {
        SwingWorker<SocketClient.Response, Void> worker = new SwingWorker<>() {
            protected SocketClient.Response doInBackground() throws Exception {
                return action.execute();
            }
            protected void done() {
                try {
                    handler.handle(get());
                } catch (Exception ex) {
                    showError("Request failed: " + rootMessage(ex));
                }
            }
        };
        worker.execute();
    }

    private void runListRequest(ListRequestAction action, ListResponseHandler handler) {
        SwingWorker<SocketClient.ListResponse, Void> worker = new SwingWorker<>() {
            protected SocketClient.ListResponse doInBackground() throws Exception {
                return action.execute();
            }
            protected void done() {
                try {
                    handler.handle(get());
                } catch (Exception ex) {
                    showError("Request failed: " + rootMessage(ex));
                }
            }
        };
        worker.execute();
    }

    private void logout() {
        int result = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to log out?", "Logout", JOptionPane.YES_NO_OPTION);
        if (result != JOptionPane.YES_OPTION) return;

        if (!hasSession()) {
            openLogin();
            return;
        }

        runRequest(() -> socketClient.sendRequest("LOGOUT", sessionToken), response -> openLogin());
    }

    private void openLogin() {
        dispose();
        SwingUtilities.invokeLater(() -> {
            LoginFrame login = new LoginFrame();
            login.setVisible(true);
        });
    }

    private boolean hasSession() {
        return sessionToken != null && !sessionToken.isBlank();
    }

    private void showCard(String name) {
        CardLayout layout = (CardLayout) contentPanel.getLayout();
        layout.show(contentPanel, name);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private JPanel pagePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_GRAY);
        return panel;
    }

    private JPanel createSidebarButtonPanel() {
        JPanel p = new JPanel();
        p.setBackground(SIDEBAR_GREEN);
        return p;
    }

    private JButton createSidebarButton(String text) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(220, 42));
        button.setPreferredSize(new Dimension(220, 42));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorder(new EmptyBorder(0, 15, 0, 5));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setBackground(SIDEBAR_GREEN);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("SansSerif", Font.PLAIN, 13));
        return button;
    }

    private JPanel createCard(String title, JLabel value, String description) {
        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(15, 15, 15, 15)));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        titleLabel.setForeground(GREY_TEXT);
        value.setFont(new Font("SansSerif", Font.BOLD, 23));
        value.setForeground(SIDEBAR_GREEN);
        JLabel desc = new JLabel(description);
        desc.setFont(new Font("SansSerif", Font.PLAIN, 10));
        desc.setForeground(GREY_TEXT);

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(5));
        card.add(value);
        card.add(Box.createVerticalStrut(3));
        card.add(desc);
        return card;
    }

    private JPanel createWhitePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(15, 15, 15, 15)));
        return panel;
    }

    private JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 15));
        label.setForeground(DARK_TEXT);
        return label;
    }

    private void addSummaryLine(JPanel parent, String title, JLabel value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(Color.WHITE);
        row.setBorder(new EmptyBorder(8, 0, 8, 0));
        JLabel left = new JLabel(title);
        left.setForeground(GREY_TEXT);
        left.setFont(new Font("SansSerif", Font.PLAIN, 12));
        JLabel right = new JLabel(value.getText());
        right.setForeground(SIDEBAR_GREEN);
        right.setFont(new Font("SansSerif", Font.BOLD, 13));
        row.add(left, BorderLayout.WEST);
        row.add(right, BorderLayout.EAST);
        parent.add(row);
    }

    private JPanel infoBox(String title, ValueProvider provider) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(LIGHT_GREEN);
        p.setBorder(new EmptyBorder(14, 14, 14, 14));
        JLabel t = new JLabel(title);
        t.setForeground(GREY_TEXT);
        t.setFont(new Font("SansSerif", Font.PLAIN, 11));
        JLabel v = new JLabel(provider.value());
        v.setForeground(SIDEBAR_GREEN);
        v.setFont(new Font("SansSerif", Font.BOLD, 24));
        p.add(t, BorderLayout.NORTH);
        p.add(v, BorderLayout.CENTER);
        return p;
    }

    private String value(JLabel label) {
        return label == null ? "--" : label.getText();
    }

    private void centerColumns(JTable table, int... indices) {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int index : indices) {
            if (index < table.getColumnCount()) {
                table.getColumnModel().getColumn(index).setCellRenderer(renderer);
            }
        }
    }

    private void showError(String message) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> showError(message));
            return;
        }
        JOptionPane.showMessageDialog(this, message, "Dean Dashboard", JOptionPane.ERROR_MESSAGE);
    }

    private static String getInitials(String name) {
        if (name == null || name.isBlank()) return "DE";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private static String rootMessage(Throwable t) {
        Throwable current = t;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private static int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value); } catch (Exception e) { return fallback; }
    }

    private static long parseLong(String value, long fallback) {
        try { return Long.parseLong(value); } catch (Exception e) { return fallback; }
    }

    private static double parseDouble(String value, double fallback) {
        try { return Double.parseDouble(value); } catch (Exception e) { return fallback; }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static JobRow parseJob(String line) {
        if (line == null) return null;
        String[] p = line.split("\\|", -1);
        if (p.length < 10) return null;
        return new JobRow(p[0], p[1], p[2], p[3], parseDouble(p[4], 0), p[5],
                parseInt(p[6], 0), parseInt(p[7], 0), p[8], p[9]);
    }

    private static StudentRow parseStudent(String line) {
        if (line == null) return null;
        String[] p = line.split("\\|", -1);
        if (p.length < 10) return null;
        return new StudentRow(p[0], p[1], p[2], p[3], parseDouble(p[4], 0),
                parseInt(p[5], 0), parseInt(p[6], 0), parseInt(p[7], 0), p[8], p[9]);
    }

    private static NotificationRow parseNotification(String line) {
        if (line == null) return null;
        String[] p = line.split("\\|", -1);
        if (p.length < 7) return null;
        return new NotificationRow(parseLong(p[0], 0), p[1], p[2], p[3], p[4], parseInt(p[5], 0), p[6]);
    }

    private interface RequestAction { SocketClient.Response execute() throws Exception; }
    private interface ResponseHandler { void handle(SocketClient.Response response); }
    private interface ListRequestAction { SocketClient.ListResponse execute() throws Exception; }
    private interface ListResponseHandler { void handle(SocketClient.ListResponse response); }
    private interface ValueProvider { String value(); }

    private interface SimpleDocumentListener extends javax.swing.event.DocumentListener {
        void update();
        default void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
        default void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
        default void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
    }

    private static class JobRow {
        final String id, company, role, minPackage, branches, requiredSkills, deadline;
        final double minCgpa;
        final int maxBacklogs, passingYear;
        JobRow(String id, String company, String role, String minPackage, double minCgpa,
               String branches, int maxBacklogs, int passingYear, String requiredSkills, String deadline) {
            this.id = safe(id); this.company = safe(company); this.role = safe(role); this.minPackage = safe(minPackage);
            this.minCgpa = minCgpa; this.branches = safe(branches); this.maxBacklogs = maxBacklogs;
            this.passingYear = passingYear; this.requiredSkills = safe(requiredSkills); this.deadline = safe(deadline);
        }
    }

    private static class StudentRow {
        final String prn, name, email, department, phone, skills;
        final double cgpa;
        final int passingYear, backlogs, semester;
        StudentRow(String prn, String name, String email, String department, double cgpa,
                   int passingYear, int backlogs, int semester, String phone, String skills) {
            this.prn = safe(prn); this.name = safe(name); this.email = safe(email); this.department = safe(department);
            this.cgpa = cgpa; this.passingYear = passingYear; this.backlogs = backlogs; this.semester = semester;
            this.phone = safe(phone); this.skills = safe(skills);
        }
    }

    private static class NotificationRow {
        final long id; final String jobId, subject, message, recipientGroup, createdAt; final int recipientCount;
        NotificationRow(long id, String jobId, String subject, String message, String recipientGroup,
                        int recipientCount, String createdAt) {
            this.id = id; this.jobId = safe(jobId); this.subject = safe(subject); this.message = safe(message);
            this.recipientGroup = safe(recipientGroup); this.recipientCount = recipientCount; this.createdAt = safe(createdAt);
        }
    }

    private class MetricBarChart extends JPanel {
        MetricBarChart() { setBackground(Color.WHITE); setPreferredSize(new Dimension(400, 330)); setBorder(new EmptyBorder(8, 8, 8, 8)); }
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(DARK_TEXT); g2.setFont(new Font("SansSerif", Font.BOLD, 14)); g2.drawString("Backend Metrics", 15, 24);
            String[] labels = {"Students", "Active Jobs", "Pending Eligible", "Notifications"};
            int[] values = {reportStudents, reportJobs, reportPending, reportNotifications};
            int max = 1; for (int v : values) max = Math.max(max, v);
            int y = 55; int maxWidth = Math.max(120, getWidth() - 150);
            for (int i = 0; i < labels.length; i++) {
                g2.setColor(GREY_TEXT); g2.setFont(new Font("SansSerif", Font.PLAIN, 11)); g2.drawString(labels[i], 15, y + 13);
                int width = (int) ((maxWidth * (double) values[i]) / max);
                g2.setColor(LIGHT_GREEN); g2.fillRoundRect(120, y, width, 22, 8, 8);
                g2.setColor(SIDEBAR_GREEN); g2.fillRoundRect(120, y, Math.max(2, width), 22, 8, 8);
                g2.drawString(String.valueOf(values[i]), Math.min(125 + width, getWidth() - 35), y + 15);
                y += 55;
            }
            g2.dispose();
        }
    }

    private class MetricDonutChart extends JPanel {
        MetricDonutChart() { setBackground(Color.WHITE); setPreferredSize(new Dimension(400, 330)); setBorder(new EmptyBorder(8, 8, 8, 8)); }
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(DARK_TEXT); g2.setFont(new Font("SansSerif", Font.BOLD, 14)); g2.drawString("Metric Composition", 15, 24);
            int[] values = {reportStudents, reportJobs, reportPending, reportNotifications};
            String[] labels = {"Students", "Jobs", "Pending", "Notifications"};
            int total = 0; for (int v : values) total += Math.max(0, v);
            if (total == 0) {
                g2.setColor(GREY_TEXT); g2.drawString("No data received yet", 130, 170); g2.dispose(); return;
            }
            int size = Math.min(190, Math.min(getWidth() - 190, getHeight() - 70));
            int x = 20, y = 60;
            double start = 0;
            for (int i = 0; i < values.length; i++) {
                double angle = values[i] <= 0 ? 0 : 360.0 * values[i] / total;
                g2.setColor(i % 2 == 0 ? SIDEBAR_GREEN : LIGHT_GREEN);
                if (i == 2) g2.setColor(new Color(170, 195, 180));
                if (i == 3) g2.setColor(new Color(105, 135, 115));
                g2.fillArc(x, y, size, size, (int) Math.round(start), (int) Math.round(angle));
                start += angle;
            }
            g2.setColor(Color.WHITE); g2.fillOval(x + size / 4, y + size / 4, size / 2, size / 2);
            int ly = 78;
            for (int i = 0; i < labels.length; i++) {
                g2.setColor(i == 0 ? SIDEBAR_GREEN : i == 1 ? LIGHT_GREEN : i == 2 ? new Color(170, 195, 180) : new Color(105, 135, 115));
                g2.fillRect(size + 45, ly - 10, 12, 12);
                g2.setColor(DARK_TEXT); g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
                g2.drawString(labels[i] + ": " + values[i], size + 65, ly);
                ly += 28;
            }
            g2.dispose();
        }
    }

    private static void centerLabel(JLabel label) { label.setHorizontalAlignment(SwingConstants.CENTER); }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DeanDashboard().setVisible(true));
    }
}
