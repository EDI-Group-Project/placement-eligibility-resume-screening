package com.placement.ui;

import static com.placement.ui.DirectorDashboard.Kit.*;

import com.placement.sockets.SocketClient;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Director dashboard - INSTITUTION-LEVEL PLACEMENT OVERSIGHT.
 *
 * The Director looks at the placement programme as a whole: drives and their
 * deadlines, partner companies, how well each department is covered by drives,
 * and college-level reports. Read-only except sending announcements.
 *
 * Backend commands used: GET_DASHBOARD_STATS, GET_JOBS, GET_STUDENTS,
 * GET_ELIGIBLE_STUDENTS, GET_NOTIFICATIONS, SEND_GENERAL_NOTIFICATION, LOGOUT.
 */
public class DirectorDashboard extends JFrame {

    private static final long serialVersionUID = 1L;

    private final String displayName;
    private final Api api;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JLabel banner = banner();

    private List<Student> students = new ArrayList<>();
    private List<Job> jobs = new ArrayList<>();
    /** jobs|pending|notifications|students from GET_DASHBOARD_STATS, or null if not loaded. */
    private int[] stats;
    private final Map<String, Integer> eligibleCounts = new HashMap<>();

    // Dashboard
    private final JLabel kDrives = valueLabel();
    private final JLabel kCompanies = valueLabel();
    private final JLabel kStudents = valueLabel();
    private final JLabel kNotices = valueLabel();
    private final Donut timelineDonut = new Donut(GREEN, AMBER, new Color(160, 168, 163));
    private final Bars deptDriveBars = new Bars(BLUE, false);
    private final JPanel upcomingList = new JPanel();

    // Drives
    private final DefaultTableModel driveModel = model(
            new String[]{"Job ID", "Company", "Role", "Package", "Min CGPA", "Branches", "Max Backlogs",
                    "Passing Year", "Deadline", "Status"},
            String.class, String.class, String.class, String.class, Double.class, String.class, Integer.class,
            Integer.class, String.class, String.class);
    private final JTable driveTable = table(driveModel);
    private final JTextField driveSearch = searchField("Company, role, branch, skill...", 240);
    private final JComboBox<String> companyCombo = combo(150, "All companies");
    private final JComboBox<String> branchCombo = combo(150, "All departments");
    private final JComboBox<String> statusCombo = combo(140, "All statuses", "Open", "Closing Soon", "Closed");
    private final JComboBox<String> driveSortCombo = combo(180, "Default order", "Deadline: soonest first",
            "Package: high to low", "Min CGPA: low to high", "Company: A to Z");
    private final JLabel driveCount = muted(" ");

    // Companies
    private final DefaultTableModel companyModel = model(
            new String[]{"Company", "Drives", "Roles", "Top Package", "Min CGPA Range", "Departments Covered",
                    "Next Deadline"},
            String.class, Integer.class, String.class, String.class, String.class, String.class, String.class);
    private final JTable companyTable = table(companyModel);
    private final JTextField companySearch = searchField("Search companies...", 240);
    private final JLabel companyCount = muted(" ");

    // Departments
    private final DefaultTableModel deptModel = model(
            new String[]{"Department", "Students", "Avg CGPA", "Drives Open To", "Drive Coverage",
                    "Best Package Available"},
            String.class, Integer.class, Double.class, Integer.class, String.class, String.class);
    private final JTable deptTable = table(deptModel);
    private final Bars coverageBars = new Bars(BLUE, false);
    private final JLabel deptInsight = muted(" ");

    // Reports
    private final JTabbedPane reportTabs = new JTabbedPane();
    private final DefaultTableModel repSummaryModel = model(
            new String[]{"Metric", "Value", "Note"}, String.class, String.class, String.class);
    private final JTable repSummaryTable = table(repSummaryModel);
    private final DefaultTableModel repDriveModel = model(
            new String[]{"Job ID", "Company", "Role", "Package", "Deadline", "Status", "Eligible Students"},
            String.class, String.class, String.class, String.class, String.class, String.class, String.class);
    private final JTable repDriveTable = table(repDriveModel);
    private final DefaultTableModel repCompanyModel = model(
            new String[]{"Company", "Drives", "Top Package", "Total Eligible Seats", "Next Deadline"},
            String.class, Integer.class, String.class, String.class, String.class);
    private final JTable repCompanyTable = table(repCompanyModel);
    private final DefaultTableModel repDeptModel = model(
            new String[]{"Department", "Students", "Avg CGPA", "Drives Open To", "Drive Coverage"},
            String.class, Integer.class, Double.class, Integer.class, String.class);
    private final JTable repDeptTable = table(repDeptModel);

    private boolean reportsOpened;
    private final Bars packageBars = new Bars(GREEN, true);
    private final Bars eligibleBars = new Bars(BLUE, false);

    private final JTable companySnapshotTable = table(companyModel);
    private final JComboBox<String> upcomingStatus = combo(150, "All open drives", "Open", "Closing Soon");
    private final JComboBox<String> upcomingDept = combo(160, "All departments");
    private final JTextField reportSearch = searchField("Search this report...", 240);
    private final JComboBox<String> reportCompanyCombo = combo(170, "All companies");

    private NoticesPage notices;

    public DirectorDashboard(String displayName, String sessionToken) {
        this.displayName = displayName == null || displayName.isBlank() ? "Director" : displayName;
        this.api = new Api(sessionToken);

        setTitle("Placement Eligibility Portal - Director Dashboard");
        setSize(1320, 820);
        setMinimumSize(new Dimension(1120, 700));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        buildUi();
        if (!api.hasSession()) {
            showBanner(banner, "No active session. Please log in again.");
            return;
        }
        loadAll();
    }

    /** Kept so old code that calls new DirectorDashboard() still compiles; it has no session. */
    private DirectorDashboard(String[] session) {
        this(session[1], session[0]);
    }

    /** Used by LoginFrame: finds the session of the login that just succeeded. */
    public DirectorDashboard() {
        this(Kit.recover("Director"));
    }

