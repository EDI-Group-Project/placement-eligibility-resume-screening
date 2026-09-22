package com.placement.ui;

import com.placement.sockets.SocketClient;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class DirectorDashboard extends JFrame {

    // =========================================================
    // THEME
    // =========================================================

    private static final Color PRIMARY_GREEN =
            new Color(27, 117, 61);

    private static final Color DARK_GREEN =
            new Color(20, 92, 48);

    private static final Color LIGHT_GREEN =
            new Color(232, 245, 236);

    private static final Color BACKGROUND =
            new Color(247, 248, 245);

    private static final Color CARD =
            Color.WHITE;

    private static final Color TEXT =
            new Color(38, 50, 43);

    private static final Color SECONDARY_TEXT =
            new Color(105, 115, 108);

    private static final Color BORDER =
            new Color(214, 220, 215);

    private static final Color ERROR =
            new Color(190, 45, 45);

    // =========================================================
    // SESSION
    // =========================================================

    private final String displayName;
    private final String sessionToken;
    private final SocketClient socketClient;

    // =========================================================
    // MAIN UI
    // =========================================================

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);

    private final JLabel pageTitle =
            new JLabel("Dashboard");

    // =========================================================
    // DASHBOARD STATISTICS
    // =========================================================

    private final JLabel totalStudentsValue =
            createMetricValueLabel();

    private final JLabel activeJobsValue =
            createMetricValueLabel();

    private final JLabel pendingEligibleValue =
            createMetricValueLabel();

    private final JLabel notificationsValue =
            createMetricValueLabel();

    // =========================================================
    // JOB TABLE
    // =========================================================

    private final DefaultTableModel jobsModel =
            createModel(new String[]{
                    "Job ID",
                    "Company",
                    "Role",
                    "Package",
                    "Min CGPA",
                    "Branches",
                    "Max Backlogs",
                    "Passing Year",
                    "Deadline"
            });

    private final JTable jobsTable =
            new JTable(jobsModel);

    private final TableRowSorter<DefaultTableModel> jobsSorter =
            new TableRowSorter<>(jobsModel);

    private JTextField jobsSearchField;
    private JComboBox<String> jobsCompanyFilter;
    private JComboBox<String> jobsYearFilter;
    private JComboBox<String> jobsSortCombo;

    // =========================================================
    // STUDENT TABLE
    // =========================================================

    private final DefaultTableModel studentsModel =
            createModel(new String[]{
                    "PRN",
                    "Name",
                    "Email",
                    "Department",
                    "CGPA",
                    "Passing Year",
                    "Backlogs",
                    "Semester",
                    "Phone",
                    "Skills"
            });

    private final JTable studentsTable =
            new JTable(studentsModel);

    private final TableRowSorter<DefaultTableModel> studentsSorter =
            new TableRowSorter<>(studentsModel);

    private JTextField studentsSearchField;
    private JComboBox<String> studentsDepartmentFilter;
    private JComboBox<String> studentsYearFilter;
    private JComboBox<String> studentsCgpaFilter;
    private JComboBox<String> studentsBacklogFilter;
    private JComboBox<String> studentsSemesterFilter;
    private JComboBox<String> studentsSortCombo;

    // =========================================================
    // COMPANY TABLE
    // =========================================================

    private final DefaultTableModel companiesModel =
            createModel(new String[]{
                    "Company",
                    "Open Jobs",
                    "Roles",
                    "Latest Deadline"
            });

    private final JTable companiesTable =
            new JTable(companiesModel);

    private final TableRowSorter<DefaultTableModel> companiesSorter =
            new TableRowSorter<>(companiesModel);

    private JTextField companiesSearchField;

    // =========================================================
    // NOTIFICATION TABLE
    // =========================================================

    private final DefaultTableModel notificationsModel =
            createModel(new String[]{
                    "ID",
                    "Job ID",
                    "Subject",
                    "Message",
                    "Recipient Group",
                    "Recipients",
                    "Created At"
            });

    private final JTable notificationsTable =
            new JTable(notificationsModel);

    private final TableRowSorter<DefaultTableModel> notificationsSorter =
            new TableRowSorter<>(notificationsModel);

    private JTextField notificationsSearchField;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public DirectorDashboard(
            String displayName,
            String sessionToken) {

        this.displayName =
                displayName == null || displayName.isBlank()
                        ? "Director"
                        : displayName;

        this.sessionToken = sessionToken;
        this.socketClient = new SocketClient();

        setTitle(
                "Placement Eligibility Portal - Director Dashboard");

        setSize(1200, 760);
        setMinimumSize(new Dimension(1050, 680));

        setDefaultCloseOperation(
                JFrame.DISPOSE_ON_CLOSE);

        setLocationRelativeTo(null);

        initializeUI();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                // Nothing to close here.
                // SocketClient creates short-lived connections.
            }
        });

        loadDashboardData();
    }

    /*
     * Compatibility constructor.
     *
     * Protected commands require a token, so the normal application
     * flow should use:
     *
     * new DirectorDashboard(displayName, sessionToken)
     */
    public DirectorDashboard() {
        this("Director", null);
    }

    // =========================================================
    // INITIALIZE UI
    // =========================================================

    private void initializeUI() {

        JPanel root =
                new JPanel(new BorderLayout());

        root.setBackground(BACKGROUND);

        root.add(
                createSidebar(),
                BorderLayout.WEST);

        root.add(
                createMainArea(),
                BorderLayout.CENTER);

        setContentPane(root);
    }

    // =========================================================
    // SIDEBAR
    // =========================================================

    private JPanel createSidebar() {

        JPanel sidebar =
                new JPanel(new BorderLayout());

        sidebar.setPreferredSize(
                new Dimension(220, 0));

        sidebar.setBackground(PRIMARY_GREEN);

        sidebar.setBorder(
                new EmptyBorder(
                        28,
                        18,
                        22,
                        18));

        // -----------------------------------------------------
        // BRAND
        // -----------------------------------------------------

        JPanel top =
                new JPanel();

        top.setOpaque(false);

        top.setLayout(
                new BoxLayout(
                        top,
                        BoxLayout.Y_AXIS));

        JLabel logo =
                new JLabel("PLACEMENT");

        logo.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        24));

        logo.setForeground(Color.WHITE);

        JLabel portal =
                new JLabel(
                        "ELIGIBILITY PORTAL");

        portal.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        11));

        portal.setForeground(
                new Color(211, 235, 218));

        top.add(logo);
        top.add(Box.createVerticalStrut(4));
        top.add(portal);
        top.add(Box.createVerticalStrut(35));

        // -----------------------------------------------------
        // MENU
        // -----------------------------------------------------

        JPanel menu =
                new JPanel();

        menu.setOpaque(false);

        menu.setLayout(
                new BoxLayout(
                        menu,
                        BoxLayout.Y_AXIS));

        JButton dashboard =
                createSidebarButton("Dashboard");

        JButton overview =
                createSidebarButton(
                        "Placement Overview");

        JButton companies =
                createSidebarButton("Companies");

        JButton students =
                createSidebarButton("Students");

        JButton reports =
                createSidebarButton("Reports");

        JButton announcements =
                createSidebarButton(
                        "Announcements");

        dashboard.addActionListener(e -> {
            showPage(
                    "dashboard",
                    "Dashboard");

            loadDashboardData();
        });

        overview.addActionListener(e -> {
            showPage(
                    "jobs",
                    "Placement Overview");

            loadJobs();
        });

        companies.addActionListener(e -> {
            showPage(
                    "companies",
                    "Companies");

            loadJobs();
        });

        students.addActionListener(e -> {
            showPage(
                    "students",
                    "Students");

            loadStudents();
        });

        reports.addActionListener(e -> {
            showPage(
                    "reports",
                    "Reports");

            loadDashboardStats();
        });

        announcements.addActionListener(e -> {
            showPage(
                    "notifications",
                    "Announcements");

            loadNotifications();
        });

        menu.add(dashboard);
        menu.add(Box.createVerticalStrut(7));

        menu.add(overview);
        menu.add(Box.createVerticalStrut(7));

        menu.add(companies);
        menu.add(Box.createVerticalStrut(7));

        menu.add(students);
        menu.add(Box.createVerticalStrut(7));

        menu.add(reports);
        menu.add(Box.createVerticalStrut(7));

        menu.add(announcements);

        top.add(menu);

        sidebar.add(
                top,
                BorderLayout.NORTH);

        // -----------------------------------------------------
        // BOTTOM
        // -----------------------------------------------------

        JPanel bottom =
                new JPanel();

        bottom.setOpaque(false);

        bottom.setLayout(
                new BoxLayout(
                        bottom,
                        BoxLayout.Y_AXIS));

        JLabel user =
                new JLabel(
                        "<html><b>"
                                + escapeHtml(displayName)
                                + "</b><br>"
                                + "<font size='2'>Director</font>"
                                + "</html>");

        user.setForeground(Color.WHITE);

        JButton logout =
                createSidebarButton("Logout");

        logout.addActionListener(
                e -> logout());

        bottom.add(user);

        bottom.add(
                Box.createVerticalStrut(15));

        bottom.add(logout);

        sidebar.add(
                bottom,
                BorderLayout.SOUTH);

        return sidebar;
    }

    // =========================================================
    // MAIN AREA
    // =========================================================

    private JPanel createMainArea() {

        JPanel main =
                new JPanel(new BorderLayout());

        main.setBackground(BACKGROUND);

        // -----------------------------------------------------
        // HEADER
        // -----------------------------------------------------

        JPanel header =
                new JPanel(
                        new BorderLayout());

        header.setBackground(Color.WHITE);

        header.setBorder(
                new EmptyBorder(
                        20,
                        28,
                        18,
                        28));

        pageTitle.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        25));

        pageTitle.setForeground(TEXT);

        JLabel subtitle =
                new JLabel(
                        "Placement management and eligibility overview");

        subtitle.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        12));

        subtitle.setForeground(
                SECONDARY_TEXT);

        JPanel titleBox =
                new JPanel();

        titleBox.setOpaque(false);

        titleBox.setLayout(
                new BoxLayout(
                        titleBox,
                        BoxLayout.Y_AXIS));

        titleBox.add(pageTitle);
        titleBox.add(
                Box.createVerticalStrut(4));
        titleBox.add(subtitle);

        header.add(
                titleBox,
                BorderLayout.WEST);

        JButton refresh =
                createSmallButton("Refresh");

        refresh.addActionListener(
                e -> refreshCurrentPage());

        header.add(
                refresh,
                BorderLayout.EAST);

        main.add(
                header,
                BorderLayout.NORTH);

        // -----------------------------------------------------
        // PAGES
        // -----------------------------------------------------

        contentPanel.setBackground(BACKGROUND);

        contentPanel.add(
                createDashboardPage(),
                "dashboard");

        contentPanel.add(
                createJobsPage(),
                "jobs");

        contentPanel.add(
                createCompaniesPage(),
                "companies");

        contentPanel.add(
                createStudentsPage(),
                "students");

        contentPanel.add(
                createReportsPage(),
                "reports");

        contentPanel.add(
                createNotificationsPage(),
                "notifications");

        main.add(
                contentPanel,
                BorderLayout.CENTER);

        return main;
    }

    // =========================================================
    // DASHBOARD
    // =========================================================

    private JPanel createDashboardPage() {

        JPanel page =
                new JPanel(
                        new BorderLayout(
                                0,
                                18));

        page.setBackground(BACKGROUND);

        page.setBorder(
                new EmptyBorder(
                        22,
                        28,
                        22,
                        28));

        // -----------------------------------------------------
        // METRIC CARDS
        // -----------------------------------------------------

        JPanel metrics =
                new JPanel(
                        new GridLayout(
                                1,
                                4,
                                14,
                                0));

        metrics.setOpaque(false);

        metrics.add(
                createMetricCard(
                        "Total Students",
                        totalStudentsValue,
                        "Students registered"));

        metrics.add(
                createMetricCard(
                        "Active Jobs",
                        activeJobsValue,
                        "Placement jobs"));

        metrics.add(
                createMetricCard(
                        "Pending Eligible",
                        pendingEligibleValue,
                        "Eligible but not notified"));

        metrics.add(
                createMetricCard(
                        "Notifications Sent",
                        notificationsValue,
                        "Notification records"));

        page.add(
                metrics,
                BorderLayout.NORTH);

        // -----------------------------------------------------
        // CENTER
        // -----------------------------------------------------

        JPanel center =
                new JPanel(
                        new BorderLayout(
                                15,
                                0));

        center.setOpaque(false);

        // -----------------------------------------------------
        // RECENT JOBS
        // -----------------------------------------------------

        JPanel jobsCard =
                createCardPanel();

        jobsCard.setLayout(
                new BorderLayout(
                        0,
                        10));

        jobsCard.add(
                createSectionTitle(
                        "Placement Jobs"),
                BorderLayout.NORTH);

        JTable recentJobs =
                new JTable(jobsModel);

        recentJobs.setRowHeight(29);

        recentJobs.setAutoCreateRowSorter(true);

        styleTable(recentJobs);

        JScrollPane scroll =
                new JScrollPane(recentJobs);

        scroll.setBorder(
                BorderFactory.createLineBorder(
                        BORDER));

        jobsCard.add(
                scroll,
                BorderLayout.CENTER);

        center.add(
                jobsCard,
                BorderLayout.CENTER);

        // -----------------------------------------------------
        // QUICK ACTIONS
        // -----------------------------------------------------

        JPanel quickActions =
                createCardPanel();

        quickActions.setPreferredSize(
                new Dimension(
                        245,
                        0));

        quickActions.setLayout(
                new BoxLayout(
                        quickActions,
                        BoxLayout.Y_AXIS));

        quickActions.add(
                createSectionTitle(
                        "Quick Actions"));

        quickActions.add(
                Box.createVerticalStrut(16));

        JButton jobs =
                createActionButton(
                        "View Placement Jobs");

        JButton students =
                createActionButton(
                        "View Students");

        JButton announcements =
                createActionButton(
                        "View Announcements");

        JButton refresh =
                createActionButton(
                        "Refresh Data");

        jobs.addActionListener(e -> {
            showPage(
                    "jobs",
                    "Placement Overview");

            loadJobs();
        });

        students.addActionListener(e -> {
            showPage(
                    "students",
                    "Students");

            loadStudents();
        });

        announcements.addActionListener(e -> {
            showPage(
                    "notifications",
                    "Announcements");

            loadNotifications();
        });

        refresh.addActionListener(
                e -> loadDashboardData());

        quickActions.add(jobs);
        quickActions.add(
                Box.createVerticalStrut(10));

        quickActions.add(students);
        quickActions.add(
                Box.createVerticalStrut(10));

        quickActions.add(announcements);
        quickActions.add(
                Box.createVerticalStrut(10));

        quickActions.add(refresh);

        center.add(
                quickActions,
                BorderLayout.EAST);

        page.add(
                center,
                BorderLayout.CENTER);

        return page;
    }

    // =========================================================
    // JOBS PAGE
    // =========================================================

    private JPanel createJobsPage() {

        JPanel page =
                new JPanel(
                        new BorderLayout(
                                0,
                                12));

        page.setBackground(BACKGROUND);

        page.setBorder(
                new EmptyBorder(
                        18,
                        28,
                        22,
                        28));

        // -----------------------------------------------------
        // FILTER BAR
        // -----------------------------------------------------

        JPanel filters =
                createCardPanel();

        filters.setLayout(
                new GridBagLayout());

        GridBagConstraints gbc =
                new GridBagConstraints();

        gbc.insets =
                new Insets(4, 5, 4, 5);

        gbc.fill =
                GridBagConstraints.HORIZONTAL;

        gbc.weightx = 1;

        // Search
        jobsSearchField =
                new JTextField(18);

        styleInput(jobsSearchField);

        addFilter(
                filters,
                gbc,
                0,
                "Search",
                jobsSearchField);

        // Company
        jobsCompanyFilter =
                new JComboBox<>();

        jobsCompanyFilter.addItem(
                "All Companies");

        styleCombo(
                jobsCompanyFilter);

        addFilter(
                filters,
                gbc,
                1,
                "Company",
                jobsCompanyFilter);

        // Year
        jobsYearFilter =
                new JComboBox<>();

        jobsYearFilter.addItem(
                "All Years");

        styleCombo(
                jobsYearFilter);

        addFilter(
                filters,
                gbc,
                2,
                "Passing Year",
                jobsYearFilter);

        // Sort
        jobsSortCombo =
                new JComboBox<>(
                        new String[]{
                                "Default",
                                "Company A-Z",
                                "Role A-Z",
                                "Minimum CGPA",
                                "Passing Year",
                                "Deadline"
                        });

        styleCombo(jobsSortCombo);

        addFilter(
                filters,
                gbc,
                3,
                "Sort",
                jobsSortCombo);

        JButton clear =
                createSmallButton("Clear");

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

        // Listeners
        jobsSearchField
                .getDocument()
                .addDocumentListener(
                        new SimpleDocumentListener(
                                this::applyJobsFilter));

        jobsCompanyFilter.addActionListener(
                e -> applyJobsFilter());

        jobsYearFilter.addActionListener(
                e -> applyJobsFilter());

        jobsSortCombo.addActionListener(
                e -> applyJobsSort());

        page.add(
                filters,
                BorderLayout.NORTH);

        // -----------------------------------------------------
        // TABLE
        // -----------------------------------------------------

        styleTable(jobsTable);

        jobsTable.setRowSorter(
                jobsSorter);

        jobsTable.setAutoResizeMode(
                JTable.AUTO_RESIZE_OFF);

        setColumnWidths(
                jobsTable,
                100,
                140,
                160,
                90,
                80,
                180,
                100,
                105,
                120);

        JPanel card =
                createCardPanel();

        card.setLayout(
                new BorderLayout());

        JScrollPane scroll =
                new JScrollPane(
                        jobsTable);

        scroll.setBorder(
                BorderFactory.createLineBorder(
                        BORDER));

        card.add(
                scroll,
                BorderLayout.CENTER);

        // -----------------------------------------------------
        // ELIGIBILITY BUTTON
        // -----------------------------------------------------

        JPanel bottom =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT));

        bottom.setOpaque(false);

        JButton eligible =
                createSmallButton(
                        "View Eligible Students");

        eligible.addActionListener(
                e -> showEligibleStudents());

        bottom.add(eligible);

        card.add(
                bottom,
                BorderLayout.SOUTH);

        page.add(
                card,
                BorderLayout.CENTER);

        return page;
    }

    // =========================================================
    // COMPANIES PAGE
    // =========================================================

    private JPanel createCompaniesPage() {

        JPanel page =
                new JPanel(
                        new BorderLayout(
                                0,
                                12));

        page.setBackground(BACKGROUND);

        page.setBorder(
                new EmptyBorder(
                        18,
                        28,
                        22,
                        28));

        JPanel searchCard =
                createCardPanel();

        searchCard.setLayout(
                new FlowLayout(
                        FlowLayout.LEFT));

        JLabel label =
                new JLabel(
                        "Search Company");

        companiesSearchField =
                new JTextField(25);

        styleInput(
                companiesSearchField);

        companiesSearchField.setPreferredSize(
                new Dimension(
                        300,
                        38));

        JButton clear =
                createSmallButton("Clear");

        clear.addActionListener(
                e -> companiesSearchField.setText(""));

        companiesSearchField
                .getDocument()
                .addDocumentListener(
                        new SimpleDocumentListener(
                                this::applyCompaniesFilter));

        searchCard.add(label);
        searchCard.add(
                companiesSearchField);
        searchCard.add(clear);

        page.add(
                searchCard,
                BorderLayout.NORTH);

        styleTable(companiesTable);

        companiesTable.setRowSorter(
                companiesSorter);

        JPanel card =
                createCardPanel();

        card.setLayout(
                new BorderLayout());

        JScrollPane scroll =
                new JScrollPane(
                        companiesTable);

        scroll.setBorder(
                BorderFactory.createLineBorder(
                        BORDER));

        card.add(
                scroll,
                BorderLayout.CENTER);

        JLabel note =
                new JLabel(
                        "Company information is derived from job postings because "
                                + "the current socket protocol has no GET_COMPANIES command.");

        note.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        11));

        note.setForeground(
                SECONDARY_TEXT);

        note.setBorder(
                new EmptyBorder(
                        8,
                        4,
                        0,
                        4));

        card.add(
                note,
                BorderLayout.SOUTH);

        page.add(
                card,
                BorderLayout.CENTER);

        return page;
    }

    // =========================================================
    // STUDENTS PAGE
    // =========================================================

    private JPanel createStudentsPage() {

        JPanel page =
                new JPanel(
                        new BorderLayout(
                                0,
                                12));

        page.setBackground(BACKGROUND);

        page.setBorder(
                new EmptyBorder(
                        18,
                        28,
                        22,
                        28));

        JPanel filters =
                createCardPanel();

        filters.setLayout(
                new GridBagLayout());

        GridBagConstraints gbc =
                new GridBagConstraints();

        gbc.insets =
                new Insets(3, 5, 3, 5);

        gbc.fill =
                GridBagConstraints.HORIZONTAL;

        gbc.weightx = 1;

        // Search
        studentsSearchField =
                new JTextField(16);

        styleInput(
                studentsSearchField);

        addFilter(
                filters,
                gbc,
                0,
                "Search",
                studentsSearchField);

        // Department
        studentsDepartmentFilter =
                new JComboBox<>();

        studentsDepartmentFilter.addItem(
                "All Departments");

        styleCombo(
                studentsDepartmentFilter);

        addFilter(
                filters,
                gbc,
                1,
                "Department",
                studentsDepartmentFilter);

        // Year
        studentsYearFilter =
                new JComboBox<>();

        studentsYearFilter.addItem(
                "All Years");

        styleCombo(
                studentsYearFilter);

        addFilter(
                filters,
                gbc,
                2,
                "Passing Year",
                studentsYearFilter);

        // CGPA
        studentsCgpaFilter =
                new JComboBox<>(
                        new String[]{
                                "All CGPA",
                                "CGPA >= 9",
                                "CGPA >= 8",
                                "CGPA >= 7",
                                "CGPA >= 6"
                        });

        styleCombo(
                studentsCgpaFilter);

        addFilter(
                filters,
                gbc,
                3,
                "CGPA",
                studentsCgpaFilter);

        // Backlogs
        studentsBacklogFilter =
                new JComboBox<>(
                        new String[]{
                                "All Backlogs",
                                "0 Backlogs",
                                "1 or less",
                                "2 or less"
                        });

        styleCombo(
                studentsBacklogFilter);

        addFilter(
                filters,
                gbc,
                4,
                "Backlogs",
                studentsBacklogFilter);

        // Semester
        studentsSemesterFilter =
                new JComboBox<>(
                        new String[]{
                                "All Semesters",
                                "Semester 1",
                                "Semester 2",
                                "Semester 3",
                                "Semester 4",
                                "Semester 5",
                                "Semester 6",
                                "Semester 7",
                                "Semester 8"
                        });

        styleCombo(
                studentsSemesterFilter);

        addFilter(
                filters,
                gbc,
                5,
                "Semester",
                studentsSemesterFilter);

        // Sort
        studentsSortCombo =
                new JComboBox<>(
                        new String[]{
                                "Default",
                                "Name A-Z",
                                "CGPA High-Low",
                                "CGPA Low-High",
                                "Backlogs Low-High",
                                "Passing Year",
                                "Department A-Z"
                        });

        styleCombo(
                studentsSortCombo);

        addFilter(
                filters,
                gbc,
                6,
                "Sort",
                studentsSortCombo);

        JButton clear =
                createSmallButton("Clear");

        clear.addActionListener(e -> {
            studentsSearchField.setText("");
            studentsDepartmentFilter
                    .setSelectedIndex(0);
            studentsYearFilter
                    .setSelectedIndex(0);
            studentsCgpaFilter
                    .setSelectedIndex(0);
            studentsBacklogFilter
                    .setSelectedIndex(0);
            studentsSemesterFilter
                    .setSelectedIndex(0);
            studentsSortCombo
                    .setSelectedIndex(0);

            applyStudentsFilter();
        });

        gbc.gridx = 7;
        gbc.gridy = 0;
        gbc.weightx = 0;

        filters.add(
                clear,
                gbc);

        // Listeners
        studentsSearchField
                .getDocument()
                .addDocumentListener(
                        new SimpleDocumentListener(
                                this::applyStudentsFilter));

        studentsDepartmentFilter
                .addActionListener(
                        e -> applyStudentsFilter());

        studentsYearFilter
                .addActionListener(
                        e -> applyStudentsFilter());

        studentsCgpaFilter
                .addActionListener(
                        e -> applyStudentsFilter());

        studentsBacklogFilter
                .addActionListener(
                        e -> applyStudentsFilter());

        studentsSemesterFilter
                .addActionListener(
                        e -> applyStudentsFilter());

        studentsSortCombo
                .addActionListener(
                        e -> applyStudentsSort());

        page.add(
                filters,
                BorderLayout.NORTH);

        // -----------------------------------------------------
        // TABLE
        // -----------------------------------------------------

        styleTable(studentsTable);

        studentsTable.setRowSorter(
                studentsSorter);

        studentsTable.setAutoResizeMode(
                JTable.AUTO_RESIZE_OFF);

        setColumnWidths(
                studentsTable,
                120,
                145,
                210,
                125,
                70,
                105,
                75,
                75,
                120,
                240);

        JPanel card =
                createCardPanel();

        card.setLayout(
                new BorderLayout());

        JScrollPane scroll =
                new JScrollPane(
                        studentsTable);

        scroll.setBorder(
                BorderFactory.createLineBorder(
                        BORDER));

        card.add(
                scroll,
                BorderLayout.CENTER);

        page.add(
                card,
                BorderLayout.CENTER);

        return page;
    }

    // =========================================================
    // REPORTS
    // =========================================================

   private JPanel createReportsPage() {

    JPanel page =
            new JPanel(new BorderLayout(0, 14));

    page.setBackground(BACKGROUND);

    page.setBorder(
            new EmptyBorder(
                    18,
                    28,
                    22,
                    28));

    // ---------------------------------------------------------
    // TOP: REPORT SUMMARY
    // ---------------------------------------------------------

    JPanel summary =
            createCardPanel();

    summary.setLayout(
            new BorderLayout(
                    0,
                    12));

    summary.add(
            createSectionTitle(
                    "Placement System Report"),
            BorderLayout.NORTH);

    JPanel metrics =
            new JPanel(
                    new GridLayout(
                            1,
                            4,
                            12,
                            0));

    metrics.setOpaque(false);

    metrics.add(
            createReportMetric(
                    "Total Students",
                    totalStudentsValue));

    metrics.add(
            createReportMetric(
                    "Active Jobs",
                    activeJobsValue));

    metrics.add(
            createReportMetric(
                    "Pending Eligible",
                    pendingEligibleValue));

    metrics.add(
            createReportMetric(
                    "Notifications Sent",
                    notificationsValue));

    summary.add(
            metrics,
            BorderLayout.CENTER);

    page.add(
            summary,
            BorderLayout.NORTH);

    // ---------------------------------------------------------
    // CENTER: VISUALIZATIONS
    // ---------------------------------------------------------

    JPanel charts =
            new JPanel(
                    new GridLayout(
                            1,
                            2,
                            14,
                            0));

    charts.setOpaque(false);

    JPanel barCard =
            createCardPanel();

    barCard.setLayout(
            new BorderLayout(
                    0,
                    10));

    barCard.add(
            createSectionTitle(
                    "Backend-Provided Counts"),
            BorderLayout.NORTH);

    barCard.add(
            new ReportBarChart(),
            BorderLayout.CENTER);

    charts.add(barCard);

    JPanel compositionCard =
            createCardPanel();

    compositionCard.setLayout(
            new BorderLayout(
                    0,
                    10));

    compositionCard.add(
            createSectionTitle(
                    "Data Composition"),
            BorderLayout.NORTH);

    compositionCard.add(
            new ReportCompositionChart(),
            BorderLayout.CENTER);

    charts.add(compositionCard);

    page.add(
            charts,
            BorderLayout.CENTER);

    // ---------------------------------------------------------
    // BOTTOM: DATA LIMITATION NOTE
    // ---------------------------------------------------------

    JPanel noteCard =
            createCardPanel();

    noteCard.setLayout(
            new BorderLayout());

    JLabel note =
            new JLabel(
                    "<html>"
                            + "<b>Report scope:</b> "
                            + "These visualizations use only the four "
                            + "statistics exposed by the current "
                            + "GET_DASHBOARD_STATS socket command. "
                            + "The backend does not currently provide "
                            + "placed-student count or placement-rate data, "
                            + "so those values are intentionally not shown."
                            + "<br><br>"
                            + "<b>Important:</b> "
                            + "The Data Composition chart is a descriptive "
                            + "comparison of the four available counts. "
                            + "It must not be interpreted as a placement "
                            + "percentage."
                            + "</html>");

    note.setFont(
            new Font(
                    "SansSerif",
                    Font.PLAIN,
                    11));

    note.setForeground(
            SECONDARY_TEXT);

    note.setBorder(
            new EmptyBorder(
                    4,
                    4,
                    4,
                    4));

    noteCard.add(
            note,
            BorderLayout.CENTER);

    page.add(
            noteCard,
            BorderLayout.SOUTH);

    return page;
}
private JPanel createReportMetric(
        String title,
        JLabel value) {

    JPanel card =
            new JPanel(
                    new BorderLayout(
                            0,
                            6));

    card.setBackground(
            LIGHT_GREEN);

    card.setBorder(
            BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(
                            new Color(
                                    190,
                                    220,
                                    198)),
                    new EmptyBorder(
                            12,
                            12,
                            12,
                            12)));

    JLabel titleLabel =
            new JLabel(title);

    titleLabel.setFont(
            new Font(
                    "SansSerif",
                    Font.BOLD,
                    11));

    titleLabel.setForeground(
            SECONDARY_TEXT);

    card.add(
            titleLabel,
            BorderLayout.NORTH);

    card.add(
            value,
            BorderLayout.CENTER);

    return card;
}

