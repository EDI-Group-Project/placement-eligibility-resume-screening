package com.placement.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.placement.sockets.SocketClient;

/**
 * TPC Dashboard
 * ---------------------------------------------------------
 * Workflow implemented here (per project notes):
 *  1. TPC receives eligibility criteria + job postings from TPO
 *     -> "Job Postings" panel lists these postings.
 *  2. TPC filters eligible students based on that criteria
 *     -> "Eligible Students" panel runs the filter and lists results.
 *  3. TPC sends notifications to eligible students so they can apply
 *     -> "Send Notification" button + "Notifications" log panel.
 *
 * Theme is copied 1:1 from LoginForm.java (same colors, fonts, spacing)
 * so the dashboard feels like part of the same app.
 *
 * Job postings, students, and notifications are loaded through the TCP
 * server when an authenticated session is supplied. A small fallback data set
 * keeps the screen previewable when it is launched standalone.
 */
public class TPCDashboard extends JFrame {

    // ---- shared placement portal theme ----
    private static final Color SIDEBAR_GREEN = new Color(20, 83, 45);
    private static final Color BG_GRAY = new Color(243, 244, 246);
    private static final Color TEXT_GRAY = Color.GRAY;
    private static final Font FONT_HEADING = new Font("SansSerif", Font.BOLD, 24);
    private static final Font FONT_LABEL = new Font("SansSerif", Font.BOLD, 12);
    private static final Font FONT_BODY = new Font("SansSerif", Font.PLAIN, 13);

    // the logged-in TPC user
    private final String tpcName;
    private final String sessionToken;