    // ------------------------------------------------------------------
    // UI CONSTRUCTION
    // ------------------------------------------------------------------
    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);

        Sidebar sidebar = new Sidebar("DIRECTOR PORTAL");
        JButton first = sidebar.addItem("Dashboard", () -> show("dash"));
        sidebar.addItem("Placement Drives", () -> show("drives"));
        sidebar.addItem("Companies", () -> show("companies"));
        sidebar.addItem("Departments", () -> show("departments"));
        sidebar.addItem("Reports", () -> {
            show("reports");
            reportsOpened = true;
            if (eligibleCounts.isEmpty()) loadEligibleCounts();
        });
        sidebar.addItem("Announcements", () -> {
            show("notices");
            notices.reload();
        });
        sidebar.addBottom("Logout", this::logout);
        sidebar.select(first);

        JPanel north = new JPanel(new BorderLayout());
        north.add(topBar("Director Dashboard", "DIRECTOR"), BorderLayout.NORTH);
        north.add(banner, BorderLayout.SOUTH);

        content.setBackground(BG);
        notices = new NoticesPage(this, api, banner,
                "Messages to the placement team, faculty and students", true);
        content.add(buildDashboardPage(), "dash");
        content.add(buildDrivesPage(), "drives");
        content.add(buildCompaniesPage(), "companies");
        content.add(buildDepartmentsPage(), "departments");
        content.add(buildReportsPage(), "reports");
        content.add(notices.panel, "notices");

        JPanel right = new JPanel(new BorderLayout());
        right.add(north, BorderLayout.NORTH);
        right.add(content, BorderLayout.CENTER);
        root.add(sidebar, BorderLayout.WEST);
        root.add(right, BorderLayout.CENTER);
        setContentPane(root);
    }

    private void show(String page) {
        cards.show(content, page);
    }

    private JButton refreshButton() {
        JButton b = ghostButton("Refresh");
        b.addActionListener(e -> loadAll());
        return b;
    }

    private static JPanel labeled(String label, JComponent c) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        p.add(filterLabel(label), BorderLayout.NORTH);
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildDashboardPage() {
        VStack stack = new VStack();
        stack.add(grid(4, 14,
                kpiCard("Active Drives", kDrives, "Current job postings", GREEN),
                kpiCard("Partner Companies", kCompanies, "Recruiting this season", BLUE),
                kpiCard("Registered Students", kStudents, "In the placement pool", AMBER),
                kpiCard("Notifications Sent", kNotices, "Messages to students", RED)), 16);

        Card upcoming = new Card();
        JPanel head = new JPanel(new BorderLayout(10, 0));
        head.setOpaque(false);
        head.add(h2("Upcoming Deadlines"), BorderLayout.WEST);
        head.add(flow(FlowLayout.RIGHT, 10, upcomingStatus, upcomingDept), BorderLayout.EAST);
        upcoming.add(head, BorderLayout.NORTH);
        upcomingList.setOpaque(false);
        upcomingList.setLayout(new BoxLayout(upcomingList, BoxLayout.Y_AXIS));
        upcoming.add(upcomingList, BorderLayout.CENTER);
        upcomingStatus.addActionListener(e -> refreshDashboard());
        upcomingDept.addActionListener(e -> refreshDashboard());
        stack.add(upcoming, 16);

        Card companies = new Card();
        companies.add(h2("Companies at a Glance"), BorderLayout.NORTH);
        widths(companySnapshotTable, 140, 70, 260, 100, 120, 240, 120);
        companies.add(tableScroll(companySnapshotTable, 220), BorderLayout.CENTER);
        stack.add(companies, 0);

        return pageShell("Dashboard", "A quick overview of the placement programme and upcoming drives",
                refreshButton(), scrollPage(stack));
    }

    private JPanel buildDrivesPage() {
        Card card = new Card();

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        filters.setOpaque(false);
        filters.add(labeled("SEARCH", driveSearch));
        filters.add(labeled("COMPANY", companyCombo));
        filters.add(labeled("DEPARTMENT", branchCombo));
        filters.add(labeled("STATUS", statusCombo));
        filters.add(labeled("SORT BY", driveSortCombo));
        JButton clear = ghostButton("Clear");
        clear.addActionListener(e -> {
            driveSearch.setText("");
            companyCombo.setSelectedIndex(0);
            branchCombo.setSelectedIndex(0);
            statusCombo.setSelectedIndex(0);
            driveSortCombo.setSelectedIndex(0);
        });
        JPanel clearWrap = new JPanel(new BorderLayout());
        clearWrap.setOpaque(false);
        clearWrap.add(Box.createVerticalStrut(16), BorderLayout.NORTH);
        clearWrap.add(clear, BorderLayout.CENTER);
        filters.add(clearWrap);

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        top.add(filters, BorderLayout.NORTH);
        top.add(driveCount, BorderLayout.SOUTH);
        card.add(top, BorderLayout.NORTH);

        statusColumn(driveTable, 9);
        widths(driveTable, 80, 110, 160, 90, 90, 170, 115, 115, 100, 125);
        sorter(driveTable).setComparator(3, (a, b) -> Double.compare(packageOf(a), packageOf(b)));
        sorter(driveTable).setComparator(8, (a, b) -> compareDates(String.valueOf(a), String.valueOf(b)));
        card.add(tableScroll(driveTable, 0), BorderLayout.CENTER);

        onChange(driveSearch, this::applyDriveFilter);
        companyCombo.addActionListener(e -> applyDriveFilter());
        branchCombo.addActionListener(e -> applyDriveFilter());
        statusCombo.addActionListener(e -> applyDriveFilter());
        driveSortCombo.addActionListener(e -> applyDriveSort());
        driveTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) showDriveDetails();
            }
        });

        JButton eligible = primaryButton("View Eligible Students");
        eligible.addActionListener(e -> showEligibleDialog());
        JButton details = ghostButton("Drive Details");
        details.addActionListener(e -> showDriveDetails());
        JButton export = ghostButton("Export CSV");
        export.addActionListener(e -> exportCsv(this, driveTable, "placement-drives"));
        return pageShell("Placement Drives", "Every drive across the college (double-click a row for details)",
                flow(FlowLayout.RIGHT, 8, details, export, eligible), card);
    }

    private JPanel buildCompaniesPage() {
        Card card = new Card();
        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setOpaque(false);
        top.add(companySearch, BorderLayout.WEST);
        top.add(companyCount, BorderLayout.CENTER);
        card.add(top, BorderLayout.NORTH);
        widths(companyTable, 140, 70, 260, 100, 120, 240, 120);
        sorter(companyTable).setComparator(3, (a, b) -> Double.compare(packageOf(a), packageOf(b)));
        sorter(companyTable).setComparator(6, (a, b) -> compareDates(String.valueOf(a), String.valueOf(b)));
        card.add(tableScroll(companyTable, 0), BorderLayout.CENTER);
        onChange(companySearch, () -> {
            sorter(companyTable).setRowFilter(textFilter(companySearch.getText()));
            companyCount.setText("  " + companyTable.getRowCount() + " of " + companyModel.getRowCount()
                    + " companies");
        });
        companyTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) showCompanyDetails();
            }
        });
        JButton export = primaryButton("Export CSV");
        export.addActionListener(e -> exportCsv(this, companyTable, "companies"));
        return pageShell("Companies", "Partner companies derived from placement drives (double-click for their drives)",
                export, card);
    }

    private JPanel buildDepartmentsPage() {
        VStack stack = new VStack();
        Card table = new Card();
        table.add(h2("Department Coverage"), BorderLayout.NORTH);
        widths(deptTable, 160, 80, 90, 110, 120, 160);
        sorter(deptTable).setComparator(5, (a, b) -> Double.compare(packageOf(a), packageOf(b)));
        table.add(tableScroll(deptTable, 230), BorderLayout.CENTER);
        table.add(deptInsight, BorderLayout.SOUTH);
        stack.add(table, 16);

        Card chart = new Card();
        chart.add(h2("Drives Open to Each Department"), BorderLayout.NORTH);
        chart.add(coverageBars, BorderLayout.CENTER);
        stack.add(chart, 0);

        JButton export = ghostButton("Export CSV");
        export.addActionListener(e -> exportCsv(this, deptTable, "department-coverage"));
        return pageShell("Departments",
                "How well each department is served by the current placement drives",
                flow(FlowLayout.RIGHT, 8, refreshButton(), export), scrollPage(stack));
    }

    private JPanel buildReportsPage() {
        reportTabs.setFont(font(Font.BOLD, 12));
        statusColumn(repDriveTable, 5);
        widths(repSummaryTable, 260, 150, 420);
        widths(repDriveTable, 80, 130, 170, 90, 100, 110, 130);
        widths(repCompanyTable, 150, 70, 110, 150, 120);
        widths(repDeptTable, 170, 80, 90, 120, 120);

        VStack analytics = new VStack();
        analytics.add(grid(2, 14,
                chartCard("Drive Timeline", timelineDonut),
                chartCard("Drives Open to Each Department", deptDriveBars)), 14);
        analytics.add(grid(2, 14,
                chartCard("Top Package by Company (LPA)", packageBars),
                chartCard("Eligible Students per Drive", eligibleBars)), 0);
        analytics.setBackground(Color.WHITE);
        analytics.setBorder(new EmptyBorder(12, 12, 12, 12));
        JScrollPane analyticsScroll = scrollPage(analytics);
        analyticsScroll.getViewport().setOpaque(true);
        analyticsScroll.getViewport().setBackground(Color.WHITE);

        reportTabs.addTab("Analytics (Charts)", analyticsScroll);
        reportTabs.addTab("Executive Summary", tableScroll(repSummaryTable, 0));
        reportTabs.addTab("Drive Summary", tableScroll(repDriveTable, 0));
        reportTabs.addTab("Company Report", tableScroll(repCompanyTable, 0));
        reportTabs.addTab("Department Report", tableScroll(repDeptTable, 0));

        Card card = new Card();
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        filters.setOpaque(false);
        filters.add(labeled("SEARCH TABLE REPORTS", reportSearch));
        filters.add(labeled("COMPANY", reportCompanyCombo));
        JButton clear = ghostButton("Clear");
        clear.addActionListener(e -> {
            reportSearch.setText("");
            reportCompanyCombo.setSelectedIndex(0);
        });
        JPanel clearWrap = new JPanel(new BorderLayout());
        clearWrap.setOpaque(false);
        clearWrap.add(Box.createVerticalStrut(16), BorderLayout.NORTH);
        clearWrap.add(clear, BorderLayout.CENTER);
        filters.add(clearWrap);
        card.add(filters, BorderLayout.NORTH);
        card.add(reportTabs, BorderLayout.CENTER);
        onChange(reportSearch, this::applyReportFilter);
        reportCompanyCombo.addActionListener(e -> applyReportFilter());

        JButton export = primaryButton("Export This Report");
        export.addActionListener(e -> {
            String[] names = {"", "executive-summary", "drive-summary", "company-report", "department-report"};
            JTable[] tables = {null, repSummaryTable, repDriveTable, repCompanyTable, repDeptTable};
            int i = reportTabs.getSelectedIndex();
            if (tables[i] == null) {
                JOptionPane.showMessageDialog(this, "Open one of the table reports to export it as CSV.",
                        "Export", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            exportCsv(this, tables[i], "director-" + names[i]);
        });
        return pageShell("Reports & Analytics", "Charts and college-level reports built from live data",
                flow(FlowLayout.RIGHT, 8, refreshButton(), export), card);
    }

    private static Card chartCard(String title, JComponent chart) {
        Card c = new Card();
        c.add(h2(title), BorderLayout.NORTH);
        c.add(chart, BorderLayout.CENTER);
        return c;
    }

    private void applyReportFilter() {
        final String text = reportSearch.getText().trim().toLowerCase(Locale.ROOT);
        final String company = String.valueOf(reportCompanyCombo.getSelectedItem());
        for (JTable t : new JTable[]{repSummaryTable, repDriveTable, repCompanyTable, repDeptTable}) {
            final int companyCol = ((DefaultTableModel) t.getModel()).findColumn("Company");
            sorter(t).setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
                @Override
                public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> e) {
                    if (companyCol >= 0 && !company.equals("All companies")
                            && !company.equalsIgnoreCase(e.getStringValue(companyCol))) return false;
                    if (text.isEmpty()) return true;
                    for (int i = 0; i < e.getValueCount(); i++) {
                        if (e.getStringValue(i).toLowerCase(Locale.ROOT).contains(text)) return true;
                    }
                    return false;
                }
            });
        }
    }

    // ------------------------------------------------------------------
    // DATA LOADING
    // ------------------------------------------------------------------
    private void loadAll() {
        hideBanner(banner);
        api.jobs(list -> {
            jobs = list;
            refreshViews();
            if (reportsOpened) loadEligibleCounts();
        }, err -> showBanner(banner, "Drives: " + err + " (press Refresh to retry)"));
        api.students(list -> {
            students = list;
            refreshViews();
        }, err -> showBanner(banner, "Students: " + err));
        api.stats(s -> {
            stats = s;
            refreshViews();
        }, err -> showBanner(banner, "Statistics: " + err));
    }

    /** Fetches the eligible-student count for each drive (used by the reports). */
    private void loadEligibleCounts() {
        eligibleCounts.clear();
        int limit = Math.min(jobs.size(), 40);
        for (int i = 0; i < limit; i++) {
            final String id = jobs.get(i).id();
            api.eligible(id, list -> {
                eligibleCounts.put(id, list.size());
                refreshReports();
            }, err -> { /* a single failed lookup just leaves "n/a" in the report */ });
        }
    }

    private void refreshViews() {
        refreshDashboard();
        refreshDrives();
        refreshCompanies();
        refreshDepartments();
        refreshReports();
    }

    // ------------------------------------------------------------------
    // DERIVED DATA
    // ------------------------------------------------------------------
    private static double packageOf(Object value) {
        String s = String.valueOf(value);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)").matcher(s);
        return m.find() ? Double.parseDouble(m.group(1)) : 0;
    }

    private static int compareDates(String a, String b) {
        LocalDate x = parseDate(a);
        LocalDate y = parseDate(b);
        if (x == null && y == null) return 0;
        if (x == null) return 1;
        if (y == null) return -1;
        return x.compareTo(y);
    }

    private static String daysLabel(Job j) {
        LocalDate d = j.deadlineDate();
        if (d == null) return "";
        long days = ChronoUnit.DAYS.between(LocalDate.now(), d);
        if (days < 0) return "closed " + (-days) + " day" + (-days == 1 ? "" : "s") + " ago";
        if (days == 0) return "closes today";
        return "in " + days + " day" + (days == 1 ? "" : "s");
    }

    private Map<String, List<Job>> jobsByCompany() {
        Map<String, List<Job>> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Job j : jobs) map.computeIfAbsent(j.company(), k -> new ArrayList<>()).add(j);
        return map;
    }

    private static Job topPackageJob(List<Job> list) {
        Job best = list.get(0);
        for (Job j : list) {
            if (j.packageValue() > best.packageValue()) best = j;
        }
        return best;
    }

    private static String nextDeadline(List<Job> list) {
        LocalDate today = LocalDate.now();
        LocalDate best = null;
        String text = "";
        for (Job j : list) {
            LocalDate d = j.deadlineDate();
            if (d != null && !d.isBefore(today) && (best == null || d.isBefore(best))) {
                best = d;
                text = j.deadline();
            }
        }
        return best == null ? "None upcoming" : text;
    }

    private static String countOrNa(Integer v) {
        return v == null ? "n/a" : String.valueOf(v);
    }

    /** All department names seen in students or in drive branch lists (first spelling wins). */
    private List<String> allDepartments() {
        Map<String, String> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Student s : students) {
            if (!s.dept().isBlank()) map.putIfAbsent(s.dept(), s.dept());
        }
        for (Job j : jobs) {
            for (String b : j.branchList()) map.putIfAbsent(b, b);
        }
        return new ArrayList<>(map.values());
    }

    // ------------------------------------------------------------------
    // REFRESH: DASHBOARD
    // ------------------------------------------------------------------
    private void refreshDashboard() {
        int drives = stats != null ? stats[0] : jobs.size();
        int studentCount = stats != null ? stats[3] : students.size();
        kDrives.setText(String.valueOf(drives));
        kCompanies.setText(String.valueOf(jobsByCompany().size()));
        kStudents.setText(String.valueOf(studentCount));
        kNotices.setText(stats != null ? String.valueOf(stats[2]) : "--");

        int open = 0;
        int soon = 0;
        int closed = 0;
        for (Job j : jobs) {
            switch (driveStatus(j)) {
                case "Open" -> open++;
                case "Closing Soon" -> soon++;
                case "Closed" -> closed++;
                default -> { /* unknown dates are not counted */ }
            }
        }
        LinkedHashMap<String, Integer> timeline = new LinkedHashMap<>();
        timeline.put("Open", open);
        timeline.put("Closing within 7 days", soon);
        timeline.put("Closed", closed);
        timelineDonut.set(timeline, String.valueOf(jobs.size()));

        LinkedHashMap<String, Integer> perDept = new LinkedHashMap<>();
        for (String d : allDepartments()) {
            int n = 0;
            for (Job j : jobs) {
                if (j.opensTo(d)) n++;
            }
            perDept.put(d, n);
        }
        deptDriveBars.set(perDept);

        upcomingList.removeAll();
        List<Job> upcoming = new ArrayList<>();
        String wantStatus = String.valueOf(upcomingStatus.getSelectedItem());
        String wantDept = String.valueOf(upcomingDept.getSelectedItem());
        for (Job j : jobs) {
            String st = driveStatus(j);
            if (st.equals("Closed")) continue;
            if (!wantStatus.equals("All open drives") && !wantStatus.equals(st)) continue;
            if (!wantDept.equals("All departments") && !j.opensTo(wantDept)) continue;
            upcoming.add(j);
        }
        upcoming.sort((a, b) -> compareDates(a.deadline(), b.deadline()));
        if (upcoming.isEmpty()) {
            upcomingList.add(muted("No upcoming deadlines."));
        }
        for (int i = 0; i < Math.min(6, upcoming.size()); i++) {
            upcomingList.add(upcomingRow(upcoming.get(i)));
        }
        upcomingList.revalidate();
        upcomingList.repaint();
    }

    private JPanel upcomingRow(Job j) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER), new EmptyBorder(9, 0, 9, 0)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));

        JLabel title = new JLabel(j.company() + BULLET + j.role());
        title.setFont(font(Font.BOLD, 14));
        title.setForeground(TEXT);
        JLabel sub = new JLabel(j.pkg() + BULLET + "Min CGPA " + fmt1(j.minCgpa()) + BULLET + "Deadline "
                + j.deadline());
        sub.setFont(font(Font.PLAIN, 12));
        sub.setForeground(MUTED);
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(title);
        left.add(Box.createVerticalStrut(3));
        left.add(sub);

        JLabel id = new JLabel(j.id());
        id.setFont(font(Font.BOLD, 12));
        id.setForeground(MUTED);
        JLabel days = new JLabel(daysLabel(j));
        days.setFont(font(Font.BOLD, 11));
        days.setForeground(driveStatus(j).equals("Closing Soon") ? new Color(190, 120, 10) : GREEN);
        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        id.setAlignmentX(Component.RIGHT_ALIGNMENT);
        days.setAlignmentX(Component.RIGHT_ALIGNMENT);
        right.add(id);
        right.add(Box.createVerticalStrut(3));
        right.add(days);

        row.add(left, BorderLayout.CENTER);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    // ------------------------------------------------------------------
    // REFRESH: DRIVES
    // ------------------------------------------------------------------
    private void refreshDrives() {
        driveModel.setRowCount(0);
        List<String> companies = new ArrayList<>(jobsByCompany().keySet());
        for (Job j : jobs) {
            driveModel.addRow(new Object[]{j.id(), j.company(), j.role(), j.pkg(), j.minCgpa(), j.branches(),
                    j.maxBacklogs(), j.year(), j.deadline(), driveStatus(j)});
        }
        resetCombo(companyCombo, "All companies", companies);
        resetCombo(branchCombo, "All departments", allDepartments());
        resetCombo(upcomingDept, "All departments", allDepartments());
        resetCombo(reportCompanyCombo, "All companies", companies);
        applyReportFilter();
        applyDriveFilter();
        applyDriveSort();
    }

    private void applyDriveFilter() {
        final String text = driveSearch.getText().trim().toLowerCase(Locale.ROOT);
        final String company = String.valueOf(companyCombo.getSelectedItem());
        final String branch = String.valueOf(branchCombo.getSelectedItem());
        final String status = String.valueOf(statusCombo.getSelectedItem());

        sorter(driveTable).setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> e) {
                if (!company.equals("All companies") && !company.equalsIgnoreCase(e.getStringValue(1))) {
                    return false;
                }
                if (!status.equals("All statuses") && !status.equals(e.getStringValue(9))) return false;
                if (!branch.equals("All departments")) {
                    boolean found = false;
                    for (String b : splitList(e.getStringValue(5))) {
                        if (b.equalsIgnoreCase(branch)) found = true;
                    }
                    if (!found) return false;
                }
                if (text.isEmpty()) return true;
                for (int i = 0; i < e.getValueCount(); i++) {
                    if (e.getStringValue(i).toLowerCase(Locale.ROOT).contains(text)) return true;
                }
                return false;
            }
        });
        driveCount.setText("Showing " + driveTable.getRowCount() + " of " + driveModel.getRowCount() + " drives");
    }

    private void applyDriveSort() {
        int col;
        SortOrder order;
        switch (driveSortCombo.getSelectedIndex()) {
            case 1 -> {
                col = 8;
                order = SortOrder.ASCENDING;
            }
            case 2 -> {
                col = 3;
                order = SortOrder.DESCENDING;
            }
            case 3 -> {
                col = 4;
                order = SortOrder.ASCENDING;
            }
            case 4 -> {
                col = 1;
                order = SortOrder.ASCENDING;
            }
            default -> {
                sorter(driveTable).setSortKeys(null);
                return;
            }
        }
        sorter(driveTable).setSortKeys(List.of(new RowSorter.SortKey(col, order)));
    }

    private Job selectedDrive() {
        int r = driveTable.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Select a placement drive first.", "No Drive Selected",
                    JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        String id = String.valueOf(driveModel.getValueAt(driveTable.convertRowIndexToModel(r), 0));
        for (Job j : jobs) {
            if (j.id().equals(id)) return j;
        }
        return null;
    }

    private void showDriveDetails() {
        Job j = selectedDrive();
        if (j == null) return;
        LinkedHashMap<String, String> f = new LinkedHashMap<>();
        f.put("Job ID", j.id());
        f.put("Company", j.company());
        f.put("Role", j.role());
        f.put("Package", j.pkg());
        f.put("Minimum CGPA", fmt1(j.minCgpa()));
        f.put("Maximum backlogs", String.valueOf(j.maxBacklogs()));
        f.put("Passing year", String.valueOf(j.year()));
        f.put("Open to branches", j.branches());
        f.put("Required skills", j.skills().isBlank() ? "None specified" : j.skills());
        f.put("Deadline", j.deadline() + " (" + daysLabel(j) + ")");
        f.put("Status", driveStatus(j));
        Integer count = eligibleCounts.get(j.id());
        if (count != null) f.put("Eligible students", String.valueOf(count));
        details(this, "Drive Details", f);
    }

    private void showEligibleDialog() {
        Job j = selectedDrive();
        if (j == null) return;

        JDialog dialog = new JDialog(this, "Eligible Students", true);
        dialog.setSize(1050, 540);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JLabel status = new JLabel("Loading eligible students...");
        status.setFont(font(Font.BOLD, 13));
        status.setBorder(new EmptyBorder(12, 14, 0, 14));
        dialog.add(status, BorderLayout.NORTH);

        DefaultTableModel m = model(
                new String[]{"PRN", "Name", "Email", "Department", "CGPA", "Backlogs", "Passing Year", "Skills"},
                String.class, String.class, String.class, String.class, Double.class, Integer.class,
                Integer.class, String.class);
        JTable t = table(m);
        widths(t, 90, 150, 200, 110, 60, 70, 90, 260);
        JPanel center = new JPanel(new BorderLayout());
        center.setBorder(new EmptyBorder(0, 14, 0, 14));
        center.add(tableScroll(t, 0), BorderLayout.CENTER);
        dialog.add(center, BorderLayout.CENTER);

        JButton export = ghostButton("Export CSV");
        export.addActionListener(e -> exportCsv(dialog, t, "eligible-" + j.id()));
        JButton close = primaryButton("Close");
        close.addActionListener(e -> dialog.dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        bottom.add(export);
        bottom.add(close);
        dialog.add(bottom, BorderLayout.SOUTH);

        api.eligible(j.id(), list -> {
            for (Student s : list) {
                m.addRow(new Object[]{s.prn(), s.name(), s.email(), s.dept(), s.cgpa(), s.backlogs(), s.year(),
                        s.skills()});
            }
            eligibleCounts.put(j.id(), list.size());
            String of = students.isEmpty() ? "" : " of " + students.size() + " students ("
                    + pct(list.size(), students.size()) + ")";
            status.setText(list.size() + " eligible" + of + " for " + j.company() + " - " + j.role());
        }, err -> status.setText("Could not load eligible students: " + err));
        dialog.setVisible(true);
    }

    // ------------------------------------------------------------------
    // REFRESH: COMPANIES
    // ------------------------------------------------------------------
    private void refreshCompanies() {
        companyModel.setRowCount(0);
        for (Map.Entry<String, List<Job>> e : jobsByCompany().entrySet()) {
            List<Job> list = e.getValue();
            Set<String> roles = new LinkedHashSet<>();
            Set<String> branches = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            double lo = Double.MAX_VALUE;
            double hi = 0;
            for (Job j : list) {
                roles.add(j.role());
                branches.addAll(j.branchList());
                lo = Math.min(lo, j.minCgpa());
                hi = Math.max(hi, j.minCgpa());
            }
            String range = lo == hi ? fmt1(lo) : fmt1(lo) + " - " + fmt1(hi);
            companyModel.addRow(new Object[]{e.getKey(), list.size(), String.join(", ", roles),
                    topPackageJob(list).pkg(), range, String.join(", ", branches), nextDeadline(list)});
        }
        sorter(companyTable).setRowFilter(textFilter(companySearch.getText()));
        companyCount.setText("  " + companyTable.getRowCount() + " of " + companyModel.getRowCount()
                + " companies");
    }

    private void showCompanyDetails() {
        int r = companyTable.getSelectedRow();
        if (r < 0) return;
        String company = String.valueOf(companyModel.getValueAt(companyTable.convertRowIndexToModel(r), 0));
        List<Job> list = jobsByCompany().get(company);
        if (list == null) return;
        LinkedHashMap<String, String> f = new LinkedHashMap<>();
        f.put("Company", company);
        f.put("Drives", String.valueOf(list.size()));
        for (Job j : list) {
            f.put(j.id(), j.role() + BULLET.trim() + " " + j.pkg() + BULLET.trim() + " min CGPA "
                    + fmt1(j.minCgpa()) + BULLET.trim() + " deadline " + j.deadline());
        }
        details(this, "Company Details", f);
    }

    // ------------------------------------------------------------------
    // REFRESH: DEPARTMENTS
    // ------------------------------------------------------------------
    private void refreshDepartments() {
        deptModel.setRowCount(0);
        LinkedHashMap<String, Integer> coverage = new LinkedHashMap<>();
        String fewest = null;
        int fewestCount = Integer.MAX_VALUE;
        for (String d : allDepartments()) {
            int count = 0;
            double sum = 0;
            for (Student s : students) {
                if (s.dept().equalsIgnoreCase(d)) {
                    count++;
                    sum += s.cgpa();
                }
            }
            int open = 0;
            double best = 0;
            String bestText = "-";
            for (Job j : jobs) {
                if (j.opensTo(d)) {
                    open++;
                    if (j.packageValue() >= best) {
                        best = j.packageValue();
                        bestText = j.pkg();
                    }
                }
            }
            deptModel.addRow(new Object[]{d, count, count == 0 ? 0.0 : sum / count, open,
                    pct(open, jobs.size()), bestText});
            coverage.put(d, open);
            if (open < fewestCount) {
                fewestCount = open;
                fewest = d;
            }
        }
        coverageBars.set(coverage);
        if (fewest == null || jobs.isEmpty()) {
            deptInsight.setText(" ");
        } else {
            deptInsight.setText("Lowest coverage: " + fewest + " is eligible for " + fewestCount + " of "
                    + jobs.size() + " drives.");
        }
    }

    // ------------------------------------------------------------------
    // REFRESH: REPORTS
    // ------------------------------------------------------------------
    private void refreshReports() {
        // Drive summary
        repDriveModel.setRowCount(0);
        for (Job j : jobs) {
            repDriveModel.addRow(new Object[]{j.id(), j.company(), j.role(), j.pkg(), j.deadline(),
                    driveStatus(j), countOrNa(eligibleCounts.get(j.id()))});
        }

        // Company report
        repCompanyModel.setRowCount(0);
        for (Map.Entry<String, List<Job>> e : jobsByCompany().entrySet()) {
            int seats = 0;
            boolean known = true;
            for (Job j : e.getValue()) {
                Integer c = eligibleCounts.get(j.id());
                if (c == null) known = false;
                else seats += c;
            }
            repCompanyModel.addRow(new Object[]{e.getKey(), e.getValue().size(), topPackageJob(e.getValue()).pkg(),
                    known ? String.valueOf(seats) : "n/a", nextDeadline(e.getValue())});
        }

        // Department report
        repDeptModel.setRowCount(0);
        for (int i = 0; i < deptModel.getRowCount(); i++) {
            repDeptModel.addRow(new Object[]{deptModel.getValueAt(i, 0), deptModel.getValueAt(i, 1),
                    deptModel.getValueAt(i, 2), deptModel.getValueAt(i, 3), deptModel.getValueAt(i, 4)});
        }

        LinkedHashMap<String, Double> pkgData = new LinkedHashMap<>();
        for (Map.Entry<String, List<Job>> e : jobsByCompany().entrySet()) {
            pkgData.put(e.getKey(), topPackageJob(e.getValue()).packageValue());
        }
        packageBars.set(pkgData);
        LinkedHashMap<String, Integer> eligData = new LinkedHashMap<>();
        for (Job j : jobs) {
            Integer c = eligibleCounts.get(j.id());
            if (c != null) eligData.put(j.company() + " (" + j.id() + ")", c);
        }
        eligibleBars.set(eligData);

        // Executive summary
        int open = 0;
        int soon = 0;
        int closed = 0;
        double cgpaSum = 0;
        Job top = null;
        for (Job j : jobs) {
            switch (driveStatus(j)) {
                case "Open" -> open++;
                case "Closing Soon" -> soon++;
                case "Closed" -> closed++;
                default -> { /* ignore */ }
            }
            cgpaSum += j.minCgpa();
            if (top == null || j.packageValue() > top.packageValue()) top = j;
        }
        repSummaryModel.setRowCount(0);
        addSummary("Registered students", String.valueOf(stats != null ? stats[3] : students.size()),
                "Total students in the placement pool");
        addSummary("Partner companies", String.valueOf(jobsByCompany().size()), "Distinct companies with a drive");
        addSummary("Total drives", String.valueOf(jobs.size()), "Open: " + open + ", closing soon: " + soon
                + ", closed: " + closed);
        addSummary("Drives closing within 7 days", String.valueOf(soon), "Needs attention from the placement team");
        addSummary("Highest package on offer", top == null ? "-" : top.pkg(),
                top == null ? "" : top.company() + " - " + top.role());
        addSummary("Average minimum CGPA across drives", jobs.isEmpty() ? "-" : fmt1(cgpaSum / jobs.size()),
                "Mean of each drive's CGPA cut-off");
        addSummary("Students pending notification", stats != null ? String.valueOf(stats[1]) : "-",
                "Eligible but not yet notified");
        addSummary("Notifications sent", stats != null ? String.valueOf(stats[2]) : "-",
                "Messages recorded by the system");
        if (deptModel.getRowCount() > 0 && !jobs.isEmpty()) {
            int min = Integer.MAX_VALUE;
            int max = -1;
            String minName = "";
            String maxName = "";
            for (int i = 0; i < deptModel.getRowCount(); i++) {
                int v = (Integer) deptModel.getValueAt(i, 3);
                if (v < min) {
                    min = v;
                    minName = String.valueOf(deptModel.getValueAt(i, 0));
                }
                if (v > max) {
                    max = v;
                    maxName = String.valueOf(deptModel.getValueAt(i, 0));
                }
            }
            addSummary("Best-covered department", maxName, "Eligible for " + max + " of " + jobs.size() + " drives");
            addSummary("Least-covered department", minName, "Eligible for " + min + " of " + jobs.size() + " drives");
        }
    }

    private void addSummary(String metric, String value, String note) {
        repSummaryModel.addRow(new Object[]{metric, value, note});
    }

    // ------------------------------------------------------------------
    // SESSION
    // ------------------------------------------------------------------
    private void logout() {
        int ok = JOptionPane.showConfirmDialog(this, "Are you sure you want to log out?", "Logout",
                JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        api.logout(() -> {
            dispose();
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
        });
    }

    public String getDisplayName() {
        return displayName;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DirectorDashboard().setVisible(true));
    }

static final class Kit {

    private Kit() { }

    // ------------------------------------------------------------------
    // THEME
    // ------------------------------------------------------------------
    static final Color SIDEBAR = new Color(26, 92, 52);
    static final Color SIDEBAR_HOVER = new Color(37, 112, 67);
    static final Color SIDEBAR_ACTIVE = new Color(15, 68, 37);
    static final Color BG = new Color(240, 242, 240);
    static final Color TEXT = new Color(24, 32, 28);
    static final Color MUTED = new Color(110, 120, 114);
    static final Color BORDER = new Color(222, 226, 223);
    static final Color GREEN = new Color(46, 125, 80);
    static final Color RED = new Color(217, 95, 76);
    static final Color BLUE = new Color(68, 104, 150);
    static final Color AMBER = new Color(224, 160, 48);
    static final Color LIGHT_GREEN = new Color(232, 245, 236);
    static final Color ACCENT = new Color(46, 125, 80);
    static final Color ACCENT_LIGHT = new Color(232, 245, 236);
    static final Color INDICATOR = new Color(15, 68, 37);
    static final Color HEADER_BG = new Color(246, 248, 246);

    static final String BULLET = " \u2022 ";

    static Font font(int style, int size) {
        return new Font("SansSerif", style, size);
    }

    // ------------------------------------------------------------------
    // DATA RECORDS (parsed from the socket protocol lines)
    // ------------------------------------------------------------------
    record Student(String prn, String name, String email, String dept, double cgpa,
                   int year, int backlogs, int semester, String phone, String skills) {
        List<String> skillList() {
            return splitList(skills);
        }
    }

    record Job(String id, String company, String role, String pkg, double minCgpa,
               String branches, int maxBacklogs, int year, String skills, String deadline) {
        List<String> branchList() {
            return splitList(branches);
        }

        List<String> skillList() {
            return splitList(skills);
        }

        double packageValue() {
            Matcher m = Pattern.compile("(\\d+(?:\\.\\d+)?)").matcher(pkg == null ? "" : pkg);
            return m.find() ? Double.parseDouble(m.group(1)) : 0;
        }

        LocalDate deadlineDate() {
            return parseDate(deadline);
        }

        boolean opensTo(String dept) {
            for (String b : branchList()) {
                if (b.equalsIgnoreCase(dept == null ? "" : dept.trim())) return true;
            }
            return false;
        }
    }

    record Notice(long id, String jobId, String subject, String message,
                  String group, int count, String createdAt) { }

    static List<String> splitList(String value) {
        List<String> out = new ArrayList<>();
        if (value == null) return out;
        for (String s : value.split("[,;/]")) {
            String t = s.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        String v = value.trim();
        if (v.length() > 10) v = v.substring(0, 10);
        for (String pattern : new String[]{"dd-MM-yyyy", "yyyy-MM-dd", "dd/MM/yyyy"}) {
            try {
                return LocalDate.parse(v, DateTimeFormatter.ofPattern(pattern));
            } catch (Exception ignored) {
                // try next pattern
            }
        }
        return null;
    }

    /** Open / Closing Soon (7 days or less) / Closed / Unknown. */
    static String driveStatus(Job job) {
        LocalDate d = job.deadlineDate();
        if (d == null) return "Unknown";
        LocalDate today = LocalDate.now();
        if (d.isBefore(today)) return "Closed";
        if (!d.isAfter(today.plusDays(7))) return "Closing Soon";
        return "Open";
    }

    static Student parseStudent(String line) {
        if (line == null) return null;
        String[] p = line.split("\\|", -1);
        if (p.length < 10) return null;
        return new Student(p[0], p[1], p[2], p[3].trim(), toDouble(p[4]), toInt(p[5]),
                toInt(p[6]), toInt(p[7]), p[8], p[9]);
    }

    static Job parseJob(String line) {
        if (line == null) return null;
        String[] p = line.split("\\|", -1);
        if (p.length < 10) return null;
        return new Job(p[0], p[1], p[2], p[3], toDouble(p[4]), p[5], toInt(p[6]), toInt(p[7]), p[8], p[9]);
    }

    static Notice parseNotice(String line) {
        if (line == null) return null;
        String[] p = line.split("\\|", -1);
        if (p.length < 7) return null;
        long id;
        try {
            id = Long.parseLong(p[0].trim());
        } catch (Exception e) {
            id = 0;
        }
        return new Notice(id, p[1], p[2], p[3], p[4], toInt(p[5]), p[6]);
    }

    static int toInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    static double toDouble(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    static String fmt1(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    static String pct(int part, int whole) {
        if (whole <= 0) return "0%";
        return Math.round(100.0 * part / whole) + "%";
    }

    // ------------------------------------------------------------------
    // SOCKET API WRAPPER
    // ------------------------------------------------------------------
    static final class Api {
        private final SocketClient client = new SocketClient();
        private final String token;
        /** One request at a time: the server times out when many connections arrive together. */
        private final java.util.concurrent.ExecutorService pool =
                java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
                    Thread t = new Thread(r, "dashboard-api");
                    t.setDaemon(true);
                    return t;
                });

        Api(String token) {
            this.token = token;
        }

        boolean hasSession() {
            return token != null && !token.isBlank();
        }

        void students(Consumer<List<Student>> ok, Consumer<String> err) {
            list(() -> client.sendListRequest("GET_STUDENTS", token), lines -> {
                List<Student> out = new ArrayList<>();
                for (String l : lines) {
                    Student s = parseStudent(l);
                    if (s != null) out.add(s);
                }
                ok.accept(out);
            }, err);
        }

        /** GET_JOBS is tried several ways because some server builds are slow or expect a token. */
        void jobs(Consumer<List<Job>> ok, Consumer<String> err) {
            submit(false, () -> {
                SocketClient.ListResponse last = null;
                Exception failure = null;
                for (int attempt = 0; attempt < 4; attempt++) {
                    try {
                        SocketClient.ListResponse r = attempt % 2 == 1
                                ? client.sendListRequest("GET_JOBS", token)
                                : client.sendListRequest("GET_JOBS");
                        if (r.success) return r;
                        last = r;
                        failure = null;
                    } catch (Exception ex) {
                        failure = ex;
                    }
                }
                if (last != null) return last;
                throw failure == null ? new IllegalStateException("No response") : failure;
            }, r -> {
                if (!r.success) {
                    err.accept(r.errorMessage);
                    return;
                }
                List<Job> out = new ArrayList<>();
                for (String l : r.lines == null ? new ArrayList<String>() : r.lines) {
                    Job j = parseJob(l);
                    if (j != null) out.add(j);
                }
                ok.accept(out);
            }, t -> err.accept("Request failed: " + rootMessage(t)));
        }

        void notices(Consumer<List<Notice>> ok, Consumer<String> err) {
            list(() -> client.sendListRequest("GET_NOTIFICATIONS", token), lines -> {
                List<Notice> out = new ArrayList<>();
                for (String l : lines) {
                    Notice n = parseNotice(l);
                    if (n != null) out.add(n);
                }
                ok.accept(out);
            }, err);
        }

        void eligible(String jobId, Consumer<List<Student>> ok, Consumer<String> err) {
            list(() -> client.sendListRequest("GET_ELIGIBLE_STUDENTS", token, jobId), lines -> {
                List<Student> out = new ArrayList<>();
                for (String l : lines) {
                    Student s = parseStudent(l);
                    if (s != null) out.add(s);
                }
                ok.accept(out);
            }, err);
        }

        /** GET_DASHBOARD_STATS returns jobs|pending|notifications|students. */
        void stats(Consumer<int[]> ok, Consumer<String> err) {
            submit(true, () -> client.sendRequest("GET_DASHBOARD_STATS", token), r -> {
                if (!r.success) {
                    err.accept(r.payload);
                    return;
                }
                String[] p = r.parts();
                if (p.length < 4) {
                    err.accept("Invalid dashboard statistics received.");
                    return;
                }
                ok.accept(new int[]{toInt(p[0]), toInt(p[1]), toInt(p[2]), toInt(p[3])});
            }, t -> err.accept("Request failed: " + rootMessage(t)));
        }

        /**
         * Sends a general announcement. Protocol assumed: token, subject, message.
         * If your ClientHandler expects different arguments, only this method
         * needs to change.
         */
        void sendGeneral(String subject, String message, Runnable ok, Consumer<String> err) {
            String s = clean(subject);
            String m = clean(message);
            submit(false, () -> client.sendRequest("SEND_GENERAL_NOTIFICATION", token, s, m), r -> {
                if (r.success) ok.run();
                else err.accept(r.payload);
            }, t -> err.accept("Request failed: " + rootMessage(t)));
        }

        void logout(Runnable after) {
            if (!hasSession()) {
                after.run();
                return;
            }
            submit(false, () -> client.sendRequest("LOGOUT", token), r -> after.run(), t -> after.run());
        }

        private void list(Callable<SocketClient.ListResponse> call, Consumer<List<String>> ok,
                          Consumer<String> err) {
            submit(true, call, r -> {
                if (!r.success) {
                    err.accept(r.errorMessage);
                    return;
                }
                ok.accept(r.lines == null ? new ArrayList<>() : r.lines);
            }, t -> err.accept("Request failed: " + rootMessage(t)));
        }

        private <T> void submit(boolean retry, Callable<T> work, Consumer<T> ok, Consumer<Throwable> fail) {
            pool.submit(() -> {
                T result = null;
                Throwable error = null;
                for (int attempt = 0; attempt < (retry ? 2 : 1); attempt++) {
                    try {
                        result = work.call();
                        error = null;
                        break;
                    } catch (Throwable t) {
                        error = t;
                        String m = rootMessage(t).toLowerCase(Locale.ROOT);
                        if (!m.contains("timed out")) break;
                    }
                }
                final T r = result;
                final Throwable e = error;
                SwingUtilities.invokeLater(() -> {
                    if (e != null) fail.accept(e);
                    else ok.accept(r);
                });
            });
        }

        private static String clean(String v) {
            return v == null ? "" : v.replace('|', '/').replaceAll("[\\r\\n]+", " ").trim();
        }
    }

    static <T> void async(Callable<T> work, Consumer<T> ok, Consumer<Throwable> fail) {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return work.call();
            }

            @Override
            protected void done() {
                try {
                    ok.accept(get());
                } catch (Exception ex) {
                    fail.accept(ex.getCause() != null ? ex.getCause() : ex);
                }
            }
        }.execute();
    }

    static String rootMessage(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null) c = c.getCause();
        return c.getMessage() == null ? c.getClass().getSimpleName() : c.getMessage();
    }

    // ------------------------------------------------------------------
    // LAYOUT HELPERS
    // ------------------------------------------------------------------

    /** Panel whose maximum height equals its preferred height (for BoxLayout stacks). */
    static class Fill extends JPanel {
        Fill(LayoutManager lm) {
            super(lm);
            setOpaque(false);
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    }

    static final class Card extends Fill {
        Card() {
            super(new BorderLayout(0, 10));
            setOpaque(true);
            setBackground(Color.WHITE);
            setBorder(javax.swing.BorderFactory.createCompoundBorder(
                    new LineBorder(BORDER, 1), new EmptyBorder(14, 16, 14, 16)));
        }
    }

    static final class VStack extends JPanel implements Scrollable {
        VStack() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(BG);
        }

        VStack add(JComponent c, int gapAfter) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            super.add(c);
            if (gapAfter > 0) super.add(Box.createVerticalStrut(gapAfter));
            return this;
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) {
            return 120;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    static JScrollPane scrollPage(JComponent view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    static JLabel h1(String text) {
        JLabel l = new JLabel(text);
        l.setFont(font(Font.BOLD, 26));
        l.setForeground(TEXT);
        return l;
    }

    static JLabel h2(String text) {
        JLabel l = new JLabel(text);
        l.setFont(font(Font.BOLD, 16));
        l.setForeground(TEXT);
        return l;
    }

    static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setFont(font(Font.PLAIN, 12));
        l.setForeground(MUTED);
        return l;
    }

    /** Page wrapper: title + subtitle + optional right-hand control, then body. */
    static JPanel pageShell(String title, String subtitle, JComponent east, JComponent body) {
        JPanel page = new JPanel(new BorderLayout(0, 14));
        page.setBackground(BG);
        page.setBorder(new EmptyBorder(22, 28, 20, 28));

        JPanel head = new JPanel(new BorderLayout(10, 0));
        head.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel t = h1(title);
        JLabel s = muted(subtitle);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        s.setAlignmentX(Component.LEFT_ALIGNMENT);
        titles.add(t);
        titles.add(Box.createVerticalStrut(4));
        titles.add(s);
        head.add(titles, BorderLayout.CENTER);
        if (east != null) head.add(east, BorderLayout.EAST);

        page.add(head, BorderLayout.NORTH);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    static JPanel flow(int align, int gap, JComponent... items) {
        JPanel p = new JPanel(new FlowLayout(align, gap, 0));
        p.setOpaque(false);
        for (JComponent c : items) p.add(c);
        return p;
    }

    // ------------------------------------------------------------------
    // KPI CARD
    // ------------------------------------------------------------------
    static Card kpiCard(String title, JLabel value, String sub, Color accent) {
        Card card = new Card();
        card.setLayout(new BorderLayout(14, 0));
        card.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1), new EmptyBorder(16, 16, 16, 14)));
        value.setFont(font(Font.BOLD, 32));
        value.setForeground(accent);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(title);
        t.setFont(font(Font.BOLD, 13));
        t.setForeground(TEXT);
        JLabel s = new JLabel(sub);
        s.setFont(font(Font.PLAIN, 11));
        s.setForeground(MUTED);
        text.add(Box.createVerticalGlue());
        text.add(t);
        text.add(Box.createVerticalStrut(2));
        text.add(s);
        text.add(Box.createVerticalGlue());

        card.add(value, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    /** Alternative KPI card: coloured top edge, title above a big number. */
    static Card kpiCardTop(String title, JLabel value, String sub, Color accent) {
        Card card = new Card();
        card.setLayout(new BorderLayout(0, 4));
        card.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createMatteBorder(4, 0, 0, 0, accent),
                javax.swing.BorderFactory.createCompoundBorder(
                        new LineBorder(BORDER, 1), new EmptyBorder(12, 16, 12, 14))));
        JLabel t = new JLabel(title.toUpperCase(Locale.ROOT));
        t.setFont(font(Font.BOLD, 11));
        t.setForeground(MUTED);
        value.setFont(font(Font.BOLD, 30));
        value.setForeground(TEXT);
        JLabel s = new JLabel(sub);
        s.setFont(font(Font.PLAIN, 11));
        s.setForeground(MUTED);
        card.add(t, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        card.add(s, BorderLayout.SOUTH);
        return card;
    }

    static JLabel valueLabel() {
        JLabel l = new JLabel("--");
        l.setFont(font(Font.BOLD, 32));
        return l;
    }

    static Fill grid(int cols, int gap, JComponent... items) {
        Fill f = new Fill(new GridLayout(1, cols, gap, 0));
        for (JComponent c : items) f.add(c);
        return f;
    }

    // ------------------------------------------------------------------
    // CHARTS
    // ------------------------------------------------------------------
    static final class Donut extends JPanel {
        private final LinkedHashMap<String, Integer> data = new LinkedHashMap<>();
        private final Color[] colors;
        private String center = "";

        Donut(Color... colors) {
            this.colors = colors;
            setOpaque(false);
            setPreferredSize(new Dimension(300, 190));
        }

        void set(Map<String, Integer> values, String centerText) {
            data.clear();
            data.putAll(values);
            center = centerText == null ? "" : centerText;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int total = 0;
            for (int v : data.values()) total += Math.max(0, v);
            int d = Math.min(getHeight() - 20, 150);
            int x = 14;
            int y = (getHeight() - d) / 2;
            float sw = d * 0.22f;

            if (total == 0) {
                g2.setColor(new Color(225, 229, 226));
                g2.setStroke(new BasicStroke(sw));
                g2.draw(new Arc2D.Double(x + sw / 2, y + sw / 2, d - sw, d - sw, 0, 360, Arc2D.OPEN));
                g2.setColor(MUTED);
                g2.setFont(font(Font.PLAIN, 12));
                g2.drawString("No data", x + d + 22, y + d / 2);
                g2.dispose();
                return;
            }

            double start = 90;
            int i = 0;
            for (int v : data.values()) {
                double extent = -360.0 * Math.max(0, v) / total;
                g2.setColor(colors[i % colors.length]);
                g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
                if (v > 0) {
                    g2.draw(new Arc2D.Double(x + sw / 2, y + sw / 2, d - sw, d - sw, start, extent, Arc2D.OPEN));
                }
                start += extent;
                i++;
            }

            g2.setColor(TEXT);
            g2.setFont(font(Font.BOLD, Math.max(13, d / 7)));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(center, x + (d - fm.stringWidth(center)) / 2, y + d / 2 + fm.getAscent() / 2 - 2);

            int ly = y + Math.max(10, d / 2 - data.size() * 12);
            int lx = x + d + 22;
            i = 0;
            g2.setFont(font(Font.PLAIN, 12));
            for (Map.Entry<String, Integer> e : data.entrySet()) {
                g2.setColor(colors[i % colors.length]);
                g2.fillRect(lx, ly, 12, 12);
                g2.setColor(TEXT);
                g2.drawString(e.getKey() + " (" + e.getValue() + ")", lx + 20, ly + 11);
                ly += 25;
                i++;
            }
            g2.dispose();
        }
    }

    static final class Bars extends JPanel {
        private final LinkedHashMap<String, Double> data = new LinkedHashMap<>();
        private final Color color;
        private final boolean decimals;

        Bars(Color color, boolean decimals) {
            this.color = color;
            this.decimals = decimals;
            setOpaque(false);
            setPreferredSize(new Dimension(300, 120));
        }

        void set(Map<String, ? extends Number> values) {
            data.clear();
            for (Map.Entry<String, ? extends Number> e : values.entrySet()) {
                data.put(e.getKey(), e.getValue().doubleValue());
            }
            setPreferredSize(new Dimension(300, Math.max(110, data.size() * 36 + 8)));
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (data.isEmpty()) {
                g2.setColor(MUTED);
                g2.setFont(font(Font.PLAIN, 12));
                g2.drawString("No data", 10, 30);
                g2.dispose();
                return;
            }
            g2.setFont(font(Font.PLAIN, 12));
            FontMetrics fm = g2.getFontMetrics();
            int labelW = 60;
            for (String k : data.keySet()) labelW = Math.max(labelW, fm.stringWidth(k) + 14);
            labelW = Math.min(labelW, 170);
            double max = 0;
            for (double v : data.values()) max = Math.max(max, v);
            if (max <= 0) max = 1;
            int barArea = Math.max(40, getWidth() - labelW - 56);
            int y = 4;
            for (Map.Entry<String, Double> e : data.entrySet()) {
                String label = e.getKey();
                while (fm.stringWidth(label) > labelW - 10 && label.length() > 3) {
                    label = label.substring(0, label.length() - 2);
                }
                g2.setColor(MUTED);
                g2.drawString(label, 0, y + 19);
                int w = (int) Math.round(barArea * e.getValue() / max);
                g2.setColor(color);
                g2.fill(new RoundRectangle2D.Double(labelW, y + 3, Math.max(e.getValue() > 0 ? 4 : 0, w), 22, 6, 6));
                g2.setColor(TEXT);
                String v = decimals ? fmt1(e.getValue()) : String.valueOf(Math.round(e.getValue()));
                g2.drawString(v, labelW + Math.max(4, w) + 8, y + 19);
                y += 36;
            }
            g2.dispose();
        }
    }

    // ------------------------------------------------------------------
    // TABLES
    // ------------------------------------------------------------------
    static DefaultTableModel model(String[] columns, Class<?>... types) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int c) {
                return c < types.length && types[c] != null ? types[c] : String.class;
            }
        };
    }

    static JTable table(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setFont(font(Font.PLAIN, 12));
        t.setForeground(TEXT);
        t.setRowHeight(30);
        t.setGridColor(BORDER);
        t.setShowVerticalLines(false);
        t.setShowHorizontalLines(true);
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setSelectionBackground(ACCENT_LIGHT);
        t.setSelectionForeground(TEXT);
        t.setFillsViewportHeight(true);
        t.setAutoCreateRowSorter(true);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setPreferredSize(new Dimension(100, 34));
        h.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tb, Object v, boolean s, boolean f, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tb, v, false, false, r, c);
                l.setFont(font(Font.BOLD, 12));
                l.setForeground(TEXT);
                l.setBackground(HEADER_BG);
                l.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                        javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                        new EmptyBorder(0, 8, 0, 8)));
                l.setHorizontalAlignment(tb.getColumnClass(c) == String.class ? SwingConstants.LEFT : SwingConstants.CENTER);
                return l;
            }
        });

        DefaultTableCellRenderer left = new DefaultTableCellRenderer();
        left.setBorder(new EmptyBorder(0, 8, 0, 8));
        t.setDefaultRenderer(String.class, left);
        t.setDefaultRenderer(Object.class, left);
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        t.setDefaultRenderer(Integer.class, center);
        t.setDefaultRenderer(Long.class, center);
        t.setDefaultRenderer(Double.class, new DefaultTableCellRenderer() {
            {
                setHorizontalAlignment(SwingConstants.CENTER);
            }

            @Override
            protected void setValue(Object value) {
                setText(value instanceof Double d ? fmt1(d) : String.valueOf(value));
            }
        });
        return t;
    }

    /** Colors a status column: green / amber / red depending on text. */
    static void statusColumn(JTable t, int col) {
        t.getColumnModel().getColumn(col).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tb, Object v, boolean s, boolean f, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tb, v, s, f, r, c);
                String text = String.valueOf(v);
                l.setBorder(new EmptyBorder(0, 8, 0, 8));
                l.setFont(font(Font.BOLD, 12));
                switch (text) {
                    case "Good Standing", "Open", "Eligible" -> l.setForeground(GREEN);
                    case "Needs Attention", "Closing Soon" -> l.setForeground(new Color(190, 120, 10));
                    case "At Risk", "Closed", "Not Eligible" -> l.setForeground(RED);
                    default -> l.setForeground(TEXT);
                }
                return l;
            }
        });
    }

    static void widths(JTable t, int... w) {
        for (int i = 0; i < w.length && i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
        }
    }

    static JScrollPane tableScroll(JTable t, int height) {
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(new LineBorder(BORDER, 1));
        sp.getViewport().setBackground(Color.WHITE);
        if (height > 0) sp.setPreferredSize(new Dimension(400, height));
        return sp;
    }

    @SuppressWarnings("unchecked")
    static TableRowSorter<DefaultTableModel> sorter(JTable t) {
        return (TableRowSorter<DefaultTableModel>) t.getRowSorter();
    }

    static RowFilter<DefaultTableModel, Integer> textFilter(String text) {
        String q = text == null ? "" : text.trim();
        if (q.isEmpty()) return null;
        return RowFilter.regexFilter("(?i)" + Pattern.quote(q));
    }

    static void onChange(JTextField field, Runnable r) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                r.run();
            }

            public void removeUpdate(DocumentEvent e) {
                r.run();
            }

            public void changedUpdate(DocumentEvent e) {
                r.run();
            }
        });
    }

    // ------------------------------------------------------------------
    // INPUTS / BUTTONS
    // ------------------------------------------------------------------
    static JTextField searchField(String placeholder, int width) {
        JTextField f = new JTextField();
        f.setPreferredSize(new Dimension(width, 34));
        f.setFont(font(Font.PLAIN, 12));
        f.putClientProperty("JTextField.placeholderText", placeholder);
        f.setToolTipText(placeholder);
        f.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1), new EmptyBorder(0, 10, 0, 10)));
        return f;
    }

    static JComboBox<String> combo(int width, String... items) {
        JComboBox<String> c = new JComboBox<>(items);
        c.setPreferredSize(new Dimension(width, 34));
        c.setFont(font(Font.PLAIN, 12));
        c.setBackground(Color.WHITE);
        return c;
    }

    static void resetCombo(JComboBox<String> combo, String first, List<String> values) {
        Object selected = combo.getSelectedItem();
        combo.removeAllItems();
        combo.addItem(first);
        for (String v : values) combo.addItem(v);
        if (selected != null) combo.setSelectedItem(selected);
        if (combo.getSelectedItem() == null) combo.setSelectedIndex(0);
    }

    static JLabel filterLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(font(Font.BOLD, 11));
        l.setForeground(MUTED);
        return l;
    }

    static JButton primaryButton(String text) {
        return button(text, ACCENT, Color.WHITE, ACCENT);
    }

    static JButton ghostButton(String text) {
        return button(text, Color.WHITE, TEXT, BORDER);
    }

    private static JButton button(String text, Color bg, Color fg, Color line) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(getModel().isRollover() && isEnabled() ? shade(bg) : bg);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setFocusPainted(false);
        b.setForeground(fg);
        b.setFont(font(Font.PLAIN, 12));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                new LineBorder(line, 1), new EmptyBorder(8, 16, 8, 16)));
        b.setRolloverEnabled(true);
        return b;
    }

    private static Color shade(Color c) {
        if (c.equals(Color.WHITE)) return new Color(244, 246, 244);
        return new Color(Math.max(0, c.getRed() - 14), Math.max(0, c.getGreen() - 14), Math.max(0, c.getBlue() - 14));
    }

    // ------------------------------------------------------------------
    // SIDEBAR
    // ------------------------------------------------------------------
    static final class Sidebar extends JPanel {
        private final List<JButton> items = new ArrayList<>();
        private JButton active;
        private final JPanel top = new JPanel();
        private final JPanel bottom = new JPanel();

        Sidebar(String portalLabel) {
            setLayout(new BorderLayout());
            setBackground(SIDEBAR);
            setPreferredSize(new Dimension(230, 0));
            setBorder(new EmptyBorder(26, 16, 22, 16));

            top.setOpaque(false);
            top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
            bottom.setOpaque(false);
            bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));

            JLabel logo = new JLabel("PLACEMENT");
            logo.setFont(font(Font.BOLD, 24));
            logo.setForeground(Color.WHITE);
            JLabel sub = new JLabel(portalLabel);
            sub.setFont(font(Font.PLAIN, 12));
            sub.setForeground(new Color(214, 236, 221));
            logo.setAlignmentX(Component.LEFT_ALIGNMENT);
            sub.setAlignmentX(Component.LEFT_ALIGNMENT);
            top.add(logo);
            top.add(sub);
            top.add(Box.createVerticalStrut(32));

            add(top, BorderLayout.NORTH);
            add(bottom, BorderLayout.SOUTH);
        }

        JButton addItem(String text, Runnable action) {
            JButton b = navButton(text);
            b.addActionListener(e -> {
                select(b);
                action.run();
            });
            items.add(b);
            top.add(b);
            top.add(Box.createVerticalStrut(2));
            return b;
        }

        void addBottom(String text, Runnable action) {
            JButton b = navButton(text);
            b.addActionListener(e -> action.run());
            bottom.add(b);
        }

        void select(JButton b) {
            active = b;
            for (JButton x : items) x.repaint();
        }

        private JButton navButton(String text) {
            JButton b = new JButton(text) {
                @Override
                protected void paintComponent(Graphics g) {
                    Color c = this == active ? SIDEBAR_ACTIVE : getModel().isRollover() ? SIDEBAR_HOVER : SIDEBAR;
                    g.setColor(c);
                    g.fillRect(0, 0, getWidth(), getHeight());
                    if (this == active) {
                        g.setColor(INDICATOR);
                        g.fillRect(0, 0, 4, getHeight());
                    }
                    super.paintComponent(g);
                }
            };
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            b.setPreferredSize(new Dimension(198, 40));
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setBorder(new EmptyBorder(0, 14, 0, 6));
            b.setFocusPainted(false);
            b.setContentAreaFilled(false);
            b.setOpaque(false);
            b.setRolloverEnabled(true);
            b.setForeground(Color.WHITE);
            b.setFont(font(Font.PLAIN, 14));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return b;
        }
    }

    /** White top bar: "<X> Dashboard" on the left, role label on the right. */
    static JPanel topBar(String title, String roleLabel) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(new EmptyBorder(16, 28, 16, 28));
        JLabel t = new JLabel(title);
        t.setFont(font(Font.BOLD, 20));
        t.setForeground(TEXT);
        JLabel r = new JLabel(roleLabel);
        r.setFont(font(Font.BOLD, 14));
        r.setForeground(TEXT);
        bar.add(t, BorderLayout.WEST);
        bar.add(r, BorderLayout.EAST);
        return bar;
    }

    static JLabel banner() {
        JLabel l = new JLabel(" ");
        l.setFont(font(Font.PLAIN, 12));
        l.setForeground(RED);
        l.setOpaque(true);
        l.setBackground(new Color(253, 238, 235));
        l.setBorder(new EmptyBorder(6, 28, 6, 28));
        l.setVisible(false);
        return l;
    }

    static void showBanner(JLabel banner, String message) {
        SwingUtilities.invokeLater(() -> {
            banner.setText(message == null || message.isBlank() ? "Request failed." : message);
            banner.setVisible(true);
        });
    }

    static void hideBanner(JLabel banner) {
        banner.setVisible(false);
    }

    // ------------------------------------------------------------------
    // DIALOGS / EXPORT
    // ------------------------------------------------------------------
    static void details(Component parent, String title, LinkedHashMap<String, String> fields) {
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setBackground(Color.WHITE);
        grid.setBorder(new EmptyBorder(14, 16, 14, 16));
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(4, 0, 4, 18);
        for (Map.Entry<String, String> e : fields.entrySet()) {
            c.gridx = 0;
            c.weightx = 0;
            JLabel k = new JLabel(e.getKey());
            k.setFont(font(Font.BOLD, 12));
            k.setForeground(MUTED);
            grid.add(k, c);
            c.gridx = 1;
            c.weightx = 1;
            JLabel v = new JLabel("<html><div style='width:320px'>" + escapeHtml(e.getValue()) + "</div></html>");
            v.setFont(font(Font.PLAIN, 12));
            v.setForeground(TEXT);
            grid.add(v, c);
            c.gridy++;
        }
        JOptionPane.showMessageDialog(parent, grid, title, JOptionPane.PLAIN_MESSAGE);
    }

    static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Exports exactly what the table currently shows (filtered and sorted). */
    static void exportCsv(Component parent, JTable table, String defaultName) {
        if (table == null || table.getRowCount() == 0) {
            JOptionPane.showMessageDialog(parent, "There are no rows to export.", "Export CSV",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(defaultName + "-" + LocalDate.now() + ".csv"));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getParentFile(), file.getName() + ".csv");
        }
        try (Writer w = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            w.write('\uFEFF');
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < table.getColumnCount(); c++) {
                if (c > 0) sb.append(',');
                sb.append(csv(table.getColumnName(c)));
            }
            sb.append("\r\n");
            for (int r = 0; r < table.getRowCount(); r++) {
                for (int c = 0; c < table.getColumnCount(); c++) {
                    if (c > 0) sb.append(',');
                    sb.append(csv(String.valueOf(table.getValueAt(r, c))));
                }
                sb.append("\r\n");
            }
            w.write(sb.toString());
            JOptionPane.showMessageDialog(parent, "Saved " + table.getRowCount() + " rows to\n" + file.getAbsolutePath(),
                    "Export CSV", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent, "Could not save file: " + ex.getMessage(), "Export CSV",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String csv(String v) {
        String s = v == null ? "" : v;
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    static void composeAnnouncement(JFrame parent, Api api, Runnable onSent) {
        JTextField subject = new JTextField(30);
        JTextArea message = new JTextArea(6, 30);
        message.setLineWrap(true);
        message.setWrapStyleWord(true);

        JPanel form = new JPanel(new BorderLayout(0, 8));
        JPanel top = new JPanel(new BorderLayout(0, 4));
        top.add(new JLabel("Subject"), BorderLayout.NORTH);
        top.add(subject, BorderLayout.CENTER);
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 4));
        bottomPanel.add(new JLabel("Message"), BorderLayout.NORTH);
        bottomPanel.add(new JScrollPane(message), BorderLayout.CENTER);
        form.add(top, BorderLayout.NORTH);
        form.add(bottomPanel, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(parent, form, "New Announcement",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;
        if (subject.getText().isBlank() || message.getText().isBlank()) {
            JOptionPane.showMessageDialog(parent, "Subject and message are both required.",
                    "New Announcement", JOptionPane.WARNING_MESSAGE);
            return;
        }
        api.sendGeneral(subject.getText(), message.getText(), () -> {
            JOptionPane.showMessageDialog(parent, "Announcement sent.", "New Announcement",
                    JOptionPane.INFORMATION_MESSAGE);
            onSent.run();
        }, err -> JOptionPane.showMessageDialog(parent, err, "Announcement not sent", JOptionPane.ERROR_MESSAGE));
    }

    // ------------------------------------------------------------------
    // ANNOUNCEMENTS PAGE (shared by Dean and Director)
    // ------------------------------------------------------------------
    static final class NoticesPage {
        final JPanel panel;
        private final DefaultTableModel model = model(
                new String[]{"ID", "Job", "Subject", "Audience", "Recipients", "Sent At", "Message"},
                Long.class, String.class, String.class, String.class, Integer.class, String.class, String.class);
        private final JTable table = table(model);
        private final JTextField search = searchField("Search announcements...", 260);
        private final JLabel count = muted(" ");
        private final Api api;
        private final JLabel banner;

        NoticesPage(JFrame frame, Api api, JLabel banner, String subtitle, boolean canCompose) {
            this.api = api;
            this.banner = banner;
            widths(table, 50, 80, 200, 110, 80, 140, 420);
            onChange(search, this::filter);

            Card card = new Card();
            JPanel tools = new JPanel(new BorderLayout(10, 0));
            tools.setOpaque(false);
            tools.add(search, BorderLayout.WEST);
            tools.add(count, BorderLayout.CENTER);
            JButton export = ghostButton("Export CSV");
            export.addActionListener(e -> exportCsv(frame, table, "announcements"));
            tools.add(export, BorderLayout.EAST);
            card.add(tools, BorderLayout.NORTH);
            card.add(tableScroll(table, 0), BorderLayout.CENTER);

            table.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                        int r = table.getSelectedRow();
                        LinkedHashMap<String, String> f = new LinkedHashMap<>();
                        f.put("Subject", String.valueOf(table.getValueAt(r, 2)));
                        f.put("Audience", String.valueOf(table.getValueAt(r, 3)));
                        f.put("Recipients", String.valueOf(table.getValueAt(r, 4)));
                        f.put("Sent at", String.valueOf(table.getValueAt(r, 5)));
                        f.put("Message", String.valueOf(table.getValueAt(r, 6)));
                        details(frame, "Announcement", f);
                    }
                }
            });

            JButton refresh = ghostButton("Refresh");
            refresh.addActionListener(e -> reload());
            JComponent east;
            if (canCompose) {
                JButton compose = primaryButton("New Announcement");
                compose.addActionListener(e -> composeAnnouncement(frame, api, this::reload));
                east = flow(FlowLayout.RIGHT, 8, refresh, compose);
            } else {
                east = flow(FlowLayout.RIGHT, 8, refresh);
            }
            panel = pageShell("Announcements", subtitle, east, card);
        }

        void reload() {
            api.notices(list -> {
                model.setRowCount(0);
                for (Notice n : list) {
                    model.addRow(new Object[]{n.id(), n.jobId(), n.subject(), n.group(), n.count(),
                            n.createdAt(), n.message()});
                }
                filter();
            }, err -> showBanner(banner, "Announcements: " + err));
        }

        private void filter() {
            sorter(table).setRowFilter(textFilter(search.getText()));
            count.setText("  " + table.getRowCount() + " of " + model.getRowCount() + " shown");
        }
    }

    // ------------------------------------------------------------------
    // SESSION RECOVERY (LoginFrame is not modified, so the dashboard finds its own session)
    // ------------------------------------------------------------------
    /** Returns {token, displayName}. Token may be null if nothing could be recovered. */
    static String[] recover(String role) {
        try {
            for (Window w : Window.getWindows()) {
                if (w.getClass().getSimpleName().equals("LoginFrame")) {
                    String token = readField(w, "sessionToken");
                    String name = readField(w, "authenticatedDisplayName");
                    if (token != null && !token.isBlank()) {
                        return new String[]{token, name == null || name.isBlank() ? role : name};
                    }
                }
            }
        } catch (Exception ignored) {
            // fall through to the sign-in prompt
        }
        return promptLogin(role);
    }

    private static String readField(Object target, String name) throws Exception {
        java.lang.reflect.Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        Object v = f.get(target);
        return v == null ? null : v.toString();
    }

    private static String[] promptLogin(String role) {
        JTextField email = new JTextField(22);
        JPasswordField pass = new JPasswordField(22);
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 6));
        p.add(new JLabel("Sign in as " + role));
        p.add(new JLabel("Email"));
        p.add(email);
        p.add(new JLabel("Password"));
        p.add(pass);
        if (JOptionPane.showConfirmDialog(null, p, role + " sign-in", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return new String[]{null, role};
        }
        try {
            SocketClient.Response r = new SocketClient().sendRequest("LOGIN", role, email.getText().trim(),
                    new String(pass.getPassword()));
            if (!r.success) {
                JOptionPane.showMessageDialog(null, r.payload, "Sign-in failed", JOptionPane.ERROR_MESSAGE);
                return new String[]{null, role};
            }
            String[] parts = r.parts();
            return new String[]{parts[0], parts.length > 1 ? parts[1] : role};
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Unable to connect to server: " + ex.getMessage(),
                    "Sign-in failed", JOptionPane.ERROR_MESSAGE);
            return new String[]{null, role};
        }
    }
}
}