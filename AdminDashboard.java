package com.placement.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Admin Dashboard - Full administrative privileges for managing students,
 * placement jobs, company summaries, system metrics, and notification history.
 */
public class AdminDashboard extends JFrame {

    // ==========================================
    // THEME & COLOR PALETTE
    // ==========================================
    private static final Color PRIMARY_GREEN = new Color(27, 117, 61);
    private static final Color DARK_GREEN = new Color(20, 92, 48);
    private static final Color LIGHT_GREEN = new Color(232, 245, 236);
    private static final Color BACKGROUND = new Color(247, 248, 245);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color TEXT = new Color(38, 50, 43);
    private static final Color SECONDARY_TEXT = new Color(105, 115, 108);
    private static final Color BORDER = new Color(214, 220, 215);

    // ==========================================
    // STATE & NETWORKING
    // ==========================================
    private final String displayName;
    private final String sessionToken;
    private final SocketClient socketClient;

    // Layout components
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);
    private final JLabel pageTitle = new JLabel("Admin Dashboard");

    // Metric Counters
    private final JLabel totalStudentsValue = createMetricValueLabel();
    private final JLabel activeJobsValue = createMetricValueLabel();
    private final JLabel pendingEligibleValue = createMetricValueLabel();
    private final JLabel notificationsValue = createMetricValueLabel();

    // Tables & Models
    private final DefaultTableModel jobsModel = createModel(new String[]{
            "Job ID", "Company", "Role", "Package", "Min CGPA", "Branches", "Max Backlogs", "Passing Year", "Deadline"
    });
    private final TableRowSorter<DefaultTableModel> jobsSorter = new TableRowSorter<>(jobsModel);
    private final JTable jobsTable = new JTable(jobsModel);

    private final DefaultTableModel studentsModel = createModel(new String[]{
            "PRN", "Name", "Email", "Department", "CGPA", "Passing Year", "Backlogs", "Semester", "Phone", "Skills"
    });
    private final TableRowSorter<DefaultTableModel> studentsSorter = new TableRowSorter<>(studentsModel);
    private final JTable studentsTable = new JTable(studentsModel);

    private final DefaultTableModel companiesModel = createModel(new String[]{
            "Company", "Open Jobs", "Roles", "Latest Deadline"
    });
    private final TableRowSorter<DefaultTableModel> companiesSorter = new TableRowSorter<>(companiesModel);
    private final JTable companiesTable = new JTable(companiesModel);

    private final DefaultTableModel notificationsModel = createModel(new String[]{
            "ID", "Job ID", "Subject", "Message", "Recipient Group", "Recipients", "Created At"
    });
    private final TableRowSorter<DefaultTableModel> notificationsSorter = new TableRowSorter<>(notificationsModel);
    private final JTable notificationsTable = new JTable(notificationsModel);

    // Filters
    private JTextField jobsSearchField;
    private JComboBox<String> jobsCompanyFilter;
    private JComboBox<String> jobsYearFilter;
    private JComboBox<String> jobsSortCombo;

    private JTextField studentsSearchField;
    private JComboBox<String> studentsDepartmentFilter;
    private JComboBox<String> studentsYearFilter;
    private JComboBox<String> studentsCgpaFilter;
    private JComboBox<String> studentsBacklogFilter;
    private JComboBox<String> studentsSemesterFilter;
    private JComboBox<String> studentsSortCombo;

    private JTextField companiesSearchField;
    private JTextField notificationsSearchField;

    // Reports Cache Data
    private int reportJobs = 0;
    private int reportPending = 0;
    private int reportNotifications = 0;
    private int reportStudents = 0;

    // ==========================================
    // CONSTRUCTORS
    // ==========================================
    public AdminDashboard(String displayName, String sessionToken) {
        this.displayName = (displayName == null || displayName.isBlank()) ? "Admin" : displayName;
        this.sessionToken = (sessionToken == null || sessionToken.isBlank()) ? "demo-token" : sessionToken;
        this.socketClient = new SocketClient();

        setTitle("Placement Eligibility Portal - Admin Dashboard");
        setSize(1200, 760);
        setMinimumSize(new Dimension(1050, 680));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        initializeUI();
        setMetricValues(420, 8, 26, 14);
        SwingUtilities.invokeLater(() -> {
            setMetricValues(420, 8, 26, 14);
            contentPanel.revalidate();
            contentPanel.repaint();
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                // Connections closed automatically by SocketClient
            }
        });

        loadDashboardData();
    }

    public AdminDashboard() {
        this("Admin", null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AdminDashboard dashboard = new AdminDashboard("Admin", "demo-token");
            dashboard.setVisible(true);
        });
    }
    // ==========================================
    private void initializeUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BACKGROUND);

        root.add(createSidebar(), BorderLayout.WEST);
        root.add(createMainArea(), BorderLayout.CENTER);

        setContentPane(root);
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBackground(PRIMARY_GREEN);
        sidebar.setBorder(new EmptyBorder(28, 18, 22, 18));

        // Top Branding
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

