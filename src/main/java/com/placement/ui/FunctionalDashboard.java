package com.placement.ui;
import java.util.ArrayList;
import java.util.List;
import com.placement.sockets.SocketClient;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.time.LocalDate;
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

    public FunctionalDashboard(String role, String name, String token) {
        this.role = role == null ? "" : role.trim().toUpperCase();
        this.name = name == null || name.isBlank() ? this.role : name;
        this.token = token;
        edit = this.role.equals("ADMIN") || this.role.equals("TPO") || this.role.equals("TPC");
        query = this.role.equals("ADMIN") || this.role.equals("TPC");
        admin = this.role.equals("ADMIN");
        setTitle("Placement Eligibility Portal - " + this.role + " Dashboard");
        setSize(1250, 760);
        setMinimumSize(new Dimension(1050, 650));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        build();
        refresh();
    }

    private void build() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG());
        root.add(side(), BorderLayout.WEST);

        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(BG());
        main.add(header(), BorderLayout.NORTH);
        content.setBackground(BG());
        content.add(dashboard(), "D");
        content.add(jobsPage(), "J");
        content.add(studentsPage(), "S");
        content.add(notesPage(), "N");
        content.add(reportPage(), "R");
        if (admin) content.add(adminPage(), "A");
        main.add(content, BorderLayout.CENTER);
        root.add(main, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel side() {
        JPanel p = new JPanel();
        p.setPreferredSize(new Dimension(220, 760));
        p.setBackground(DARK());
        p.setBorder(new EmptyBorder(28, 16, 18, 16));
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
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (card == null) {
            b.addActionListener(e -> logout());
        } else {
            b.addActionListener(e -> {
                cards.show(content, card);
                if (card.equals("J")) loadJobs();
                if (card.equals("S")) loadStudents();
                if (card.equals("N")) loadNotes();
                if (card.equals("A")) loadFaculty();
            });
        }
        p.add(b);
        p.add(Box.createVerticalStrut(4));
    }

    private JPanel header() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(18, 24, 18, 24));
        p.add(lbl(role + " Dashboard", 22, true, TEXT()), BorderLayout.WEST);
        p.add(lbl(name, 13, false, MUTED()), BorderLayout.EAST);
        return p;
    }

    private JPanel dashboard() {
        JPanel p = page();
        p.add(lbl("Welcome, " + name, 22, true, TEXT()), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 18));
        center.setOpaque(false);
        stats = lbl("Loading live statistics...", 16, true, TEXT());
        center.add(stats, BorderLayout.NORTH);

        JPanel info = new JPanel(new GridLayout(1, 2, 16, 0));
        info.setOpaque(false);
        info.add(card("Placement Overview", "Live data is loaded directly from the placement database."));
        info.add(card("Quick Guide", edit
                ? "Use Job Postings to manage drives and Students to review eligibility."
                : "Use Job Postings and Students to view placement information."));
        center.add(info, BorderLayout.CENTER);
        p.add(center, BorderLayout.CENTER);
        return p;
    }

    private JPanel card(String title, String text) {
        JPanel c = new JPanel(new BorderLayout(0, 8));
        c.setBackground(Color.WHITE);
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(215, 222, 216)),
                new EmptyBorder(18, 18, 18, 18)));
        c.add(lbl(title, 16, true, DARK()), BorderLayout.NORTH);
        c.add(lbl("<html><div style='width:360px'>" + text + "</div></html>", 13, false, MUTED()), BorderLayout.CENTER);
        return c;
    }

    private JPanel jobsPage() {
        JPanel p = page();
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
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
        p.add(new JScrollPane(jobsT), BorderLayout.CENTER);
        return p;
    }

    private JPanel studentsPage() {
        JPanel p = page();
        if (query) {
            JPanel f = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
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
            JButton a = btn("Apply"); a.addActionListener(e -> loadStudents());
            JButton c = btn("Clear");
            c.addActionListener(e -> {
                search.setText(""); dept.setSelectedIndex(0); year.setSelectedIndex(0); cgpa.setSelectedIndex(0);
                back.setSelectedIndex(0); sem.setSelectedIndex(0); sort.setSelectedIndex(0); loadStudents();
            });
            f.add(a); f.add(c);
            p.add(f, BorderLayout.NORTH);
        } else {
            p.add(lbl("Student Roster (view only)", 18, true, TEXT()), BorderLayout.NORTH);
        }
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
        p.add(lbl("Reports", 20, true, TEXT()), BorderLayout.NORTH);
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(lbl("Basic Placement Report", 16, true, DARK()), BorderLayout.NORTH);
        JTextArea report = new JTextArea();
        report.setEditable(false);
        report.setFont(new Font("SansSerif", Font.PLAIN, 14));
        report.setLineWrap(true);
        report.setWrapStyleWord(true);
        report.setBackground(Color.WHITE);
        report.setBorder(new EmptyBorder(16, 16, 16, 16));
        report.setText("Loading report...");
        body.add(report, BorderLayout.CENTER);
        JLabel fallback = lbl("Advanced analytics: planned for a future version. Basic live totals are shown here for now.", 12, false, MUTED());
        body.add(fallback, BorderLayout.SOUTH);
        p.add(body, BorderLayout.CENTER);

        try {
            SocketClient.Response r = client.sendRequest("GET_DASHBOARD_STATS", token);
            if (r.success) {
                String[] x = r.parts();
                if (x.length >= 4) {
                    report.setText("Jobs currently available: " + x[0] + "\n"
                            + "Pending eligible student records: " + x[1] + "\n"
                            + "Notifications sent: " + x[2] + "\n"
                            + "Registered students: " + x[3] + "\n\n"
                            + "This is the basic operational report. Advanced charts and historical analytics can be added later without changing the current workflow.");
                }
            }
        } catch (Exception ignored) {
            report.setText("Live report data is temporarily unavailable.\n\nAdvanced analytics are planned for a future version.");
        }
        return p;
    }

    private JPanel adminPage() {
        JPanel p = page();
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.setOpaque(false);
        JButton addUser = btn("Add User");
        addUser.addActionListener(e -> addFaculty());
        JButton addStudent = btn("Add Student");
        addStudent.addActionListener(e -> addStudent());
        JButton ref = btn("Refresh");
        ref.addActionListener(e -> loadFaculty());
        top.add(addUser); top.add(addStudent); top.add(ref);
        p.add(top, BorderLayout.NORTH);
        style(facultyT);
        p.add(new JScrollPane(facultyT), BorderLayout.CENTER);
        return p;
    }

    private void refresh() {
        loadStats(); loadJobs(); loadStudents(); loadNotes(); if (admin) loadFaculty();
    }

    private void loadStats() {
        try {
            SocketClient.Response r = client.sendRequest("GET_DASHBOARD_STATS", token);
            if (r.success) {
                String[] x = r.parts();
                if (x.length >= 4) stats.setText("Active Jobs: " + x[0] + "    |    Pending Eligible: " + x[1] + "    |    Notifications: " + x[2] + "    |    Students: " + x[3]);
            }
        } catch (Exception e) { stats.setText("Live statistics are temporarily unavailable."); }
    }

    private void loadJobs() {
        jobs.setRowCount(0);
        try {
            SocketClient.ListResponse r = client.sendListRequest("GET_JOBS", token);
            if (!r.success) throw new Exception(r.errorMessage);
            for (String l : r.lines) {
                String[] x = l.split("\\|", -1);
                if (x.length >= 10) {
                    // Protocol: ID, Company, Role, Package, CGPA, Branches, Backlogs, Year, Skills, Deadline.
                    jobs.addRow(new Object[]{x[0].isBlank() ? "-" : x[0], x[1], x[2], x[3], x[4], x[5], x[6], x[7], formatDeadline(x[9])});
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
                String so = (String) sort.getSelectedItem(); String[] z = so.split(" ");
                boolean asc = z.length > 1 && z[1].equals("ASC");
                r = client.sendListRequest("SEARCH_SORT_FILTER_STUDENTS", token, q, d, String.valueOf(y), String.valueOf(c), String.valueOf(m), String.valueOf(s), z[0], String.valueOf(asc));
            } else r = client.sendListRequest("GET_STUDENTS", token);
            if (!r.success) throw new Exception(r.errorMessage);
            for (String l : r.lines) { String[] x = l.split("\\|", -1); if (x.length >= 10) students.addRow(x); }
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Unable to load students: " + e.getMessage(), "Server error", JOptionPane.ERROR_MESSAGE); }
    }

    private void loadNotes() {
        notes.setRowCount(0);
        try {
            SocketClient.ListResponse r = client.sendListRequest("GET_NOTIFICATIONS", token);
            if (!r.success) return;
            for (String l : r.lines) { String[] x = l.split("\\|", -1); if (x.length >= 7) notes.addRow(x); }
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
                    if (x[4].isBlank()) x[4] = "No Department";
                    faculty.addRow(x);
                }
            }
        } catch (Exception e) { JOptionPane.showMessageDialog(this, e.getMessage()); }
    }

    private void addJob() {
        JTextField c = new JTextField(), ro = new JTextField(), pack = new JTextField(), cg = new JTextField("7"), br = new JTextField("CSE,IT"), ma = new JTextField("0"), dl = new JTextField("2026-12-31");
        JPanel p = new JPanel(new GridLayout(0, 2, 8, 8)); add(p, "Company", c); add(p, "Role", ro); add(p, "Package", pack); add(p, "Min CGPA", cg); add(p, "Branches", br); add(p, "Max Backlogs", ma); add(p, "Deadline", dl);
        if (JOptionPane.showConfirmDialog(this, p, "Add Job", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            SocketClient.Response r = client.sendRequest("ADD_JOB", token, c.getText(), ro.getText(), pack.getText(), cg.getText(), br.getText(), ma.getText(), dl.getText());
            if (!r.success) throw new Exception(r.payload); loadJobs(); loadStats();
        } catch (Exception e) { JOptionPane.showMessageDialog(this, e.getMessage()); }
    }

    private void eligible() {
        int row = jobsT.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a job first.", "Eligible Students", JOptionPane.WARNING_MESSAGE); return; }
        int modelRow = jobsT.convertRowIndexToModel(row);
        String jobId = String.valueOf(jobs.getValueAt(modelRow, 0));
        try {
            SocketClient.ListResponse r = client.sendListRequest("GET_ELIGIBLE_STUDENTS", token, jobId);
            if (!r.success) throw new Exception(r.errorMessage);

            DefaultTableModel m = model(new String[]{"PRN", "Name", "Email", "Department", "CGPA", "Year", "Backlogs", "Semester", "Phone", "Skills"});
            for (String line : r.lines) { String[] x = line.split("\\|", -1); if (x.length >= 10) m.addRow(x); }
            JTable table = new JTable(m);
            style(table);
            table.setAutoCreateRowSorter(true);
            table.setPreferredScrollableViewportSize(new Dimension(1000, Math.min(360, Math.max(120, table.getRowCount() * 28 + 35))));

            JPanel panel = new JPanel(new BorderLayout(0, 10));
            panel.setBorder(new EmptyBorder(8, 8, 8, 8));
            panel.add(lbl("Eligible Students — " + jobId, 16, true, DARK()), BorderLayout.NORTH);
            panel.add(new JScrollPane(table), BorderLayout.CENTER);
            panel.add(lbl(table.getRowCount() + " eligible student(s) found", 12, false, MUTED()), BorderLayout.SOUTH);
            JOptionPane.showMessageDialog(this, panel, "Eligible Students", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) { JOptionPane.showMessageDialog(this, e.getMessage(), "Eligible Students", JOptionPane.ERROR_MESSAGE); }
    }

    private void notifyAllStudents() {
        int row = jobsT.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a job first so the notification can be linked to its Job ID.", "Send Notification", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = jobsT.convertRowIndexToModel(row);
        String jobId = String.valueOf(jobs.getValueAt(modelRow, 0));
        try {
            SocketClient.ListResponse er = client.sendListRequest("GET_ELIGIBLE_STUDENTS", token, jobId);
            if (!er.success) throw new Exception(er.errorMessage);
            List<String> prns = new ArrayList<>();
            for (String line : er.lines) {
                String[] x = line.split("\\|", -1);
                if (x.length > 0 && !x[0].isBlank()) prns.add(x[0]);
            }
            if (prns.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No eligible students are available for " + jobId + ".", "Send Notification", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            JTextField subject = new JTextField("Placement Opportunity");
            JTextField message = new JTextField("You are eligible to apply for " + jobId + ".");
            JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
            add(panel, "Job ID", new JLabel(jobId));
            add(panel, "Subject", subject);
            add(panel, "Message", message);
            add(panel, "Recipients", new JLabel(String.valueOf(prns.size()) + " eligible students"));
            if (JOptionPane.showConfirmDialog(this, panel, "Send Notification", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
            SocketClient.Response r = client.sendRequest("SEND_NOTIFICATION", token, jobId, String.join(",", prns));
            if (!r.success) throw new Exception(r.payload);
            loadNotes(); loadStats();
            JOptionPane.showMessageDialog(this, "Notification sent for " + jobId + ".", "Notification Sent", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Send Notification", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addStudent() {
        JTextField prn = new JTextField();
        JTextField name = new JTextField();
        JTextField email = new JTextField();
        JPasswordField password = new JPasswordField("pass1234");
        JComboBox<String> department = box("CSE", "IT", "ECE", "MECH", "CIVIL");
        JTextField cgpa = new JTextField("7.0");
        JTextField year = new JTextField("2027");
        JTextField backlogs = new JTextField("0");
        JTextField semester = new JTextField("5");
        JTextField phone = new JTextField();
        JTextField skills = new JTextField("Java,SQL,Python");

        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        add(panel, "PRN (optional)", prn);
        add(panel, "Name", name);
        add(panel, "Email", email);
        add(panel, "Password", password);
        panel.add(new JLabel("Department")); panel.add(department);
        add(panel, "CGPA", cgpa);
        add(panel, "Passing Year", year);
        add(panel, "Backlogs", backlogs);
        add(panel, "Semester", semester);
        add(panel, "Phone", phone);
        add(panel, "Skills", skills);

        if (JOptionPane.showConfirmDialog(this, panel, "Add Student", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            SocketClient.Response r = client.sendRequest("ADD_STUDENT", token, prn.getText().trim(), name.getText().trim(), email.getText().trim(),
                    new String(password.getPassword()), (String) department.getSelectedItem(), cgpa.getText().trim(), year.getText().trim(),
                    backlogs.getText().trim(), semester.getText().trim(), phone.getText().trim(), skills.getText().trim());
            if (!r.success) throw new Exception(r.payload);
            loadStudents(); loadStats();
            JOptionPane.showMessageDialog(this, "Student added successfully.", "Add Student", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Add Student", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addFaculty() {
        JTextField n = new JTextField(), e = new JTextField(), p = new JTextField("pass123"), d = new JTextField(), ph = new JTextField();
        JComboBox<String> ro = box("TPO", "TPC", "DIRECTOR", "DEAN", "ADMIN");
        JPanel x = new JPanel(new GridLayout(0, 2, 8, 8)); add(x, "Name", n); add(x, "Email", e); add(x, "Password", p); x.add(new JLabel("Role")); x.add(ro); add(x, "Department", d); add(x, "Phone", ph);
        if (JOptionPane.showConfirmDialog(this, x, "Add User", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            SocketClient.Response r = client.sendRequest("ADD_FACULTY", token, n.getText().trim(), e.getText().trim(), p.getText(), (String) ro.getSelectedItem(), d.getText().trim(), ph.getText().trim());
            if (!r.success) throw new Exception(r.payload);
            loadFaculty();
            JOptionPane.showMessageDialog(this, "User added successfully.\nThey can now log in using the selected role and password.", "User Added", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception z) { JOptionPane.showMessageDialog(this, z.getMessage(), "Add User", JOptionPane.ERROR_MESSAGE); }
    }

    private void logout() { try { if (token != null) client.sendRequest("LOGOUT", token); } catch (Exception ignored) {} dispose(); new LoginFrame().setVisible(true); }

    private static String formatDeadline(String value) {
        if (value == null || value.isBlank()) return "-";
        try {
            LocalDate d = LocalDate.parse(value.length() >= 10 ? value.substring(0, 10) : value);
            long days = ChronoUnit.DAYS.between(LocalDate.now(), d);
            if (days == 0) return "Due today";
            if (days == 1) return "1 day left";
            if (days > 1) return days + " days left";
            long late = Math.abs(days);
            return late == 1 ? "Expired 1 day ago" : "Expired " + late + " days ago";
        } catch (Exception e) { return value; }
    }

    private static JPanel page() { JPanel p = new JPanel(new BorderLayout(0, 12)); p.setBackground(BG()); p.setBorder(new EmptyBorder(20, 20, 20, 20)); return p; }
    private static Color BG() { return new Color(247, 248, 245); }
    private static Color DARK() { return new Color(20, 92, 48); }
    private static Color TEXT() { return new Color(38, 50, 43); }
    private static Color MUTED() { return new Color(105, 115, 108); }
    private static JLabel lbl(String s, int z, boolean b, Color c) { JLabel l = new JLabel(s); l.setFont(new Font("SansSerif", b ? Font.BOLD : Font.PLAIN, z)); l.setForeground(c); return l; }
    private static JButton btn(String s) { JButton b = new JButton(s); b.setFocusPainted(false); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); return b; }
    private static DefaultTableModel model(String[] c) { return new DefaultTableModel(c, 0) { public boolean isCellEditable(int r, int c) { return false; } }; }
    private static void style(JTable t) {
        t.setRowHeight(30); t.setAutoCreateRowSorter(true); t.setFillsViewportHeight(true); t.setShowGrid(true);
        t.setGridColor(new Color(205, 212, 207)); t.setIntercellSpacing(new Dimension(1, 1));
        t.setSelectionBackground(new Color(205, 224, 211)); t.setSelectionForeground(TEXT());
        DefaultTableCellRenderer center = new DefaultTableCellRenderer(); center.setHorizontalAlignment(SwingConstants.CENTER);
        t.setDefaultRenderer(Object.class, center);
        JTableHeader h = t.getTableHeader(); h.setReorderingAllowed(false); h.setPreferredSize(new Dimension(h.getPreferredSize().width, 32));
        h.setDefaultRenderer(new DefaultTableCellRenderer() {{ setHorizontalAlignment(SwingConstants.CENTER); setOpaque(true); setBackground(new Color(232, 238, 234)); setForeground(TEXT()); setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(170, 180, 173))); }});
    }
    private static JComboBox<String> box(String... x) { return new JComboBox<>(x); }
    private static int num(JComboBox<String> b) { String x = (String) b.getSelectedItem(); return x == null || x.equals("ALL") ? 0 : Integer.parseInt(x); }
    private static double decimal(JComboBox<String> b, double n) { String x = (String) b.getSelectedItem(); return x == null || x.equals("ALL") ? n : Double.parseDouble(x); }
    private static void add(JPanel p, String n, JComponent c) { p.add(new JLabel(n)); p.add(c); }
}
