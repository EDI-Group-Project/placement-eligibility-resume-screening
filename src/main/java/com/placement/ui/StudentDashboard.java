package com.placement.ui;

import com.placement.sockets.SocketClient;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.DefaultTableCellRenderer;

import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Student-facing dashboard. Uses the application's existing Swing visual style
 * and only displays records returned by the existing server/session.
 *
 * Uses existing placement/application commands and the resume commands routed
 * by the matching backend version. ATS values are rendered only from server responses.
 */
public class StudentDashboard extends JFrame {
    private static final Color BACKGROUND = new Color(247, 248, 245);
    private static final Color GREEN = new Color(20, 92, 48);
    private static final Color TEXT = new Color(38, 50, 43);
    private static final Color MUTED = new Color(105, 115, 108);
    private static final Color BORDER = new Color(215, 222, 216);
    private static final Color WHITE = Color.WHITE;

    private final String name;
    private final String email;
    private final String token;
    private final SocketClient client = new SocketClient();
    private final Map<String, JLabel> profileValues = new LinkedHashMap<>();
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel content = new JPanel(cardLayout);

    private final DefaultTableModel jobsModel = new DefaultTableModel(new String[]{
            "Job ID", "Company", "Role", "Package", "Min CGPA", "Branches",
            "Max Backlogs", "Passing Year", "Required Skills", "Deadline", "Apply"}, 0) {
        // JTable will never activate ApplyButtonEditor if the model reports the cell
        // as non-editable. Keep all data cells read-only and enable only the action cell.
        @Override public boolean isCellEditable(int row, int column) {
            return column == 10 && "Apply".equals(String.valueOf(getValueAt(row, column)));
        }
    };
    private final DefaultTableModel applicationsModel = model(new String[]{
            "Job ID", "Company", "Role", "Status", "Screening Score", "Applied At"});
    private final DefaultTableModel reportStatusModel = model(new String[]{"Application Status", "Count", "Share"});
    private final DefaultTableModel notificationsModel = model(new String[]{"Date", "Subject", "Message"});

    private final JTable jobsTable = new JTable(jobsModel);
    private final JTable applicationsTable = new JTable(applicationsModel);
    private final JTable reportStatusTable = new JTable(reportStatusModel);
    private final JTable notificationsTable = new JTable(notificationsModel);

    private final List<String[]> allJobs = new ArrayList<>();
    private final Set<String> appliedJobIds = new HashSet<>();
    private final Set<String> pendingJobIds = new HashSet<>();
    private boolean jobsLoadedSuccessfully;
    private boolean applicationsLoadedSuccessfully;
    private JTextField jobSearchField;
    private JLabel overviewJobsValue;
    private JLabel overviewApplicationsValue;
    private JLabel overviewStatus;
    private JLabel jobsStatus;
    private JLabel applicationsStatus;
    private JLabel notificationsStatus;
    private JLabel reportsStatus;
    private JLabel atsScoreValue;
    private JLabel atsFeedbackStatus;
    private JTextArea atsFeedbackArea;
    private JLabel selectedResumeLabel;
    private JButton uploadResumeButton;
    private JButton getFeedbackButton;
    private File selectedResumeFile;
    private boolean resumeUploadedThisSession;
    private JTextArea jobScreeningArea;
    private ScoreGauge atsGauge;
    private ScoreGauge jobGauge;
    private ScoreBreakdownPanel scoreBreakdownPanel;
    private SkillListPanel matchedSkillsPanel;
    private SkillListPanel missingSkillsPanel;
    private JComboBox<JobOption> screeningJobBox;
    private JPanel reportsSummaryCards;
    private StatusChartPanel overviewStatusChart;
    private ApplicationPieChartPanel overviewPieChart;
    private CompanyOpportunitiesChartPanel companyOpportunitiesChart;

    public StudentDashboard(String name, String email, String token) {
        this.name = name == null || name.isBlank() ? "Student" : name;
        this.email = email == null ? "" : email;
        this.token = token;

        setTitle("Placement Eligibility Portal - Student Dashboard");
        setSize(1250, 760);
        setMinimumSize(new Dimension(1050, 650));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        build();
        refreshAll();
    }

    public StudentDashboard(String name, String email) { this(name, email, null); }
    public StudentDashboard() { this("Student", "", null); }