JLabel logo = new JLabel("Admin Dashboard");
        logo.setFont(new Font("SansSerif", Font.BOLD, 20));
        logo.setForeground(Color.WHITE);

        JLabel portal = new JLabel("ELIGIBILITY PORTAL");
        portal.setFont(new Font("SansSerif", Font.PLAIN, 11));
        portal.setForeground(new Color(211, 235, 218));

        top.add(logo);
        top.add(Box.createVerticalStrut(4));
        top.add(portal);
        top.add(Box.createVerticalStrut(30));

        // Navigation Menu
        JPanel menu = new JPanel();
        menu.setOpaque(false);
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));

        JButton dashboardBtn = createSidebarButton("Dashboard");
        JButton jobsBtn = createSidebarButton("Placement Overview");
        JButton companiesBtn = createSidebarButton("Companies");
        JButton studentsBtn = createSidebarButton("Students");
        JButton reportsBtn = createSidebarButton("Reports");
        JButton announcementsBtn = createSidebarButton("Announcements");

        dashboardBtn.addActionListener(e -> {
            showPage("dashboard", "Dashboard");
            loadDashboardData();
        });
        jobsBtn.addActionListener(e -> {
            showPage("jobs", "Placement Overview");
            loadJobs();
        });
        companiesBtn.addActionListener(e -> {
            showPage("companies", "Companies");
            loadJobs();
        });
        studentsBtn.addActionListener(e -> {
            showPage("students", "Students");
            loadStudents();
        });
        reportsBtn.addActionListener(e -> {
            showPage("reports", "Reports & Analytics");
            loadDashboardStats();
        });
        announcementsBtn.addActionListener(e -> {
            showPage("notifications", "Announcements");
            loadNotifications();
        });

        menu.add(dashboardBtn);
        menu.add(Box.createVerticalStrut(6));
        menu.add(jobsBtn);
        menu.add(Box.createVerticalStrut(6));
        menu.add(companiesBtn);
        menu.add(Box.createVerticalStrut(6));
        menu.add(studentsBtn);
        menu.add(Box.createVerticalStrut(6));
        menu.add(reportsBtn);
        menu.add(Box.createVerticalStrut(6));
        menu.add(announcementsBtn);

        top.add(menu);
        sidebar.add(top, BorderLayout.NORTH);

        // Sidebar Bottom / Profile
        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));

        JLabel user = new JLabel("<html><b>" + escapeHtml(displayName) + "</b><br><font size='2'>Administrator</font></html>");
        user.setForeground(Color.WHITE);

        JButton logout = createSidebarButton("Logout");
        logout.addActionListener(e -> logout());

        bottom.add(user);
        bottom.add(Box.createVerticalStrut(12));
        bottom.add(logout);

        sidebar.add(bottom, BorderLayout.SOUTH);

        return sidebar;
    }

    private JPanel createMainArea() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(BACKGROUND);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(20, 28, 18, 28));

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));

        pageTitle.setFont(new Font("SansSerif", Font.BOLD, 25));
        pageTitle.setForeground(PRIMARY_GREEN);

        JLabel subtitle = new JLabel("Admin Portal");
        subtitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        subtitle.setForeground(PRIMARY_GREEN);

        titleBox.add(pageTitle);
        titleBox.add(Box.createVerticalStrut(4));
        titleBox.add(subtitle);

        header.add(titleBox, BorderLayout.WEST);

        main.add(header, BorderLayout.NORTH);

        // Content Area Card Pages
        contentPanel.setBackground(BACKGROUND);
        contentPanel.add(createDashboardPage(), "dashboard");
        contentPanel.add(createJobsPage(), "jobs");
        contentPanel.add(createCompaniesPage(), "companies");
        contentPanel.add(createStudentsPage(), "students");
        contentPanel.add(createReportsPage(), "reports");
        contentPanel.add(createNotificationsPage(), "notifications");

        main.add(contentPanel, BorderLayout.CENTER);

        return main;
    }

    // ==========================================
    // PAGE 1: DASHBOARD
    // ==========================================
    private JPanel createDashboardPage() {
        JPanel page = new JPanel(new BorderLayout(0, 18));
        page.setBackground(BACKGROUND);
        page.setBorder(new EmptyBorder(22, 28, 22, 28));

        // Metric Cards
        JPanel metrics = new JPanel(new GridLayout(1, 4, 14, 0));
        metrics.setOpaque(false);
        metrics.add(createMetricCard("Total Students", totalStudentsValue, "Registered students"));
        metrics.add(createMetricCard("Active Jobs", activeJobsValue, "Placement jobs"));
        metrics.add(createMetricCard("Pending Eligible", pendingEligibleValue, "Eligible but not notified"));
        metrics.add(createMetricCard("Notifications Sent", notificationsValue, "Notification history"));

        page.add(metrics, BorderLayout.NORTH);

        // Center Area (Recent Jobs + Quick Actions)
        JPanel center = new JPanel(new BorderLayout(15, 0));
        center.setOpaque(false);

        JPanel jobsCard = createCardPanel();
        jobsCard.setLayout(new BorderLayout(0, 10));
        jobsCard.add(createSectionTitle("Placement Job Listings"), BorderLayout.NORTH);

        JTable recentJobs = new JTable(jobsModel);
        recentJobs.setRowHeight(28);
        styleTable(recentJobs);
        recentJobs.setAutoCreateRowSorter(true);

        JScrollPane scroll = new JScrollPane(recentJobs);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        jobsCard.add(scroll, BorderLayout.CENTER);

        // Quick Actions Sidebar
        JPanel quickActions = createCardPanel();
        quickActions.setPreferredSize(new Dimension(240, 0));
        quickActions.setLayout(new BoxLayout(quickActions, BoxLayout.Y_AXIS));

        quickActions.add(createSectionTitle("Quick Actions"));
        quickActions.add(Box.createVerticalStrut(14));

        JButton viewJobs = createActionButton("View Placement Jobs");
        JButton viewStudents = createActionButton("Manage Students");
        JButton viewAnnounce = createActionButton("View Announcements");

        viewJobs.addActionListener(e -> {
            showPage("jobs", "Placement Overview");
            loadJobs();
        });
        viewStudents.addActionListener(e -> {
            showPage("students", "Students");
            loadStudents();
        });
        viewAnnounce.addActionListener(e -> {
            showPage("notifications", "Announcements");
            loadNotifications();
        });

        quickActions.add(viewJobs);
        quickActions.add(Box.createVerticalStrut(10));
        quickActions.add(viewStudents);
        quickActions.add(Box.createVerticalStrut(10));
        quickActions.add(viewAnnounce);

        center.add(jobsCard, BorderLayout.CENTER);
        center.add(quickActions, BorderLayout.EAST);

        page.add(center, BorderLayout.CENTER);

        return page;
    }

    // ==========================================
    // PAGE 2: PLACEMENT JOBS OVERVIEW
    // ==========================================
    private JPanel createJobsPage() {
        JPanel page = new JPanel(new BorderLayout(0, 12));
        page.setBackground(BACKGROUND);
        page.setBorder(new EmptyBorder(18, 28, 22, 28));

        // Filters Card
        JPanel filters = createCardPanel();
        filters.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 5, 4, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        jobsSearchField = new JTextField(18);
        styleInput(jobsSearchField);
        addFilter(filters, gbc, 0, "Search", jobsSearchField);

        jobsCompanyFilter = new JComboBox<>();
        jobsCompanyFilter.addItem("All Companies");
        styleCombo(jobsCompanyFilter);
        addFilter(filters, gbc, 1, "Company", jobsCompanyFilter);

        jobsYearFilter = new JComboBox<>();
        jobsYearFilter.addItem("All Years");
        styleCombo(jobsYearFilter);
        addFilter(filters, gbc, 2, "Passing Year", jobsYearFilter);

        jobsSortCombo = new JComboBox<>(new String[]{
                "Default", "Company A-Z", "Role A-Z", "Minimum CGPA", "Passing Year", "Deadline"
        });
        styleCombo(jobsSortCombo);
        addFilter(filters, gbc, 3, "Sort", jobsSortCombo);

        JButton clear = createSmallButton("Clear");
        clear.addActionListener(e -> {
            jobsSearchField.setText("");
            jobsCompanyFilter.setSelectedIndex(0);
            jobsYearFilter.setSelectedIndex(0);
            jobsSortCombo.setSelectedIndex(0);
            applyJobsFilter();
        });
        gbc.gridx = 4;
        gbc.gridy = 0;
        gbc.weightx = 0;
        filters.add(clear, gbc);

        // Document/Action Listeners
        jobsSearchField.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void update() {
                applyJobsFilter();
            }
        });
        jobsCompanyFilter.addActionListener(e -> applyJobsFilter());
        jobsYearFilter.addActionListener(e -> applyJobsFilter());
        jobsSortCombo.addActionListener(e -> applyJobsSort());

        page.add(filters, BorderLayout.NORTH);

        // Table Panel
        styleTable(jobsTable);
        jobsTable.setRowSorter(jobsSorter);
        jobsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        setColumnWidths(jobsTable, 100, 140, 160, 90, 80, 180, 100, 105, 120);

        JPanel card = createCardPanel();
        card.setLayout(new BorderLayout());

        JScrollPane scroll = new JScrollPane(jobsTable);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        card.add(scroll, BorderLayout.CENTER);

        // Action Toolbar at Bottom
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);

        JButton eligibleBtn = createSmallButton("View Eligible Students");
        eligibleBtn.addActionListener(e -> showEligibleStudents());
        bottom.add(eligibleBtn);

        card.add(bottom, BorderLayout.SOUTH);
        page.add(card, BorderLayout.CENTER);

        return page;
    }

    // ==========================================
    // PAGE 3: COMPANIES PAGE
    // ==========================================
    private JPanel createCompaniesPage() {
        JPanel page = new JPanel(new BorderLayout(0, 12));
        page.setBackground(BACKGROUND);
        page.setBorder(new EmptyBorder(18, 28, 22, 28));

        JPanel searchCard = createCardPanel();
        searchCard.setLayout(new FlowLayout(FlowLayout.LEFT));

        JLabel label = new JLabel("Search Company: ");
        label.setFont(new Font("SansSerif", Font.BOLD, 12));
        label.setForeground(TEXT);

        companiesSearchField = new JTextField(25);
        styleInput(companiesSearchField);
        companiesSearchField.setPreferredSize(new Dimension(300, 36));

        JButton clear = createSmallButton("Clear");
        clear.addActionListener(e -> companiesSearchField.setText(""));

        companiesSearchField.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void update() {
                applyCompaniesFilter();
            }
        });

        searchCard.add(label);
        searchCard.add(companiesSearchField);
        searchCard.add(clear);

        page.add(searchCard, BorderLayout.NORTH);

        styleTable(companiesTable);
        companiesTable.setRowSorter(companiesSorter);

        JPanel card = createCardPanel();
        card.setLayout(new BorderLayout());

        JScrollPane scroll = new JScrollPane(companiesTable);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        card.add(scroll, BorderLayout.CENTER);

        JLabel note = new JLabel("Company info is generated dynamically from active job postings.");
        note.setFont(new Font("SansSerif", Font.PLAIN, 11));
        note.setForeground(SECONDARY_TEXT);
        note.setBorder(new EmptyBorder(8, 4, 0, 4));

        card.add(note, BorderLayout.SOUTH);
        page.add(card, BorderLayout.CENTER);

        return page;
    }

    // ==========================================
    // PAGE 4: STUDENTS PAGE
    // ==========================================
    private JPanel createStudentsPage() {
        JPanel page = new JPanel(new BorderLayout(0, 12));
        page.setBackground(BACKGROUND);
        page.setBorder(new EmptyBorder(18, 28, 22, 28));

        // Multi-Filter Bar
        JPanel filters = createCardPanel();
        filters.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        studentsSearchField = new JTextField(16);
        styleInput(studentsSearchField);
        addFilter(filters, gbc, 0, "Search", studentsSearchField);

        studentsDepartmentFilter = new JComboBox<>();
        studentsDepartmentFilter.addItem("All Departments");
        styleCombo(studentsDepartmentFilter);
        addFilter(filters, gbc, 1, "Department", studentsDepartmentFilter);

        studentsYearFilter = new JComboBox<>();
        studentsYearFilter.addItem("All Years");
        styleCombo(studentsYearFilter);
        addFilter(filters, gbc, 2, "Passing Year", studentsYearFilter);

        studentsCgpaFilter = new JComboBox<>(new String[]{"All CGPA", "CGPA >= 9", "CGPA >= 8", "CGPA >= 7", "CGPA >= 6"});
        styleCombo(studentsCgpaFilter);
        addFilter(filters, gbc, 3, "CGPA", studentsCgpaFilter);

        studentsBacklogFilter = new JComboBox<>(new String[]{"All Backlogs", "0 Backlogs", "1 or less", "2 or less"});
        styleCombo(studentsBacklogFilter);
        addFilter(filters, gbc, 4, "Backlogs", studentsBacklogFilter);

        studentsSemesterFilter = new JComboBox<>(new String[]{
                "All Semesters", "Semester 1", "Semester 2", "Semester 3", "Semester 4", "Semester 5", "Semester 6", "Semester 7", "Semester 8"
        });
        styleCombo(studentsSemesterFilter);
        addFilter(filters, gbc, 5, "Semester", studentsSemesterFilter);

        studentsSortCombo = new JComboBox<>(new String[]{
                "Default", "Name A-Z", "CGPA High-Low", "CGPA Low-High", "Backlogs Low-High", "Passing Year", "Department A-Z"
        });
        styleCombo(studentsSortCombo);
        addFilter(filters, gbc, 6, "Sort", studentsSortCombo);

        JButton clear = createSmallButton("Clear");
        clear.addActionListener(e -> {
            studentsSearchField.setText("");
            studentsDepartmentFilter.setSelectedIndex(0);
            studentsYearFilter.setSelectedIndex(0);
            studentsCgpaFilter.setSelectedIndex(0);
            studentsBacklogFilter.setSelectedIndex(0);
            studentsSemesterFilter.setSelectedIndex(0);
            studentsSortCombo.setSelectedIndex(0);
            applyStudentsFilter();
        });

        gbc.gridx = 7;
        gbc.gridy = 0;
        gbc.weightx = 0;
        filters.add(clear, gbc);

        // Listeners
        studentsSearchField.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void update() {
                applyStudentsFilter();
            }
        });
        studentsDepartmentFilter.addActionListener(e -> applyStudentsFilter());
        studentsYearFilter.addActionListener(e -> applyStudentsFilter());
        studentsCgpaFilter.addActionListener(e -> applyStudentsFilter());
        studentsBacklogFilter.addActionListener(e -> applyStudentsFilter());
        studentsSemesterFilter.addActionListener(e -> applyStudentsFilter());
        studentsSortCombo.addActionListener(e -> applyStudentsSort());

        page.add(filters, BorderLayout.NORTH);

        // Table Panel
        styleTable(studentsTable);
        studentsTable.setRowSorter(studentsSorter);
        studentsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        setColumnWidths(studentsTable, 120, 145, 210, 125, 70, 105, 75, 75, 120, 240);

        JPanel card = createCardPanel();
        card.setLayout(new BorderLayout());

        JScrollPane scroll = new JScrollPane(studentsTable);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        card.add(scroll, BorderLayout.CENTER);

        page.add(card, BorderLayout.CENTER);

        return page;
    }

    // ==========================================
    // PAGE 5: REPORTS & SYSTEM METRICS
    // ==========================================
    private JPanel createReportsPage() {
        JPanel page = new JPanel(new BorderLayout(0, 14));
        page.setBackground(BACKGROUND);
        page.setBorder(new EmptyBorder(18, 28, 22, 28));

        // Metric Summary Header
        JPanel summary = createCardPanel();
        summary.setLayout(new BorderLayout(0, 12));
        summary.add(createSectionTitle("Placement Metric Summary"), BorderLayout.NORTH);

        JPanel metrics = new JPanel(new GridLayout(1, 4, 12, 0));
        metrics.setOpaque(false);
        metrics.add(createReportMetric("Total Students", totalStudentsValue));
        metrics.add(createReportMetric("Active Jobs", activeJobsValue));
        metrics.add(createReportMetric("Pending Eligible", pendingEligibleValue));
        metrics.add(createReportMetric("Notifications Sent", notificationsValue));

        summary.add(metrics, BorderLayout.CENTER);
        page.add(summary, BorderLayout.NORTH);

        // Visualizations Center
        JPanel charts = new JPanel(new GridLayout(1, 2, 14, 0));
        charts.setOpaque(false);

        // Bar Chart Box
        JPanel barCard = createCardPanel();
        barCard.setLayout(new BorderLayout(0, 10));
        barCard.add(createSectionTitle("System Metric Breakdown"), BorderLayout.NORTH);
        barCard.add(new ReportBarChart(), BorderLayout.CENTER);

        // Donut Chart Box
        JPanel compositionCard = createCardPanel();
        compositionCard.setLayout(new BorderLayout(0, 10));
        compositionCard.add(createSectionTitle("Metric Composition Share"), BorderLayout.NORTH);
        compositionCard.add(new ReportCompositionChart(), BorderLayout.CENTER);

        charts.add(barCard);
        charts.add(compositionCard);

        page.add(charts, BorderLayout.CENTER);

        // Data Limitation Footer Note
        JPanel noteCard = createCardPanel();
        noteCard.setLayout(new BorderLayout());

        JLabel note = new JLabel("<html><b>System Note:</b> Reports reflect active backend database telemetry including total registered students, job postings, eligible queue records, and sent announcements.</html>");
        note.setFont(new Font("SansSerif", Font.PLAIN, 11));
        note.setForeground(SECONDARY_TEXT);
        note.setBorder(new EmptyBorder(4, 4, 4, 4));

        noteCard.add(note, BorderLayout.CENTER);
        page.add(noteCard, BorderLayout.SOUTH);

        return page;
    }

    private JPanel createReportMetric(String title, JLabel value) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(LIGHT_GREEN);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(190, 220, 198)),
                new EmptyBorder(12, 12, 12, 12)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        titleLabel.setForeground(SECONDARY_TEXT);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);

        return card;
    }

    // Custom Custom AWT Horizontal Bar Chart
    private class ReportBarChart extends JPanel {
        ReportBarChart() {
            setOpaque(false);
            setPreferredSize(new Dimension(430, 310));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                String[] labels = {"Total Students", "Active Jobs", "Pending Eligible", "Notifications Sent"};
                int[] values = {
                        parseInt(totalStudentsValue.getText(), reportStudents),
                        parseInt(activeJobsValue.getText(), reportJobs),
                        parseInt(pendingEligibleValue.getText(), reportPending),
                        parseInt(notificationsValue.getText(), reportNotifications)
                };

                int left = 145;
                int right = 24;
                int top = 28;
                int bottom = 30;

                int chartWidth = Math.max(1, getWidth() - left - right);
                int chartHeight = Math.max(1, getHeight() - top - bottom);

                int max = 1;
                for (int value : values) {
                    max = Math.max(max, value);
                }

                int rowHeight = chartHeight / labels.length;
                FontMetrics metrics = g.getFontMetrics();

                for (int i = 0; i < labels.length; i++) {
                    int y = top + i * rowHeight + rowHeight / 2;

                    g.setColor(SECONDARY_TEXT);
                    g.setFont(new Font("SansSerif", Font.PLAIN, 11));
                    g.drawString(labels[i], 8, y + 4);

                    int barHeight = Math.min(28, rowHeight - 14);
                    int barY = y - barHeight / 2;
                    int barWidth = (int) (chartWidth * (values[i] / (double) max));

                    g.setColor(LIGHT_GREEN);
                    g.fillRoundRect(left, barY, chartWidth, barHeight, 8, 8);

                    g.setColor(PRIMARY_GREEN);
                    g.fillRoundRect(left, barY, Math.max(2, barWidth), barHeight, 8, 8);

                    g.setColor(TEXT);
                    g.setFont(new Font("SansSerif", Font.BOLD, 11));

                    String valStr = String.valueOf(values[i]);
                    g.drawString(valStr, Math.min(left + chartWidth - 5 - metrics.stringWidth(valStr), left + barWidth + 8), y + 4);
                }
            } finally {
                g.dispose();
            }
        }
    }

    // Custom AWT Donut Chart
    private class ReportCompositionChart extends JPanel {
        ReportCompositionChart() {
            setOpaque(false);
            setPreferredSize(new Dimension(430, 310));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                String[] labels = {"Students", "Jobs", "Pending", "Notifications"};
                int[] values = {
                        parseInt(totalStudentsValue.getText(), reportStudents),
                        parseInt(activeJobsValue.getText(), reportJobs),
                        parseInt(pendingEligibleValue.getText(), reportPending),
                        parseInt(notificationsValue.getText(), reportNotifications)
                };

                long total = 0;
                for (int value : values) {
                    total += Math.max(0, value);
                }

                int cx = Math.min(150, getWidth() / 3);
                int cy = Math.max(120, getHeight() / 2 - 5);
                int diameter = Math.min(185, Math.max(130, Math.min(getWidth() / 2, getHeight() - 70)));

                int startAngle = 90;

                g.setColor(BORDER);
                g.fillOval(cx - diameter / 2, cy - diameter / 2, diameter, diameter);

                if (total > 0) {
                    for (int i = 0; i < values.length; i++) {
                        if (values[i] <= 0) continue;

                        int angle = (int) Math.round(360.0 * values[i] / total);
                        if (i == values.length - 1) angle = 360 - (90 - startAngle);

                        g.setColor(reportChartColor(i));
                        g.fillArc(cx - diameter / 2, cy - diameter / 2, diameter, diameter, startAngle, -angle);

                        startAngle -= angle;
                    }
                }

                // Inner Cutout Oval
                int inner = diameter / 2;
                g.setColor(Color.WHITE);
                g.fillOval(cx - inner / 2, cy - inner / 2, inner, inner);

                g.setColor(TEXT);
                g.setFont(new Font("SansSerif", Font.BOLD, 13));
                String center = "Metrics";
                FontMetrics fm = g.getFontMetrics();
                g.drawString(center, cx - fm.stringWidth(center) / 2, cy + 4);

                // Legend
                int legendX = Math.min(cx + diameter / 2 + 25, getWidth() - 165);
                int