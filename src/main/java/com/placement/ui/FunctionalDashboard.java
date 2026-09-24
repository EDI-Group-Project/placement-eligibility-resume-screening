package com.placement.ui;

import com.placement.sockets.SocketClient;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class FunctionalDashboard extends JFrame {
    private final String role, name, token;
    private final boolean edit, query, admin;
    private final SocketClient client = new SocketClient();
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final DefaultTableModel jobs = model(new String[]{"Job ID", "Company", "Role", "Package", "Min CGPA", "Branches", "Backlogs", "Year", "Deadline"});
    private final DefaultTableModel students = model(new String[]{"PRN", "Name", "Email", "Department", "CGPA", "Year", "Backlogs", "Semester", "Phone", "Skills"});
    private final DefaultTableModel notes = model(new String[]{"ID", "Job ID", "Subject", "Message", "Group", "Recipients", "Created"});
    private final DefaultTableModel faculty = model(new String[]{"ID", "Name", "Email", "Role", "Department", "Phone"});
    private final JTable jobsT = new JTable(jobs), studentsT = new JTable(students), notesT = new JTable(notes), facultyT = new JTable(faculty);
    private JTextField search;
    private JComboBox<String> dept, year, cgpa, back, sem, sort;
    private JLabel stats;
    private JLabel reportStats;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    public FunctionalDashboard(String role, String name, String token) {
        this.role = role.toUpperCase();
        this.name = name == null || name.isBlank() ? role : name;
        this.token = token;
        edit = role.equals("ADMIN") || role.equals("TPO") || role.equals("TPC");
        query = role.equals("ADMIN") || role.equals("TPC");
        admin = role.equals("ADMIN");
        setTitle("Placement Eligibility Portal - " + role + " Dashboard");
        setSize(1250, 760);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        build();
        refresh();
    }

    private void build() {
        JPanel root = new JPanel(new BorderLayout());
        root.add(side(), BorderLayout.WEST);
        JPanel main = new JPanel(new BorderLayout());
        main.add(header(), BorderLayout.NORTH);
        content.setBackground(BG());
        content.add(dashboard(), "D");
        content.add(jobsPage(), "J");
        content.add(studentsPage(), "S");
        content.add(notesPage(), "N");
        content.add(reportPage(), "R");
        if (admin) content.add(adminPage(), "A");
        main.add(content);
        root.add(main);
        setContentPane(root);
    }

    private JPanel side() {
        JPanel p = new JPanel();
        p.setPreferredSize(new Dimension(220, 760));
        p.setBackground(DARK());
        p.setBorder(BorderFactory.createEmptyBorder(28, 16, 18, 16));
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(lbl("PLACEMENT", 23, true, Color.WHITE));
        p.add(lbl("ELIGIBILITY PORTAL", 11, false, new Color(220, 245, 225)));
        p.add(Box.createVerticalStrut(25));
        nav(p, "Dashboard", "D");
        nav(p, "Job Postings", "J");
        nav(p, "Students", "S");
        nav(p, "Notifications", "N");
        nav(p, "Reports", "R");
        if (admin) nav(p, "Manage Users", "A");
        p.add(Box.createVerticalGlue());
        nav(p, "Logout", null);
        return p;
    }

    private void nav(JPanel p, String text, String card) {
        JButton b = new JButton(text);
        b.setMaximumSize(new Dimension(188, 40));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setForeground(Color.WHITE);
        b.setBackground(DARK());
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        if (card == null) b.addActionListener(e -> logout());
        else b.addActionListener(e -> {
            cards.show(content, card);
            if (card.equals("J")) loadJobs();
            if (card.equals("S")) loadStudents();
            if (card.equals("N")) loadNotes();
            if (card.equals("R")) loadStats();
            if (card.equals("A")) loadFaculty();
        });
        p.add(b);
        p.add(Box.createVerticalStrut(4));
    }

    private JPanel header() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        p.add(lbl(role + " Dashboard", 22, true, TEXT()), BorderLayout.WEST);
        p.add(lbl(name, 13, false, MUTED()), BorderLayout.EAST);
        return p;
    }

    private JPanel dashboard() {
        JPanel p = page();
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        JLabel welcome = lbl("Welcome, " + name, 20, true, TEXT());
        welcome.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(welcome);
        body.add(Box.createVerticalStrut(24));
        stats = lbl("Loading live statistics...", 16, true, TEXT());
        stats.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(stats);
        body.add(Box.createVerticalStrut(28));
        JLabel info = lbl("Use the navigation menu to manage jobs, students, notifications and reports.", 14, false, MUTED());
        info.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(info);
        body.add(Box.createVerticalStrut(12));
        JLabel data = lbl("Live data is loaded from the placement portal database.", 13, false, MUTED());
        data.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(data);
        p.add(body, BorderLayout.NORTH);
        return p;
    }

    private JPanel jobsPage() {
        JPanel p = page();
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setOpaque(false);
        JButton ref = btn("Refresh");
        ref.addActionListener(e -> loadJobs());
        top.add(ref);
        if (edit) {
            JButton add = btn("Add Job");
            add.addActionListener(e -> addJob());
            JButton el = btn("Eligible Students");
            el.addActionListener(e -> eligible());
            JButton no = btn("Send Notification");
            no.addActionListener(e -> notifyAllStudents());
            top.add(add);
            top.add(el);
            top.add(no);
        }
        p.add(top, BorderLayout.NORTH);
        style(jobsT);
        p.add(new JScrollPane(jobsT));
        return p;
    }

    private JPanel studentsPage() {
        JPanel p = page();
        if (query) {
            JPanel f = new JPanel(new FlowLayout(FlowLayout.LEFT));
            f.setOpaque(false);
            search = new JTextField(12);
            dept = box("ALL", "CSE", "IT", "ECE", "MECH", "CIVIL");
            year = box("ALL", "2026", "2027", "2028", "2029");
            cgpa = box("ALL", "6", "7", "8", "9");
            back = box("ALL", "0", "1", "2", "3");
            sem = box("ALL", "1", "2", "3", "4", "5", "6", "7", "8");
            sort = box("CGPA DESC", "CGPA ASC", "NAME ASC", "NAME DESC", "YEAR ASC", "BACKLOGS ASC");
            f.add(new JLabel("Search")); f.add(search);
            f.add(new JLabel("Dept")); f.add(dept);
            f.add(new JLabel("Year")); f.add(year);
            f.add(new JLabel("Min CGPA")); f.add(cgpa);
            f.add(new JLabel("Max Backlogs")); f.add(back);
            f.add(new JLabel("Semester")); f.add(sem);
            f.add(new JLabel("Sort")); f.add(sort);
            JButton a = btn("Apply");
            a.addActionListener(e -> loadStudents());
            JButton c = btn("Clear");
            c.addActionListener(e -> {
                search.setText(""); dept.setSelectedIndex(0); year.setSelectedIndex(0); cgpa.setSelectedIndex(0);
                back.setSelectedIndex(0); sem.setSelectedIndex(0); sort.setSelectedIndex(0); loadStudents();
            });
            f.add(a); f.add(c);
            p.add(f, BorderLayout.NORTH);
        } else p.add(lbl("Student Roster (view only)", 18, true, TEXT()), BorderLayout.NORTH);
        style(studentsT);
        p.add(new JScrollPane(studentsT), BorderLayout.CENTER);
        return p;
    }

    private JPanel notesPage() {
        JPanel p = page();
        JButton r = btn("Refresh");
        r.addActionListener(e -> loadNotes());
        p.add(r, BorderLayout.NORTH);
        style(notesT);
        p.add(new JScrollPane(notesT), BorderLayout.CENTER);
        return p;
    }

    private JPanel reportPage() {
        JPanel p = page();
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        JLabel title = lbl("Placement Report", 20, true, TEXT());
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(title);
        body.add(Box.createVerticalStrut(20));
        reportStats = lbl("Loading report...", 16, true, TEXT());
        reportStats.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(reportStats);
        body.add(Box.createVerticalStrut(18));
        JLabel basic = lbl("Basic report: current jobs, eligible-student workload, notifications and total students are shown above.", 14, false, MUTED());
        basic.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(basic);
        body.add(Box.createVerticalStrut(10));
        JLabel fallback = lbl("Advanced analytics: not configured yet. This section is reserved for future placement trends, charts and analytics.", 14, false, MUTED());
        fallback.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(fallback);
        JButton refresh = btn("Refresh Report");
        refresh.setAlignmentX(Component.LEFT_ALIGNMENT);
        refresh.addActionListener(e -> loadStats());
        body.add(Box.createVerticalStrut(18));
        body.add(refresh);
        p.add(body, BorderLayout.NORTH);
        return p;
    }

    private JPanel adminPage() {
        JPanel p = page();
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setOpaque(false);
        JButton add = btn("Add User");
        add.addActionListener(e -> addFaculty());
        JButton student = btn("Add Student");
        student.addActionListener(e -> addStudent());
        JButton ref = btn("Refresh");
        ref.addActionListener(e -> loadFaculty());
        top.add(add); top.add(student); top.add(ref);
        p.add(top, BorderLayout.NORTH);
        style(facultyT);
        p.add(new JScrollPane(facultyT), BorderLayout.CENTER);
        return p;
    }

    private void refresh() {
        loadStats(); loadJobs(); loadStudents(); loadNotes();
        if (admin) loadFaculty();
    }

    private void loadStats() {
        try {
            SocketClient.Response r = client.sendRequest("GET_DASHBOARD_STATS", token);
            if (r.success) {
                String[] x = r.parts();
                String value = "Active Jobs: " + x[0] + "    Pending Eligible: " + x[1] + "    Notifications: " + x[2] + "    Students: " + x[3];
                stats.setText(value);
                if (reportStats != null) reportStats.setText(value);
            }
        } catch (Exception e) {
            String fallback = "Live statistics are temporarily unavailable.";
            if (stats != null) stats.setText(fallback);
            if (reportStats != null) reportStats.setText(fallback);
        }
    }

    private void loadJobs() {
        jobs.setRowCount(0);
        try {
            SocketClient.ListResponse r = client.sendListRequest("GET_JOBS", token);
            if (!r.success) throw new Exception(r.errorMessage);
            for (String l : r.lines) {
                String[] x = l.split("\\|", -1);
                if (x.length >= 10) {
                    // Protocol: ID, Company, Role, Package, Min CGPA, Branches, Backlogs, Year, Skills, Deadline.
                    // The dashboard intentionally hides Skills and displays the real Deadline in the last column.
                    jobs.addRow(new Object[]{x[0], x[1], x[2], x[3], x[4], x[5], x[6], x[7], formatDeadline(x[9])});
                }
            }
        } catch (Exception ignored) {}
    }

    private void loadStudents() {
        students.setRowCount(0);
        try {
            SocketClient.ListResponse r;
            if (query) {
                String q = search == null ? "" : search.getText();
                String d = (String) dept.getSelectedItem();
                int y = num(year), m = num(back), s = num(sem);
                double c = decimal(cgpa, -1);
                String so = (String) sort.getSelectedItem();
                String[] z = so.split(" ");
                boolean asc = z.length > 1 && z[1].equals("ASC");
                r = client.sendListRequest("SEARCH_SORT_FILTER_STUDENTS", token, q, d, String.valueOf(y), String.valueOf(c), String.valueOf(m), String.valueOf(s), z[0], String.valueOf(asc));
            } else r = client.sendListRequest("GET_STUDENTS", token);
            if (!r.success) throw new Exception(r.errorMessage);
            for (String l : r.lines) {
                String[] x = l.split("\\|", -1);
                if (x.length >= 10) students.addRow(x);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unable to load students: " + e.getMessage(), "Server error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadNotes() {
        notes.setRowCount(0);
        try {
            SocketClient.ListResponse r = client.sendListRequest("GET_NOTIFICATIONS", token);
            if (!r.success) return;
            for (String l : r.lines) {
                String[] x = l.split("\\|", -1);
                if (x.length >= 7) notes.addRow(x);
            }
        } catch (Exception ignored) {}
    }

    private void loadFaculty() {
        faculty.setRowCount(0);
        try {
            SocketClient.ListResponse r = client.sendListRequest("GET_FACULTY", token);
            if (!r.success) throw new Exception(r.errorMessage);
            for (String l : r.lines) {
                String[] x = l.split("\\|", -1);
                if (x.length >= 6 && !"FACULTY".equalsIgnoreCase(x[3])) {
                    if (x[4] == null || x[4].isBlank()) x[4] = "No Department";
                    faculty.addRow(x);
                }
            }
        } catch (Exception e) { JOptionPane.showMessageDialog(this, e.getMessage()); }
    }

    private void addJob() {
        JTextField c = new JTextField(), ro = new JTextField(), pack = new JTextField(), cg = new JTextField("7"), br = new JTextField("CSE,IT"), ma = new JTextField("0"), dl = new JTextField("2026-12-31");
        JPanel p = new JPanel(new GridLayout(0, 2));
        add(p, "Company", c); add(p, "Role", ro); add(p, "Package", pack); add(p, "Min CGPA", cg); add(p, "Branches", br); add(p, "Max Backlogs", ma); add(p, "Deadline", dl);
        if (JOptionPane.showConfirmDialog(this, p, "Add Job", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            SocketClient.Response r = client.sendRequest("ADD_JOB", token, c.getText(), ro.getText(), pack.getText(), cg.getText(), br.getText(), ma.getText(), dl.getText());
            if (!r.success) throw new Exception(r.payload);
            loadJobs(); loadStats();
        } catch (Exception e) { JOptionPane.showMessageDialog(this, e.getMessage()); }
    }

    private void eligible() {
        int row = jobsT.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a job first."); return; }
        try {
            SocketClient.ListResponse r = client.sendListRequest("GET_ELIGIBLE_STUDENTS", token, String.valueOf(jobsT.getValueAt(row, 0)));
            if (!r.success) throw new Exception(r.errorMessage);
            JTextArea a = new JTextArea(String.join("\n", r.lines)); a.setEditable(false);
            JOptionPane.showMessageDialog(this, new JScrollPane(a), "Eligible Students", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) { JOptionPane.showMessageDialog(this, e.getMessage()); }
    }

    private void notifyAllStudents() {
        JTextField s = new JTextField(), m = new JTextField();
        JPanel p = new JPanel(new GridLayout(0, 2)); add(p, "Subject", s); add(p, "Message", m);
        if (JOptionPane.showConfirmDialog(this, p, "Send Notification", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            SocketClient.Response r = client.sendRequest("SEND_GENERAL_NOTIFICATION", token, s.getText(), m.getText(), "ALL");
            if (!r.success) throw new Exception(r.payload); loadNotes(); loadStats();
        } catch (Exception e) { JOptionPane.showMessageDialog(this, e.getMessage()); }
    }

    private void addFaculty() {
        JTextField n = new JTextField(), e = new JTextField(), p = new JTextField("pass123"), d = new JTextField(), ph = new JTextField();
        JComboBox<String> ro = box("TPO", "TPC", "DIRECTOR", "DEAN", "ADMIN");
        JPanel x = new JPanel(new GridLayout(0, 2)); add(x, "Name", n); add(x, "Email", e); add(x, "Password", p); x.add(new JLabel("Role")); x.add(ro); add(x, "Department", d); add(x, "Phone", ph);
        if (JOptionPane.showConfirmDialog(this, x, "Add User", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            SocketClient.Response r = client.sendRequest("ADD_FACULTY", token, n.getText(), e.getText(), p.getText(), (String) ro.getSelectedItem(), d.getText(), ph.getText());
            if (!r.success) throw new Exception(r.payload); loadFaculty();
        } catch (Exception z) { JOptionPane.showMessageDialog(this, z.getMessage()); }
    }

    private void addStudent() {
        JTextField prn = new JTextField(), n = new JTextField(), e = new JTextField(), p = new JTextField("pass1234");
        JComboBox<String> d = box("CSE", "IT", "ECE", "MECH", "CIVIL");
        JTextField cg = new JTextField("7.5"), py = new JTextField("2027"), bl = new JTextField("0"), sem = new JTextField("5"), ph = new JTextField(), skills = new JTextField();
        JPanel x = new JPanel(new GridLayout(0, 2));
        add(x, "PRN", prn); add(x, "Name", n); add(x, "Email", e); add(x, "Password", p); x.add(new JLabel("Department")); x.add(d);
        add(x, "CGPA", cg); add(x, "Passing Year", py); add(x, "Backlogs", bl); add(x, "Semester", sem); add(x, "Phone", ph); add(x, "Skills", skills);
        if (JOptionPane.showConfirmDialog(this, x, "Add Student", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            SocketClient.Response r = client.sendRequest("ADD_STUDENT", token, prn.getText(), n.getText(), e.getText(), p.getText(), (String) d.getSelectedItem(), cg.getText(), py.getText(), bl.getText(), sem.getText(), ph.getText(), skills.getText());
            if (!r.success) throw new Exception(r.payload);
            loadStudents(); loadStats(); JOptionPane.showMessageDialog(this, "Student added successfully.");
        } catch (Exception z) { JOptionPane.showMessageDialog(this, z.getMessage()); }
    }

    private void logout() { try { client.sendRequest("LOGOUT", token); } catch (Exception ignored) {} dispose(); new LoginFrame().setVisible(true); }

    private static String formatDeadline(String raw) {
        if (raw == null || raw.isBlank()) return "Not specified";
        try {
            LocalDate d = LocalDate.parse(raw, DATE), today = LocalDate.now();
            long days = ChronoUnit.DAYS.between(today, d);
            if (days == 0) return "Due today";
            if (days == 1) return "1 day left";
            if (days > 1) return days + " days left";
            if (days == -1) return "Expired 1 day ago";
            return "Expired " + Math.abs(days) + " days ago";
        } catch (Exception e) { return raw; }
    }

    private static JPanel page() { JPanel p = new JPanel(new BorderLayout(0, 10)); p.setBackground(BG()); p.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); return p; }
    private static Color BG() { return new Color(247, 248, 245); }
    private static Color DARK() { return new Color(20, 92, 48); }
    private static Color TEXT() { return new Color(38, 50, 43); }
    private static Color MUTED() { return new Color(105, 115, 108); }
    private static JLabel lbl(String s, int z, boolean b, Color c) { JLabel l = new JLabel(s); l.setFont(new Font("SansSerif", b ? Font.BOLD : Font.PLAIN, z)); l.setForeground(c); return l; }
    private static JButton btn(String s) { JButton b = new JButton(s); b.setFocusPainted(false); return b; }
    private static DefaultTableModel model(String[] c) { return new DefaultTableModel(c, 0) { public boolean isCellEditable(int r, int c) { return false; } }; }
    private static void style(JTable t) {
        t.setRowHeight(28); t.setAutoCreateRowSorter(true); t.setFillsViewportHeight(true);
        DefaultTableCellRenderer center = new DefaultTableCellRenderer(); center.setHorizontalAlignment(SwingConstants.CENTER);
        t.setDefaultRenderer(Object.class, center);
        t.getTableHeader().setDefaultRenderer(center);
    }
    private static JComboBox<String> box(String... x) { return new JComboBox<>(x); }
    private static int num(JComboBox<String> b) { String x = (String) b.getSelectedItem(); return x == null || x.equals("ALL") ? 0 : Integer.parseInt(x); }
    private static double decimal(JComboBox<String> b, double n) { String x = (String) b.getSelectedItem(); return x == null || x.equals("ALL") ? n : Double.parseDouble(x); }
    private static void add(JPanel p, String n, JComponent c) { p.add(new JLabel(n)); p.add(c); }
}