/*
 * Horizontal bar chart using Swing/AWT only.
 * Values are read directly from the existing metric labels, which are
 * populated from GET_DASHBOARD_STATS.
 */
private class ReportBarChart extends JPanel {

    ReportBarChart() {

        setOpaque(false);

        setPreferredSize(
                new Dimension(
                        430,
                        310));
    }

    @Override
    protected void paintComponent(
            Graphics graphics) {

        super.paintComponent(graphics);

        Graphics2D g =
                (Graphics2D) graphics.create();

        try {

            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            String[] labels = {
                    "Total Students",
                    "Active Jobs",
                    "Pending Eligible",
                    "Notifications Sent"
            };

            int[] values = {
                    parseInt(
                            totalStudentsValue.getText(),
                            0),
                    parseInt(
                            activeJobsValue.getText(),
                            0),
                    parseInt(
                            pendingEligibleValue.getText(),
                            0),
                    parseInt(
                            notificationsValue.getText(),
                            0)
            };

            int left = 145;
            int right = 24;
            int top = 28;
            int bottom = 30;

            int chartWidth =
                    Math.max(
                            1,
                            getWidth()
                                    - left
                                    - right);

            int chartHeight =
                    Math.max(
                            1,
                            getHeight()
                                    - top
                                    - bottom);

            int max =
                    1;

            for (int value : values) {
                max = Math.max(max, value);
            }

            int rowHeight =
                    chartHeight / labels.length;

            FontMetrics metrics =
                    g.getFontMetrics();

            for (int i = 0;
                 i < labels.length;
                 i++) {

                int y =
                        top
                                + i * rowHeight
                                + rowHeight / 2;

                g.setColor(
                        SECONDARY_TEXT);

                g.setFont(
                        new Font(
                                "SansSerif",
                                Font.PLAIN,
                                11));

                g.drawString(
                        labels[i],
                        8,
                        y + 4);

                int barHeight =
                        Math.min(
                                28,
                                rowHeight - 14);

                int barY =
                        y
                                - barHeight / 2;

                int barWidth =
                        (int) (
                                chartWidth
                                        * (values[i]
                                        / (double) max));

                g.setColor(
                        LIGHT_GREEN);

                g.fillRoundRect(
                        left,
                        barY,
                        chartWidth,
                        barHeight,
                        8,
                        8);

                g.setColor(
                        PRIMARY_GREEN);

                g.fillRoundRect(
                        left,
                        barY,
                        Math.max(
                                2,
                                barWidth),
                        barHeight,
                        8,
                        8);

                g.setColor(TEXT);

                g.setFont(
                        new Font(
                                "SansSerif",
                                Font.BOLD,
                                11));

                String value =
                        String.valueOf(
                                values[i]);

                g.drawString(
                        value,
                        left
                                + Math.min(
                                chartWidth - 5
                                        - metrics.stringWidth(value),
                                barWidth + 8),
                        y + 4);
            }

        } finally {

            g.dispose();
        }
    }
}

