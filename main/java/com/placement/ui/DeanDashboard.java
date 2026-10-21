package com.placement.ui;

import static com.placement.ui.DeanDashboard.Kit.*;

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
 * Dean dashboard - ACADEMIC OVERSIGHT.
 *
 * The Dean looks at students from an academic point of view: standing, backlogs,
 * department performance, skills, and how many students each drive would reach.
 * Everything here is read-only except sending announcements.
 *
 * Backend commands used: GET_STUDENTS, GET_JOBS, GET_ELIGIBLE_STUDENTS,
 * GET_NOTIFICATIONS, SEND_GENERAL_NOTIFICATION, LOGOUT.
 */
public class DeanDashboard extends JFrame {

    private static final long serialVersionUID = 1L;

    private final String displayName;
    private final Api api;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JLabel banner = banner();

    private List<Student> students = new ArrayList<>();
    private List<Job> jobs = new ArrayList<>();

    // Academic standing thresholds (Dean-adjustable)
    private final JSpinner cgpaMin = new JSpinner(new SpinnerNumberModel(6.0, 0.0, 10.0, 0.1));
    private final JSpinner backlogMax = new JSpinner(new SpinnerNumberModel(1, 0, 20, 1));

    // Dashboard
    private final JLabel kTotal = valueLabel();
    private final JLabel kAvg = valueLabel();
    private final JLabel kAttention = valueLabel();
    private final JLabel kBacklogs = valueLabel();
    private final Donut standingDonut = new Donut(GREEN, AMBER, RED);
    private final Donut dashDonut = new Donut(GREEN, AMBER, RED).legendBelow();
    private final Bars dashDeptBars = new Bars(BLUE, false);
    private final JLabel kGood = valueLabel();
    private final JLabel kDrives = valueLabel();
    private final JLabel kReady = valueLabel();
    private final JPanel attentionList = new JPanel();
    private final JLabel attentionCount = muted(" ");
    private final Bars cgpaBars = new Bars(BLUE, false);
    private final DefaultTableModel attentionModel = model(
            new String[]{"PRN", "Name", "Department", "CGPA", "Backlogs", "Standing"},
            String.class, String.class, String.class, Double.class, Integer.class, String.class);
    private final JTable attentionTable = table(attentionModel);

    // Academic overview
    private final Bars backlogBars = new Bars(RED, false);
    private final Bars skillBars = new Bars(GREEN, false);
    private final DefaultTableModel deptModel = model(
            new String[]{"Department", "Students", "Avg CGPA", "With Backlogs", "Needs Attention", "Good Standing"},
            String.class, Integer.class, Double.class, Integer.class, Integer.class, String.class);
    private final JTable deptTable = table(deptModel);

    // Students
    private final DefaultTableModel studentModel = model(
            new String[]{"PRN", "Name", "Email", "Department", "CGPA", "Passing Year", "Backlogs", "Semester",
                    "Standing", "Skills"},
            String.class, String.class, String.class, String.class, Double.class, Integer.class, Integer.class,
            Integer.class, String.class, String.class);
    private final JTable studentTable = table(studentModel);
    private final JTextField studentSearch = searchField("Name, PRN, email, skill...", 240);
    private final JComboBox<String> deptCombo = combo(150, "All departments");
    private final JComboBox<String> standingCombo = combo(150, "All standings", "Good Standing",
            "Needs Attention", "At Risk");
    private final JComboBox<String> backlogCombo = combo(140, "Any backlogs", "No backlogs", "1 backlog",
            "2 or more");
    private final JComboBox<String> sortCombo = combo(170, "Default order", "CGPA: high to low",
            "CGPA: low to high", "Name: A to Z", "Backlogs: most first");
    private final JLabel studentCount = muted(" ");

    // Drive eligibility
    private final DefaultTableModel jobModel = model(
            new String[]{"Job ID", "Company", "Role", "Package", "Min CGPA", "Branches", "Max Backlogs",
                    "Passing Year", "Deadline"},
            String.class, String.class, String.class, String.class, Double.class, String.class, Integer.class,
            Integer.class, String.class);
    private final JTable jobTable = table(jobModel);
    private final JLabel eEligible = valueLabel();
    private final JLabel eNot = valueLabel();
    private final JLabel eRate = valueLabel();
    private final JLabel eNear = valueLabel();
    private final DefaultTableModel eligibleModel = model(
            new String[]{"PRN", "Name", "Department", "CGPA", "Backlogs", "Passing Year", "Skills"},
            String.class, String.class, String.class, Double.class, Integer.class, Integer.class, String.class);
    private final JTable eligibleTable = table(eligibleModel);
    private final DefaultTableModel gapModel = model(
            new String[]{"PRN", "Name", "Department", "CGPA", "Backlogs", "Why not eligible"},
            String.class, String.class, String.class, Double.class, Integer.class, String.class);
    private final JTable gapTable = table(gapModel);
    private final Bars gapBars = new Bars(AMBER, false);
    private final JLabel eligibleHint = muted("Select a drive above, then press Check Eligibility.");