    private void build() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BACKGROUND);
        root.add(sidebar(), BorderLayout.WEST);

        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(BACKGROUND);
        main.add(header(), BorderLayout.NORTH);
        content.setBackground(BACKGROUND);
        content.add(dashboardPage(), "DASHBOARD");
        content.add(placementPage(), "PLACEMENTS");
        content.add(applicationsPage(), "APPLICATIONS");
        content.add(atsPage(), "ATS");
        content.add(notificationsPage(), "NOTIFICATIONS");
        content.add(profilePage(), "PROFILE");
        content.add(reportsPage(), "REPORTS");
        main.add(content, BorderLayout.CENTER);
        root.add(main, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel sidebar() {
        JPanel side = new JPanel();
        side.setPreferredSize(new Dimension(220, 760));
        side.setBackground(GREEN);
        side.setBorder(new EmptyBorder(28, 16, 18, 16));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.add(label("PLACEMENT", 23, true, WHITE));
        side.add(label("ELIGIBILITY PORTAL", 11, false, new Color(220, 245, 225)));
        side.add(Box.createVerticalStrut(25));
        addNav(side, "Dashboard", "DASHBOARD");
        addNav(side, "Placement Drives", "PLACEMENTS");
        addNav(side, "My Applications", "APPLICATIONS");
        addNav(side, "ATS Screening", "ATS");
        addNav(side, "Notifications", "NOTIFICATIONS");
        addNav(side, "My Profile", "PROFILE");
        addNav(side, "My Reports", "REPORTS");
        side.add(Box.createVerticalGlue());
        addNav(side, "Logout", null);
        return side;
    }

    private void addNav(JPanel side, String title, String card) {
        JButton button = new JButton(title);
        button.setMaximumSize(new Dimension(188, 40));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setForeground(WHITE);
        button.setBackground(GREEN);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (card == null) {
            button.addActionListener(e -> logout());
        } else {
            button.addActionListener(e -> {
                cardLayout.show(content, card);
                switch (card) {
                    case "DASHBOARD" -> refreshAll();
                    case "PLACEMENTS" -> loadJobs();
                    case "APPLICATIONS" -> loadApplications();
                    case "ATS" -> refreshScreeningJobs();
                    case "NOTIFICATIONS" -> loadNotifications();
                    case "REPORTS" -> loadApplications();
                    default -> { }
                }
            });
        }
        side.add(button);
        side.add(Box.createVerticalStrut(4));
    }

    private JPanel header() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(WHITE);
        header.setBorder(new EmptyBorder(18, 24, 18, 24));
        header.add(label("Student Dashboard", 22, true, TEXT), BorderLayout.WEST);
        header.add(label(name, 13, false, MUTED), BorderLayout.EAST);
        return header;
    }

    private JPanel dashboardPage() {
        JPanel page = page();
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JPanel titleGroup = verticalGroup();
        titleGroup.add(label("Dashboard", 25, true, TEXT));
        titleGroup.add(Box.createVerticalStrut(4));
        titleGroup.add(label("Your placement activity at a glance", 13, false, MUTED));
        heading.add(titleGroup, BorderLayout.WEST);
        JButton refresh = button("Refresh");
        refresh.addActionListener(e -> refreshAll());
        heading.add(refresh, BorderLayout.EAST);
        page.add(heading, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 16));
        center.setOpaque(false);
        JPanel cards = new JPanel(new GridLayout(1, 2, 14, 0));
        cards.setOpaque(false);
        overviewJobsValue = label("—", 27, true, TEXT);
        overviewApplicationsValue = label("—", 27, true, TEXT);
        cards.add(metricCard("Placement Opportunities", overviewJobsValue));
        cards.add(metricCard("My Applications", overviewApplicationsValue));
        center.add(cards, BorderLayout.NORTH);

        JPanel charts = new JPanel();
        charts.setOpaque(false);
        charts.setLayout(new BoxLayout(charts, BoxLayout.Y_AXIS));

        JPanel statusCharts = new JPanel(new GridLayout(1, 2, 14, 0));
        statusCharts.setOpaque(false);
        statusCharts.setPreferredSize(new Dimension(900, 275));
        statusCharts.setMinimumSize(new Dimension(500, 240));

        JPanel statusBarCard = whiteCard();
        statusBarCard.setLayout(new BorderLayout(0, 8));
        statusBarCard.add(label("Applications by Status", 16, true, GREEN), BorderLayout.NORTH);
        overviewStatusChart = new StatusChartPanel();
        statusBarCard.add(overviewStatusChart, BorderLayout.CENTER);
        statusCharts.add(statusBarCard);

        JPanel statusPieCard = whiteCard();
        statusPieCard.setLayout(new BorderLayout(0, 8));
        statusPieCard.add(label("Application Status Share", 16, true, GREEN), BorderLayout.NORTH);
        overviewPieChart = new ApplicationPieChartPanel();
        statusPieCard.add(overviewPieChart, BorderLayout.CENTER);
        statusCharts.add(statusPieCard);
        charts.add(statusCharts);
        charts.add(Box.createVerticalStrut(14));

        JPanel companyCard = whiteCard();
        companyCard.setLayout(new BorderLayout(0, 8));
        companyCard.add(label("Available Placement Roles by Company", 16, true, GREEN), BorderLayout.NORTH);
        companyOpportunitiesChart = new CompanyOpportunitiesChartPanel();
        companyCard.add(companyOpportunitiesChart, BorderLayout.CENTER);
        companyCard.setPreferredSize(new Dimension(900, 250));
        companyCard.setMinimumSize(new Dimension(500, 220));
        companyCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));
        charts.add(companyCard);

        JScrollPane chartScroll = new JScrollPane(charts);
        chartScroll.setBorder(BorderFactory.createEmptyBorder());
        chartScroll.setOpaque(false);
        chartScroll.getViewport().setOpaque(false);
        chartScroll.getVerticalScrollBar().setUnitIncrement(16);
        center.add(chartScroll, BorderLayout.CENTER);
        page.add(center, BorderLayout.CENTER);
        overviewStatus = label("Loading live data…", 12, false, MUTED);
        page.add(overviewStatus, BorderLayout.SOUTH);
        return page;
    }

    private JPanel placementPage() {
        JPanel page = page();
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        top.setOpaque(false);
        jobSearchField = new JTextField(18);
        jobSearchField.setToolTipText("Filter the jobs already returned by the server");
        JButton search = button("Filter");
        search.addActionListener(e -> applyJobFilter());
        JButton clear = button("Clear");
        clear.addActionListener(e -> { jobSearchField.setText(""); applyJobFilter(); });
        JButton refresh = button("Refresh");
        refresh.addActionListener(e -> loadJobs());
        top.add(label("Search company or role", 12, false, TEXT));
        top.add(jobSearchField);
        top.add(search);
        top.add(clear);
        top.add(refresh);
        page.add(top, BorderLayout.NORTH);
        styleTable(jobsTable);
        jobsTable.getColumnModel().getColumn(10).setCellRenderer(new ApplyButtonRenderer());
        jobsTable.getColumnModel().getColumn(10).setCellEditor(new ApplyButtonEditor());
        jobsTable.getColumnModel().getColumn(10).setPreferredWidth(82);
        jobsTable.getColumnModel().getColumn(10).setMinWidth(82);
        jobsTable.getColumnModel().getColumn(10).setMaxWidth(96);
        page.add(new JScrollPane(jobsTable), BorderLayout.CENTER);
        jobsStatus = label("", 12, false, MUTED);
        page.add(jobsStatus, BorderLayout.SOUTH);
        return page;
    }

    private JPanel applicationsPage() {
        JPanel page = page();
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(label("My Applications", 22, true, TEXT), BorderLayout.WEST);
        JButton refresh = button("Refresh");
        refresh.addActionListener(e -> loadApplications());
        top.add(refresh, BorderLayout.EAST);
        page.add(top, BorderLayout.NORTH);
        styleTable(applicationsTable);
        applicationsTable.getColumnModel().getColumn(3).setCellRenderer(new ApplicationStatusRenderer());
        page.add(new JScrollPane(applicationsTable), BorderLayout.CENTER);
        applicationsStatus = label("", 12, false, MUTED);
        page.add(applicationsStatus, BorderLayout.SOUTH);
        return page;
    }

    private JPanel atsPage() {
        JPanel page = page();
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JPanel titles = verticalGroup();
        titles.add(label("ATS Screening", 24, true, TEXT));
        titles.add(Box.createVerticalStrut(4));
        titles.add(label("Review your resume and compare it with a placement drive", 13, false, MUTED));
        heading.add(titles, BorderLayout.WEST);
        JButton feedback = button("Refresh ATS Feedback");
        feedback.addActionListener(e -> loadAtsFeedback());
        heading.add(feedback, BorderLayout.EAST);
        page.add(heading, BorderLayout.NORTH);

        // Use a vertically scrolling layout instead of forcing both complex cards
        // into equal GridLayout rows. Equal-height rows squeezed the nested upload,
        // feedback, gauge and skills components and caused visual overlap.
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        // Overall report: values are populated only from GET_ATS_FEEDBACK.
        JPanel overall = whiteCard();
        overall.setLayout(new BorderLayout(0, 10));
        overall.add(label("Overall Resume Report", 16, true, GREEN), BorderLayout.NORTH);
        JPanel overallContent = new JPanel(new BorderLayout(0, 8));
        overallContent.setOpaque(false);

        JPanel uploadSection = new JPanel();
        uploadSection.setOpaque(false);
        uploadSection.setLayout(new BoxLayout(uploadSection, BoxLayout.Y_AXIS));
        JPanel uploadTitle = new JPanel(new BorderLayout(10, 0));
        uploadTitle.setOpaque(false);
        JPanel uploadText = verticalGroup();
        uploadText.add(label("My Resume", 14, true, TEXT));
        uploadText.add(label("Choose a resume file, review it, then upload it for screening.", 12, false, MUTED));
        uploadTitle.add(uploadText, BorderLayout.CENTER);
        uploadSection.add(uploadTitle);
        uploadSection.add(Box.createVerticalStrut(10));
        JPanel chooseCard = new JPanel(new BorderLayout(12, 0));
        chooseCard.setOpaque(true);
        chooseCard.setBackground(new Color(248, 252, 249));
        chooseCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createDashedBorder(new Color(90, 155, 112), 1.4f, 5f, 3f, true),
                new EmptyBorder(13, 14, 13, 14)));
        JLabel uploadIcon = label("↑", 28, true, GREEN);
        uploadIcon.setHorizontalAlignment(SwingConstants.CENTER);
        uploadIcon.setPreferredSize(new Dimension(42, 42));
        chooseCard.add(uploadIcon, BorderLayout.WEST);
        JPanel chooseText = verticalGroup();
        chooseText.add(label("Choose your resume file", 14, true, TEXT));
        chooseText.add(Box.createVerticalStrut(3));
        chooseText.add(label("PDF, DOCX or TXT · Maximum 2 MB", 12, false, MUTED));
        chooseCard.add(chooseText, BorderLayout.CENTER);
        JButton chooseFileButton = button("Choose File");
        chooseFileButton.addActionListener(e -> chooseResumeFile());
        chooseCard.add(chooseFileButton, BorderLayout.EAST);
        uploadSection.add(chooseCard);
        uploadSection.add(Box.createVerticalStrut(10));
        selectedResumeLabel = label("No file selected", 12, false, MUTED);
        selectedResumeLabel.setBorder(new EmptyBorder(5, 2, 5, 2));
        uploadSection.add(selectedResumeLabel);
        uploadSection.add(Box.createVerticalStrut(7));
        JPanel uploadActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        uploadActions.setOpaque(false);
        uploadResumeButton = button("Upload Resume");
        uploadResumeButton.setEnabled(false);
        uploadResumeButton.addActionListener(e -> sendSelectedResume());
        getFeedbackButton = button("Get ATS Feedback");
        getFeedbackButton.setEnabled(false);
        getFeedbackButton.addActionListener(e -> loadAtsFeedback());
        JButton removeFileButton = button("Remove");
        removeFileButton.addActionListener(e -> clearSelectedResume());
        uploadActions.add(uploadResumeButton);
        uploadActions.add(getFeedbackButton);
        uploadActions.add(removeFileButton);
        uploadSection.add(uploadActions);
        uploadSection.add(Box.createVerticalStrut(5));
        uploadSection.add(label("Upload your resume first to enable ATS feedback.", 11, false, MUTED));
        overallContent.add(uploadSection, BorderLayout.NORTH);

        JPanel overallResults = new JPanel(new BorderLayout(12, 8));
        overallResults.setOpaque(false);
        atsGauge = new ScoreGauge(GREEN, "ATS Score");
        overallResults.add(atsGauge, BorderLayout.WEST);
        JPanel reportColumn = new JPanel(new BorderLayout(0, 6));
        reportColumn.setOpaque(false);
        atsScoreValue = label("ATS Score: —", 16, true, TEXT);
        reportColumn.add(atsScoreValue, BorderLayout.NORTH);
        atsFeedbackArea = reportTextArea();
        atsFeedbackArea.setRows(5);
        atsFeedbackArea.setText("Upload your resume, then select Get Feedback to request the current server report.");
        reportColumn.add(new JScrollPane(atsFeedbackArea), BorderLayout.CENTER);
        overallResults.add(reportColumn, BorderLayout.CENTER);
        overallResults.setMinimumSize(new Dimension(500, 170));
        overallContent.add(overallResults, BorderLayout.CENTER);
        overall.add(overallContent, BorderLayout.CENTER);
        overall.setAlignmentX(Component.LEFT_ALIGNMENT);
        overall.setPreferredSize(new Dimension(900, 430));
        overall.setMinimumSize(new Dimension(650, 390));
        overall.setMaximumSize(new Dimension(Integer.MAX_VALUE, 430));
        body.add(overall);
        body.add(Box.createVerticalStrut(12));

        // Job-specific report: only real jobs from GET_JOBS are selectable.
        JPanel jobSpecific = whiteCard();
        jobSpecific.setLayout(new BorderLayout(0, 10));
        jobSpecific.add(label("Job-Specific Screening", 16, true, GREEN), BorderLayout.NORTH);
        JPanel jobContent = new JPanel(new BorderLayout(0, 8));
        jobContent.setOpaque(false);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.setOpaque(false);
        screeningJobBox = new JComboBox<>();
        screeningJobBox.setPreferredSize(new Dimension(330, 28));
        JButton screen = button("Screen My Resume");
        screen.addActionListener(e -> screenAgainstSelectedJob());
        JButton refreshJobs = button("Refresh Drives");
        refreshJobs.addActionListener(e -> loadJobs());
        controls.add(label("Placement drive", 12, false, TEXT));
        controls.add(screeningJobBox);
        controls.add(screen);
        controls.add(refreshJobs);
        jobContent.add(controls, BorderLayout.NORTH);
        JPanel screeningResults = new JPanel(new BorderLayout(0, 8));
        screeningResults.setOpaque(false);
        JPanel scoreHeader = new JPanel(new BorderLayout(10, 0));
        scoreHeader.setOpaque(false);
        jobGauge = new ScoreGauge(new Color(37, 99, 235), "Match Score");
        scoreHeader.add(jobGauge, BorderLayout.WEST);
        scoreBreakdownPanel = new ScoreBreakdownPanel();
        scoreHeader.add(scoreBreakdownPanel, BorderLayout.CENTER);
        screeningResults.add(scoreHeader, BorderLayout.NORTH);
        JPanel skills = new JPanel(new GridLayout(1, 2, 8, 0));
        skills.setOpaque(false);
        matchedSkillsPanel = new SkillListPanel("Matched Skills", new Color(34, 139, 74));
        missingSkillsPanel = new SkillListPanel("Missing Skills", new Color(190, 55, 55));
        skills.add(matchedSkillsPanel); skills.add(missingSkillsPanel);
        screeningResults.add(skills, BorderLayout.CENTER);
        jobScreeningArea = reportTextArea();
        jobScreeningArea.setRows(3);
        jobScreeningArea.setText("Choose a placement drive and select Screen My Resume to view the server's comparison.");
        JScrollPane detailsScroll = new JScrollPane(jobScreeningArea);
        detailsScroll.setPreferredSize(new Dimension(500, 95));
        screeningResults.add(detailsScroll, BorderLayout.SOUTH);
        screeningResults.setMinimumSize(new Dimension(500, 250));
        jobContent.add(screeningResults, BorderLayout.CENTER);
        jobSpecific.add(jobContent, BorderLayout.CENTER);
        jobSpecific.setAlignmentX(Component.LEFT_ALIGNMENT);
        jobSpecific.setPreferredSize(new Dimension(900, 430));
        jobSpecific.setMinimumSize(new Dimension(650, 390));
        jobSpecific.setMaximumSize(new Dimension(Integer.MAX_VALUE, 430));
        body.add(jobSpecific);

        JScrollPane bodyScroll = new JScrollPane(body);
        bodyScroll.setBorder(BorderFactory.createEmptyBorder());
        bodyScroll.setOpaque(false);
        bodyScroll.getViewport().setOpaque(false);
        bodyScroll.getVerticalScrollBar().setUnitIncrement(18);
        page.add(bodyScroll, BorderLayout.CENTER);
        atsFeedbackStatus = label("Scores and feedback are shown only when returned by the server.", 11, false, MUTED);
        page.add(atsFeedbackStatus, BorderLayout.SOUTH);
        return page;
    }

    private JPanel notificationsPage() {
        JPanel page = page();
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(label("Notifications", 22, true, TEXT), BorderLayout.WEST);
        JButton refresh = button("Refresh Notifications");
        refresh.addActionListener(e -> loadNotifications());
        top.add(refresh, BorderLayout.EAST);
        page.add(top, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);
        body.add(infoCard("Student announcements",
                "This page displays general announcements marked for all students. The current server returns shared notification history, not a verified personal feed, so job-specific or individually targeted notifications are not shown here."), BorderLayout.NORTH);

        styleTable(notificationsTable);
        notificationsTable.setRowHeight(38);
        notificationsTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        notificationsTable.setFillsViewportHeight(true);
        notificationsTable.setColumnSelectionAllowed(false);
        notificationsTable.setDefaultRenderer(Object.class, new NotificationCellRenderer());
        notificationsTable.getColumnModel().getColumn(0).setPreferredWidth(125);
        notificationsTable.getColumnModel().getColumn(1).setPreferredWidth(190);
        notificationsTable.getColumnModel().getColumn(2).setPreferredWidth(650);
        JScrollPane scroll = new JScrollPane(notificationsTable);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        body.add(scroll, BorderLayout.CENTER);
        notificationsStatus = label("Select Refresh Notifications to load announcements.", 12, false, MUTED);
        body.add(notificationsStatus, BorderLayout.SOUTH);
        page.add(body, BorderLayout.CENTER);
        return page;
    }
