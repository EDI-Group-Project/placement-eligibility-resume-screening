package com.placement.ui;

import com.placement.sockets.SocketClient;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class StudentDashboard extends JFrame {
    private final String name, email, token;
    private final SocketClient client = new SocketClient();
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final DefaultTableModel jobs = model(new String[]{"Job ID", "Company", "Role", "Package", "Min CGPA", "Branches", "Backlogs", "Year", "Deadline"});
    private final DefaultTableModel apps = model(new String[]{"Job ID", "Company", "Role", "Status", "Score", "Applied At"});
    private final DefaultTableModel notes = model(new String[]{"ID", "Job ID", "Subject", "Message", "Recipients", "Created"});
    private final JTable jobsT = new JTable(jobs);
    private JLabel dashboardStats;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    public StudentDashboard(String name, String email, String token) {
        this.name = name == null || name.isBlank() ? "Student" : name;
        this.email = email;
        this.token = token;
        setTitle("Placement Eligibility Portal - Student Dashboard");
        setSize(1200, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        build();
        refresh();
    }

    public StudentDashboard(String name, String email) { this(name, email, null); }
    public StudentDashboard() { this("Student", "", null); }

    private void build() {
        JPanel root = new JPanel(new BorderLayout());
        JPanel side = new JPanel();
        side.setPreferredSize(new Dimension(210, 720));
        side.setBackground(new Color(20, 92, 48));
        side.setBorder(BorderFactory.createEmptyBorder(28, 16, 18, 16));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        JLabel a = new JLabel("PLACEMENT"), b = new JLabel("ELIGIBILITY PORTAL");
        a.setForeground(Color.WHITE); a.setFont(new Font("SansSerif", Font.BOLD, 22));
        b.setForeground(new Color(220, 245, 225)); b.setFont(new Font("SansSerif", Font.PLAIN, 11));
        side.add(a); side.add(b); side.add(Box.createVerticalStrut(28));
        nav(side, "Dashboard", "D"); nav(side, "Placement Drives", "J"); nav(side, "My Applications", "A"); nav(side, "Notifications", "N"); nav(side, "My Profile", "P");
        side.add(Box.createVerticalGlue()); nav(side, "Logout", null); root.add(side, BorderLayout.WEST);

        JPanel main = new JPanel(new BorderLayout());
        JPanel head = new JPanel(new BorderLayout()); head.setBackground(Color.WHITE); head.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        JLabel t = new JLabel("Student Dashboard"); t.setFont(new Font("SansSerif", Font.BOLD, 22)); head.add(t, BorderLayout.WEST); head.add(new JLabel(name), BorderLayout.EAST); main.add(head, BorderLayout.NORTH);
        content.setBackground(new Color(247, 248, 245));
        content.add(dashboard(), "D"); content.add(jobsPage(), "J"); content.add(appsPage(), "A"); content.add(notesPage(), "N"); content.add(profile(), "P");
        main.add(content); root.add(main); setContentPane(root);
    }

    private void nav(JPanel p, String text, String card) {
        JButton b = new JButton(text); b.setMaximumSize(new Dimension(178, 40)); b.setAlignmentX(Component.LEFT_ALIGNMENT); b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setForeground(Color.WHITE); b.setBackground(new Color(20, 92, 48)); b.setBorderPainted(false); b.setFocusPainted(false);
        if (card == null) b.addActionListener(e -> logout());
        else b.addActionListener(e -> { cards.show(content, card); if (card.equals("J")) loadJobs(); if (card.equals("A")) loadApps(); if (card.equals("N")) loadNotes(); });
        p.add(b); p.add(Box.createVerticalStrut(4));
    }

    private JPanel dashboard() {
        JPanel p = page();
        JPanel body = new JPanel(); body.setOpaque(false); body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        JLabel welcome = label("Welcome, " + name, 22, true); welcome.setAlignmentX(Component.LEFT_ALIGNMENT); body.add(welcome);
        body.add(Box.createVerticalStrut(22));
        dashboardStats = label("Loading your placement activity...", 16, true); dashboardStats.setAlignmentX(Component.LEFT_ALIGNMENT); body.add(dashboardStats);
        body.add(Box.createVerticalStrut(18));
        JLabel info = label("Browse current placement drives, apply to eligible jobs and track your applications.", 14, false); info.setAlignmentX(Component.LEFT_ALIGNMENT); body.add(info);
        p.add(body, BorderLayout.NORTH); return p;
    }

    private JPanel jobsPage() {
        JPanel p = page(); JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT)); top.setOpaque(false);
        JButton r = button("Refresh"), a = button("Apply to Selected Job"); r.addActionListener(e -> loadJobs()); a.addActionListener(e -> apply()); top.add(r); top.add(a); p.add(top, BorderLayout.NORTH);
        style(jobsT); p.add(new JScrollPane(jobsT)); return p;
    }

    private JPanel appsPage() { JPanel p = page(); JButton r = button("Refresh"); r.addActionListener(e -> loadApps()); p.add(r, BorderLayout.NORTH); JTable t = new JTable(apps); style(t); p.add(new JScrollPane(t)); return p; }
    private JPanel notesPage() { JPanel p = page(); JButton r = button("Refresh"); r.addActionListener(e -> loadNotes()); p.add(r, BorderLayout.NORTH); JTable t = new JTable(notes); style(t); p.add(new JScrollPane(t)); return p; }

    private JPanel profile() {
        JPanel p = page(); JPanel f = new JPanel(new GridLayout(0, 2, 8, 8)); f.setBackground(Color.WHITE);
        JTextField n = new JTextField(name), e = new JTextField(email == null ? "" : email); n.setEditable(false); e.setEditable(false);
        f.add(new JLabel("Name")); f.add(n); f.add(new JLabel("Login ID")); f.add(e); p.add(f, BorderLayout.NORTH); return p;
    }

    private void refresh() { loadJobs(); loadApps(); loadNotes(); updateDashboardStats(); }

    private void updateDashboardStats() {
        int jobCount = jobs.getRowCount(), appCount = apps.getRowCount(), noteCount = notes.getRowCount();
        if (dashboardStats != null) dashboardStats.setText("Available Jobs: " + jobCount + "    My Applications: " + appCount + "    Notifications: " + noteCount);
    }

    private void loadJobs() {
        jobs.setRowCount(0);
        try {
            SocketClient.ListResponse r = client.sendListRequest("GET_JOBS", token); if (!r.success) throw new Exception(r.errorMessage);
            for (String l : r.lines) { String[] x = l.split("\\|", -1); if (x.length >= 10) jobs.addRow(new Object[]{x[0], x[1], x[2], x[3], x[4], x[5], x[6], x[7], formatDeadline(x[9])}); }
            updateDashboardStats();
        } catch (Exception ignored) {}
    }

    private void loadApps() {
        apps.setRowCount(0);
        try { SocketClient.ListResponse r = client.sendListRequest("GET_MY_APPLICATIONS", token); if (!r.success) return; for (String l : r.lines) { String[] x = l.split("\\|", -1); if (x.length >= 6) apps.addRow(x); } updateDashboardStats(); } catch (Exception ignored) {}
    }

    private void loadNotes() {
        notes.setRowCount(0);
        try { SocketClient.ListResponse r = client.sendListRequest("GET_NOTIFICATIONS", token); if (!r.success) return; for (String l : r.lines) { String[] x = l.split("\\|", -1); if (x.length >= 7) notes.addRow(new Object[]{x[0], x[1], x[2], x[3], x[5], x[6]}); } updateDashboardStats(); } catch (Exception ignored) {}
    }

    private void apply() {
        int row = jobsT.getSelectedRow(); if (row < 0) { JOptionPane.showMessageDialog(this, "Select a job first."); return; }
        try { SocketClient.Response r = client.sendRequest("APPLY_JOB", token, String.valueOf(jobsT.getValueAt(row, 0))); if (!r.success) throw new Exception(r.payload); JOptionPane.showMessageDialog(this, "Application submitted successfully."); loadApps(); } catch (Exception e) { JOptionPane.showMessageDialog(this, e.getMessage()); }
    }

    private void logout() { try { client.sendRequest("LOGOUT", token); } catch (Exception ignored) {} dispose(); new LoginFrame().setVisible(true); }

    private static String formatDeadline(String raw) {
        if (raw == null || raw.isBlank()) return "Not specified";
        try { LocalDate d = LocalDate.parse(raw, DATE), today = LocalDate.now(); long days = ChronoUnit.DAYS.between(today, d); if (days == 0) return "Due today"; if (days == 1) return "1 day left"; if (days > 1) return days + " days left"; if (days == -1) return "Expired 1 day ago"; return "Expired " + Math.abs(days) + " days ago"; }
        catch (Exception e) { return raw; }
    }

    private static JPanel page() { JPanel p = new JPanel(new BorderLayout(0, 12)); p.setBackground(new Color(247, 248, 245)); p.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); return p; }
    private static JLabel label(String s, int z, boolean b) { JLabel l = new JLabel(s); l.setFont(new Font("SansSerif", b ? Font.BOLD : Font.PLAIN, z)); l.setForeground(new Color(38, 50, 43)); return l; }
    private static JButton button(String s) { JButton b = new JButton(s); b.setFocusPainted(false); return b; }
    private static DefaultTableModel model(String[] c) { return new DefaultTableModel(c, 0) { public boolean isCellEditable(int r, int c) { return false; } }; }
    private static void style(JTable t) { t.setRowHeight(28); t.setAutoCreateRowSorter(true); t.setFillsViewportHeight(true); DefaultTableCellRenderer center = new DefaultTableCellRenderer(); center.setHorizontalAlignment(SwingConstants.CENTER); t.setDefaultRenderer(Object.class, center); t.getTableHeader().setDefaultRenderer(center); }
}