    // Reports
    private final JTabbedPane reportTabs = new JTabbedPane();
    private final DefaultTableModel repStandingModel = model(
            new String[]{"PRN", "Name", "Department", "CGPA", "Backlogs", "Standing"},
            String.class, String.class, String.class, Double.class, Integer.class, String.class);
    private final JTable repStandingTable = table(repStandingModel);
    private final DefaultTableModel repDeptModel = model(
            new String[]{"Department", "Students", "Avg CGPA", "Highest CGPA", "Lowest CGPA", "With Backlogs",
                    "Good Standing %"},
            String.class, Integer.class, Double.class, Double.class, Double.class, Integer.class, String.class);
    private final JTable repDeptTable = table(repDeptModel);
    private final DefaultTableModel repRiskModel = model(
            new String[]{"PRN", "Name", "Department", "CGPA", "Backlogs", "Standing", "Concern"},
            String.class, String.class, String.class, Double.class, Integer.class, String.class, String.class);
    private final JTable repRiskTable = table(repRiskModel);
    private final DefaultTableModel repSkillModel = model(
            new String[]{"Skill", "Students", "Share of Students"},
            String.class, Integer.class, String.class);
    private final JTable repSkillTable = table(repSkillModel);

    private final Bars avgDeptBars = new Bars(BLUE, true);
    private final Bars backlogDeptBars = new Bars(RED, false);

    private final JTable deptSnapshotTable = table(deptModel);
    private final JComboBox<String> attentionStanding = combo(150, "All flagged", "Needs Attention", "At Risk");
    private final JComboBox<String> attentionDept = combo(150, "All departments");
    private final JTextField reportSearch = searchField("Search this report...", 240);
    private final JComboBox<String> reportDeptCombo = combo(160, "All departments");

    private NoticesPage notices;
    private boolean updatingUi;

    public DeanDashboard(String displayName, String sessionToken) {
        this.displayName = displayName == null || displayName.isBlank() ? "Dean" : displayName;
        this.api = new Api(sessionToken);

        setTitle("Placement Eligibility Portal - Dean Dashboard");
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

    /** Kept so old code that calls new DeanDashboard() still compiles; it has no session. */
    private DeanDashboard(String[] session) {
        this(session[1], session[0]);
    }

    /** Used by LoginFrame: finds the session of the login that just succeeded. */
    public DeanDashboard() {
        this(Kit.recover("Dean"));
    }

    // ------------------------------------------------------------------
    // UI CONSTRUCTION
    // ------------------------------------------------------------------
    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);

        Sidebar sidebar = new Sidebar("DEAN PORTAL");
        JButton first = sidebar.addItem("Dashboard", () -> show("dash"));
        sidebar.addItem("Academic Overview", () -> show("overview"));
        sidebar.addItem("Students", () -> show("students"));
        sidebar.addItem("Drive Eligibility", () -> show("eligibility"));
        sidebar.addItem("Reports", () -> show("reports"));
        sidebar.addItem("Announcements", () -> {
            show("notices");
            notices.reload();
        });
        sidebar.addBottom("Logout", this::logout);
        sidebar.select(first);

        JPanel north = new JPanel(new BorderLayout());
        north.add(topBar("Dean Dashboard", "DEAN"), BorderLayout.NORTH);
        north.add(banner, BorderLayout.SOUTH);

        content.setBackground(BG);
        notices = new NoticesPage(this, api, banner,
                "Messages sent to students and placement staff", true);
        content.add(buildDashboardPage(), "dash");
        content.add(buildOverviewPage(), "overview");
        content.add(buildStudentsPage(), "students");
        content.add(buildEligibilityPage(), "eligibility");
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

    private JPanel buildDashboardPage() {
        VStack stack = new VStack();
        stack.add(grid(4, 14,
                kpiCardPlain("Total Students", kTotal, "Registered students"),
                kpiCardPlain("Good Standing", kGood, "Within CGPA/backlog limits"),
                kpiCardPlain("Open Drives", kDrives, "Active job postings"),
                kpiCardPlain("Readiness", kReady, "Overall good standing")), 16);

        Card standingCard = new Card();
        standingCard.add(h2("Academic Standing"), BorderLayout.NORTH);
        standingCard.add(dashDonut, BorderLayout.CENTER);
        Card deptCard = new Card();
        deptCard.add(h2("Students by Department"), BorderLayout.NORTH);
        deptCard.add(dashDeptBars, BorderLayout.CENTER);
        stack.add(grid(2, 16, standingCard, deptCard), 16);

        Card attention = new Card();
        attention.add(listHead("Students Needing Attention", attentionCount), BorderLayout.NORTH);
        attentionList.setOpaque(false);
        attentionList.setLayout(new BoxLayout(attentionList, BoxLayout.Y_AXIS));
        attention.add(attentionList, BorderLayout.CENTER);
        stack.add(attention, 0);

        // tables below are still filled for the other pages
        statusColumn(attentionTable, 5);
        widths(attentionTable, 90, 170, 120, 70, 80, 130);
        widths(deptSnapshotTable, 160, 80, 90, 110, 120, 120);

        return pageShell("Dashboard", "A quick overview of academic standing and current opportunities",
                refreshButton(), scrollPage(stack));
    }