// now added
    private JPanel profilePage() {
    JPanel page = page();

    JPanel title = new JPanel(new BorderLayout());
    title.setOpaque(false);

    JPanel titleGroup = verticalGroup();
    titleGroup.add(label("My Profile", 24, true, TEXT));
    titleGroup.add(Box.createVerticalStrut(4));
    titleGroup.add(label(
            "Your personal and academic details",
            13, false, MUTED
    ));

    title.add(titleGroup, BorderLayout.WEST);

    JButton refresh = button("Refresh Profile");
    refresh.addActionListener(e -> loadMyProfile());
    title.add(refresh, BorderLayout.EAST);

    page.add(title, BorderLayout.NORTH);

    JPanel profile = new JPanel(new GridLayout(0, 2, 10, 10));
    profile.setBackground(WHITE);
    profile.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            new EmptyBorder(18, 18, 18, 18)
    ));

    profileValues.clear();

    addProfileValue(profile, "Full Name", name);
    addProfileValue(profile, "Email Address", email);
    addProfileValue(profile, "PRN / Student ID", "Loading...");
    addProfileValue(profile, "Department / Branch", "Loading...");
    addProfileValue(profile, "Current CGPA", "Loading...");
    addProfileValue(profile, "Passing Year", "Loading...");
    addProfileValue(profile, "Active Backlogs", "Loading...");
    addProfileValue(profile, "Current Semester", "Loading...");
    addProfileValue(profile, "Phone Number", "Loading...");
    addProfileValue(profile, "Skills", "Loading...");

    JPanel wrapper = new JPanel(new BorderLayout());
    wrapper.setOpaque(false);
    wrapper.add(profile, BorderLayout.NORTH);

    page.add(wrapper, BorderLayout.CENTER);

    return page;
}