/*
 * Descriptive composition visualization.
 * Each available backend count is represented as a share of the sum of
 * the four available counts. This is explicitly NOT a placement rate.
 */
private class ReportCompositionChart extends JPanel {

    ReportCompositionChart() {

        setOpaque(false);

        setPreferredSize(
                new Dimension(
                        430,
                        310));
    }

    @Override
    protected void paintComponent(
            Graphics graphics) {

        super.paintComponent(graphics);

        Graphics2D g =
                (Graphics2D) graphics.create();

        try {

            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            String[] labels = {
                    "Total Students",
                    "Active Jobs",
                    "Pending Eligible",
                    "Notifications Sent"
            };

            int[] values = {
                    parseInt(
                            totalStudentsValue.getText(),
                            0),
                    parseInt(
                            activeJobsValue.getText(),
                            0),
                    parseInt(
                            pendingEligibleValue.getText(),
                            0),
                    parseInt(
                            notificationsValue.getText(),
                            0)
            };

            long total = 0;

            for (int value : values) {
                total += Math.max(0, value);
            }

            int cx =
                    Math.min(
                            150,
                            getWidth() / 3);

            int cy =
                    Math.max(
                            120,
                            getHeight() / 2 - 5);

            int diameter =
                    Math.min(
                            185,
                            Math.max(
                                    130,
                                    Math.min(
                                            getWidth() / 2,
                                            getHeight() - 70)));

            int startAngle = 90;

            // Draw a neutral full circle first so zero/empty data is clear.
            g.setColor(
                    BORDER);

            g.fillOval(
                    cx - diameter / 2,
                    cy - diameter / 2,
                    diameter,
                    diameter);

            if (total > 0) {

                for (int i = 0;
                     i < values.length;
                     i++) {

                    if (values[i] <= 0) {
                        continue;
                    }

                    int angle =
                            (int) Math.round(
                                    360.0
                                            * values[i]
                                            / total);

                    if (i == values.length - 1) {
                        angle =
                                90
                                        - startAngle;
                    }

                    g.setColor(
                            reportChartColor(i));

                    g.fillArc(
                            cx - diameter / 2,
                            cy - diameter / 2,
                            diameter,
                            diameter,
                            startAngle,
                            -angle);

                    startAngle -= angle;
                }
            }

            // Center circle creates a simple donut-style visualization.
            int inner =
                    diameter / 2;

            g.setColor(
                    Color.WHITE);

            g.fillOval(
                    cx - inner / 2,
                    cy - inner / 2,
                    inner,
                    inner);

            g.setColor(
                    TEXT);

            g.setFont(
                    new Font(
                            "SansSerif",
                            Font.BOLD,
                            13));

            String center =
                    "Available";

            FontMetrics fm =
                    g.getFontMetrics();

            g.drawString(
                    center,
                    cx
                            - fm.stringWidth(center) / 2,
                    cy + 4);

            int legendX =
                    Math.min(
                            cx + diameter / 2 + 25,
                            getWidth() - 165);

            int legendY =
                    Math.max(
                            42,
                            cy - 55);

            g.setFont(
                    new Font(
                            "SansSerif",
                            Font.PLAIN,
                            11));

            for (int i = 0;
                 i < labels.length;
                 i++) {

                int y =
                        legendY
                                + i * 42;

                g.setColor(
                        reportChartColor(i));

                g.fillRoundRect(
                        legendX,
                        y,
                        13,
                        13,
                        4,
                        4);

                g.setColor(TEXT);

                g.drawString(
                        labels[i],
                        legendX + 21,
                        y + 11);

                double percentage =
                        total == 0
                                ? 0
                                : values[i]
                                * 100.0
                                / total;

                g.setColor(
                        SECONDARY_TEXT);

                g.drawString(
                        String.format(
                                "%.1f%%  (%d)",
                                percentage,
                                values[i]),
                        legendX + 21,
                        y + 26);
            }

        } finally {

            g.dispose();
        }
    }
}