    // main content area, swapped via CardLayout
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);

    // data
    private final List<JobPosting> jobPostings = new ArrayList<>();
    private final List<Student> allStudents = new ArrayList<>();
    private final List<Notification> notifications = new ArrayList<>();

    // tracks which student PRNs have already been notified for each job (jobId -> set of PRNs),
    // so the same student isn't shown/notified twice for the same posting and the
    // "Students Pending Notification" stat on the dashboard is accurate
    private final Map<String, Set<String>> notifiedPrnsByJob = new HashMap<>();

    // job postings table
    private JTable jobTable;
    private DefaultTableModel jobTableModel;

    // eligible students table
    private JTable eligibleTable;
    private DefaultTableModel eligibleTableModel;
    private JLabel eligibleHeaderLabel;
    private JButton btnSendNotification;
    private JobPosting currentJob; // job currently being viewed in the Eligible Students panel

    // notifications table
    private DefaultTableModel notificationsTableModel;

    // dashboard stat labels (need to update after data changes)
    private JLabel statJobsValue;
    private JLabel statPendingValue;
    private JLabel statSentValue;

    public TPCDashboard(String tpcName) {
        this(tpcName, null);
    }

    public TPCDashboard(String tpcName, String sessionToken) {
        this.tpcName = (tpcName == null || tpcName.isBlank()) ? "TPC" : tpcName;
        this.sessionToken = sessionToken;

        setTitle("Placement Eligibility Portal - TPC Dashboard");
        setSize(1100, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        loadJobsFromServer();
        loadStudentsFromServer();

        initComponents();
    }

    // ------------------------------------------------------------------
    // UI setup
    // ------------------------------------------------------------------

    private void initComponents() {
        add(buildSidebar(), BorderLayout.WEST);

        contentPanel.setBackground(BG_GRAY);
        contentPanel.add(buildDashboardPanel(), "DASHBOARD");
        contentPanel.add(buildJobPostingsPanel(), "JOBS");
        contentPanel.add(buildEligibleStudentsPanel(), "ELIGIBLE");
        contentPanel.add(buildNotificationsPanel(), "NOTIFICATIONS");

        add(contentPanel, BorderLayout.CENTER);

        cardLayout.show(contentPanel, "DASHBOARD");
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(SIDEBAR_GREEN);
        sidebar.setPreferredSize(new Dimension(280, getHeight()));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(40, 30, 40, 30));

        JLabel title = new JLabel("PLACEMENT");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("ELIGIBILITY PORTAL");
        subtitle.setForeground(new Color(200, 220, 210));
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel heading = new JLabel("<html>TPC<br>Dashboard</html>");
        heading.setForeground(Color.WHITE);
        heading.setFont(new Font("SansSerif", Font.BOLD, 22));
        heading.setBorder(BorderFactory.createEmptyBorder(30, 0, 5, 0));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel welcomeLbl = new JLabel("Signed in as " + tpcName);
        welcomeLbl.setForeground(new Color(200, 220, 210));
        welcomeLbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        welcomeLbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 25, 0));
        welcomeLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        sidebar.add(title);
        sidebar.add(subtitle);
        sidebar.add(heading);
        sidebar.add(welcomeLbl);

        sidebar.add(sidebarNavButton("Dashboard", e -> cardLayout.show(contentPanel, "DASHBOARD")));
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));
        sidebar.add(sidebarNavButton("Job Postings", e -> cardLayout.show(contentPanel, "JOBS")));
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));
        sidebar.add(sidebarNavButton("Eligible Students", e -> cardLayout.show(contentPanel, "ELIGIBLE")));
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));
        sidebar.add(sidebarNavButton("Notifications", e -> cardLayout.show(contentPanel, "NOTIFICATIONS")));

        sidebar.add(Box.createVerticalGlue());

        JButton logout = sidebarNavButton("Logout", e -> handleLogout());
        sidebar.add(logout);

        return sidebar;
    }

    private JButton sidebarNavButton(String text, ActionListener onClick) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(220, 34));
        button.setFocusPainted(false);
        button.setForeground(Color.WHITE);
        button.setBackground(SIDEBAR_GREEN);
        button.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.addActionListener(onClick);
        return button;
    }

    /** Reusable "card" wrapper matching the login screen's white card style. */
    private JPanel card(int width) {
        JPanel c = new JPanel();
        c.setBackground(Color.WHITE);
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        if (width > 0) {
            c.setMaximumSize(new Dimension(width, Integer.MAX_VALUE));
        }
        return c;
    }

    private JLabel sectionHeading(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_HEADING);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JLabel sectionSubtext(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(TEXT_GRAY);
        lbl.setFont(FONT_BODY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setBorder(BorderFactory.createEmptyBorder(4, 0, 20, 0));
        return lbl;
    }

    // ------------------------------------------------------------------
    // Dashboard panel
    // ------------------------------------------------------------------

    private JPanel buildDashboardPanel() {
        JPanel wrapper = new JPanel();
        wrapper.setBackground(BG_GRAY);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        JLabel welcome = sectionHeading("Welcome, " + tpcName);
        JLabel desc = sectionSubtext("Here's what's happening with placements today.");

        JPanel statsRow = new JPanel(new GridLayout(1, 3, 20, 0));
        statsRow.setBackground(BG_GRAY);
        statsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsRow.setMaximumSize(new Dimension(760, 120));

        statJobsValue = new JLabel();
        statPendingValue = new JLabel();
        statSentValue = new JLabel();

        statsRow.add(statCard("Active Job Postings", statJobsValue));
        statsRow.add(statCard("Students Pending Notification", statPendingValue));
        statsRow.add(statCard("Notifications Sent", statSentValue));

        JLabel footer = new JLabel("Authorized users only");
        footer.setForeground(TEXT_GRAY);
        footer.setFont(new Font("SansSerif", Font.PLAIN, 11));
        footer.setBorder(BorderFactory.createEmptyBorder(30, 0, 0, 0));
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);

        wrapper.add(welcome);
        wrapper.add(desc);
        wrapper.add(statsRow);
        wrapper.add(footer);

        refreshDashboardStats();
        return wrapper;
    }

    private JPanel statCard(String label, JLabel valueLabel) {
        JPanel c = card(0);
        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(TEXT_GRAY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 30));
        valueLabel.setForeground(SIDEBAR_GREEN);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        valueLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        c.add(lbl);
        c.add(valueLabel);
        return c;
    }

    private void refreshDashboardStats() {
        statJobsValue.setText(String.valueOf(jobPostings.size()));
        statPendingValue.setText(String.valueOf(computeTotalPendingNotifications()));
        statSentValue.setText(String.valueOf(notifications.size()));
    }

    /**
     * Total, across every job posting, of eligible students who have NOT yet
     * been notified for that specific posting. This is what "Students Pending
     * Notification" on the dashboard actually means.
     */
    private int computeTotalPendingNotifications() {
        int pending = 0;
        for (JobPosting job : jobPostings) {
            for (Student s : allStudents) {
                if (isEligible(s, job) && !isAlreadyNotified(job.id, s.prn)) {
                    pending++;
                }
            }
        }
        return pending;
    }

    // ------------------------------------------------------------------
    // Job Postings panel  (data coming FROM the TPO)
    // ------------------------------------------------------------------

    private JPanel buildJobPostingsPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG_GRAY);
        wrapper.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        JPanel headerBox = new JPanel();
        headerBox.setBackground(BG_GRAY);
        headerBox.setLayout(new BoxLayout(headerBox, BoxLayout.Y_AXIS));
        headerBox.add(sectionHeading("Job Postings"));
        headerBox.add(sectionSubtext("Eligibility criteria and job postings received from the TPO."));

        String[] columns = {"Job ID", "Company", "Role", "Min Package", "Min CGPA", "Branches", "Max Backlogs", "Deadline"};
        jobTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        jobTable = new JTable(jobTableModel);
        jobTable.setRowHeight(28);
        jobTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        refreshJobTable();

        JScrollPane scrollPane = new JScrollPane(jobTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)));

        JButton btnViewEligible = new JButton("View Eligible Students");
        btnViewEligible.setBackground(SIDEBAR_GREEN);
        btnViewEligible.setForeground(Color.WHITE);
        btnViewEligible.setFocusPainted(false);
        btnViewEligible.addActionListener(e -> openEligibleStudentsForSelectedJob());

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actionRow.setBackground(BG_GRAY);
        actionRow.add(btnViewEligible);

        JPanel tableWrap = new JPanel(new BorderLayout());
        tableWrap.setBackground(BG_GRAY);
        tableWrap.add(scrollPane, BorderLayout.CENTER);
        tableWrap.add(actionRow, BorderLayout.SOUTH);

        wrapper.add(headerBox, BorderLayout.NORTH);
        wrapper.add(tableWrap, BorderLayout.CENTER);
        return wrapper;
    }

    private void refreshJobTable() {
        jobTableModel.setRowCount(0);
        for (JobPosting j : jobPostings) {
            jobTableModel.addRow(new Object[]{
                    j.id, j.company, j.role, j.minPackage,
                    j.minCgpa, String.join(", ", j.allowedBranches),
                    j.maxBacklogs, j.deadline
            });
        }
    }

    private void openEligibleStudentsForSelectedJob() {
        int row = jobTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a job posting first.",
                    "No job selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        currentJob = jobPostings.get(row);
        runEligibilityFilter(currentJob);
        cardLayout.show(contentPanel, "ELIGIBLE");
    }

    // ------------------------------------------------------------------
    // Eligible Students panel (TPC's filtering step)
    // ------------------------------------------------------------------

    private JPanel buildEligibleStudentsPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG_GRAY);
        wrapper.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        JPanel headerBox = new JPanel();
        headerBox.setBackground(BG_GRAY);
        headerBox.setLayout(new BoxLayout(headerBox, BoxLayout.Y_AXIS));
        headerBox.add(sectionHeading("Eligible Students"));
        eligibleHeaderLabel = sectionSubtext("Select a job posting from \"Job Postings\" to filter eligible students.");
        headerBox.add(eligibleHeaderLabel);

        String[] columns = {"Notify", "PRN", "Name", "Course", "CGPA", "Branch", "Backlogs"};
        eligibleTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public Class<?> getColumnClass(int col) {
                return col == 0 ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 0; // only the checkbox column is editable
            }
        };
        eligibleTable = new JTable(eligibleTableModel);
        eligibleTable.setRowHeight(28);

        JScrollPane scrollPane = new JScrollPane(eligibleTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)));

        JButton btnSelectAll = new JButton("Select All");
        btnSelectAll.addActionListener(e -> setAllEligibleChecks(true));

        JButton btnSelectNone = new JButton("Select None");
        btnSelectNone.addActionListener(e -> setAllEligibleChecks(false));

        btnSendNotification = new JButton("Send Notification to Selected");
        btnSendNotification.setBackground(SIDEBAR_GREEN);
        btnSendNotification.setForeground(Color.WHITE);
        btnSendNotification.setFocusPainted(false);
        btnSendNotification.addActionListener(e -> sendNotificationsToSelected());

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actionRow.setBackground(BG_GRAY);
        actionRow.add(btnSelectAll);
        actionRow.add(btnSelectNone);
        actionRow.add(btnSendNotification);

        JPanel tableWrap = new JPanel(new BorderLayout());
        tableWrap.setBackground(BG_GRAY);
        tableWrap.add(scrollPane, BorderLayout.CENTER);
        tableWrap.add(actionRow, BorderLayout.SOUTH);

        wrapper.add(headerBox, BorderLayout.NORTH);
        wrapper.add(tableWrap, BorderLayout.CENTER);
        return wrapper;
    }

    /** Whether a student matches a job posting's eligibility criteria (CGPA, backlogs, branch). */
    private boolean isEligible(Student s, JobPosting job) {
        boolean cgpaOk = s.cgpa >= job.minCgpa;
        boolean backlogOk = s.backlogs <= job.maxBacklogs;
        boolean branchOk = job.allowedBranches.contains(s.branch);
        return cgpaOk && backlogOk && branchOk;
    }

    /** Whether this student has already been notified about this specific job. */
    private boolean isAlreadyNotified(String jobId, String prn) {
        Set<String> notified = notifiedPrnsByJob.get(jobId);
        return notified != null && notified.contains(prn);
    }

    /**
     * Core filtering logic: students matching a job's eligibility criteria who
     * have not already been notified about it. This is the TPC's filtering step
     * described in the workflow (criteria comes from TPO, TPC filters students).
     */
    private void runEligibilityFilter(JobPosting job) {
        eligibleHeaderLabel.setText("Students eligible for " + job.company + " - " + job.role
                + "  (Min CGPA " + job.minCgpa + ", max " + job.maxBacklogs + " backlogs)");

        eligibleTableModel.setRowCount(0);
        for (Student s : allStudents) {
            if (isEligible(s, job) && !isAlreadyNotified(job.id, s.prn)) {
                eligibleTableModel.addRow(new Object[]{
                        Boolean.FALSE, s.prn, s.name, s.course, s.cgpa, s.branch, s.backlogs
                });
            }
        }

        if (eligibleTableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this,
                    "No eligible students remaining to notify for this posting.",
                    "No eligible students", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void setAllEligibleChecks(boolean checked) {
        for (int i = 0; i < eligibleTableModel.getRowCount(); i++) {
            eligibleTableModel.setValueAt(checked, i, 0);
        }
    }

    private void sendNotificationsToSelected() {
        if (currentJob == null) {
            JOptionPane.showMessageDialog(this, "Open a job posting from \"Job Postings\" first.",
                    "No job selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<String> selectedPrns = new ArrayList<>();
        for (int i = 0; i < eligibleTableModel.getRowCount(); i++) {
            Boolean checked = (Boolean) eligibleTableModel.getValueAt(i, 0);
            if (Boolean.TRUE.equals(checked)) {
                selectedPrns.add((String) eligibleTableModel.getValueAt(i, 1));
            }
        }

        if (selectedPrns.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select at least one student to notify.",
                    "Nothing selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean sentOk = sendNotificationToServer(currentJob, selectedPrns);

        if (sentOk) {
            notifiedPrnsByJob.computeIfAbsent(currentJob.id, k -> new HashSet<>()).addAll(selectedPrns);

            notifications.add(new Notification(currentJob.company + " - " + currentJob.role,
                    selectedPrns.size(), java.time.LocalDateTime.now().toString()));
            refreshNotificationsTable();
            refreshDashboardStats();
            JOptionPane.showMessageDialog(this,
                    "Notification sent to " + selectedPrns.size() + " student(s).",
                    "Notification sent", JOptionPane.INFORMATION_MESSAGE);

            // re-run the filter so students who were just notified drop off this list
            runEligibilityFilter(currentJob);
        } else {
            JOptionPane.showMessageDialog(this, "Could not send notifications. Please try again.",
                    "Send failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ------------------------------------------------------------------
    // Notifications panel (log of what TPC has sent)
    // ------------------------------------------------------------------

    private JPanel buildNotificationsPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG_GRAY);
        wrapper.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        JPanel headerBox = new JPanel();
        headerBox.setBackground(BG_GRAY);
        headerBox.setLayout(new BoxLayout(headerBox, BoxLayout.Y_AXIS));
        headerBox.add(sectionHeading("Notifications"));
        headerBox.add(sectionSubtext("History of eligibility notifications sent to students."));

        String[] columns = {"Job Posting", "Students Notified", "Sent At"};
        notificationsTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable table = new JTable(notificationsTableModel);
        table.setRowHeight(28);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)));

        wrapper.add(headerBox, BorderLayout.NORTH);
        wrapper.add(scrollPane, BorderLayout.CENTER);
        return wrapper;
    }

    private void refreshNotificationsTable() {
        notificationsTableModel.setRowCount(0);
        for (Notification n : notifications) {
            notificationsTableModel.addRow(new Object[]{n.jobLabel, n.studentCount, n.sentAt});
        }
    }

    // ------------------------------------------------------------------
    // TCP server integration
    // ------------------------------------------------------------------

    private void loadJobsFromServer() {
        jobPostings.clear();
        try {
            SocketClient.ListResponse response = new SocketClient().sendListRequest("GET_JOBS");
            if (!response.success) throw new IllegalStateException(response.errorMessage);
            for (String line : response.lines) {
                String[] p = line.split("\\|", -1);
                if (p.length < 8) continue;
                jobPostings.add(new JobPosting(p[0], p[1], p[2], p[3],
                        Double.parseDouble(p[4]), List.of(p[5].split(",")), Integer.parseInt(p[6]), p[p.length - 1]));
            }
        } catch (Exception e) {
            // Keep a deterministic fallback for opening the UI without a server.
            jobPostings.add(new JobPosting("JOB001", "TCS", "Software Engineer", "6 LPA",
                    7.5, List.of("CSE", "IT"), 0, "2026-09-15"));
        }
    }

    private void loadStudentsFromServer() {
        allStudents.clear();
        try {
            if (sessionToken == null) throw new IllegalStateException("No authenticated session.");
            SocketClient.ListResponse response = new SocketClient().sendListRequest("GET_STUDENTS", sessionToken);
            if (!response.success) throw new IllegalStateException(response.errorMessage);
            for (String line : response.lines) {
                String[] p = line.split("\\|", -1);
                if (p.length < 10) continue;
                allStudents.add(new Student(p[0], p[1], p[3], p[3],
                        Double.parseDouble(p[4]), Integer.parseInt(p[6])));
            }
        } catch (Exception e) {
            allStudents.add(new Student("PRN001", "Aarav Sharma", "B.Tech CSE", "CSE", 8.5, 0));
            allStudents.add(new Student("PRN002", "Priya Patil", "B.Tech IT", "IT", 7.8, 0));
        }
    }

    private boolean sendNotificationToServer(JobPosting job, List<String> studentPrns) {
        if (sessionToken == null) return false;
        try {
            SocketClient.Response response = new SocketClient().sendRequest(
                    "SEND_NOTIFICATION", sessionToken, job.id, String.join(",", studentPrns));
            return response.success;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Server error: " + e.getMessage(),
                    "Notification error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private void handleLogout() {
        int choice = JOptionPane.showConfirmDialog(this, "Are you sure you want to log out?",
                "Confirm logout", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            dispose();
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
        }
    }

    // ------------------------------------------------------------------
    // Simple model classes
    // ------------------------------------------------------------------

    private static class JobPosting {
        String id, company, role, minPackage, deadline;
        double minCgpa;
        List<String> allowedBranches;
        int maxBacklogs;

        JobPosting(String id, String company, String role, String minPackage,
                   double minCgpa, List<String> allowedBranches, int maxBacklogs, String deadline) {
            this.id = id;
            this.company = company;
            this.role = role;
            this.minPackage = minPackage;
            this.minCgpa = minCgpa;
            this.allowedBranches = allowedBranches;
            this.maxBacklogs = maxBacklogs;
            this.deadline = deadline;
        }
    }

    private static class Student {
        String prn, name, course, branch;
        double cgpa;
        int backlogs;

        Student(String prn, String name, String course, String branch, double cgpa, int backlogs) {
            this.prn = prn;
            this.name = name;
            this.course = course;
            this.branch = branch;
            this.cgpa = cgpa;
            this.backlogs = backlogs;
        }
    }

    private static class Notification {
        String jobLabel;
        int studentCount;
        String sentAt;

        Notification(String jobLabel, int studentCount, String sentAt) {
            this.jobLabel = jobLabel;
            this.studentCount = studentCount;
            this.sentAt = sentAt;
        }
    }

    // ------------------------------------------------------------------
    // Standalone launch (for testing this screen without going through login)
    // ------------------------------------------------------------------

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TPCDashboard("Demo TPC").setVisible(true));
    }
}