private void addProfileValue(
        JPanel panel, String fieldName, String initialValue) {

    panel.add(label(fieldName, 13, true, TEXT));

    JLabel valueLabel = label(
            initialValue == null || initialValue.isBlank()
                    ? "Not provided"
                    : initialValue,
            13, false, MUTED
    );

    profileValues.put(fieldName, valueLabel);
    panel.add(valueLabel);
}

    private static String unavailableProfileValue() {
        return "Not available from current backend";
    }
    
    private void loadMyProfile() {

    if (token == null || token.isBlank()) {
        JOptionPane.showMessageDialog(
                this,
                "Your session is missing. Please log in again.",
                "Profile Error",
                JOptionPane.ERROR_MESSAGE
        );
        return;
    }

    async(
        () -> client.sendListRequest("GET_MY_PROFILE", token),

        response -> {
            if (!response.success) {
                JOptionPane.showMessageDialog(
                        this,
                        "Unable to load profile: "
                                + response.errorMessage,
                        "Profile Error",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            if (response.lines.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "The server returned no student profile.",
                        "Profile Error",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            String[] fields =
                    response.lines.get(0).split("\\|", -1);

            if (fields.length < 10) {
                JOptionPane.showMessageDialog(
                        this,
                        "The profile response has an unexpected format. "
                                + "Received " + fields.length + " fields.",
                        "Profile Error",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            setProfileValue("PRN / Student ID", fields[0]);
            setProfileValue("Full Name", fields[1]);
            setProfileValue("Email Address", fields[2]);
            setProfileValue("Department / Branch", fields[3]);
            setProfileValue("Current CGPA", fields[4]);
            setProfileValue("Passing Year", fields[5]);
            setProfileValue("Active Backlogs", fields[6]);
            setProfileValue("Current Semester", fields[7]);
            setProfileValue("Phone Number", fields[8]);
            setProfileValue("Skills", fields[9]);
        },

        error -> JOptionPane.showMessageDialog(
                this,
                "Could not contact the backend: "
                        + error.getMessage(),
                "Connection Error",
                JOptionPane.ERROR_MESSAGE
        )
    );
}

private void setProfileValue(String fieldName, String value) {
    JLabel valueLabel = profileValues.get(fieldName);

    if (valueLabel != null) {
        valueLabel.setText(
                value == null || value.isBlank()
                        ? "Not provided"
                        : value
        );
    }
}

    /** Wraps notification text and grows row height so long messages remain readable. */
    private final class NotificationCellRenderer extends JTextArea implements TableCellRenderer {
        private NotificationCellRenderer() {
            setLineWrap(true);
            setWrapStyleWord(true);
            setOpaque(true);
            setBorder(new EmptyBorder(5, 7, 5, 7));
        }

        @Override public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value == null ? "" : value.toString());
            setFont(table.getFont());
            setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            int width = Math.max(80, table.getColumnModel().getColumn(column).getWidth() - 14);
            setSize(width, Short.MAX_VALUE);
            int needed = Math.max(38, getPreferredSize().height + 2);
            if (table.getRowHeight(row) != needed) table.setRowHeight(row, needed);
            return this;
        }
    }

    private JPanel reportsPage() {
        JPanel page = page();
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JPanel titles = verticalGroup();
        titles.add(label("My Reports", 24, true, TEXT));
        titles.add(Box.createVerticalStrut(4));
        titles.add(label("Application summary and screening information from actual records", 13, false, MUTED));
        top.add(titles, BorderLayout.WEST);
        JButton refresh = button("Refresh Data");
        refresh.addActionListener(e -> loadApplications());
        top.add(refresh, BorderLayout.EAST);
        page.add(top, BorderLayout.NORTH);

        // Stack report sections vertically inside a scroll pane to prevent cards/tables
        // from competing for the same BorderLayout space at different window sizes.
        JPanel sections = new JPanel();
        sections.setOpaque(false);
        sections.setLayout(new BoxLayout(sections, BoxLayout.Y_AXIS));

        reportsSummaryCards = new JPanel(new GridLayout(1, 3, 14, 0));
        reportsSummaryCards.setOpaque(false);
        reportsSummaryCards.setAlignmentX(Component.LEFT_ALIGNMENT);
        reportsSummaryCards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));
        reportsSummaryCards.setPreferredSize(new Dimension(900, 96));
        sections.add(reportsSummaryCards);
        sections.add(Box.createVerticalStrut(14));

        JPanel statusCard = whiteCard();
        statusCard.setLayout(new BorderLayout(0, 8));
        statusCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusCard.setPreferredSize(new Dimension(900, 190));
        statusCard.setMinimumSize(new Dimension(500, 170));
        statusCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
        statusCard.add(label("Application Status Breakdown", 16, true, GREEN), BorderLayout.NORTH);
        styleTable(reportStatusTable);
        reportStatusTable.setRowHeight(30);
        reportStatusTable.setFillsViewportHeight(true);
        JScrollPane statusScroll = new JScrollPane(reportStatusTable);
        statusScroll.setBorder(BorderFactory.createLineBorder(BORDER));
        statusCard.add(statusScroll, BorderLayout.CENTER);
        sections.add(statusCard);
        sections.add(Box.createVerticalStrut(14));

        JPanel recordCard = whiteCard();
        recordCard.setLayout(new BorderLayout(0, 8));
        recordCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        recordCard.setPreferredSize(new Dimension(900, 310));
        recordCard.setMinimumSize(new Dimension(500, 260));
        recordCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 420));
        recordCard.add(label("Application Records", 16, true, GREEN), BorderLayout.NORTH);
        JTable reportApps = new JTable(applicationsModel);
        styleTable(reportApps);
        reportApps.setRowHeight(30);
        reportApps.setFillsViewportHeight(true);
        JScrollPane recordScroll = new JScrollPane(reportApps);
        recordScroll.setBorder(BorderFactory.createLineBorder(BORDER));
        recordCard.add(recordScroll, BorderLayout.CENTER);
        sections.add(recordCard);

        JScrollPane sectionsScroll = new JScrollPane(sections);
        sectionsScroll.setBorder(BorderFactory.createEmptyBorder());
        sectionsScroll.setOpaque(false);
        sectionsScroll.getViewport().setOpaque(false);
        sectionsScroll.getVerticalScrollBar().setUnitIncrement(16);
        page.add(sectionsScroll, BorderLayout.CENTER);
        reportsStatus = label("Report summaries are calculated from GET_MY_APPLICATIONS responses.", 11, false, MUTED);
        page.add(reportsStatus, BorderLayout.SOUTH);
        updateReports();
        return page;
    }

    private void refreshAll() {
        loadJobs();
        loadApplications();
    }

    private void loadJobs() {
        if (!hasSession()) {
            setStatus(jobsStatus, "No authenticated session is available. Sign in again to load placement opportunities.");
            setStatus(overviewStatus, "No authenticated session is available.");
            updateOverview();
            return;
        }
        setStatus(jobsStatus, "Loading placement opportunities…");
        setStatus(overviewStatus, "Loading live data…");
        async(() -> client.sendListRequest("GET_JOBS", token), response -> {
            if (!response.success) {
                setStatus(jobsStatus, "Unable to refresh placement opportunities: " + safe(response.errorMessage));
                updateOverview(); return;
            }
            jobsLoadedSuccessfully = true;
            allJobs.clear();
            for (String line : response.lines) {
                if (line == null) continue;
                String[] values = line.split("\\|", -1);
                if (values.length >= 10) allJobs.add(values);
            }
            applyJobFilter();
            setStatus(jobsStatus, allJobs.isEmpty() ? "No placement opportunities were returned by the server." : "Showing " + allJobs.size() + " placement opportunities returned by the server.");
            updateOverview();
        }, error -> {
            setStatus(jobsStatus, "Unable to refresh placement opportunities: " + error.getMessage());
            if (!jobsLoadedSuccessfully) setStatus(overviewStatus, "Placement data has not loaded yet.");
            updateOverview();
        });
    }

    private void applyJobFilter() {
        jobsModel.setRowCount(0);
        String query = jobSearchField == null ? "" : jobSearchField.getText().trim().toLowerCase(java.util.Locale.ROOT);
        for (String[] row : allJobs) {
            String company = valueAt(row, 1).toLowerCase(java.util.Locale.ROOT);
            String role = valueAt(row, 2).toLowerCase(java.util.Locale.ROOT);
            String branches = valueAt(row, 5).toLowerCase(java.util.Locale.ROOT);
            String skills = valueAt(row, 8).toLowerCase(java.util.Locale.ROOT);
            if (!query.isBlank() && !company.contains(query) && !role.contains(query)
                    && !branches.contains(query) && !skills.contains(query)) continue;
            String jobId = valueAt(row, 0);
            String action = pendingJobIds.contains(jobId) ? "Applying…"
                    : appliedJobIds.contains(jobId) ? "Applied" : "Apply";
            jobsModel.addRow(new Object[]{row[0], row[1], row[2], row[3], row[4], row[5], row[6], row[7], row[8], row[9], action});
        }
        updateOverview();
        refreshScreeningJobs();
    }

    private void applyToJobAtViewRow(int viewRow) {
        if (viewRow < 0 || viewRow >= jobsTable.getRowCount()) return;
        int modelRow = jobsTable.convertRowIndexToModel(viewRow);
        String jobId = String.valueOf(jobsModel.getValueAt(modelRow, 0));
        if (appliedJobIds.contains(jobId)) {
            JOptionPane.showMessageDialog(this, "Your application for this placement is already recorded.",
                    "Already Applied", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (pendingJobIds.contains(jobId)) return;

        int confirm = JOptionPane.showConfirmDialog(this,
                "Submit an application for " + jobsModel.getValueAt(modelRow, 1) + " — " + jobsModel.getValueAt(modelRow, 2) + "?",
                "Confirm Application", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        pendingJobIds.add(jobId);
        applyJobFilter();
        async(() -> client.sendRequest("APPLY_JOB", token, jobId), response -> {
            pendingJobIds.remove(jobId);
            if (!response.success) {
                applyJobFilter();
                JOptionPane.showMessageDialog(this, safe(response.payload), "Application Not Submitted", JOptionPane.ERROR_MESSAGE);
                // Refresh the real application list so a previously submitted application is reflected.
                loadApplications();
                return;
            }
            // The applications list returned by the server is the source of truth.
            // Refresh it before deciding the final button state.
            JOptionPane.showMessageDialog(this, safe(response.payload), "Server Response", JOptionPane.INFORMATION_MESSAGE);
            loadApplications();
        }, error -> {
            pendingJobIds.remove(jobId);
            applyJobFilter();
            JOptionPane.showMessageDialog(this, "Unable to submit application: " + error.getMessage(),
                    "Request Error", JOptionPane.ERROR_MESSAGE);
        });
    }

    private void loadApplications() {
        if (!hasSession()) {
            setStatus(applicationsStatus, "No authenticated session is available. Sign in again to load your applications.");
            setStatus(reportsStatus, "No authenticated session is available.");
            setStatus(overviewStatus, "No authenticated session is available.");
            updateOverview();
            updateReports();
            return;
        }
        setStatus(applicationsStatus, "Loading application records…");
        async(() -> client.sendListRequest("GET_MY_APPLICATIONS", token), response -> {
            if (!response.success) {
                setStatus(applicationsStatus, "Unable to load applications: " + safe(response.errorMessage));
                setStatus(reportsStatus, "Unable to retrieve report data: " + safe(response.errorMessage));
                return;
            }
            applicationsLoadedSuccessfully = true;
            applicationsModel.setRowCount(0);
            appliedJobIds.clear();
            for (String line : response.lines) {
                if (line == null) continue;
                String[] values = line.split("\\|", -1);
                if (values.length >= 6) {
                    appliedJobIds.add(values[0]);
                    applicationsModel.addRow(new Object[]{values[0], values[1], values[2], values[3],
                            displayScreeningScore(values[4]), values[5]});
                }
            }
            applyJobFilter();
            setStatus(applicationsStatus, applicationsModel.getRowCount() == 0
                    ? "No application records were returned by the server."
                    : "Showing " + applicationsModel.getRowCount() + " application records.");
            updateOverview();
            updateReports();
        }, error -> {
            setStatus(applicationsStatus, "Unable to load applications: " + error.getMessage());
            setStatus(reportsStatus, "Unable to retrieve report data: " + error.getMessage());
        });
    }

    private void loadNotifications() {
        if (!hasSession()) {
            notificationsModel.setRowCount(0);
            setStatus(notificationsStatus, "No authenticated session is available. Sign in again to load announcements.");
            return;
        }
        setStatus(notificationsStatus, "Loading announcements from the server…");
        async(() -> client.sendListRequest("GET_NOTIFICATIONS", token), response -> {
            notificationsModel.setRowCount(0);
            if (!response.success) {
                setStatus(notificationsStatus, "Unable to load announcements: " + safe(response.errorMessage));
                return;
            }
            int total = 0;
            for (String line : response.lines) {
                if (line == null || line.isBlank()) continue;
                String[] fields = line.split("\\|", -1);
                // Notification protocol: id | jobId | subject | message | recipientGroup | recipientCount | createdAt
                if (fields.length < 7) continue;
                String group = fields[4].trim();
                if (!(group.equalsIgnoreCase("ALL") || group.equalsIgnoreCase("ALL STUDENTS") || group.equalsIgnoreCase("ALL_STUDENTS"))) continue;
                String jobId = fields[1].trim();
                String subject = fields[2].trim();
                String message = fields[3].trim();
                String createdAt = fields[6].trim();
                notificationsModel.addRow(new Object[]{
                        createdAt.isBlank() ? "Date unavailable" : createdAt,
                        subject.isBlank() ? "Announcement" : subject,
                        message.isBlank() ? "No message provided" : message
                });
                total++;
            }
            setStatus(notificationsStatus, total == 0
                    ? "No general announcements for all students were returned. Job-specific notifications cannot be verified by this frontend."
                    : "Loaded " + total + " general announcement(s). This is not a verified personal notification feed.");
        }, error -> setStatus(notificationsStatus, "Unable to load announcements: " + safe(error.getMessage())));
    }

    private void loadAtsFeedback() {
        if (!hasSession()) { setStatus(atsFeedbackStatus, "No authenticated session is available."); return; }
        if (!resumeUploadedThisSession) {
            setStatus(atsFeedbackStatus, "Upload a resume successfully before requesting ATS feedback.");
            return;
        }
        setStatus(atsFeedbackStatus, "Requesting ATS feedback from the server…");
        clearOverallAtsResults("Waiting for fresh ATS feedback from the server…");
        async(() -> client.sendListRequest("GET_ATS_FEEDBACK", token), response -> {
            if (!response.success) {
                String error = safe(response.errorMessage);
                if (error.toLowerCase(java.util.Locale.ROOT).contains("unknown command")) {
                    error = "ATS feedback is not connected in the current server build. The backend has not routed GET_ATS_FEEDBACK to its resume handler.";
                }
                setStatus(atsFeedbackStatus, "ATS feedback unavailable: " + error);
                atsFeedbackArea.setText("The server did not return an ATS report.\n\n" + error);
                atsGauge.setScore(null);
                return;
            }
            StringBuilder report = new StringBuilder();
            Map<String, List<String>> grouped = new LinkedHashMap<>();
            for (String line : response.lines) {
                if (line == null) continue;
                int separator = line.indexOf('|');
                String key = separator >= 0 ? line.substring(0, separator).trim().toUpperCase(java.util.Locale.ROOT) : "RESULT";
                String value = separator >= 0 ? line.substring(separator + 1).trim() : line.trim();
                if (key.equals("SCORE")) {
                    atsScoreValue.setText("ATS Score: " + value);
                    atsGauge.setScore(parseScore(value));
                } else {
                    grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(value);
                }
            }
            Map<String, String> headings = Map.of(
                    "POSITIVE", "Positive findings",
                    "WARNING", "Warnings",
                    "SUGGESTION", "Suggestions");
            for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
                report.append(headings.getOrDefault(entry.getKey(), entry.getKey())).append('\n');
                for (String value : entry.getValue()) report.append("  • ").append(value).append('\n');
                report.append('\n');
            }
            atsFeedbackArea.setText(report.toString().isBlank() ? "No additional feedback was returned." : report.toString().trim());
            setStatus(atsFeedbackStatus, "ATS feedback loaded from the server.");
        }, error -> {
            setStatus(atsFeedbackStatus, "ATS feedback unavailable: " + error.getMessage());
            atsFeedbackArea.setText("No ATS report was loaded.\n" + error.getMessage());
        });
    }

    /** Opens the operating system's native file browser instead of Swing JFileChooser. */
    private void chooseResumeFile() {
        FileDialog dialog = new FileDialog(this, "Select Resume File", FileDialog.LOAD);
        dialog.setFilenameFilter((dir, name) -> {
            String lower = name.toLowerCase(java.util.Locale.ROOT);
            return lower.endsWith(".pdf") || lower.endsWith(".docx") || lower.endsWith(".txt");
        });
        dialog.setVisible(true);
        String fileName = dialog.getFile();
        String directory = dialog.getDirectory();
        if (fileName == null || directory == null) return;
        File file = new File(directory, fileName);
        String lower = file.getName().toLowerCase(java.util.Locale.ROOT);
        if (!(lower.endsWith(".pdf") || lower.endsWith(".docx") || lower.endsWith(".txt"))) {
            JOptionPane.showMessageDialog(this, "Choose a PDF, DOCX or TXT file.", "Unsupported File Type", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!file.isFile()) {
            JOptionPane.showMessageDialog(this, "The selected file could not be found.", "File Unavailable", JOptionPane.WARNING_MESSAGE);
            return;
        }
        long size = file.length();
        if (size > 2L * 1024 * 1024) {
            JOptionPane.showMessageDialog(this, "The selected file exceeds the 2 MB limit.", "File Too Large", JOptionPane.WARNING_MESSAGE);
            return;
        }
        selectedResumeFile = file;
        resumeUploadedThisSession = false;
        clearOverallAtsResults("A new resume is selected. Upload it to request fresh ATS feedback.");
        clearJobScreeningResults("A new resume is selected. Run screening again to get updated job-specific results.");
        selectedResumeLabel.setText("Selected: " + file.getName() + "  ·  " + formatFileSize(size));
        selectedResumeLabel.setForeground(TEXT);
        uploadResumeButton.setEnabled(true);
        getFeedbackButton.setEnabled(false);
        setStatus(atsFeedbackStatus, "Resume selected. Upload it before requesting ATS feedback.");
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024L * 1024) return String.format(java.util.Locale.ROOT, "%.1f KB", bytes / 1024.0);
        return String.format(java.util.Locale.ROOT, "%.2f MB", bytes / (1024.0 * 1024.0));
    }

    private void clearSelectedResume() {
        selectedResumeFile = null;
        resumeUploadedThisSession = false;
        clearOverallAtsResults("Choose and upload a resume to request ATS feedback.");
        clearJobScreeningResults("Choose and upload a resume, then run screening for job-specific results.");
        if (selectedResumeLabel != null) { selectedResumeLabel.setText("No file selected"); selectedResumeLabel.setForeground(MUTED); }
        if (uploadResumeButton != null) uploadResumeButton.setEnabled(false);
        if (getFeedbackButton != null) getFeedbackButton.setEnabled(false);
        setStatus(atsFeedbackStatus, "Choose a resume file to begin.");
    }

    private void sendSelectedResume() {
        if (!hasSession()) { setStatus(atsFeedbackStatus, "No authenticated session is available."); return; }
        File file = selectedResumeFile;
        if (file == null || !file.isFile()) { setStatus(atsFeedbackStatus, "Choose a valid resume file first."); return; }
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            if (bytes.length > 2L * 1024 * 1024) {
                JOptionPane.showMessageDialog(this, "The selected file exceeds the 2 MB limit.", "File Too Large", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String lower = file.getName().toLowerCase(java.util.Locale.ROOT);
            if (!(lower.endsWith(".pdf") || lower.endsWith(".docx") || lower.endsWith(".txt"))) {
                JOptionPane.showMessageDialog(this, "Choose a PDF, DOCX or TXT file.", "Unsupported File Type", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String encoded = Base64.getEncoder().encodeToString(bytes);
            uploadResumeButton.setEnabled(false);
            getFeedbackButton.setEnabled(false);
            clearOverallAtsResults("Uploading a new resume. Waiting for the upload result…");
            clearJobScreeningResults("Resume changed. Run screening again after upload succeeds.");
            setStatus(atsFeedbackStatus, "Uploading " + file.getName() + "…");
            async(() -> client.sendRequest("UPLOAD_RESUME", token, file.getName(), encoded), response -> {
                if (!response.success) {
                    uploadResumeButton.setEnabled(selectedResumeFile != null);
                    resumeUploadedThisSession = false;
                    getFeedbackButton.setEnabled(false);
                    setStatus(atsFeedbackStatus, "Resume upload unavailable: " + safe(response.payload));
                    JOptionPane.showMessageDialog(this, safe(response.payload), "Resume Upload", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                resumeUploadedThisSession = true;
                uploadResumeButton.setEnabled(selectedResumeFile != null);
                getFeedbackButton.setEnabled(true);
                setStatus(atsFeedbackStatus, "Resume uploaded. You can now request ATS feedback.");
                JOptionPane.showMessageDialog(this, safe(response.payload), "Resume Upload", JOptionPane.INFORMATION_MESSAGE);
            }, error -> {
                uploadResumeButton.setEnabled(selectedResumeFile != null);
                resumeUploadedThisSession = false;
                getFeedbackButton.setEnabled(false);
                setStatus(atsFeedbackStatus, "Resume upload failed: " + error.getMessage());
                JOptionPane.showMessageDialog(this, error.getMessage(), "Resume Upload", JOptionPane.ERROR_MESSAGE);
            });
        } catch (Exception ex) {
            setStatus(atsFeedbackStatus, "Unable to read selected resume: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Unable to read the selected file: " + ex.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshScreeningJobs() {
        if (screeningJobBox == null) return;
        Object previous = screeningJobBox.getSelectedItem();
        screeningJobBox.removeAllItems();
        for (String[] row : allJobs) {
            screeningJobBox.addItem(new JobOption(valueAt(row, 0), valueAt(row, 1) + " — " + valueAt(row, 2)));
        }
        if (previous instanceof JobOption old) {
            for (int i = 0; i < screeningJobBox.getItemCount(); i++) {
                if (screeningJobBox.getItemAt(i).id.equals(old.id)) {
                    screeningJobBox.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void screenAgainstSelectedJob() {
        if (!hasSession()) { setStatus(atsFeedbackStatus, "No authenticated session is available."); return; }
        JobOption selected = (JobOption) screeningJobBox.getSelectedItem();
        if (selected == null) {
            jobScreeningArea.setText("No placement jobs are available to select.");
            return;
        }
        clearJobScreeningResults("Screening in progress. Waiting for fresh job-specific results…");
        async(() -> client.sendListRequest("SCREEN_STUDENT", token, selected.id, ""), response -> {
            if (!response.success) {
                String error = safe(response.errorMessage);
                if (error.toLowerCase(java.util.Locale.ROOT).contains("unknown command")) {
                    error = "Job-specific screening is not connected in the current server build. The backend has not routed SCREEN_STUDENT to its resume handler.";
                }
                clearJobScreeningResults("No current screening results. The last request failed.\n\n" + error);
                return;
            }
            jobGauge.setScore(null);
            scoreBreakdownPanel.clearScores();
            matchedSkillsPanel.setItems(List.of());
            missingSkillsPanel.setItems(List.of());
            StringBuilder report = new StringBuilder();
            Map<String, List<String>> grouped = new LinkedHashMap<>();
            for (String line : response.lines) {
                if (line == null) continue;
                int separator = line.indexOf('|');
                String key = separator >= 0 ? line.substring(0, separator).trim().toUpperCase(java.util.Locale.ROOT) : "RESULT";
                String value = separator >= 0 ? line.substring(separator + 1).trim() : line.trim();
                grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(value);
                if (key.equals("TOTAL")) jobGauge.setScore(parseScore(value));
                if (key.equals("SKILL") || key.equals("PROJECTS") || key.equals("EXPERIENCE") || key.equals("CERTIFICATIONS") || key.equals("KEYWORDS")) scoreBreakdownPanel.setScore(key, parseScore(value));
                if (key.equals("MATCHED")) matchedSkillsPanel.setItems(splitItems(value));
                if (key.equals("MISSING")) missingSkillsPanel.setItems(splitItems(value));
            }
            Map<String, String> headings = Map.ofEntries(
                    Map.entry("TOTAL", "Overall screening score"),
                    Map.entry("SKILL", "Skill score"),
                    Map.entry("PROJECTS", "Projects score"),
                    Map.entry("EXPERIENCE", "Experience score"),
                    Map.entry("CERTIFICATIONS", "Certifications score"),
                    Map.entry("KEYWORDS", "Keyword score"),
                    Map.entry("ATS", "Resume ATS score"),
                    Map.entry("MATCHED", "Matched skills"),
                    Map.entry("MISSING", "Missing skills"),
                    Map.entry("STRENGTH", "Strengths"),
                    Map.entry("IMPROVE", "Suggested improvements"));
            for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
                report.append(headings.getOrDefault(entry.getKey(), entry.getKey())).append('\n');
                for (String value : entry.getValue()) {
                    if (entry.getKey().equals("MATCHED") || entry.getKey().equals("MISSING")) {
                        if (value.isBlank()) {
                            report.append("  • None returned\n");
                        } else {
                            for (String item : value.split(",")) if (!item.isBlank()) report.append("  • ").append(item.trim()).append('\n');
                        }
                    } else {
                        report.append("  • ").append(value).append('\n');
                    }
                }
                report.append('\n');
            }
            jobScreeningArea.setText(report.toString().isBlank() ? "The server returned an empty screening report." : report.toString().trim());
        }, error -> jobScreeningArea.setText("Screening report unavailable: " + error.getMessage()));
    }

    private void updateOverview() {
        if (overviewJobsValue != null) overviewJobsValue.setText(jobsLoadedSuccessfully ? String.valueOf(allJobs.size()) : "—");
        if (overviewApplicationsValue != null) overviewApplicationsValue.setText(applicationsLoadedSuccessfully ? String.valueOf(applicationsModel.getRowCount()) : "—");
        if (overviewStatus != null) {
            if (!hasSession()) overviewStatus.setText("No authenticated session is available.");
            else if (!jobsLoadedSuccessfully && !applicationsLoadedSuccessfully) overviewStatus.setText("Waiting for placement and application data from the server.");
            else if (!jobsLoadedSuccessfully || !applicationsLoadedSuccessfully) overviewStatus.setText("Some dashboard data has not loaded successfully.");
            else overviewStatus.setText("Counts and chart are based on records returned by the server.");
        }
        if (overviewStatusChart != null) overviewStatusChart.repaint();
        if (overviewPieChart != null) overviewPieChart.repaint();
        if (companyOpportunitiesChart != null) companyOpportunitiesChart.repaint();
    }

    private void updateReports() {
        if (reportStatusModel == null || reportsSummaryCards == null) return;
        reportStatusModel.setRowCount(0);
        if (!applicationsLoadedSuccessfully) {
            reportsSummaryCards.removeAll();
            reportsSummaryCards.add(metricCard("Total Applications", label("—", 27, true, TEXT)));
            reportsSummaryCards.add(metricCard("Distinct Statuses", label("—", 27, true, TEXT)));
            reportsSummaryCards.add(metricCard("Applications With Score", label("—", 27, true, TEXT)));
            reportsSummaryCards.revalidate();
            reportsSummaryCards.repaint();
            if (reportsStatus != null) reportsStatus.setText("Waiting for application records from the server.");
            return;
        }
        Map<String, Integer> statusCounts = new LinkedHashMap<>();
        int scoreRecords = 0;
        for (int row = 0; row < applicationsModel.getRowCount(); row++) {
            String status = String.valueOf(applicationsModel.getValueAt(row, 3));
            statusCounts.merge(status.isBlank() ? "(Blank status returned)" : status, 1, Integer::sum);
            String score = String.valueOf(applicationsModel.getValueAt(row, 4));
            if (!score.isBlank() && !score.equals("—")) scoreRecords++;
        }
        int total = applicationsModel.getRowCount();
        for (Map.Entry<String, Integer> entry : statusCounts.entrySet()) {
            String share = total == 0 ? "—" : String.format(java.util.Locale.ROOT, "%.1f%%", entry.getValue() * 100.0 / total);
            reportStatusModel.addRow(new Object[]{entry.getKey(), entry.getValue(), share});
        }
        reportsSummaryCards.removeAll();
        reportsSummaryCards.add(metricCard("Total Applications", label(String.valueOf(total), 27, true, TEXT)));
        reportsSummaryCards.add(metricCard("Statuses Returned", label(String.valueOf(statusCounts.size()), 27, true, TEXT)));
        reportsSummaryCards.add(metricCard("Applications With Score", label(String.valueOf(scoreRecords), 27, true, TEXT)));
        reportsSummaryCards.revalidate();
        reportsSummaryCards.repaint();
        reportStatusTable.repaint();
        if (reportsStatus != null && total == 0) reportsStatus.setText("No application records are available for reporting yet.");
        else if (reportsStatus != null) reportsStatus.setText("Summary calculated from the application records currently loaded from the server. Scores shown as — are not available as a meaningful result.");
    }

    private void logout() {
        if (!hasSession()) {
            dispose();
            new LoginFrame().setVisible(true);
            return;
        }
        async(() -> client.sendRequest("LOGOUT", token), response -> {
            dispose();
            new LoginFrame().setVisible(true);
        }, error -> {
            JOptionPane.showMessageDialog(this, "Could not confirm logout with the server: " + error.getMessage(), "Logout", JOptionPane.WARNING_MESSAGE);
            dispose();
            new LoginFrame().setVisible(true);
        });
    }

    private boolean hasSession() { return token != null && !token.isBlank(); }

    private <T> void async(Callable<T> task, Consumer<T> onSuccess, Consumer<Exception> onFailure) {
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return task.call(); }
            @Override protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    onFailure.accept(cause instanceof Exception e ? e : new Exception(cause));
                }
            }
        }.execute();
    }


    /** Draws an application-status chart using only records loaded from GET_MY_APPLICATIONS. */
    /** Clears overall ATS values so old results cannot be mistaken for a new request. */
    private void clearOverallAtsResults(String message) {
        if (atsScoreValue != null) atsScoreValue.setText("ATS Score: —");
        if (atsGauge != null) atsGauge.setScore(null);
        if (atsFeedbackArea != null) atsFeedbackArea.setText(message);
    }

    /** Clears job-specific visual results before a new run or after a failed run. */
    private void clearJobScreeningResults(String message) {
        if (jobGauge != null) jobGauge.setScore(null);
        if (scoreBreakdownPanel != null) scoreBreakdownPanel.clearScores();
        if (matchedSkillsPanel != null) matchedSkillsPanel.setItems(List.of());
        if (missingSkillsPanel != null) missingSkillsPanel.setItems(List.of());
        if (jobScreeningArea != null) jobScreeningArea.setText(message);
    }

    private Integer parseScore(String raw) {
        if (raw == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^\\s*(\\d+(?:\\.\\d+)?)\\s*%?\\s*$").matcher(raw);
        if (!m.matches()) return null;
        try { double n = Double.parseDouble(m.group(1)); return n >= 0 && n <= 100 ? (int)Math.round(n) : null; }
        catch (NumberFormatException ex) { return null; }
    }

    private List<String> splitItems(String raw) {
        List<String> result = new ArrayList<>();
        if (raw != null) for (String s : raw.split(",")) if (!s.isBlank()) result.add(s.trim());
        return result;
    }

    private static final class ScoreGauge extends JPanel {
        private final Color accent; private final String caption; private Integer score;
        ScoreGauge(Color accent, String caption) { this.accent=accent; this.caption=caption; setOpaque(false); setPreferredSize(new Dimension(122,130)); }
        void setScore(Integer value) { score=value; repaint(); }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics); Graphics2D g=(Graphics2D)graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int d=Math.min(88, Math.min(getWidth()-14,getHeight()-30)); if(d<25){g.dispose();return;} int x=(getWidth()-d)/2,y=5;
            g.setStroke(new BasicStroke(8,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND)); g.setColor(new Color(231,236,232)); g.drawArc(x+4,y+4,d-8,d-8,90,-360);
            if(score!=null){g.setColor(accent);g.drawArc(x+4,y+4,d-8,d-8,90,(int)(-3.6*score));}
            String v=score==null?"—":score+"%";g.setFont(getFont().deriveFont(Font.BOLD,18f));g.setColor(TEXT);FontMetrics fm=g.getFontMetrics();g.drawString(v,(getWidth()-fm.stringWidth(v))/2,y+d/2+6);
            g.setFont(getFont().deriveFont(Font.PLAIN,11f));g.setColor(MUTED);fm=g.getFontMetrics();g.drawString(caption,(getWidth()-fm.stringWidth(caption))/2,y+d+18);g.dispose();
        }
    }

    private static final class ScoreBreakdownPanel extends JPanel {
        private final Map<String,Integer> scores=new LinkedHashMap<>();
        private final String[] keys={"SKILL","PROJECTS","EXPERIENCE","CERTIFICATIONS","KEYWORDS"};
        private final String[] names={"Skills","Projects","Experience","Certifications","Keywords"};
        ScoreBreakdownPanel(){setOpaque(false);setLayout(new GridLayout(keys.length,1,0,3));rebuild();}
        void setScore(String key,Integer value){if(value==null)scores.remove(key);else scores.put(key,value);rebuild();}
        void clearScores(){scores.clear();rebuild();}
        private void rebuild(){removeAll();for(int i=0;i<keys.length;i++){final String k=keys[i];JPanel row=new JPanel(new BorderLayout(6,0));row.setOpaque(false);JLabel name=new JLabel(names[i]);name.setFont(name.getFont().deriveFont(11f));name.setForeground(TEXT);name.setPreferredSize(new Dimension(82,16));row.add(name,BorderLayout.WEST);
            JComponent bar=new JComponent(){protected void paintComponent(Graphics gg){Graphics2D g=(Graphics2D)gg.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);int w=getWidth(),h=10,y=(getHeight()-h)/2;g.setColor(new Color(235,239,236));g.fillRoundRect(0,y,w,h,8,8);Integer n=scores.get(k);if(n!=null&&w>0){Color[] cs={new Color(62,160,83),new Color(49,139,216),new Color(235,166,51),new Color(220,76,76),new Color(126,92,190)};g.setColor(cs[java.util.Arrays.asList(keys).indexOf(k)]);g.fillRoundRect(0,y,w*n/100,h,8,8);}g.dispose();}};row.add(bar,BorderLayout.CENTER);JLabel val=new JLabel(scores.containsKey(k)?scores.get(k)+"%":"—",SwingConstants.RIGHT);val.setFont(val.getFont().deriveFont(11f));val.setForeground(MUTED);val.setPreferredSize(new Dimension(34,16));row.add(val,BorderLayout.EAST);add(row);}revalidate();repaint();}
    }

    private static final class SkillListPanel extends JPanel {
        private final Color accent; private final JPanel chips=new JPanel(new FlowLayout(FlowLayout.LEFT,5,4));
        SkillListPanel(String title,Color accent){this.accent=accent;setLayout(new BorderLayout(0,4));setBackground(Color.WHITE);setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),new EmptyBorder(6,7,6,7)));JLabel label=new JLabel(title);label.setFont(label.getFont().deriveFont(Font.BOLD,12f));label.setForeground(accent);add(label,BorderLayout.NORTH);chips.setOpaque(false);add(chips,BorderLayout.CENTER);setItems(List.of());}
        void setItems(List<String> items){chips.removeAll();if(items==null||items.isEmpty()){JLabel empty=new JLabel("No items returned");empty.setForeground(MUTED);empty.setFont(empty.getFont().deriveFont(11f));chips.add(empty);}else for(String s:items){JLabel chip=new JLabel(s);chip.setFont(chip.getFont().deriveFont(11f));chip.setForeground(accent.darker());chip.setOpaque(true);chip.setBackground(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),22));chip.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),75)),new EmptyBorder(3,6,3,6)));chips.add(chip);}revalidate();repaint();}
    }

    /** Bar chart of application statuses; all values come from GET_MY_APPLICATIONS. */
    private final class StatusChartPanel extends JPanel {
        private StatusChartPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(400, 260));
            setMinimumSize(new Dimension(220, 180));
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            if (!applicationsLoadedSuccessfully) {
                drawChartMessage(g, "Chart appears after application data loads.", w);
                g.dispose(); return;
            }
            Map<String, Integer> counts = getApplicationStatusCounts();
            if (counts.isEmpty()) {
                drawChartMessage(g, "Apply to a placement drive to see your application analytics.", w);
                g.dispose(); return;
            }
            int left = 34, right = 12, top = 18, bottom = 42;
            int plotW = Math.max(1, w - left - right), plotH = Math.max(1, h - top - bottom);
            int max = counts.values().stream().mapToInt(Integer::intValue).max().orElse(1);
            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            // Use distinct integer tick values so labels never repeat after rounding.
            int tickStep = Math.max(1, (int)Math.ceil(max / 4.0));
            int axisMax = Math.max(tickStep, (int)Math.ceil(max / (double) tickStep) * tickStep);
            for (int value = 0; value <= axisMax; value += tickStep) {
                int y = top + plotH - (int)Math.round(value / (double) axisMax * plotH);
                g.setColor(new Color(229, 234, 230));
                g.drawLine(left, y, left + plotW, y);
                g.setColor(MUTED);
                g.drawString(String.valueOf(value), 4, y + 4);
            }
            int n = counts.size(), slot = Math.max(1, plotW / n), barW = Math.max(12, Math.min(48, slot - 14));
            Color[] palette = {new Color(59, 130, 246), new Color(168, 85, 247), new Color(245, 158, 11), new Color(239, 68, 68), new Color(20, 184, 166), new Color(236, 72, 153), new Color(99, 102, 241), new Color(132, 204, 22)};
            int i = 0;
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                int barH = (int)Math.round((entry.getValue() / (double)axisMax) * plotH);
                int x = left + i * slot + (slot - barW) / 2, y = top + plotH - barH;
                Color base = palette[i % palette.length];
                g.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 35));
                g.fillRoundRect(x, top, barW, plotH, 10, 10);
                g.setPaint(new GradientPaint(x, y, base.brighter(), x, y + Math.max(1, barH), base.darker()));
                g.fillRoundRect(x, y, barW, barH, 10, 10);
                g.setPaint(null);
                g.setColor(TEXT); g.setFont(new Font("SansSerif", Font.BOLD, 11));
                String count = String.valueOf(entry.getValue()); g.drawString(count, x + (barW - g.getFontMetrics().stringWidth(count))/2, Math.max(top + 11, y - 4));
                g.setFont(new Font("SansSerif", Font.PLAIN, 10)); g.setColor(MUTED);
                String name = entry.getKey();
                if (g.getFontMetrics().stringWidth(name) > slot + 8 && name.length() > 7) name = name.substring(0, 6) + "…";
                int labelX = left + i * slot + (slot - g.getFontMetrics().stringWidth(name))/2;
                g.drawString(name, labelX, top + plotH + 16);
                i++;
            }
            g.dispose();
        }
    }

    /** Pie chart of the same live application-status counts. */
    private final class ApplicationPieChartPanel extends JPanel {
        private ApplicationPieChartPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(400, 260));
            setMinimumSize(new Dimension(220, 180));
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            if (!applicationsLoadedSuccessfully) {
                drawChartMessage(g, "Chart appears after application data loads.", w);
                g.dispose(); return;
            }
            Map<String, Integer> counts = getApplicationStatusCounts();
            int total = counts.values().stream().mapToInt(Integer::intValue).sum();
            if (total == 0) {
                drawChartMessage(g, "No application records yet.", w);
                g.dispose(); return;
            }
            Color[] palette = {new Color(59, 130, 246), new Color(168, 85, 247), new Color(245, 158, 11), new Color(239, 68, 68), new Color(20, 184, 166), new Color(236, 72, 153), new Color(99, 102, 241), new Color(132, 204, 22)};
            int diameter = Math.max(90, Math.min(Math.min(h - 24, w / 2), 190));
            int x = Math.max(4, (w / 2 - diameter) / 2), y = Math.max(4, (h - diameter) / 2);
            int startAngle = 90, i = 0;
            for (int value : counts.values()) {
                int extent = (int)Math.round(value * 360.0 / total);
                g.setColor(palette[i % palette.length]);
                g.fillArc(x, y, diameter, diameter, startAngle, -extent);
                startAngle -= extent; i++;
            }
            // Clean donut centre makes the plot easier to scan at a glance.
            int hole = (int)(diameter * 0.54);
            int hx = x + (diameter - hole) / 2, hy = y + (diameter - hole) / 2;
            g.setColor(Color.WHITE); g.fillOval(hx, hy, hole, hole);
            g.setColor(TEXT); g.setFont(new Font("SansSerif", Font.BOLD, 19));
            String totalText = String.valueOf(total);
            g.drawString(totalText, hx + (hole - g.getFontMetrics().stringWidth(totalText)) / 2, hy + hole / 2 + 2);
            g.setColor(MUTED); g.setFont(new Font("SansSerif", Font.PLAIN, 9));
            String appsText = "APPS";
            g.drawString(appsText, hx + (hole - g.getFontMetrics().stringWidth(appsText)) / 2, hy + hole / 2 + 16);
            int legendX = Math.min(w / 2 + 4, x + diameter + 12), legendY = Math.max(18, (h - counts.size() * 23) / 2);
            i = 0;
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                int yy = legendY + i * 23;
                g.setColor(palette[i % palette.length]); g.fillRoundRect(legendX, yy - 9, 10, 10, 3, 3);
                g.setColor(TEXT); g.setFont(new Font("SansSerif", Font.PLAIN, 11));
                String legend = entry.getKey() + "  " + entry.getValue() + " (" + Math.round(entry.getValue() * 100.0 / total) + "%)";
                int maxWidth = Math.max(30, w - legendX - 8);
                FontMetrics fm = g.getFontMetrics();
                while (fm.stringWidth(legend) > maxWidth && legend.length() > 8) legend = legend.substring(0, legend.length() - 4) + "…";
                g.drawString(legend, legendX + 15, yy);
                i++;
            }
            g.dispose();
        }
    }


    /** Counts available placement roles per company using live GET_JOBS records. */
    private final class CompanyOpportunitiesChartPanel extends JPanel {
        private CompanyOpportunitiesChartPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(700, 190));
            setMinimumSize(new Dimension(300, 150));
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            if (!jobsLoadedSuccessfully) {
                drawChartMessage(g, "Chart appears after placement opportunities load.", w);
                g.dispose(); return;
            }
            Map<String, Integer> counts = new LinkedHashMap<>();
            for (String[] job : allJobs) {
                String company = valueAt(job, 1).trim();
                if (company.isBlank()) company = "Unknown company";
                counts.merge(company, 1, Integer::sum);
            }
            if (counts.isEmpty()) {
                drawChartMessage(g, "No placement opportunities were returned by the server.", w);
                g.dispose(); return;
            }
            List<Map.Entry<String, Integer>> entries = new ArrayList<>(counts.entrySet());
            entries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
            if (entries.size() > 8) entries = new ArrayList<>(entries.subList(0, 8));
            int left = 130, right = 34, top = 10, rowH = Math.max(20, (h - top - 8) / entries.size());
            int maxBarW = Math.max(1, w - left - right);
            int max = entries.stream().mapToInt(Map.Entry::getValue).max().orElse(1);
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            for (int i = 0; i < entries.size(); i++) {
                Map.Entry<String, Integer> entry = entries.get(i);
                int y = top + i * rowH;
                String name = entry.getKey();
                FontMetrics fm = g.getFontMetrics();
                while (fm.stringWidth(name) > left - 12 && name.length() > 4) name = name.substring(0, name.length() - 2) + "…";
                g.setColor(TEXT); g.drawString(name, Math.max(2, left - 8 - fm.stringWidth(name)), y + rowH / 2 + 4);
                int barH = Math.min(16, Math.max(8, rowH - 5));
                int barY = y + (rowH - barH) / 2;
                g.setColor(new Color(237, 241, 247)); g.fillRoundRect(left, barY, maxBarW, barH, 10, 10);
                int barW = (int)Math.round(maxBarW * entry.getValue() / (double)max);
                Color[] companyPalette = {new Color(59, 130, 246), new Color(168, 85, 247), new Color(20, 184, 166), new Color(245, 158, 11), new Color(236, 72, 153), new Color(99, 102, 241), new Color(14, 165, 233), new Color(249, 115, 22)};
                Color base = companyPalette[i % companyPalette.length];
                g.setPaint(new GradientPaint(left, barY, base.brighter(), left + Math.max(1, barW), barY + barH, base.darker()));
                g.fillRoundRect(left, barY, Math.max(2, barW), barH, 10, 10);
                g.setPaint(null);
                g.setColor(TEXT); g.drawString(String.valueOf(entry.getValue()), Math.min(w - 20, left + barW + 7), y + rowH / 2 + 4);
            }
            g.dispose();
        }
    }

    private Map<String, Integer> getApplicationStatusCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (int row = 0; row < applicationsModel.getRowCount(); row++) {
            Object raw = applicationsModel.getValueAt(row, 3);
            String status = raw == null ? "Unknown" : raw.toString().trim();
            if (status.isBlank() || status.equalsIgnoreCase("null")) status = "Unknown";
            counts.merge(status, 1, Integer::sum);
        }
        return counts;
    }

    private void drawChartMessage(Graphics2D g, String message, int width) {
        g.setColor(MUTED);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        FontMetrics fm = g.getFontMetrics();
        int maxWidth = Math.max(100, width - 16), y = 28;
        StringBuilder line = new StringBuilder();
        for (String word : message.split("\\s+")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (fm.stringWidth(candidate) > maxWidth && line.length() > 0) {
                g.drawString(line.toString(), 8, y); y += fm.getHeight() + 4; line = new StringBuilder(word);
            } else line = new StringBuilder(candidate);
        }
        if (line.length() > 0) g.drawString(line.toString(), 8, y);
    }

    private static JPanel page() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(BACKGROUND);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        return panel;
    }

    private static JPanel verticalGroup() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    private static JPanel metricCard(String title, JLabel value) {
        JPanel card = whiteCard();
        card.setLayout(new BorderLayout(0, 6));
        card.add(label(title, 13, false, MUTED), BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private static JPanel infoCard(String title, String message) {
        JPanel card = whiteCard();
        card.setLayout(new BorderLayout(0, 8));
        card.add(label(title, 16, true, GREEN), BorderLayout.NORTH);
        JTextArea text = new JTextArea(message);
        text.setWrapStyleWord(true);
        text.setLineWrap(true);
        text.setEditable(false);
        text.setFocusable(false);
        text.setOpaque(false);
        text.setFont(new Font("SansSerif", Font.PLAIN, 13));
        text.setForeground(MUTED);
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    private static JPanel whiteCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(14, 14, 14, 14)));
        return card;
    }

    private static JTextArea reportTextArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("SansSerif", Font.PLAIN, 13));
        area.setForeground(TEXT);
        area.setBackground(WHITE);
        area.setBorder(new EmptyBorder(8, 8, 8, 8));
        area.setText("No report loaded. Use the action above to request data from the server.");
        return area;
    }

    private static void addProfileField(JPanel panel, String title, String value) {
        panel.add(label(title, 12, true, MUTED));
        JTextField field = new JTextField(value);
        field.setEditable(false);
        field.setBackground(new Color(240, 242, 239));
        field.setForeground(TEXT);
        panel.add(field);
    }

    private static void styleTable(JTable table) {
        table.setRowHeight(28);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setBackground(WHITE);
        table.setForeground(TEXT);
        table.setGridColor(new Color(230, 233, 229));
        table.setSelectionBackground(new Color(220, 238, 224));
        table.setSelectionForeground(TEXT);
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setFont(new Font("SansSerif", Font.BOLD, 12));
        header.setBackground(new Color(239, 243, 238));
        header.setForeground(TEXT);
    }

    private static JLabel label(String text, int size, boolean bold, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, size));
        label.setForeground(color);
        return label;
    }

    private static JButton button(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private static DefaultTableModel model(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    private static void setStatus(JLabel label, String message) {
        if (label != null) label.setText(message == null ? "" : message);
    }

    private static String safe(String value) { return value == null || value.isBlank() ? "No details returned." : value; }
    private static String valueAt(String[] row, int index) { return row != null && index >= 0 && index < row.length && row[index] != null ? row[index] : ""; }

    private static final class ApplyButtonRenderer extends JButton implements TableCellRenderer {
        private ApplyButtonRenderer() {
            setText("Apply");
            setOpaque(true);
            setFocusPainted(false);
            setForeground(WHITE);
            setBackground(GREEN);
            setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            setFont(new Font("SansSerif", Font.BOLD, 11));
        }
        @Override public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            String text = value == null ? "Apply" : value.toString();
            setText(text);
            boolean actionable = "Apply".equals(text);
            setEnabled(actionable);
            setBackground(actionable ? GREEN : new Color(232, 237, 232));
            setForeground(actionable ? WHITE : MUTED);
            return this;
        }
    }

    /** A real table-cell editor makes the Apply control respond like a button,
     * rather than relying on a click listener attached to the whole table. */
    private final class ApplyButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private final JButton button = new JButton("Apply");
        private int viewRow = -1;

        private ApplyButtonEditor() {
            button.setOpaque(true);
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            button.setFont(new Font("SansSerif", Font.BOLD, 11));
            button.addActionListener(event -> {
                int rowToApply = viewRow;
                fireEditingStopped();
                if (rowToApply >= 0) applyToJobAtViewRow(rowToApply);
            });
        }

        @Override public boolean isCellEditable(EventObject event) {
            if (event instanceof java.awt.event.MouseEvent mouse && mouse.getSource() instanceof JTable table) {
                int row = table.rowAtPoint(mouse.getPoint());
                int column = table.columnAtPoint(mouse.getPoint());
                if (row < 0 || column != 10) return false;
                Object value = table.getValueAt(row, column);
                return "Apply".equals(String.valueOf(value));
            }
            return false;
        }

        @Override public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            viewRow = row;
            String text = value == null ? "Apply" : value.toString();
            button.setText(text);
            boolean actionable = "Apply".equals(text);
            button.setEnabled(actionable);
            button.setBackground(actionable ? GREEN : new Color(232, 237, 232));
            button.setForeground(actionable ? WHITE : MUTED);
            return button;
        }

        @Override public Object getCellEditorValue() { return button.getText(); }
    }

    private static String displayScreeningScore(String raw) {
        if (raw == null || raw.isBlank()) return "—";
        try {
            double score = Double.parseDouble(raw.trim());
            // The supplied backend query COALESCEs an unset score to 0, so a zero
            // cannot reliably be distinguished from a real score. Avoid presenting
            // that default as a confirmed screening result.
            return score <= 0 ? "—" : raw;
        } catch (NumberFormatException ex) {
            return "—";
        }
    }

    private static final class ApplicationStatusRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String status = value == null ? "" : value.toString().trim().toUpperCase(java.util.Locale.ROOT);
            if (!isSelected) {
                if (status.contains("SELECT") || status.contains("ACCEPT")) {
                    component.setForeground(new Color(20, 112, 64));
                } else if (status.contains("REJECT") || status.contains("WITHDRAW")) {
                    component.setForeground(new Color(165, 55, 48));
                } else {
                    component.setForeground(TEXT);
                }
            }
            return component;
        }
    }

    private static final class JobOption {
        private final String id;
        private final String label;
        private JobOption(String id, String label) { this.id = id; this.label = label; }
        @Override public String toString() { return label + " (" + id + ")"; }
    }
}