    private void refreshDashboardView(int good, int total, List<Student> flagged) {
        kGood.setText(String.valueOf(good));
        int open = 0;
        for (Job jb : jobs) {
            if (!driveStatus(jb).equals("Closed")) open++;
        }
        kDrives.setText(String.valueOf(open));
        kReady.setText(pct(good, total));

        LinkedHashMap<String, Integer> deptData = new LinkedHashMap<>();
        for (Map.Entry<String, List<Student>> e : byDepartment().entrySet()) {
            deptData.put(e.getKey(), e.getValue().size());
        }
        dashDeptBars.set(deptData);

        attentionList.removeAll();
        attentionCount.setText(flagged.size() + " flagged");
        if (flagged.isEmpty()) attentionList.add(muted("No students need attention."));
        for (int i = 0; i < Math.min(6, flagged.size()); i++) {
            Student s = flagged.get(i);
            String st = standing(s);
            attentionList.add(listRow(s.name() + BULLET + s.dept(),
                    "CGPA " + fmt1(s.cgpa()) + BULLET + s.backlogs() + " backlog(s)" + BULLET + st,
                    s.prn(), MUTED));
        }
        attentionList.revalidate();
        attentionList.repaint();
    }

    private JPanel buildOverviewPage() {
        VStack stack = new VStack();

        Card thresholds = new Card();
        thresholds.add(h2("Standing Thresholds"), BorderLayout.NORTH);
        styleSpinner(cgpaMin);
        styleSpinner(backlogMax);
        JPanel row = flow(FlowLayout.LEFT, 14,
                filterLabel("MINIMUM CGPA"), cgpaMin, filterLabel("MAXIMUM BACKLOGS"), backlogMax);
        thresholds.add(row, BorderLayout.CENTER);
        thresholds.add(muted("Good Standing = meets both limits. Needs Attention = misses one. "
                + "At Risk = misses both. These limits apply across the whole dashboard."), BorderLayout.SOUTH);
        cgpaMin.addChangeListener(e -> {
            if (!updatingUi) refreshViews();
        });
        backlogMax.addChangeListener(e -> {
            if (!updatingUi) refreshViews();
        });
        stack.add(thresholds, 16);

        Card dept = new Card();
        dept.add(h2("Department Performance"), BorderLayout.NORTH);
        widths(deptTable, 160, 80, 90, 110, 120, 120);
        dept.add(tableScroll(deptTable, 260), BorderLayout.CENTER);
        stack.add(dept, 0);

        return pageShell("Academic Overview", "Department performance and standing limits",
                refreshButton(), scrollPage(stack));
    }