/*
 * Different shades are used only to distinguish chart segments.
 * These do not represent positive/negative status.
 */
private Color reportChartColor(
        int index) {

    switch (index) {

        case 0:
            return PRIMARY_GREEN;

        case 1:
            return DARK_GREEN;

        case 2:
            return new Color(
                    84,
                    145,
                    105);

        default:
            return new Color(
                    133,
                    171,
                    145);
    }
}


    // =========================================================
    // NOTIFICATIONS
    // =========================================================

    private JPanel createNotificationsPage() {

        JPanel page =
                new JPanel(
                        new BorderLayout(
                                0,
                                12));

        page.setBackground(BACKGROUND);

        page.setBorder(
                new EmptyBorder(
                        18,
                        28,
                        22,
                        28));

        JPanel filters =
                createCardPanel();

        filters.setLayout(
                new FlowLayout(
                        FlowLayout.LEFT));

        JLabel label =
                new JLabel(
                        "Search Notifications");

        notificationsSearchField =
                new JTextField(28);

        styleInput(
                notificationsSearchField);

        notificationsSearchField.setPreferredSize(
                new Dimension(
                        320,
                        38));

        JButton clear =
                createSmallButton("Clear");

        clear.addActionListener(
                e -> notificationsSearchField.setText(""));

        notificationsSearchField
                .getDocument()
                .addDocumentListener(
                        new SimpleDocumentListener(
                                this::applyNotificationsFilter));

        filters.add(label);
        filters.add(
                notificationsSearchField);
        filters.add(clear);

        page.add(
                filters,
                BorderLayout.NORTH);

        styleTable(
                notificationsTable);

        notificationsTable.setRowSorter(
                notificationsSorter);

        notificationsTable.setAutoResizeMode(
                JTable.AUTO_RESIZE_OFF);

        setColumnWidths(
                notificationsTable,
                65,
                100,
                200,
                350,
                130,
                90,
                150);

        JPanel card =
                createCardPanel();

        card.setLayout(
                new BorderLayout());

        JScrollPane scroll =
                new JScrollPane(
                        notificationsTable);

        scroll.setBorder(
                BorderFactory.createLineBorder(
                        BORDER));

        card.add(
                scroll,
                BorderLayout.CENTER);

        page.add(
                card,
                BorderLayout.CENTER);

        return page;
    }

    // =========================================================
    // LOAD DASHBOARD DATA
    // =========================================================

    private void loadDashboardData() {

        loadDashboardStats();

        loadJobs();
    }

    // =========================================================
    // LOAD DASHBOARD STATS
    // =========================================================

    private void loadDashboardStats() {

        if (!hasSession()) {
            showError(
                    "No active session. Please log in again.");

            return;
        }

        runRequest(
                () -> socketClient.sendRequest(
                        "GET_DASHBOARD_STATS",
                        sessionToken),

                response -> {

                    if (!response.success) {
                        showError(
                                response.payload);

                        return;
                    }

                    String[] parts =
                            response.parts();

                    /*
                     * ClientHandler sends:
                     *
                     * SUCCESS|jobs|pending|sent|students
                     */

                    if (parts.length < 4) {
                        showError(
                                "Invalid dashboard statistics received.");

                        return;
                    }

                    try {

                        int jobs =
                                Integer.parseInt(parts[0]);

                        int pending =
                                Integer.parseInt(parts[1]);

                        int sent =
                                Integer.parseInt(parts[2]);

                        int students =
                                Integer.parseInt(parts[3]);

                        activeJobsValue.setText(
                                String.valueOf(jobs));

                        pendingEligibleValue.setText(
                                String.valueOf(pending));

                        notificationsValue.setText(
                                String.valueOf(sent));

                        totalStudentsValue.setText(
                                String.valueOf(students));

                    } catch (NumberFormatException ex) {

                        showError(
                                "Invalid numeric dashboard data.");
                    }
                });
    }

    // =========================================================
    // LOAD JOBS
    // =========================================================

    private void loadJobs() {

        runListRequest(
                () -> socketClient.sendListRequest(
                        "GET_JOBS"),

                response -> {

                    if (!response.success) {
                        showError(
                                response.errorMessage);

                        return;
                    }

                    jobsModel.setRowCount(0);

                    for (String line :
                            response.lines) {

                        JobRow job =
                                parseJob(line);

                        if (job == null) {
                            continue;
                        }

                        jobsModel.addRow(
                                new Object[]{
                                        job.id,
                                        job.company,
                                        job.role,
                                        job.minPackage,
                                        job.minCgpa,
                                        job.branches,
                                        job.maxBacklogs,
                                        job.passingYear,
                                        job.deadline
                                });
                    }

                    rebuildJobFilters();

                    applyJobsFilter();

                    rebuildCompanies();
                });
    }

    // =========================================================
    // LOAD STUDENTS
    // =========================================================

    private void loadStudents() {

        if (!hasSession()) {
            showError(
                    "No active session. Please log in again.");

            return;
        }

        runListRequest(
                () -> socketClient.sendListRequest(
                        "GET_STUDENTS",
                        sessionToken),

                response -> {

                    if (!response.success) {
                        showError(
                                response.errorMessage);

                        return;
                    }

                    studentsModel.setRowCount(0);

                    for (String line :
                            response.lines) {

                        StudentRow student =
                                parseStudent(line);

                        if (student == null) {
                            continue;
                        }

                        studentsModel.addRow(
                                new Object[]{
                                        student.prn,
                                        student.name,
                                        student.email,
                                        student.department,
                                        student.cgpa,
                                        student.passingYear,
                                        student.backlogs,
                                        student.semester,
                                        student.phone,
                                        student.skills
                                });
                    }

                    rebuildStudentFilters();

                    applyStudentsFilter();
                });
    }

    // =========================================================
    // LOAD NOTIFICATIONS
    // =========================================================

    private void loadNotifications() {

        if (!hasSession()) {
            showError(
                    "No active session. Please log in again.");

            return;
        }

        runListRequest(
                () -> socketClient.sendListRequest(
                        "GET_NOTIFICATIONS",
                        sessionToken),

                response -> {

                    if (!response.success) {
                        showError(
                                response.errorMessage);

                        return;
                    }

                    notificationsModel.setRowCount(0);

                    for (String line :
                            response.lines) {

                        NotificationRow notification =
                                parseNotification(line);

                        if (notification == null) {
                            continue;
                        }

                        notificationsModel.addRow(
                                new Object[]{
                                        notification.id,
                                        notification.jobId,
                                        notification.subject,
                                        notification.message,
                                        notification.recipientGroup,
                                        notification.recipientCount,
                                        notification.createdAt
                                });
                    }

                    applyNotificationsFilter();
                });
    }

    // =========================================================
    // ELIGIBLE STUDENTS
    // =========================================================

    private void showEligibleStudents() {

        int selectedRow =
                jobsTable.getSelectedRow();

        if (selectedRow < 0) {

            JOptionPane.showMessageDialog(
                    this,
                    "Select a placement job first.",
                    "No Job Selected",
                    JOptionPane.INFORMATION_MESSAGE);

            return;
        }

        int modelRow =
                jobsTable.convertRowIndexToModel(
                        selectedRow);

        String jobId =
                String.valueOf(
                        jobsModel.getValueAt(
                                modelRow,
                                0));

        String company =
                String.valueOf(
                        jobsModel.getValueAt(
                                modelRow,
                                1));

        String role =
                String.valueOf(
                        jobsModel.getValueAt(
                                modelRow,
                                2));

        if (!hasSession()) {
            showError(
                    "No active session. Please log in again.");

            return;
        }

        JDialog dialog =
                new JDialog(
                        this,
                        "Eligible Students",
                        true);

        dialog.setSize(
                1050,
                560);

        dialog.setLocationRelativeTo(
                this);

        dialog.setLayout(
                new BorderLayout(
                        10,
                        10));

        JLabel status =
                new JLabel(
                        "Loading eligible students...",
                        SwingConstants.CENTER);

        status.setBorder(
                new EmptyBorder(
                        10,
                        10,
                        10,
                        10));

        dialog.add(
                status,
                BorderLayout.NORTH);

        DefaultTableModel model =
                createModel(
                        new String[]{
                                "PRN",
                                "Name",
                                "Email",
                                "Department",
                                "CGPA",
                                "Passing Year",
                                "Backlogs",
                                "Semester",
                                "Phone",
                                "Skills"
                        });

        JTable table =
                new JTable(model);

        styleTable(table);

        table.setAutoCreateRowSorter(
                true);

        table.setAutoResizeMode(
                JTable.AUTO_RESIZE_OFF);

        setColumnWidths(
                table,
                120,
                145,
                210,
                125,
                70,
                105,
                75,
                75,
                120,
                240);

        dialog.add(
                new JScrollPane(table),
                BorderLayout.CENTER);

        JButton close =
                createSmallButton("Close");

        close.addActionListener(
                e -> dialog.dispose());

        JPanel bottom =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT));

        bottom.add(close);

        dialog.add(
                bottom,
                BorderLayout.SOUTH);

        SwingWorker<
                SocketClient.ListResponse,
                Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected SocketClient.ListResponse
                    doInBackground()
                            throws Exception {

                        return socketClient.sendListRequest(
                                "GET_ELIGIBLE_STUDENTS",
                                sessionToken,
                                jobId);
                    }

                    @Override
                    protected void done() {

                        try {

                            SocketClient.ListResponse response =
                                    get();

                            if (!response.success) {

                                status.setText(
                                        response.errorMessage);

                                return;
                            }

                            model.setRowCount(0);

                            for (String line :
                                    response.lines) {

                                StudentRow student =
                                        parseStudent(line);

                                if (student == null) {
                                    continue;
                                }

                                model.addRow(
                                        new Object[]{
                                                student.prn,
                                                student.name,
                                                student.email,
                                                student.department,
                                                student.cgpa,
                                                student.passingYear,
                                                student.backlogs,
                                                student.semester,
                                                student.phone,
                                                student.skills
                                        });
                            }

                            status.setText(
                                    response.lines.size()
                                            + " eligible student(s) for "
                                            + company
                                            + " - "
                                            + role);

                        } catch (Exception ex) {

                            status.setText(
                                    "Unable to load eligible students: "
                                            + rootMessage(ex));
                        }
                    }
                };

        worker.execute();

        dialog.setVisible(true);
    }

    // =========================================================
    // JOB FILTER
    // =========================================================

    private void applyJobsFilter() {

        if (jobsSearchField == null) {
            return;
        }

        String search =
                jobsSearchField.getText()
                        .trim()
                        .toLowerCase();

        String company =
                String.valueOf(
                        jobsCompanyFilter
                                .getSelectedItem());

        String year =
                String.valueOf(
                        jobsYearFilter
                                .getSelectedItem());

        jobsSorter.setRowFilter(
                new RowFilter<
                        DefaultTableModel,
                        Integer>() {

                    @Override
                    public boolean include(
                            Entry<
                                    ? extends DefaultTableModel,
                                    ? extends Integer> entry) {

                        StringBuilder all =
                                new StringBuilder();

                        for (int i = 0;
                             i < entry.getValueCount();
                             i++) {

                            all.append(
                                    String.valueOf(
                                            entry.getValue(i)))
                                    .append(" ");
                        }

                        if (!search.isEmpty()
                                && !all.toString()
                                .toLowerCase()
                                .contains(search)) {

                            return false;
                        }

                        if (!"All Companies".equals(company)
                                && !company.equals(
                                String.valueOf(
                                        entry.getValue(1)))) {

                            return false;
                        }

                        if (!"All Years".equals(year)
                                && !year.equals(
                                String.valueOf(
                                        entry.getValue(7)))) {

                            return false;
                        }

                        return true;
                    }
                });
    }

    // =========================================================
    // STUDENT FILTER
    // =========================================================

    private void applyStudentsFilter() {

        if (studentsSearchField == null) {
            return;
        }

        String search =
                studentsSearchField.getText()
                        .trim()
                        .toLowerCase();

        String department =
                String.valueOf(
                        studentsDepartmentFilter
                                .getSelectedItem());

        String year =
                String.valueOf(
                        studentsYearFilter
                                .getSelectedItem());

        String cgpaFilter =
                String.valueOf(
                        studentsCgpaFilter
                                .getSelectedItem());

        String backlogFilter =
                String.valueOf(
                        studentsBacklogFilter
                                .getSelectedItem());

        String semesterFilter =
                String.valueOf(
                        studentsSemesterFilter
                                .getSelectedItem());

        studentsSorter.setRowFilter(
                new RowFilter<
                        DefaultTableModel,
                        Integer>() {

                    @Override
                    public boolean include(
                            Entry<
                                    ? extends DefaultTableModel,
                                    ? extends Integer> entry) {

                        StringBuilder all =
                                new StringBuilder();

                        for (int i = 0;
                             i < entry.getValueCount();
                             i++) {

                            all.append(
                                    String.valueOf(
                                            entry.getValue(i)))
                                    .append(" ");
                        }

                        if (!search.isEmpty()
                                && !all.toString()
                                .toLowerCase()
                                .contains(search)) {

                            return false;
                        }

                        if (!"All Departments".equals(
                                department)
                                && !department.equals(
                                String.valueOf(
                                        entry.getValue(3)))) {

                            return false;
                        }

                        if (!"All Years".equals(year)
                                && !year.equals(
                                String.valueOf(
                                        entry.getValue(5)))) {

                            return false;
                        }

                        double cgpa =
                                parseDouble(
                                        entry.getValue(4),
                                        0);

                        int backlogs =
                                parseInt(
                                        entry.getValue(6),
                                        0);

                        int semester =
                                parseInt(
                                        entry.getValue(7),
                                        0);

                        if ("CGPA >= 9".equals(
                                cgpaFilter)
                                && cgpa < 9) {

                            return false;
                        }

                        if ("CGPA >= 8".equals(
                                cgpaFilter)
                                && cgpa < 8) {

                            return false;
                        }

                        if ("CGPA >= 7".equals(
                                cgpaFilter)
                                && cgpa < 7) {

                            return false;
                        }

                        if ("CGPA >= 6".equals(
                                cgpaFilter)
                                && cgpa < 6) {

                            return false;
                        }

                        if ("0 Backlogs".equals(
                                backlogFilter)
                                && backlogs != 0) {

                            return false;
                        }

                        if ("1 or less".equals(
                                backlogFilter)
                                && backlogs > 1) {

                            return false;
                        }

                        if ("2 or less".equals(
                                backlogFilter)
                                && backlogs > 2) {

                            return false;
                        }

                        if (semesterFilter.startsWith(
                                "Semester ")) {

                            int required =
                                    parseInt(
                                            semesterFilter.substring(
                                                    9),
                                            -1);

                            if (semester != required) {
                                return false;
                            }
                        }

                        return true;
                    }
                });
    }

    // =========================================================
    // COMPANY FILTER
    // =========================================================

    private void applyCompaniesFilter() {

        String search =
                companiesSearchField
                        .getText()
                        .trim();

        if (search.isEmpty()) {

            companiesSorter.setRowFilter(
                    null);

        } else {

            companiesSorter.setRowFilter(
                    RowFilter.regexFilter(
                            "(?i)"
                                    + Pattern.quote(
                                    search)));
        }
    }

    // =========================================================
    // NOTIFICATION FILTER
    // =========================================================

    private void applyNotificationsFilter() {

        String search =
                notificationsSearchField
                        .getText()
                        .trim()
                        .toLowerCase();

        notificationsSorter.setRowFilter(
                new RowFilter<
                        DefaultTableModel,
                        Integer>() {

                    @Override
                    public boolean include(
                            Entry<
                                    ? extends DefaultTableModel,
                                    ? extends Integer> entry) {

                        if (search.isEmpty()) {
                            return true;
                        }

                        for (int i = 0;
                             i < entry.getValueCount();
                             i++) {

                            String value =
                                    String.valueOf(
                                            entry.getValue(i));

                            if (value
                                    .toLowerCase()
                                    .contains(search)) {

                                return true;
                            }
                        }

                        return false;
                    }
                });
    }

    // =========================================================
    // JOB SORTING
    // =========================================================

    private void applyJobsSort() {

        String option =
                String.valueOf(
                        jobsSortCombo
                                .getSelectedItem());

        switch (option) {

            case "Company A-Z" ->
                    jobsSorter.setComparator(
                            1,
                            Comparator.comparing(
                                    value -> String.valueOf(value),
                                    String.CASE_INSENSITIVE_ORDER));

            case "Role A-Z" ->
                    jobsSorter.setComparator(
                            2,
                            Comparator.comparing(
                                    value -> String.valueOf(value),
                                    String.CASE_INSENSITIVE_ORDER));

            case "Minimum CGPA" ->
                    jobsSorter.setComparator(
                            4,
                            Comparator.comparingDouble(
                                    value -> parseDouble(
                                            value,
                                            0)));

            case "Passing Year" ->
                    jobsSorter.setComparator(
                            7,
                            Comparator.comparingInt(
                                    value -> parseInt(
                                            value,
                                            0)));

            case "Deadline" ->
                    jobsSorter.setComparator(
                            8,
                            Comparator.comparing(
                                    value -> String.valueOf(value),
                                    String.CASE_INSENSITIVE_ORDER));

            default -> {
                jobsSorter.setComparator(1, null);
                jobsSorter.setComparator(2, null);
                jobsSorter.setComparator(4, null);
                jobsSorter.setComparator(7, null);
                jobsSorter.setComparator(8, null);
            }
        }

        applyJobsFilter();
    }

    // =========================================================
    // STUDENT SORTING
    // =========================================================

    private void applyStudentsSort() {

        String option =
                String.valueOf(
                        studentsSortCombo
                                .getSelectedItem());

        switch (option) {

            case "Name A-Z" ->
                    studentsSorter.setComparator(
                            1,
                            Comparator.comparing(
                                    value -> String.valueOf(value),
                                    String.CASE_INSENSITIVE_ORDER));

            case "CGPA High-Low" ->
                    studentsSorter.setComparator(
                            4,
                            Comparator.comparingDouble(
                                            value -> parseDouble(
                                                    value,
                                                    0))
                                    .reversed());

            case "CGPA Low-High" ->
                    studentsSorter.setComparator(
                            4,
                            Comparator.comparingDouble(
                                    value -> parseDouble(
                                            value,
                                            0)));

            case "Backlogs Low-High" ->
                    studentsSorter.setComparator(
                            6,
                            Comparator.comparingInt(
                                    value -> parseInt(
                                            value,
                                            0)));

            case "Passing Year" ->
                    studentsSorter.setComparator(
                            5,
                            Comparator.comparingInt(
                                    value -> parseInt(
                                            value,
                                            0)));

            case "Department A-Z" ->
                    studentsSorter.setComparator(
                            3,
                            Comparator.comparing(
                                    value -> String.valueOf(value),
                                    String.CASE_INSENSITIVE_ORDER));

            default -> {
                studentsSorter.setComparator(1, null);
                studentsSorter.setComparator(3, null);
                studentsSorter.setComparator(4, null);
                studentsSorter.setComparator(5, null);
                studentsSorter.setComparator(6, null);
            }
        }

        applyStudentsFilter();
    }

    // =========================================================
    // REBUILD JOB FILTERS
    // =========================================================

    private void rebuildJobFilters() {

        if (jobsCompanyFilter == null) {
            return;
        }

        String oldCompany =
                String.valueOf(
                        jobsCompanyFilter
                                .getSelectedItem());

        String oldYear =
                String.valueOf(
                        jobsYearFilter
                                .getSelectedItem());

        Set<String> companies =
                new TreeSet<>(
                        String.CASE_INSENSITIVE_ORDER);

        Set<Integer> years =
                new TreeSet<>();

        for (int i = 0;
             i < jobsModel.getRowCount();
             i++) {

            companies.add(
                    String.valueOf(
                            jobsModel.getValueAt(
                                    i,
                                    1)));

            int year =
                    parseInt(
                            jobsModel.getValueAt(
                                    i,
                                    7),
                            0);

            if (year != 0) {
                years.add(year);
            }
        }

        jobsCompanyFilter.removeAllItems();

        jobsCompanyFilter.addItem(
                "All Companies");

        for (String company :
                companies) {

            jobsCompanyFilter.addItem(
                    company);
        }

        jobsYearFilter.removeAllItems();

        jobsYearFilter.addItem(
                "All Years");

        for (Integer year :
                years) {

            jobsYearFilter.addItem(
                    String.valueOf(year));
        }

        selectItem(
                jobsCompanyFilter,
                oldCompany);

        selectItem(
                jobsYearFilter,
                oldYear);
    }

    // =========================================================
    // REBUILD STUDENT FILTERS
    // =========================================================

    private void rebuildStudentFilters() {

        if (studentsDepartmentFilter == null) {
            return;
        }

        String oldDepartment =
                String.valueOf(
                        studentsDepartmentFilter
                                .getSelectedItem());

        String oldYear =
                String.valueOf(
                        studentsYearFilter
                                .getSelectedItem());

        Set<String> departments =
                new TreeSet<>(
                        String.CASE_INSENSITIVE_ORDER);

        Set<Integer> years =
                new TreeSet<>();

        for (int i = 0;
             i < studentsModel.getRowCount();
             i++) {

            String department =
                    String.valueOf(
                            studentsModel.getValueAt(
                                    i,
                                    3));

            int year =
                    parseInt(
                            studentsModel.getValueAt(
                                    i,
                                    5),
                            0);

            if (!department.isBlank()) {
                departments.add(
                        department);
            }

            if (year != 0) {
                years.add(year);
            }
        }

        studentsDepartmentFilter.removeAllItems();

        studentsDepartmentFilter.addItem(
                "All Departments");

        for (String department :
                departments) {

            studentsDepartmentFilter.addItem(
                    department);
        }

        studentsYearFilter.removeAllItems();

        studentsYearFilter.addItem(
                "All Years");

        for (Integer year :
                years) {

            studentsYearFilter.addItem(
                    String.valueOf(year));
        }

        selectItem(
                studentsDepartmentFilter,
                oldDepartment);

        selectItem(
                studentsYearFilter,
                oldYear);
    }

    // =========================================================
    // BUILD COMPANY SUMMARY FROM JOBS
    // =========================================================

    private void rebuildCompanies() {

        class CompanySummary {

            String name;
            int jobs;
            Set<String> roles =
                    new TreeSet<>(
                            String.CASE_INSENSITIVE_ORDER);

            String latestDeadline = "";

            CompanySummary(String name) {
                this.name = name;
            }
        }

        Map<String, CompanySummary> companies =
                new TreeMap<>(
                        String.CASE_INSENSITIVE_ORDER);

        for (int i = 0;
             i < jobsModel.getRowCount();
             i++) {

            String company =
                    String.valueOf(
                            jobsModel.getValueAt(
                                    i,
                                    1));

            String role =
                    String.valueOf(
                            jobsModel.getValueAt(
                                    i,
                                    2));

            String deadline =
                    String.valueOf(
                            jobsModel.getValueAt(
                                    i,
                                    8));

            CompanySummary summary =
                    companies.computeIfAbsent(
                            company,
                            CompanySummary::new);

            summary.jobs++;

            if (!role.isBlank()) {
                summary.roles.add(role);
            }

            if (summary.latestDeadline.isBlank()
                    || deadline.compareTo(
                    summary.latestDeadline) > 0) {

                summary.latestDeadline =
                        deadline;
            }
        }

        companiesModel.setRowCount(0);

        for (CompanySummary summary :
                companies.values()) {

            companiesModel.addRow(
                    new Object[]{
                            summary.name,
                            summary.jobs,
                            String.join(
                                    ", ",
                                    summary.roles),
                            summary.latestDeadline
                    });
        }

        applyCompaniesFilter();
    }

    // =========================================================
    // SOCKET PARSING
    // =========================================================

    /*
     * JobPosting.toProtocolLine():
     *
     * id|company|role|minPackage|minCgpa|branches|
     * maxBacklogs|passingYear|requiredSkills|deadline
     */

    private static JobRow parseJob(
            String line) {

        if (line == null) {
            return null;
        }

        String[] p =
                line.split(
                        "\\|",
                        -1);

        if (p.length < 10) {
            return null;
        }

        return new JobRow(
                p[0],
                p[1],
                p[2],
                p[3],
                parseDouble(p[4], 0),
                p[5],
                parseInt(p[6], 0),
                parseInt(p[7], 0),
                p[8],
                p[9]);
    }

    /*
     * Student.toProtocolLine():
     *
     * PRN|name|email|department|CGPA|passingYear|
     * backlogs|semester|phone|skills
     */

    private static StudentRow parseStudent(
            String line) {

        if (line == null) {
            return null;
        }

        String[] p =
                line.split(
                        "\\|",
                        -1);

        if (p.length < 10) {
            return null;
        }

        return new StudentRow(
                p[0],
                p[1],
                p[2],
                p[3],
                parseDouble(p[4], 0),
                parseInt(p[5], 0),
                parseInt(p[6], 0),
                parseInt(p[7], 0),
                p[8],
                p[9]);
    }

    /*
     * Notification.toProtocolLine():
     *
     * id|jobId|subject|message|recipientGroup|
     * recipientCount|createdAt
     */

    private static NotificationRow parseNotification(
            String line) {

        if (line == null) {
            return null;
        }

        String[] p =
                line.split(
                        "\\|",
                        -1);

        if (p.length < 7) {
            return null;
        }

        return new NotificationRow(
                parseLong(p[0], 0),
                p[1],
                p[2],
                p[3],
                p[4],
                parseInt(p[5], 0),
                p[6]);
    }

    // =========================================================
    // LOGOUT
    // =========================================================

    private void logout() {

        int result =
                JOptionPane.showConfirmDialog(
                        this,
                        "Are you sure you want to log out?",
                        "Logout",
                        JOptionPane.YES_NO_OPTION);

        if (result != JOptionPane.YES_OPTION) {
            return;
        }

        if (!hasSession()) {

            openLogin();

            return;
        }

        runRequest(
                () -> socketClient.sendRequest(
                        "LOGOUT",
                        sessionToken),

                response -> {

                    openLogin();
                });
    }

    private void openLogin() {

        dispose();

        SwingUtilities.invokeLater(() -> {

            LoginFrame login =
                    new LoginFrame();

            login.setVisible(true);
        });
    }

    // =========================================================
    // PAGE CONTROL
    // =========================================================

    private void showPage(
            String page,
            String title) {

        pageTitle.setText(title);

        cardLayout.show(
                contentPanel,
                page);
    }

    private void refreshCurrentPage() {

        String title =
                pageTitle.getText();

        switch (title) {

            case "Dashboard" ->
                    loadDashboardData();

            case "Placement Overview" ->
                    loadJobs();

            case "Companies" ->
                    loadJobs();

            case "Students" ->
                    loadStudents();

            case "Reports" ->
                    loadDashboardStats();

            case "Announcements" ->
                    loadNotifications();

            default ->
                    loadDashboardData();
        }
    }

    // =========================================================
    // BACKGROUND SOCKET REQUESTS
    // =========================================================

    private void runRequest(
            RequestAction action,
            ResponseHandler handler) {

        SwingWorker<
                SocketClient.Response,
                Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected SocketClient.Response
                    doInBackground()
                            throws Exception {

                        return action.execute();
                    }

                    @Override
                    protected void done() {

                        try {

                            handler.handle(
                                    get());

                        } catch (Exception ex) {

                            showError(
                                    "Server request failed: "
                                            + rootMessage(ex));
                        }
                    }
                };

        worker.execute();
    }

    private void runListRequest(
            ListRequestAction action,
            ListResponseHandler handler) {

        SwingWorker<
                SocketClient.ListResponse,
                Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected SocketClient.ListResponse
                    doInBackground()
                            throws Exception {

                        return action.execute();
                    }

                    @Override
                    protected void done() {

                        try {

                            handler.handle(
                                    get());

                        } catch (Exception ex) {

                            showError(
                                    "Server request failed: "
                                            + rootMessage(ex));
                        }
                    }
                };

        worker.execute();
    }

    // =========================================================
    // UI HELPERS
    // =========================================================

    private static DefaultTableModel createModel(
            String[] columns) {

        return new DefaultTableModel(
                columns,
                0) {

            @Override
            public boolean isCellEditable(
                    int row,
                    int column) {

                return false;
            }
        };
    }

    private JPanel createMetricCard(
            String title,
            JLabel value,
            String description) {

        JPanel card =
                createCardPanel();

        card.setLayout(
                new BorderLayout(
                        0,
                        8));

        card.setBorder(
                new EmptyBorder(
                        18,
                        18,
                        18,
                        18));

        JLabel titleLabel =
                new JLabel(title);

        titleLabel.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        13));

        titleLabel.setForeground(
                SECONDARY_TEXT);

        JLabel descriptionLabel =
                new JLabel(
                        "<html>"
                                + escapeHtml(
                                description)
                                + "</html>");

        descriptionLabel.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        11));

        descriptionLabel.setForeground(
                SECONDARY_TEXT);

        card.add(
                titleLabel,
                BorderLayout.NORTH);

        card.add(
                value,
                BorderLayout.CENTER);

        card.add(
                descriptionLabel,
                BorderLayout.SOUTH);

        return card;
    }

    private JPanel createReportRow(
            String title,
            JLabel value) {

        JPanel row =
                new JPanel(
                        new BorderLayout());

        row.setOpaque(false);

        row.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        55));

        JLabel label =
                new JLabel(title);

        label.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        14));

        label.setForeground(TEXT);

        row.add(
                label,
                BorderLayout.WEST);

        row.add(
                value,
                BorderLayout.EAST);

        return row;
    }

    private JPanel createCardPanel() {

        JPanel panel =
                new JPanel();

        panel.setBackground(CARD);

        panel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                BORDER),
                        new EmptyBorder(
                                14,
                                14,
                                14,
                                14)));

        return panel;
    }

    private JLabel createSectionTitle(
            String text) {

        JLabel label =
                new JLabel(text);

        label.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        16));

        label.setForeground(TEXT);

        return label;
    }

    private JLabel createMetricValueLabel() {

        JLabel label =
                new JLabel("—");

        label.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        27));

        label.setForeground(
                PRIMARY_GREEN);

        return label;
    }

    private JButton createSidebarButton(
            String text) {

        JButton button =
                new JButton(text);

        button.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        12));

        button.setForeground(
                Color.WHITE);

        button.setBackground(
                PRIMARY_GREEN);

        button.setHorizontalAlignment(
                SwingConstants.LEFT);

        button.setFocusPainted(false);

        button.setBorderPainted(false);

        button.setOpaque(true);

        button.setBorder(
                new EmptyBorder(
                        10,
                        12,
                        10,
                        12));

        button.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        42));

        button.setCursor(
                new Cursor(
                        Cursor.HAND_CURSOR));

        button.addMouseListener(
                new java.awt.event.MouseAdapter() {

                    @Override
                    public void mouseEntered(
                            java.awt.event.MouseEvent e) {

                        button.setBackground(
                                DARK_GREEN);
                    }

                    @Override
                    public void mouseExited(
                            java.awt.event.MouseEvent e) {

                        button.setBackground(
                                PRIMARY_GREEN);
                    }
                });

        return button;
    }

    private JButton createSmallButton(
            String text) {

        JButton button =
                new JButton(text);

        button.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        12));

        button.setForeground(
                PRIMARY_GREEN);

        button.setBackground(
                Color.WHITE);

        button.setFocusPainted(false);

        button.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                PRIMARY_GREEN),
                        new EmptyBorder(
                                7,
                                13,
                                7,
                                13)));

        button.setCursor(
                new Cursor(
                        Cursor.HAND_CURSOR));

        return button;
    }

    private JButton createActionButton(
            String text) {

        JButton button =
                new JButton(text);

        button.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        12));

        button.setForeground(
                PRIMARY_GREEN);

        button.setBackground(
                LIGHT_GREEN);

        button.setFocusPainted(false);

        button.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                new Color(
                                        190,
                                        220,
                                        198)),
                        new EmptyBorder(
                                9,
                                10,
                                9,
                                10)));

        button.setAlignmentX(
                Component.LEFT_ALIGNMENT);

        button.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        42));

        button.setCursor(
                new Cursor(
                        Cursor.HAND_CURSOR));

        return button;
    }

    private void styleInput(
            JTextField field) {

        field.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        12));

        field.setBackground(
                Color.WHITE);

        field.setForeground(TEXT);

        field.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                BORDER),
                        new EmptyBorder(
                                7,
                                9,
                                7,
                                9)));
    }

    private void styleCombo(
            JComboBox<String> combo) {

        combo.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        12));

        combo.setBackground(
                Color.WHITE);

        combo.setForeground(TEXT);
    }

    private void addFilter(
            JPanel panel,
            GridBagConstraints gbc,
            int x,
            String title,
            JComponent component) {

        JPanel box =
                new JPanel();

        box.setOpaque(false);

        box.setLayout(
                new BoxLayout(
                        box,
                        BoxLayout.Y_AXIS));

        JLabel label =
                new JLabel(title);

        label.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        10));

        label.setForeground(
                SECONDARY_TEXT);

        box.add(label);

        box.add(
                Box.createVerticalStrut(4));

        box.add(component);

        gbc.gridx = x;
        gbc.gridy = 0;
        gbc.weightx = 1;

        panel.add(
                box,
                gbc);
    }

    private void styleTable(
            JTable table) {

        table.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        12));

        table.setForeground(TEXT);

        table.setBackground(
                Color.WHITE);

        table.setRowHeight(30);

        table.setGridColor(
                BORDER);

        table.setShowVerticalLines(false);

        table.setSelectionBackground(
                LIGHT_GREEN);

        table.setSelectionForeground(
                TEXT);

        DefaultTableCellRenderer header =
                new DefaultTableCellRenderer();

        header.setHorizontalAlignment(
                SwingConstants.LEFT);

        header.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        12));

        header.setForeground(TEXT);

        header.setBackground(
                LIGHT_GREEN);

        table.getTableHeader()
                .setDefaultRenderer(header);

        table.getTableHeader()
                .setPreferredSize(
                        new Dimension(
                                0,
                                34));
    }

    private void setColumnWidths(
            JTable table,
            int... widths) {

        for (int i = 0;
             i < widths.length
                     && i < table.getColumnCount();
             i++) {

            table.getColumnModel()
                    .getColumn(i)
                    .setPreferredWidth(
                            widths[i]);
        }
    }

    // =========================================================
    // UTILITY
    // =========================================================

    private boolean hasSession() {

        return sessionToken != null
                && !sessionToken.isBlank();
    }

    private void showError(
            String message) {

        if (!SwingUtilities.isEventDispatchThread()) {

            SwingUtilities.invokeLater(
                    () -> showError(message));

            return;
        }

        JOptionPane.showMessageDialog(
                this,
                message == null
                        || message.isBlank()
                        ? "Unknown error."
                        : message,
                "Request Error",
                JOptionPane.ERROR_MESSAGE);
    }

    private static void selectItem(
            JComboBox<String> combo,
            String value) {

        if (value == null) {
            return;
        }

        for (int i = 0;
             i < combo.getItemCount();
             i++) {

            if (value.equals(
                    combo.getItemAt(i))) {

                combo.setSelectedIndex(i);
                return;
            }
        }

        combo.setSelectedIndex(0);
    }

    private static int parseInt(
            Object value,
            int fallback) {

        try {

            return Integer.parseInt(
                    String.valueOf(value)
                            .trim());

        } catch (Exception ignored) {

            return fallback;
        }
    }

    private static long parseLong(
            Object value,
            long fallback) {

        try {

            return Long.parseLong(
                    String.valueOf(value)
                            .trim());

        } catch (Exception ignored) {

            return fallback;
        }
    }

    private static double parseDouble(
            Object value,
            double fallback) {

        try {

            return Double.parseDouble(
                    String.valueOf(value)
                            .trim());

        } catch (Exception ignored) {

            return fallback;
        }
    }

    private static String rootMessage(
            Throwable throwable) {

        Throwable current =
                throwable;

        while (current.getCause() != null) {
            current = current.getCause();
        }

        String message =
                current.getMessage();

        return message == null
                || message.isBlank()
                ? current.getClass()
                .getSimpleName()
                : message;
    }

    private static String escapeHtml(
            String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace(
                        "&",
                        "&amp;")
                .replace(
                        "<",
                        "&lt;")
                .replace(
                        ">",
                        "&gt;")
                .replace(
                        "\"",
                        "&quot;");
    }

    // =========================================================
    // FUNCTIONAL INTERFACES
    // =========================================================

    @FunctionalInterface
    private interface RequestAction {

        SocketClient.Response execute()
                throws Exception;
    }

    @FunctionalInterface
    private interface ResponseHandler {

        void handle(
                SocketClient.Response response);
    }

    @FunctionalInterface
    private interface ListRequestAction {

        SocketClient.ListResponse execute()
                throws Exception;
    }

    @FunctionalInterface
    private interface ListResponseHandler {

        void handle(
                SocketClient.ListResponse response);
    }

    // =========================================================
    // DATA HOLDERS
    // =========================================================

    private static class JobRow {

        final String id;
        final String company;
        final String role;
        final String minPackage;
        final double minCgpa;
        final String branches;
        final int maxBacklogs;
        final int passingYear;
        final String requiredSkills;
        final String deadline;

        JobRow(
                String id,
                String company,
                String role,
                String minPackage,
                double minCgpa,
                String branches,
                int maxBacklogs,
                int passingYear,
                String requiredSkills,
                String deadline) {

            this.id = id;
            this.company = company;
            this.role = role;
            this.minPackage = minPackage;
            this.minCgpa = minCgpa;
            this.branches = branches;
            this.maxBacklogs = maxBacklogs;
            this.passingYear = passingYear;
            this.requiredSkills = requiredSkills;
            this.deadline = deadline;
        }
    }

    private static class StudentRow {

        final String prn;
        final String name;
        final String email;
        final String department;
        final double cgpa;
        final int passingYear;
        final int backlogs;
        final int semester;
        final String phone;
        final String skills;

        StudentRow(
                String prn,
                String name,
                String email,
                String department,
                double cgpa,
                int passingYear,
                int backlogs,
                int semester,
                String phone,
                String skills) {

            this.prn = prn;
            this.name = name;
            this.email = email;
            this.department = department;
            this.cgpa = cgpa;
            this.passingYear = passingYear;
            this.backlogs = backlogs;
            this.semester = semester;
            this.phone = phone;
            this.skills = skills;
        }
    }

    private static class NotificationRow {

        final long id;
        final String jobId;
        final String subject;
        final String message;
        final String recipientGroup;
        final int recipientCount;
        final String createdAt;

        NotificationRow(
                long id,
                String jobId,
                String subject,
                String message,
                String recipientGroup,
                int recipientCount,
                String createdAt) {

            this.id = id;
            this.jobId = jobId;
            this.subject = subject;
            this.message = message;
            this.recipientGroup = recipientGroup;
            this.recipientCount = recipientCount;
            this.createdAt = createdAt;
        }
    }

    // =========================================================
    // DOCUMENT LISTENER
    // =========================================================

    private static class SimpleDocumentListener
            implements DocumentListener {

        private final Runnable action;

        SimpleDocumentListener(
                Runnable action) {

            this.action = action;
        }

        @Override
        public void insertUpdate(
                DocumentEvent e) {

            action.run();
        }

        @Override
        public void removeUpdate(
                DocumentEvent e) {

            action.run();
        }

        @Override
        public void changedUpdate(
                DocumentEvent e) {

            action.run();
        }
    }

    // =========================================================
    // MAIN
    // =========================================================

    public static void main(
            String[] args) {

        try {

            UIManager.setLookAndFeel(
                    UIManager
                            .getSystemLookAndFeelClassName());

        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {

            DirectorDashboard dashboard =
                    new DirectorDashboard();

            dashboard.setVisible(true);
        });
    }
}