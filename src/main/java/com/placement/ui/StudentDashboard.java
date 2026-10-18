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
import javax.swing.AbstractCellEditor;
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
 * Current server commands used here:
 * GET_JOBS, APPLY_JOB, GET_MY_APPLICATIONS, GET_NOTIFICATIONS, LOGOUT.
 * Resume commands are included in the ATS page but may return the server's
 * actual unsupported-command response if ResumeCommandHandler is not routed
 * by the running server. No result is fabricated in that case.
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
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel content = new JPanel(cardLayout);

    private final DefaultTableModel jobsModel = model(new String[]{
            "Job ID", "Company", "Role", "Package", "Min CGPA", "Branches",
            "Max Backlogs", "Passing Year", "Required Skills", "Deadline", "Apply"});
    private final DefaultTableModel applicationsModel = model(new String[]{
            "Job ID", "Company", "Role", "Status", "Screening Score", "Applied At"});
    private final DefaultTableModel reportStatusModel = model(new String[]{"Application Status", "Count", "Share"});

    private final JTable jobsTable = new JTable(jobsModel);
    private final JTable applicationsTable = new JTable(applicationsModel);
    private final JTable reportStatusTable = new JTable(reportStatusModel);

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
    private JTextArea jobScreeningArea;
    private JComboBox<JobOption> screeningJobBox;
    private JPanel reportsSummaryCards;

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

        JPanel lower = new JPanel(new GridLayout(1, 2, 14, 0));
        lower.setOpaque(false);
        lower.add(infoCard("Placement Overview",
                "Placement opportunities and application information are loaded from the existing server."));
        lower.add(infoCard("Quick Guide",
                "Use Placement Drives to explore jobs, My Applications to track submitted applications, and ATS Screening to review screening availability and reports when supported by the running server."));
        center.add(lower, BorderLayout.CENTER);
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
        titles.add(label("Resume feedback and job-specific screening reports", 13, false, MUTED));
        heading.add(titles, BorderLayout.WEST);
        JButton feedback = button("Get ATS Feedback");
        feedback.addActionListener(e -> loadAtsFeedback());
        heading.add(feedback, BorderLayout.EAST);
        page.add(heading, BorderLayout.NORTH);

        JPanel body = new JPanel(new GridLayout(2, 1, 0, 14));
        body.setOpaque(false);

        JPanel overall = whiteCard();
        overall.setLayout(new BorderLayout(0, 10));
        overall.add(label("Overall Resume ATS Report", 16, true, GREEN), BorderLayout.NORTH);
        JPanel overallContent = new JPanel(new BorderLayout(12, 8));
        overallContent.setOpaque(false);
        atsScoreValue = label("Score: —", 20, true, TEXT);
        overallContent.add(atsScoreValue, BorderLayout.NORTH);
        atsFeedbackArea = reportTextArea();
        overallContent.add(new JScrollPane(atsFeedbackArea), BorderLayout.CENTER);
        JPanel uploadActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        uploadActions.setOpaque(false);
        JButton upload = button("Upload / Replace Resume");
        upload.addActionListener(e -> uploadResume());
        uploadActions.add(upload);
        overallContent.add(uploadActions, BorderLayout.SOUTH);
        overall.add(overallContent, BorderLayout.CENTER);
        body.add(overall);

        JPanel jobSpecific = whiteCard();
        jobSpecific.setLayout(new BorderLayout(0, 10));
        jobSpecific.add(label("Job-Specific Screening Report", 16, true, GREEN), BorderLayout.NORTH);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.setOpaque(false);
        screeningJobBox = new JComboBox<>();
        screeningJobBox.setPreferredSize(new Dimension(360, 28));
        JButton screen = button("Screen My Resume");
        screen.addActionListener(e -> screenAgainstSelectedJob());
        JButton refreshJobs = button("Refresh Jobs");
        refreshJobs.addActionListener(e -> loadJobs());
        controls.add(label("Placement", 12, false, TEXT));
        controls.add(screeningJobBox);
        controls.add(screen);
        controls.add(refreshJobs);
        jobSpecific.add(controls, BorderLayout.NORTH);
        jobScreeningArea = reportTextArea();
        jobSpecific.add(new JScrollPane(jobScreeningArea), BorderLayout.CENTER);
        body.add(jobSpecific);
        page.add(body, BorderLayout.CENTER);
        atsFeedbackStatus = label("ATS results appear only when the running server returns them.", 11, false, MUTED);
        page.add(atsFeedbackStatus, BorderLayout.SOUTH);
        return page;
    }

    private JPanel notificationsPage() {
        JPanel page = page();
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(label("Notifications", 22, true, TEXT), BorderLayout.WEST);
        JButton refresh = button("Check Availability");
        refresh.addActionListener(e -> loadNotifications());
        top.add(refresh, BorderLayout.EAST);
        page.add(top, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(infoCard("Student notifications are not available yet",
                "The current server endpoint returns general notification history and does not verify that each record belongs to the signed-in student. To protect student privacy, this page does not display those records. The frontend can show a personal feed once the backend provides student-scoped results."), BorderLayout.NORTH);
        notificationsStatus = label("No notification records have been displayed.", 12, false, MUTED);
        body.add(notificationsStatus, BorderLayout.SOUTH);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private JPanel profilePage() {
        JPanel page = page();
        JPanel title = verticalGroup();
        title.add(label("My Profile", 24, true, TEXT));
        title.add(Box.createVerticalStrut(4));
        title.add(label("Personal and academic details available for your account", 13, false, MUTED));
        page.add(title, BorderLayout.NORTH);

        JPanel profile = new JPanel(new GridLayout(0, 2, 10, 10));
        profile.setBackground(WHITE);
        profile.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(18, 18, 18, 18)));
        addProfileField(profile, "Full Name", name);
        addProfileField(profile, "Email Address", email.isBlank() ? "Not provided by login session" : email);
        addProfileField(profile, "PRN / Student ID", unavailableProfileValue());
        addProfileField(profile, "Department / Branch", unavailableProfileValue());
        addProfileField(profile, "Current CGPA", unavailableProfileValue());
        addProfileField(profile, "10th Standard Marks", unavailableProfileValue());
        addProfileField(profile, "12th / Diploma Marks", unavailableProfileValue());
        addProfileField(profile, "Passing Year", unavailableProfileValue());
        addProfileField(profile, "Active Backlogs", unavailableProfileValue());
        addProfileField(profile, "Current Semester", unavailableProfileValue());
        addProfileField(profile, "Phone Number", unavailableProfileValue());
        addProfileField(profile, "Skills", unavailableProfileValue());

        JPanel wrapper = new JPanel(new BorderLayout(0, 12));
        wrapper.setOpaque(false);
        wrapper.add(profile, BorderLayout.NORTH);
        wrapper.add(label("The current login session provides name and email only. The server does not expose a student-specific profile retrieval command, so CGPA, branch, passing year, backlogs, semester, phone and skills cannot be fetched by this frontend even though some exist in the Student model. The supplied student schema has no 10th/12th marks fields. Unavailable fields are shown explicitly rather than guessed.", 12, false, MUTED), BorderLayout.CENTER);
        page.add(wrapper, BorderLayout.CENTER);
        return page;
    }

    private static String unavailableProfileValue() {
        return "Not available from current backend";
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

        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setOpaque(false);
        reportsSummaryCards = new JPanel(new GridLayout(1, 3, 14, 0));
        reportsSummaryCards.setOpaque(false);
        body.add(reportsSummaryCards, BorderLayout.NORTH);

        JPanel statusCard = whiteCard();
        statusCard.setLayout(new BorderLayout(0, 8));
        statusCard.add(label("Application Status Breakdown", 16, true, GREEN), BorderLayout.NORTH);
        styleTable(reportStatusTable);
        statusCard.add(new JScrollPane(reportStatusTable), BorderLayout.CENTER);
        body.add(statusCard, BorderLayout.CENTER);

        JPanel recordCard = whiteCard();
        recordCard.setLayout(new BorderLayout(0, 8));
        recordCard.add(label("Application Screening Records", 16, true, GREEN), BorderLayout.NORTH);
        JTable reportApps = new JTable(applicationsModel);
        styleTable(reportApps);
        recordCard.add(new JScrollPane(reportApps), BorderLayout.CENTER);
        body.add(recordCard, BorderLayout.SOUTH);
        page.add(body, BorderLayout.CENTER);
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
            appliedJobIds.add(jobId);
            applyJobFilter();
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
        // The current GET_NOTIFICATIONS command returns shared history and does not
        // verify which records belong to the authenticated student. Do not expose it
        // as a personal feed from the student UI.
        setStatus(notificationsStatus,
                "Notifications are unavailable in this server version because the endpoint returns shared history, not a verified student-specific feed.");
    }

    private void loadAtsFeedback() {
        if (!hasSession()) { setStatus(atsFeedbackStatus, "No authenticated session is available."); return; }
        setStatus(atsFeedbackStatus, "Requesting ATS feedback from the server…");
        atsScoreValue.setText("ATS Score: —");
        atsFeedbackArea.setText("Waiting for a response from the server…");
        async(() -> client.sendListRequest("GET_ATS_FEEDBACK", token), response -> {
            if (!response.success) {
                String error = safe(response.errorMessage);
                if (error.toLowerCase(java.util.Locale.ROOT).contains("unknown command")) {
                    error = "ATS feedback is not connected in the current server build. The backend has not routed GET_ATS_FEEDBACK to its resume handler.";
                }
                setStatus(atsFeedbackStatus, "ATS feedback unavailable: " + error);
                atsFeedbackArea.setText("The server did not return an ATS report.\n\n" + error);
                return;
            }
            StringBuilder report = new StringBuilder();
            for (String line : response.lines) {
                if (line == null) continue;
                int separator = line.indexOf('|');
                String key = separator >= 0 ? line.substring(0, separator) : "Result";
                String value = separator >= 0 ? line.substring(separator + 1) : line;
                if (key.equalsIgnoreCase("SCORE")) atsScoreValue.setText("ATS Score: " + value);
                else report.append(key).append(": ").append(value).append('\n');
            }
            atsFeedbackArea.setText(report.toString().isBlank() ? "No additional feedback was returned." : report.toString());
            setStatus(atsFeedbackStatus, "ATS feedback loaded from the server.");
        }, error -> {
            setStatus(atsFeedbackStatus, "ATS feedback unavailable: " + error.getMessage());
            atsFeedbackArea.setText("No ATS report was loaded.\n" + error.getMessage());
        });
    }

    private void uploadResume() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Resume files (PDF, DOCX, TXT)", "pdf", "docx", "txt"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            if (bytes.length > 2 * 1024 * 1024) {
                JOptionPane.showMessageDialog(this, "The selected file exceeds the 2 MB limit.", "File Too Large", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String encoded = Base64.getEncoder().encodeToString(bytes);
            setStatus(atsFeedbackStatus, "Uploading resume to the server…");
            async(() -> client.sendRequest("UPLOAD_RESUME", token, file.getName(), encoded), response -> {
                if (!response.success) {
                    setStatus(atsFeedbackStatus, "Resume upload unavailable: " + safe(response.payload));
                    JOptionPane.showMessageDialog(this, safe(response.payload), "Resume Upload", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                setStatus(atsFeedbackStatus, "Server response received for resume upload.");
                JOptionPane.showMessageDialog(this, response.payload, "Resume Upload", JOptionPane.INFORMATION_MESSAGE);
            }, error -> {
                setStatus(atsFeedbackStatus, "Resume upload failed: " + error.getMessage());
                JOptionPane.showMessageDialog(this, error.getMessage(), "Resume Upload", JOptionPane.ERROR_MESSAGE);
            });
        } catch (Exception ex) {
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
        jobScreeningArea.setText("Requesting screening report from the server…");
        async(() -> client.sendListRequest("SCREEN_STUDENT", token, selected.id, ""), response -> {
            if (!response.success) {
                String error = safe(response.errorMessage);
                if (error.toLowerCase(java.util.Locale.ROOT).contains("unknown command")) {
                    error = "Job-specific screening is not connected in the current server build. The backend has not routed SCREEN_STUDENT to its resume handler.";
                }
                jobScreeningArea.setText("The server did not return a screening report.\n\n" + error);
                return;
            }
            StringBuilder report = new StringBuilder();
            for (String line : response.lines) {
                if (line == null) continue;
                int separator = line.indexOf('|');
                report.append(separator >= 0 ? line.substring(0, separator) + ": " + line.substring(separator + 1) : line).append('\n');
            }
            jobScreeningArea.setText(report.toString());
        }, error -> jobScreeningArea.setText("Screening report unavailable: " + error.getMessage()));
    }

    private void updateOverview() {
        if (overviewJobsValue != null) overviewJobsValue.setText(jobsLoadedSuccessfully ? String.valueOf(allJobs.size()) : "—");
        if (overviewApplicationsValue != null) overviewApplicationsValue.setText(applicationsLoadedSuccessfully ? String.valueOf(applicationsModel.getRowCount()) : "—");
        if (overviewStatus != null) {
            if (!hasSession()) overviewStatus.setText("No authenticated session is available.");
            else if (!jobsLoadedSuccessfully && !applicationsLoadedSuccessfully) overviewStatus.setText("Waiting for placement and application data from the server.");
            else if (!jobsLoadedSuccessfully || !applicationsLoadedSuccessfully) overviewStatus.setText("Some dashboard data has not loaded successfully.");
            else overviewStatus.setText("Counts are based on live job and application records returned by the server.");
        }
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