    private JPanel buildStudentsPage() {
        Card card = new Card();

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        filters.setOpaque(false);
        filters.add(labeled("SEARCH", studentSearch));
        filters.add(labeled("DEPARTMENT", deptCombo));
        filters.add(labeled("STANDING", standingCombo));
        filters.add(labeled("BACKLOGS", backlogCombo));
        filters.add(labeled("SORT BY", sortCombo));
        JButton clear = ghostButton("Clear");
        clear.addActionListener(e -> {
            studentSearch.setText("");
            deptCombo.setSelectedIndex(0);
            standingCombo.setSelectedIndex(0);
            backlogCombo.setSelectedIndex(0);
            sortCombo.setSelectedIndex(0);
        });
        JPanel clearWrap = new JPanel(new BorderLayout());
        clearWrap.setOpaque(false);
        clearWrap.add(Box.createVerticalStrut(16), BorderLayout.NORTH);
        clearWrap.add(clear, BorderLayout.CENTER);
        filters.add(clearWrap);

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        top.add(filters, BorderLayout.NORTH);
        top.add(studentCount, BorderLayout.SOUTH);
        card.add(top, BorderLayout.NORTH);

        statusColumn(studentTable, 8);
        widths(studentTable, 90, 150, 190, 120, 70, 110, 85, 95, 150, 280);
        studentTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JScrollPane sp = tableScroll(studentTable, 0);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        card.add(sp, BorderLayout.CENTER);

        onChange(studentSearch, this::applyStudentFilter);
        deptCombo.addActionListener(e -> applyStudentFilter());
        standingCombo.addActionListener(e -> applyStudentFilter());
        backlogCombo.addActionListener(e -> applyStudentFilter());
        sortCombo.addActionListener(e -> applyStudentSort());

        studentTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) showStudentDetails();
            }
        });

        JButton details = ghostButton("View Details");
        details.addActionListener(e -> showStudentDetails());
        JButton export = primaryButton("Export CSV");
        export.addActionListener(e -> exportCsv(this, studentTable, "students"));
        return pageShell("Students", "Search, filter and sort every registered student (double-click a row for details)",
                flow(FlowLayout.RIGHT, 8, details, export), card);
    }

    private JPanel buildEligibilityPage() {
        VStack stack = new VStack();

        Card picker = new Card();
        picker.add(h2("Choose a Placement Drive"), BorderLayout.NORTH);
        widths(jobTable, 80, 110, 160, 90, 90, 180, 115, 115, 100);
        picker.add(tableScroll(jobTable, 150), BorderLayout.CENTER);
        JButton check = primaryButton("Check Eligibility");
        check.addActionListener(e -> checkEligibility());
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.add(eligibleHint, BorderLayout.CENTER);
        bar.add(check, BorderLayout.EAST);
        picker.add(bar, BorderLayout.SOUTH);
        stack.add(picker, 16);

        stack.add(grid(4, 14,
                kpiCard("Eligible", eEligible, "Meet every drive criterion", GREEN),
                kpiCard("Not Eligible", eNot, "Miss at least one criterion", RED),
                kpiCard("Eligibility Rate", eRate, "Eligible / all students", BLUE),
                kpiCard("Near Misses", eNear, "Miss exactly one criterion", AMBER)), 16);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(font(Font.BOLD, 12));
        widths(eligibleTable, 90, 150, 120, 70, 80, 100, 280);
        widths(gapTable, 90, 150, 120, 70, 80, 380);
        tabs.addTab("Eligible Students", tableScroll(eligibleTable, 0));
        tabs.addTab("Not Eligible - Why", tableScroll(gapTable, 0));
        JPanel gapSummary = new JPanel(new BorderLayout());
        gapSummary.setBackground(Color.WHITE);
        gapSummary.setBorder(new EmptyBorder(12, 14, 12, 14));
        gapSummary.add(gapBars, BorderLayout.NORTH);
        tabs.addTab("Gap Summary", scrollPage(gapSummary));
        Card results = new Card();
        results.add(h2("Results"), BorderLayout.NORTH);
        tabs.setPreferredSize(new Dimension(400, 340));
        results.add(tabs, BorderLayout.CENTER);
        stack.add(results, 0);

        JButton exportEligible = ghostButton("Export Eligible List");
        exportEligible.addActionListener(e -> exportCsv(this, eligibleTable, "eligible-students"));
        return pageShell("Drive Eligibility",
                "Who the backend says is eligible for a drive, and why the rest are not",
                exportEligible, scrollPage(stack));
    }

    private JPanel buildReportsPage() {
        reportTabs.setFont(font(Font.BOLD, 12));
        statusColumn(repStandingTable, 5);
        statusColumn(repRiskTable, 5);
        widths(repStandingTable, 100, 180, 140, 80, 80, 140);
        widths(repDeptTable, 160, 80, 90, 100, 100, 110, 130);
        widths(repRiskTable, 100, 170, 130, 70, 80, 130, 180);
        widths(repSkillTable, 200, 100, 140);

        VStack analytics = new VStack();
        analytics.add(grid(2, 14,
                chartCard("Academic Standing", standingDonut),
                chartCard("CGPA Distribution", cgpaBars)), 14);
        analytics.add(grid(2, 14,
                chartCard("Average CGPA by Department", avgDeptBars),
                chartCard("Students With Backlogs by Department", backlogDeptBars)), 14);
        analytics.add(grid(2, 14,
                chartCard("Backlog Distribution", backlogBars),
                chartCard("Most Common Skills", skillBars)), 0);
        analytics.setBackground(Color.WHITE);
        analytics.setBorder(new EmptyBorder(12, 12, 12, 12));
        JScrollPane analyticsScroll = scrollPage(analytics);
        analyticsScroll.getViewport().setOpaque(true);
        analyticsScroll.getViewport().setBackground(Color.WHITE);

        reportTabs.addTab("Analytics (Charts)", analyticsScroll);
        reportTabs.addTab("Student Standing", tableScroll(repStandingTable, 0));
        reportTabs.addTab("Department Report", tableScroll(repDeptTable, 0));
        reportTabs.addTab("Risk Report", tableScroll(repRiskTable, 0));
        reportTabs.addTab("Skills Report", tableScroll(repSkillTable, 0));

        Card card = new Card();
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        filters.setOpaque(false);
        filters.add(labeled("SEARCH TABLE REPORTS", reportSearch));
        filters.add(labeled("DEPARTMENT", reportDeptCombo));
        JButton clear = ghostButton("Clear");
        clear.addActionListener(e -> {
            reportSearch.setText("");
            reportDeptCombo.setSelectedIndex(0);
        });
        JPanel clearWrap = new JPanel(new BorderLayout());
        clearWrap.setOpaque(false);
        clearWrap.add(Box.createVerticalStrut(16), BorderLayout.NORTH);
        clearWrap.add(clear, BorderLayout.CENTER);
        filters.add(clearWrap);
        card.add(filters, BorderLayout.NORTH);
        card.add(reportTabs, BorderLayout.CENTER);
        onChange(reportSearch, this::applyReportFilter);
        reportDeptCombo.addActionListener(e -> applyReportFilter());

        JButton export = primaryButton("Export This Report");
        export.addActionListener(e -> {
            String[] names = {"", "student-standing", "department-report", "risk-report", "skills-report"};
            JTable[] tables = {null, repStandingTable, repDeptTable, repRiskTable, repSkillTable};
            int i = reportTabs.getSelectedIndex();
            if (tables[i] == null) {
                JOptionPane.showMessageDialog(this, "Open one of the table reports to export it as CSV.",
                        "Export", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            exportCsv(this, tables[i], "dean-" + names[i]);
        });
        return pageShell("Reports & Analytics", "Charts and academic reports built from live student data",
                flow(FlowLayout.RIGHT, 8, refreshButton(), export), card);
    }

    private static Card chartCard(String title, JComponent chart) {
        Card c = new Card();
        c.add(h2(title), BorderLayout.NORTH);
        c.add(chart, BorderLayout.CENTER);
        return c;
    }

    private void applyAttentionFilter() {
        final String st = String.valueOf(attentionStanding.getSelectedItem());
        final String dept = String.valueOf(attentionDept.getSelectedItem());
        sorter(attentionTable).setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> e) {
                if (!st.equals("All flagged") && !st.equals(e.getStringValue(5))) return false;
                return dept.equals("All departments") || dept.equalsIgnoreCase(e.getStringValue(2));
            }
        });
    }

    private void applyReportFilter() {
        final String text = reportSearch.getText().trim().toLowerCase(Locale.ROOT);
        final String dept = String.valueOf(reportDeptCombo.getSelectedItem());
        for (JTable t : new JTable[]{repStandingTable, repDeptTable, repRiskTable, repSkillTable}) {
            final int deptCol = ((DefaultTableModel) t.getModel()).findColumn("Department");
            sorter(t).setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
                @Override
                public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> e) {
                    if (deptCol >= 0 && !dept.equals("All departments")
                            && !dept.equalsIgnoreCase(e.getStringValue(deptCol))) return false;
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
    // SMALL UI HELPERS
    // ------------------------------------------------------------------
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

    private static void styleSpinner(JSpinner s) {
        s.setPreferredSize(new Dimension(90, 34));
        s.setFont(font(Font.PLAIN, 12));
    }

    // ------------------------------------------------------------------
    // DATA LOADING
    // ------------------------------------------------------------------
    private void loadAll() {
        hideBanner(banner);
        api.students(list -> {
            students = list;
            refreshViews();
        }, err -> showBanner(banner, "Students: " + err));
        api.jobs(list -> {
            jobs = list;
            jobModel.setRowCount(0);
            for (Job j : jobs) {
                jobModel.addRow(new Object[]{j.id(), j.company(), j.role(), j.pkg(), j.minCgpa(), j.branches(),
                        j.maxBacklogs(), j.year(), j.deadline()});
            }
        }, err -> showBanner(banner, "Drives: " + err));
    }

    private double minCgpa() {
        return ((Number) cgpaMin.getValue()).doubleValue();
    }

    private int maxBacklogs() {
        return ((Number) backlogMax.getValue()).intValue();
    }

    private String standing(Student s) {
        boolean lowCgpa = s.cgpa() < minCgpa();
        boolean manyBacklogs = s.backlogs() > maxBacklogs();
        if (lowCgpa && manyBacklogs) return "At Risk";
        if (lowCgpa || manyBacklogs) return "Needs Attention";
        return "Good Standing";
    }

    private String concern(Student s) {
        boolean lowCgpa = s.cgpa() < minCgpa();
        boolean manyBacklogs = s.backlogs() > maxBacklogs();
        if (lowCgpa && manyBacklogs) return "Low CGPA and backlogs";
        if (lowCgpa) return "CGPA below " + fmt1(minCgpa());
        if (manyBacklogs) return "Backlogs above " + maxBacklogs();
        return "";
    }

    private Map<String, List<Student>> byDepartment() {
        Map<String, List<Student>> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Student s : students) {
            map.computeIfAbsent(s.dept().isBlank() ? "Unassigned" : s.dept(), k -> new ArrayList<>()).add(s);
        }
        return map;
    }

    /** Rebuilds every table and chart from the current student list and thresholds. */
    private void refreshViews() {
        int total = students.size();
        int good = 0;
        int attention = 0;
        int risk = 0;
        int withBacklogs = 0;
        double sum = 0;
        int[] bands = new int[5];
        for (Student s : students) {
            switch (standing(s)) {
                case "Good Standing" -> good++;
                case "Needs Attention" -> attention++;
                default -> risk++;
            }
            if (s.backlogs() > 0) withBacklogs++;
            sum += s.cgpa();
            if (s.cgpa() < 6) bands[0]++;
            else if (s.cgpa() < 7) bands[1]++;
            else if (s.cgpa() < 8) bands[2]++;
            else if (s.cgpa() < 9) bands[3]++;
            else bands[4]++;
        }

        kTotal.setText(String.valueOf(total));
        kAvg.setText(total == 0 ? "--" : fmt1(sum / total));
        kAttention.setText(String.valueOf(attention + risk));
        kBacklogs.setText(String.valueOf(withBacklogs));

        LinkedHashMap<String, Integer> standingData = new LinkedHashMap<>();
        standingData.put("Good Standing", good);
        standingData.put("Needs Attention", attention);
        standingData.put("At Risk", risk);
        standingDonut.set(standingData, pct(good, total));
        dashDonut.set(standingData, pct(good, total));

        LinkedHashMap<String, Integer> cgpaData = new LinkedHashMap<>();
        cgpaData.put("Below 6.0", bands[0]);
        cgpaData.put("6.0 - 6.9", bands[1]);
        cgpaData.put("7.0 - 7.9", bands[2]);
        cgpaData.put("8.0 - 8.9", bands[3]);
        cgpaData.put("9.0 and above", bands[4]);
        cgpaBars.set(cgpaData);

        // students needing attention (lowest CGPA first)
        List<Student> flagged = new ArrayList<>();
        for (Student s : students) {
            if (!standing(s).equals("Good Standing")) flagged.add(s);
        }
        flagged.sort(Comparator.comparingDouble(Student::cgpa));
        refreshDashboardView(good, total, flagged);
        attentionModel.setRowCount(0);
        for (Student s : flagged) {
            attentionModel.addRow(new Object[]{s.prn(), s.name(), s.dept(), s.cgpa(), s.backlogs(), standing(s)});
        }

        // backlog distribution
        int[] bl = new int[4];
        for (Student s : students) bl[Math.min(3, Math.max(0, s.backlogs()))]++;
        LinkedHashMap<String, Integer> backlogData = new LinkedHashMap<>();
        backlogData.put("No backlogs", bl[0]);
        backlogData.put("1 backlog", bl[1]);
        backlogData.put("2 backlogs", bl[2]);
        backlogData.put("3 or more", bl[3]);
        backlogBars.set(backlogData);

        // skills
        Map<String, Integer> skillCount = new HashMap<>();
        for (Student s : students) {
            Set<String> seen = new HashSet<>();
            for (String k : s.skillList()) {
                String key = k.toLowerCase(Locale.ROOT);
                if (seen.add(key)) skillCount.merge(key, 1, Integer::sum);
            }
        }
        List<Map.Entry<String, Integer>> skillRows = new ArrayList<>(skillCount.entrySet());
        skillRows.sort((a, b) -> b.getValue() != a.getValue().intValue() ? b.getValue() - a.getValue()
                : a.getKey().compareTo(b.getKey()));
        LinkedHashMap<String, Integer> skillData = new LinkedHashMap<>();
        repSkillModel.setRowCount(0);
        int n = 0;
        for (Map.Entry<String, Integer> e : skillRows) {
            String label = titleCase(e.getKey());
            if (n++ < 8) skillData.put(label, e.getValue());
            repSkillModel.addRow(new Object[]{label, e.getValue(), pct(e.getValue(), total)});
        }
        skillBars.set(skillData);

        // department tables
        deptModel.setRowCount(0);
        repDeptModel.setRowCount(0);
        LinkedHashMap<String, Double> avgByDept = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> backByDept = new LinkedHashMap<>();
        List<String> deptNames = new ArrayList<>();
        for (Map.Entry<String, List<Student>> e : byDepartment().entrySet()) {
            deptNames.add(e.getKey());
            List<Student> list = e.getValue();
            double dSum = 0;
            double hi = 0;
            double lo = 10;
            int dBack = 0;
            int dAttention = 0;
            int dGood = 0;
            for (Student s : list) {
                dSum += s.cgpa();
                hi = Math.max(hi, s.cgpa());
                lo = Math.min(lo, s.cgpa());
                if (s.backlogs() > 0) dBack++;
                if (standing(s).equals("Good Standing")) dGood++;
                else dAttention++;
            }
            double avg = dSum / list.size();
            deptModel.addRow(new Object[]{e.getKey(), list.size(), avg, dBack, dAttention,
                    pct(dGood, list.size())});
            repDeptModel.addRow(new Object[]{e.getKey(), list.size(), avg, hi, lo, dBack,
                    pct(dGood, list.size())});
            avgByDept.put(e.getKey(), avg);
            backByDept.put(e.getKey(), dBack);
        }

        avgDeptBars.set(avgByDept);
        backlogDeptBars.set(backByDept);

        // students table + reports
        studentModel.setRowCount(0);
        repStandingModel.setRowCount(0);
        repRiskModel.setRowCount(0);
        for (Student s : students) {
            String st = standing(s);
            studentModel.addRow(new Object[]{s.prn(), s.name(), s.email(), s.dept(), s.cgpa(), s.year(),
                    s.backlogs(), s.semester(), st, s.skills()});
            repStandingModel.addRow(new Object[]{s.prn(), s.name(), s.dept(), s.cgpa(), s.backlogs(), st});
            if (!st.equals("Good Standing")) {
                repRiskModel.addRow(new Object[]{s.prn(), s.name(), s.dept(), s.cgpa(), s.backlogs(), st,
                        concern(s)});
            }
        }
        resetCombo(deptCombo, "All departments", deptNames);
        resetCombo(attentionDept, "All departments", deptNames);
        resetCombo(reportDeptCombo, "All departments", deptNames);
        applyAttentionFilter();
        applyReportFilter();
        applyStudentFilter();
        applyStudentSort();
    }

    private static String titleCase(String s) {
        if (s == null || s.isEmpty()) return s;
        if (s.length() <= 3) return s.toUpperCase(Locale.ROOT);
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ------------------------------------------------------------------
    // STUDENT FILTER / SORT / DETAILS
    // ------------------------------------------------------------------
    private void applyStudentFilter() {
        final String text = studentSearch.getText().trim().toLowerCase(Locale.ROOT);
        final String dept = String.valueOf(deptCombo.getSelectedItem());
        final String st = String.valueOf(standingCombo.getSelectedItem());
        final int back = backlogCombo.getSelectedIndex();

        sorter(studentTable).setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> e) {
                if (!dept.equals("All departments") && !dept.equalsIgnoreCase(e.getStringValue(3))) return false;
                if (!st.equals("All standings") && !st.equals(e.getStringValue(8))) return false;
                int b = toInt(e.getStringValue(6));
                if (back == 1 && b != 0) return false;
                if (back == 2 && b != 1) return false;
                if (back == 3 && b < 2) return false;
                if (text.isEmpty()) return true;
                for (int i = 0; i < e.getValueCount(); i++) {
                    if (e.getStringValue(i).toLowerCase(Locale.ROOT).contains(text)) return true;
                }
                return false;
            }
        });
        studentCount.setText("Showing " + studentTable.getRowCount() + " of " + studentModel.getRowCount()
                + " students");
    }

    private void applyStudentSort() {
        int col;
        SortOrder order;
        switch (sortCombo.getSelectedIndex()) {
            case 1 -> {
                col = 4;
                order = SortOrder.DESCENDING;
            }
            case 2 -> {
                col = 4;
                order = SortOrder.ASCENDING;
            }
            case 3 -> {
                col = 1;
                order = SortOrder.ASCENDING;
            }
            case 4 -> {
                col = 6;
                order = SortOrder.DESCENDING;
            }
            default -> {
                sorter(studentTable).setSortKeys(null);
                return;
            }
        }
        sorter(studentTable).setSortKeys(List.of(new RowSorter.SortKey(col, order)));
    }

    private void showStudentDetails() {
        int r = studentTable.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Select a student first.", "Student Details",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int m = studentTable.convertRowIndexToModel(r);
        String prn = String.valueOf(studentModel.getValueAt(m, 0));
        for (Student s : students) {
            if (s.prn().equals(prn)) {
                LinkedHashMap<String, String> f = new LinkedHashMap<>();
                f.put("PRN", s.prn());
                f.put("Name", s.name());
                f.put("Email", s.email());
                f.put("Phone", s.phone());
                f.put("Department", s.dept());
                f.put("CGPA", fmt1(s.cgpa()));
                f.put("Passing year", String.valueOf(s.year()));
                f.put("Semester", String.valueOf(s.semester()));
                f.put("Backlogs", String.valueOf(s.backlogs()));
                f.put("Academic standing", standing(s));
                f.put("Skills", s.skills());
                details(this, "Student Details", f);
                return;
            }
        }
    }

    // ------------------------------------------------------------------
    // DRIVE ELIGIBILITY
    // ------------------------------------------------------------------
    private void checkEligibility() {
        int r = jobTable.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Select a placement drive first.", "Drive Eligibility",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String jobId = String.valueOf(jobModel.getValueAt(jobTable.convertRowIndexToModel(r), 0));
        Job job = null;
        for (Job j : jobs) {
            if (j.id().equals(jobId)) job = j;
        }
        if (job == null) return;
        final Job selected = job;
        eligibleHint.setText("Checking " + selected.company() + " - " + selected.role() + "...");

        api.eligible(jobId, eligible -> {
            Set<String> eligiblePrns = new HashSet<>();
            eligibleModel.setRowCount(0);
            for (Student s : eligible) {
                eligiblePrns.add(s.prn());
                eligibleModel.addRow(new Object[]{s.prn(), s.name(), s.dept(), s.cgpa(), s.backlogs(), s.year(),
                        s.skills()});
            }

            gapModel.setRowCount(0);
            LinkedHashMap<String, Integer> gaps = new LinkedHashMap<>();
            for (String k : new String[]{"CGPA too low", "Too many backlogs", "Branch not allowed",
                    "Passing year differs", "Missing skills", "Other backend rule"}) {
                gaps.put(k, 0);
            }
            int near = 0;
            int notEligible = 0;
            for (Student s : students) {
                if (eligiblePrns.contains(s.prn())) continue;
                notEligible++;
                List<String> reasons = new ArrayList<>();
                if (s.cgpa() < selected.minCgpa()) {
                    reasons.add("CGPA " + fmt1(s.cgpa()) + " is below " + fmt1(selected.minCgpa()));
                    gaps.merge("CGPA too low", 1, Integer::sum);
                }
                if (s.backlogs() > selected.maxBacklogs()) {
                    reasons.add("Backlogs " + s.backlogs() + " exceed " + selected.maxBacklogs());
                    gaps.merge("Too many backlogs", 1, Integer::sum);
                }
                if (!selected.branchList().isEmpty() && !selected.opensTo(s.dept())) {
                    reasons.add("Branch " + s.dept() + " not allowed");
                    gaps.merge("Branch not allowed", 1, Integer::sum);
                }
                if (selected.year() > 0 && s.year() != selected.year()) {
                    reasons.add("Passing year " + s.year() + " (needs " + selected.year() + ")");
                    gaps.merge("Passing year differs", 1, Integer::sum);
                }
                List<String> missing = new ArrayList<>();
                String have = s.skills() == null ? "" : s.skills().toLowerCase(Locale.ROOT);
                for (String need : selected.skillList()) {
                    if (!have.contains(need.toLowerCase(Locale.ROOT))) missing.add(need);
                }
                if (!missing.isEmpty()) {
                    reasons.add("Missing skills: " + String.join(", ", missing));
                    gaps.merge("Missing skills", 1, Integer::sum);
                }
                if (reasons.isEmpty()) {
                    reasons.add("Not selected by backend rules (for example already notified)");
                    gaps.merge("Other backend rule", 1, Integer::sum);
                }
                if (reasons.size() == 1) near++;
                gapModel.addRow(new Object[]{s.prn(), s.name(), s.dept(), s.cgpa(), s.backlogs(),
                        String.join("; ", reasons)});
            }
            LinkedHashMap<String, Integer> shown = new LinkedHashMap<>();
            for (Map.Entry<String, Integer> e : gaps.entrySet()) {
                if (e.getValue() > 0) shown.put(e.getKey(), e.getValue());
            }
            gapBars.set(shown);

            eEligible.setText(String.valueOf(eligible.size()));
            eNot.setText(String.valueOf(notEligible));
            eRate.setText(pct(eligible.size(), students.size()));
            eNear.setText(String.valueOf(near));
            eligibleHint.setText(selected.company() + " - " + selected.role() + ": " + eligible.size()
                    + " eligible out of " + students.size() + " students.");
        }, err -> {
            eligibleHint.setText("Could not check eligibility.");
            showBanner(banner, "Eligibility: " + err);
        });
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
        SwingUtilities.invokeLater(() -> new DeanDashboard().setVisible(true));
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

    /** Admin-style KPI card: big dark number on the left, title and caption on the right. */
    static Card kpiCardPlain(String title, JLabel value, String sub) {
        Card card = new Card();
        card.setLayout(new BorderLayout(12, 0));
        card.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1), new EmptyBorder(14, 16, 14, 14)));
        value.setFont(font(Font.BOLD, 30));
        value.setForeground(Color.BLACK);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(title);
        t.setFont(font(Font.BOLD, 13));
        t.setForeground(Color.BLACK);
        JLabel c = new JLabel(sub);
        c.setFont(font(Font.PLAIN, 11));
        c.setForeground(MUTED);
        text.add(Box.createVerticalGlue());
        text.add(t);
        text.add(Box.createVerticalStrut(2));
        text.add(c);
        text.add(Box.createVerticalGlue());
        card.add(value, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    /** One row of a "recent items" list: bold title, muted detail, right-aligned tag. */
    static JPanel listRow(String title, String detail, String tag, Color tagColor) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER), new EmptyBorder(9, 0, 9, 0)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
        JLabel t = new JLabel(title);
        t.setFont(font(Font.BOLD, 14));
        t.setForeground(Color.BLACK);
        JLabel d = new JLabel(detail);
        d.setFont(font(Font.PLAIN, 12));
        d.setForeground(MUTED);
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(t);
        left.add(Box.createVerticalStrut(3));
        left.add(d);
        JLabel g = new JLabel(tag);
        g.setFont(font(Font.BOLD, 12));
        g.setForeground(tagColor);
        row.add(left, BorderLayout.CENTER);
        row.add(g, BorderLayout.EAST);
        return row;
    }

    /** Card header with a title on the left and a small muted count on the right. */
    static JPanel listHead(String title, JLabel right) {
        JPanel head = new JPanel(new BorderLayout(10, 0));
        head.setOpaque(false);
        head.add(h2(title), BorderLayout.WEST);
        head.add(right, BorderLayout.EAST);
        return head;
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
        private boolean below;

        Donut(Color... colors) {
            this.colors = colors;
            setOpaque(false);
            setPreferredSize(new Dimension(300, 190));
        }

        /** Draws the legend in a row under the ring (admin dashboard look). */
        Donut legendBelow() {
            below = true;
            setPreferredSize(new Dimension(300, 215));
            return this;
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
            int d = below ? Math.min(getHeight() - 62, 120) : Math.min(getHeight() - 20, 150);
            int x = 14;
            int y = below ? 8 : (getHeight() - d) / 2;
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

            if (below) {
                int bx = x + 6;
                int by = y + d + 22;
                i = 0;
                g2.setFont(font(Font.PLAIN, 12));
                FontMetrics lm = g2.getFontMetrics();
                for (Map.Entry<String, Integer> e : data.entrySet()) {
                    String txt = e.getKey() + " (" + e.getValue() + ")";
                    if (bx + 20 + lm.stringWidth(txt) > getWidth() && bx > x + 6) {
                        bx = x + 6;
                        by += 22;
                    }
                    g2.setColor(colors[i % colors.length]);
                    g2.fillRect(bx, by - 10, 12, 12);
                    g2.setColor(TEXT);
                    g2.drawString(txt, bx + 18, by);
                    bx += 18 + lm.stringWidth(txt) + 28;
                    i++;
                }
                g2.dispose();
                return;
            }
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