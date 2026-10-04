package com.placement.ui;

import com.placement.sockets.SocketClient;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;

/**
 * Student Dashboard.
 *
 * Commands already supported by the server today: GET_JOBS, APPLY_JOB,
 * GET_MY_APPLICATIONS, GET_NOTIFICATIONS, LOGOUT.
 *
 * Commands this screen also calls that the backend does not implement yet
 * (tracked separately with the backend team): GET_MY_PROFILE, UPDATE_MY_PROFILE,
 * WITHDRAW_APPLICATION, UPLOAD_RESUME, GET_RESUME. Every call to one of these
 * degrades gracefully — on "FAILED"/"Unknown command" the affected feature
 * falls back to its pre-backend behaviour (e.g. the jobs list simply stays
 * unfiltered) instead of breaking the screen. Once the backend adds a given
 * command, the matching feature activates with no further frontend changes.
 *
 * Proposed wire formats for the new commands (for the backend team):
 *   GET_MY_PROFILE    req: token
 *                     res: SUCCESS|prn|name|email|department|cgpa|passingYear|backlogs|semester|phone|skills
 *   UPDATE_MY_PROFILE req: token, phone, skills
 *                     res: SUCCESS|Updated. / FAILED|reason
 *   WITHDRAW_APPLICATION req: token, jobId
 *                     res: SUCCESS|Withdrawn. / FAILED|reason
 *   UPLOAD_RESUME     req: token, filename, base64Content
 *                     res: SUCCESS|Uploaded. / FAILED|reason
 *   GET_RESUME        req: token
 *                     res: SUCCESS|filename|base64Content / FAILED|No resume uploaded.
 */
public class StudentDashboard extends JFrame {

    private final String name, email, token;
    private final SocketClient client = new SocketClient();
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);

    // --- The student's own profile, fetched via GET_MY_PROFILE. -----------
    // Used for the Profile tab and for client-side eligibility filtering in
    // Placement Drives. Until the backend ships GET_MY_PROFILE this stays
    // unloaded and every feature that depends on it falls back gracefully.
    private boolean profileLoaded = false;
    private String profilePrn = "", profileDept = "", profileSkills = "", profilePhone = "";
    private double profileCgpa = -1;
    private int profilePassingYear = -1, profileBacklogs = -1, profileSemester = -1;

    // Raw rows from the last GET_JOBS call (unfiltered), re-used so changing
    // a search/filter/sort control never needs a fresh server round trip.
    private final List<String[]> allJobs = new ArrayList<>();

    // Notification read state. Client-side only for now (no backend
    // persistence exists yet) — resets whenever the app restarts.
    private final Set<String> readNotifications = new HashSet<>();

    private final DefaultTableModel jobs = model(new String[]{
            "Job ID", "Company", "Role", "Package", "Min CGPA", "Branches", "Backlogs", "Year", "Required Skills", "Deadline"});
    private final DefaultTableModel apps = model(new String[]{
            "Job ID", "Company", "Role", "Status", "Score", "Applied At"});
    private final DefaultTableModel notes = model(new String[]{
            "Status", "ID", "Job ID", "Subject", "Message", "Recipients", "Created"});

    private final JTable jobsT = new JTable(jobs), appsT = new JTable(apps), notesT = new JTable(notes);

    private JTextField search;
    private JComboBox<String> branchFilter, sortBox;
    private JLabel statOpen, statApplied, statSelected;

    private JTextField prnField, nameField, emailField, deptField, cgpaField, yearField, backlogsField, semField;
    private JTextField phoneField, skillsField;
    private JLabel resumeStatus;
    private String resumeFileName = null;

    public StudentDashboard(String name, String email, String token) {
        this.name = name == null || name.isBlank() ? "Student" : name;
        this.email = email;
        this.token = token;
        setTitle("Placement Eligibility Portal - Student Dashboard");
        setSize(1250, 760);
        setMinimumSize(new Dimension(1050, 650));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        build();
        refresh();
    }

    public StudentDashboard(String name, String email) { this(name, email, null); }
    public StudentDashboard() { this("Student", "", null); }

    // =========================================================
    // LAYOUT
    // =========================================================

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
        content.add(appsPage(), "A");
        content.add(notesPage(), "N");
        content.add(profilePage(), "P");
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
        nav(p, "Placement Drives", "J");
        nav(p, "My Applications", "A");
        nav(p, "Notifications", "N");
        nav(p, "My Profile", "P");
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
                if (card.equals("A")) loadApps();
                if (card.equals("N")) loadNotes();
                if (card.equals("P")) loadProfile();
            });
        }
        p.add(b);
        p.add(Box.createVerticalStrut(4));
    }

    private JPanel header() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(18, 24, 18, 24));
        p.add(lbl("Student Dashboard", 22, true, TEXT()), BorderLayout.WEST);
        p.add(lbl(name, 13, false, MUTED()), BorderLayout.EAST);
        return p;
    }

    // =========================================================
    // DASHBOARD (home)
    // =========================================================

    private JPanel dashboard() {
        JPanel p = page();
        p.add(lbl("Welcome, " + name, 22, true, TEXT()), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 18));
        center.setOpaque(false);

        JPanel statsRow = new JPanel(new GridLayout(1, 3, 16, 0));
        statsRow.setOpaque(false);
        statOpen = lbl("-", 28, true, DARK());
        statApplied = lbl("-", 28, true, DARK());
        statSelected = lbl("-", 28, true, DARK());
        statsRow.add(statCard("Drives You Can Apply To", statOpen));
        statsRow.add(statCard("Applications Submitted", statApplied));
        statsRow.add(statCard("Offers / Selected", statSelected));
        center.add(statsRow, BorderLayout.NORTH);

        JPanel info = new JPanel(new GridLayout(1, 2, 16, 0));
        info.setOpaque(false);
        info.add(card("Placement Overview", "Live data is loaded directly from the placement database."));
        info.add(card("Quick Guide", "Use Placement Drives to view and apply to eligible jobs, and track progress under My Applications."));
        center.add(info, BorderLayout.CENTER);

        p.add(center, BorderLayout.CENTER);
        return p;
    }

    private JPanel statCard(String title, JLabel valueLabel) {
        JPanel c = new JPanel(new BorderLayout(0, 6));
        c.setBackground(Color.WHITE);
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(215, 222, 216)),
                new EmptyBorder(16, 18, 16, 18)));
        c.add(lbl(title, 13, false, MUTED()), BorderLayout.NORTH);
        c.add(valueLabel, BorderLayout.CENTER);
        return c;
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

    private void updateStats() {
        statOpen.setText(String.valueOf(jobs.getRowCount()));
        statApplied.setText(String.valueOf(apps.getRowCount()));
        int selected = 0;
        for (int i = 0; i < apps.getRowCount(); i++) {
            Object status = apps.getValueAt(i, 3);
            if (status != null && status.toString().toUpperCase().contains("SELECT")) selected++;
        }
        statSelected.setText(String.valueOf(selected));
    }

    // =========================================================
    // PLACEMENT DRIVES
    // =========================================================

    private JPanel jobsPage() {
        JPanel p = page();

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        top.setOpaque(false);
        search = new JTextField(12);
        branchFilter = box("ALL", "CSE", "IT", "ECE", "MECH", "CIVIL");
        sortBox = box("Deadline (Soonest)", "Company (A-Z)", "Min CGPA (Low-High)", "Min CGPA (High-Low)");
        top.add(new JLabel("Search Company")); top.add(search);
        top.add(new JLabel("Branch")); top.add(branchFilter);
        top.add(new JLabel("Sort")); top.add(sortBox);
        JButton applyFilter = btn("Apply"); applyFilter.addActionListener(e -> applyFilters());
        JButton clear = btn("Clear");
        clear.addActionListener(e -> {
            search.setText(""); branchFilter.setSelectedIndex(0); sortBox.setSelectedIndex(0); applyFilters();
        });
        JButton refresh = btn("Refresh"); refresh.addActionListener(e -> loadJobs());
        JButton applyJob = btn("Apply to Selected Job"); applyJob.addActionListener(e -> apply());
        top.add(applyFilter); top.add(clear); top.add(refresh); top.add(applyJob);
        p.add(top, BorderLayout.NORTH);

        style(jobsT);
        p.add(new JScrollPane(jobsT), BorderLayout.CENTER);

        JLabel note = lbl("Showing only drives you currently qualify for. If this list ever looks wrong, it means the server does not yet support profile lookups and all open drives are shown instead.", 11, false, MUTED());
        p.add(note, BorderLayout.SOUTH);
        return p;
    }

    private void loadJobs() {
        allJobs.clear();
        try {
            SocketClient.ListResponse r = client.sendListRequest("GET_JOBS", token);
            if (!r.success) throw new Exception(r.errorMessage);
            for (String l : r.lines) {
                String[] x = l.split("\\|", -1);
                if (x.length >= 10) allJobs.add(x);
            }
        } catch (Exception ignored) {}
        if (!profileLoaded) loadProfile();
        applyFilters();
    }

    private void applyFilters() {
        jobs.setRowCount(0);
        String companyQuery = search == null ? "" : search.getText().trim().toLowerCase();
        String branch = branchFilter == null ? "ALL" : (String) branchFilter.getSelectedItem();
        String sort = sortBox == null ? "Deadline (Soonest)" : (String) sortBox.getSelectedItem();

        List<String[]> rows = new ArrayList<>();
        for (String[] x : allJobs) {
            if (!isEligible(x)) continue;
            if (!companyQuery.isBlank() && !x[1].toLowerCase().contains(companyQuery)) continue;
            if (branch != null && !branch.equals("ALL") && !branchListContains(x[5], branch)) continue;
            rows.add(x);
        }

        rows.sort((a, b) -> {
            if ("Company (A-Z)".equals(sort)) return a[1].compareToIgnoreCase(b[1]);
            if ("Min CGPA (Low-High)".equals(sort)) return Double.compare(parseD(a[4]), parseD(b[4]));
            if ("Min CGPA (High-Low)".equals(sort)) return Double.compare(parseD(b[4]), parseD(a[4]));
            return deadlineRank(a[9]).compareTo(deadlineRank(b[9])); // Deadline (Soonest), default
        });

        for (String[] x : rows) {
            jobs.addRow(new Object[]{x[0].isBlank() ? "-" : x[0], x[1], x[2], x[3], x[4], x[5], x[6], x[7], x[8].isBlank() ? "-" : x[8], formatDeadline(x[9])});
        }
        updateStats();
    }

    /**
     * Mirrors the server's own eligibility rule (EligibilityDAO.findEligible):
     * cgpa >= min, backlogs <= max, passing year matches exactly, department
     * is one of the job's allowed branches, and (if the job names required
     * skills) the student has every one of them.
     *
     * Falls back to "eligible" for every job when the student's own profile
     * has not been loaded yet (GET_MY_PROFILE not available from the backend
     * today), so the list simply shows everything until that command ships.
     */
    private boolean isEligible(String[] job) {
        if (!profileLoaded) return true;
        double jobMinCgpa = parseD(job[4]);
        int jobMaxBacklogs = parseI(job[6]);
        int jobYear = parseI(job[7]);
        if (profileCgpa < jobMinCgpa) return false;
        if (profileBacklogs > jobMaxBacklogs) return false;
        if (jobYear != 0 && profilePassingYear != jobYear) return false;
        if (!branchListContains(job[5], profileDept)) return false;
        return skillsMatch(profileSkills, job[8]);
    }

    private boolean branchListContains(String csv, String branch) {
        if (csv == null || csv.isBlank() || branch == null || branch.isBlank()) return true;
        for (String b : csv.split(",")) if (b.trim().equalsIgnoreCase(branch.trim())) return true;
        return false;
    }

    private boolean skillsMatch(String studentSkillsCsv, String requiredSkillsCsv) {
        if (requiredSkillsCsv == null || requiredSkillsCsv.isBlank()) return true;
        Set<String> have = new HashSet<>();
        if (studentSkillsCsv != null) for (String s : studentSkillsCsv.split(",")) have.add(s.trim().toLowerCase());
        for (String req : requiredSkillsCsv.split(",")) {
            if (req.isBlank()) continue;
            if (!have.contains(req.trim().toLowerCase())) return false;
        }
        return true;
    }

    private void apply() {
        int row = jobsT.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a job first."); return; }
        int modelRow = jobsT.convertRowIndexToModel(row);
        try {
            SocketClient.Response r = client.sendRequest("APPLY_JOB", token, String.valueOf(jobs.getValueAt(modelRow, 0)));
            if (!r.success) throw new Exception(r.payload);
            JOptionPane.showMessageDialog(this, "Application submitted successfully.");
            loadApps();
        } catch (Exception e) { JOptionPane.showMessageDialog(this, e.getMessage()); }
    }

    // =========================================================
    // MY APPLICATIONS
    // =========================================================

    private JPanel appsPage() {
        JPanel p = page();
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.setOpaque(false);
        JButton r = btn("Refresh"); r.addActionListener(e -> loadApps());
        JButton w = btn("Withdraw Selected"); w.addActionListener(e -> withdraw());
        top.add(r); top.add(w);
        p.add(top, BorderLayout.NORTH);
        style(appsT);
        p.add(new JScrollPane(appsT), BorderLayout.CENTER);
        return p;
    }

    private void loadApps() {
        apps.setRowCount(0);
        try {
            SocketClient.ListResponse r = client.sendListRequest("GET_MY_APPLICATIONS", token);
            if (!r.success) return;
            for (String l : r.lines) {
                String[] x = l.split("\\|", -1);
                if (x.length >= 6) apps.addRow(x);
            }
        } catch (Exception ignored) {}
        updateStats();
    }

    private void withdraw() {
        int row = appsT.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select an application first."); return; }
        int modelRow = appsT.convertRowIndexToModel(row);
        String status = String.valueOf(apps.getValueAt(modelRow, 3));
        if (!status.equalsIgnoreCase("APPLIED")) {
            JOptionPane.showMessageDialog(this, "Only a pending application can be withdrawn.");
            return;
        }
        String jobId = String.valueOf(apps.getValueAt(modelRow, 0));
        if (JOptionPane.showConfirmDialog(this, "Withdraw your application for " + jobId + "?", "Withdraw Application", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            SocketClient.Response r = client.sendRequest("WITHDRAW_APPLICATION", token, jobId);
            if (!r.success) throw new Exception(r.payload);
            JOptionPane.showMessageDialog(this, "Application withdrawn.");
            loadApps();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unable to withdraw: " + e.getMessage()
                    + "\n(This action needs the WITHDRAW_APPLICATION command, which the server may not support yet.)");
        }
    }

    // =========================================================
    // NOTIFICATIONS
    // =========================================================

    private JPanel notesPage() {
        JPanel p = page();
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.setOpaque(false);
        JButton r = btn("Refresh"); r.addActionListener(e -> loadNotes());
        JButton m = btn("Mark as Read"); m.addActionListener(e -> markRead());
        top.add(r); top.add(m);
        p.add(top, BorderLayout.NORTH);
        style(notesT);
        notesT.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setHorizontalAlignment(SwingConstants.CENTER);
                int modelRow = t.convertRowIndexToModel(row);
                boolean unread = "Unread".equals(t.getModel().getValueAt(modelRow, 0));
                c.setFont(c.getFont().deriveFont(unread ? Font.BOLD : Font.PLAIN));
                return c;
            }
        });
        p.add(new JScrollPane(notesT), BorderLayout.CENTER);
        return p;
    }

    private void loadNotes() {
        notes.setRowCount(0);
        try {
            SocketClient.ListResponse r = client.sendListRequest("GET_NOTIFICATIONS", token);
            if (!r.success) return;
            for (String l : r.lines) {
                String[] x = l.split("\\|", -1);
                if (x.length >= 7) {
                    String id = x[0];
                    String status = readNotifications.contains(id) ? "Read" : "Unread";
                    notes.addRow(new Object[]{status, id, x[1], x[2], x[3], x[5], x[6]});
                }
            }
        } catch (Exception ignored) {}
    }

    private void markRead() {
        int row = notesT.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a notification first."); return; }
        int modelRow = notesT.convertRowIndexToModel(row);
        String id = String.valueOf(notes.getValueAt(modelRow, 1));
        readNotifications.add(id);
        notes.setValueAt("Read", modelRow, 0);
        notesT.repaint();
    }

    // =========================================================
    // MY PROFILE
    // =========================================================

    private JPanel profilePage() {
        JPanel p = page();
        JPanel outer = new JPanel(new BorderLayout(0, 18));
        outer.setOpaque(false);

        JPanel viewCard = new JPanel(new GridLayout(0, 2, 10, 10));
        viewCard.setBackground(Color.WHITE);
        viewCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(215, 222, 216)), new EmptyBorder(18, 18, 18, 18)));
        prnField = readOnlyField(); nameField = readOnlyField(); emailField = readOnlyField();
        deptField = readOnlyField(); cgpaField = readOnlyField(); yearField = readOnlyField();
        backlogsField = readOnlyField(); semField = readOnlyField();
        addRow(viewCard, "PRN", prnField); addRow(viewCard, "Name", nameField);
        addRow(viewCard, "Login Email", emailField); addRow(viewCard, "Department", deptField);
        addRow(viewCard, "CGPA", cgpaField); addRow(viewCard, "Passing Year", yearField);
        addRow(viewCard, "Backlogs", backlogsField); addRow(viewCard, "Semester", semField);

        JPanel editCard = new JPanel(new GridLayout(0, 2, 10, 10));
        editCard.setBackground(Color.WHITE);
        editCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(215, 222, 216)), new EmptyBorder(18, 18, 18, 18)));
        phoneField = new JTextField(); skillsField = new JTextField();
        addRow(editCard, "Phone", phoneField); addRow(editCard, "Skills (comma-separated)", skillsField);
        JButton save = btn("Save Changes"); save.addActionListener(e -> saveProfile());
        editCard.add(new JLabel()); editCard.add(save);

        JPanel resumeCard = new JPanel(new BorderLayout(0, 10));
        resumeCard.setBackground(Color.WHITE);
        resumeCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(215, 222, 216)), new EmptyBorder(18, 18, 18, 18)));
        resumeCard.add(lbl("Resume", 16, true, DARK()), BorderLayout.NORTH);
        resumeStatus = lbl("Checking resume status...", 13, false, MUTED());
        JPanel resumeButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        resumeButtons.setOpaque(false);
        JButton upload = btn("Upload / Replace Resume"); upload.addActionListener(e -> uploadResume());
        JButton download = btn("Download Resume"); download.addActionListener(e -> downloadResume());
        resumeButtons.add(upload); resumeButtons.add(download);
        JPanel resumeBody = new JPanel(new BorderLayout(0, 8));
        resumeBody.setOpaque(false);
        resumeBody.add(resumeStatus, BorderLayout.NORTH);
        resumeBody.add(resumeButtons, BorderLayout.CENTER);
        resumeCard.add(resumeBody, BorderLayout.CENTER);

        JPanel top = new JPanel(new GridLayout(1, 2, 16, 0));
        top.setOpaque(false);
        JPanel viewWrap = new JPanel(new BorderLayout(0, 8)); viewWrap.setOpaque(false);
        viewWrap.add(lbl("Academic Record (view only)", 16, true, DARK()), BorderLayout.NORTH);
        viewWrap.add(viewCard, BorderLayout.CENTER);
        JPanel editWrap = new JPanel(new BorderLayout(0, 8)); editWrap.setOpaque(false);
        editWrap.add(lbl("Contact Details (editable)", 16, true, DARK()), BorderLayout.NORTH);
        editWrap.add(editCard, BorderLayout.CENTER);
        top.add(viewWrap); top.add(editWrap);

        outer.add(top, BorderLayout.NORTH);
        outer.add(resumeCard, BorderLayout.CENTER);
        p.add(outer, BorderLayout.CENTER);
        return p;
    }

    private JTextField readOnlyField() {
        JTextField f = new JTextField();
        f.setEditable(false);
        f.setBackground(new Color(240, 242, 239));
        return f;
    }

    private void addRow(JPanel panel, String label, JComponent field) {
        panel.add(new JLabel(label));
        panel.add(field);
    }

    private void loadProfile() {
        prnField.setText(""); nameField.setText(name); emailField.setText(email == null ? "" : email);
        deptField.setText(""); cgpaField.setText(""); yearField.setText(""); backlogsField.setText(""); semField.setText("");
        phoneField.setText(""); skillsField.setText("");
        resumeStatus.setText("Checking resume status...");
        try {
            SocketClient.Response r = client.sendRequest("GET_MY_PROFILE", token);
            if (!r.success) throw new Exception(r.payload);
            // Split with limit -1: unlike r.parts() (limit 0), this keeps a trailing
            // empty field (e.g. a student with no skills set yet) instead of silently
            // dropping it and under-counting the fields.
            String[] x = r.payload.split("\\|", -1);
            if (x.length < 10) throw new Exception("Unexpected profile response.");
            profilePrn = x[0]; nameField.setText(x[1]); emailField.setText(x[2]); profileDept = x[3];
            profileCgpa = parseD(x[4]); profilePassingYear = parseI(x[5]); profileBacklogs = parseI(x[6]);
            profileSemester = parseI(x[7]); profilePhone = x[8]; profileSkills = x[9];
            profileLoaded = true;

            prnField.setText(profilePrn); deptField.setText(profileDept);
            cgpaField.setText(String.valueOf(profileCgpa)); yearField.setText(String.valueOf(profilePassingYear));
            backlogsField.setText(String.valueOf(profileBacklogs)); semField.setText(String.valueOf(profileSemester));
            phoneField.setText(profilePhone); skillsField.setText(profileSkills);
        } catch (Exception e) {
            profileLoaded = false;
            prnField.setText("-"); deptField.setText("-"); cgpaField.setText("-");
            yearField.setText("-"); backlogsField.setText("-"); semField.setText("-");
            resumeStatus.setText("Profile data is not available from the server yet.");
            return;
        }
        loadResumeStatus();
    }

    private void saveProfile() {
        try {
            SocketClient.Response r = client.sendRequest("UPDATE_MY_PROFILE", token, phoneField.getText().trim(), skillsField.getText().trim());
            if (!r.success) throw new Exception(r.payload);
            JOptionPane.showMessageDialog(this, "Profile updated.");
            loadProfile();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unable to save: " + e.getMessage()
                    + "\n(This action needs the UPDATE_MY_PROFILE command, which the server may not support yet.)");
        }
    }

    private void loadResumeStatus() {
        try {
            SocketClient.Response r = client.sendRequest("GET_RESUME", token);
            if (!r.success) throw new Exception(r.payload);
            String[] x = r.payload.split("\\|", -1);
            resumeFileName = x.length > 0 ? x[0] : null;
            resumeStatus.setText(resumeFileName == null || resumeFileName.isBlank()
                    ? "No resume uploaded yet." : "Current resume: " + resumeFileName);
        } catch (Exception e) {
            resumeFileName = null;
            resumeStatus.setText("No resume on file yet, or the server does not support resumes.");
        }
    }

    private void uploadResume() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("PDF or Word document", "pdf", "doc", "docx"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            String encoded = Base64.getEncoder().encodeToString(bytes);
            SocketClient.Response r = client.sendRequest("UPLOAD_RESUME", token, file.getName(), encoded);
            if (!r.success) throw new Exception(r.payload);
            JOptionPane.showMessageDialog(this, "Resume uploaded.");
            loadResumeStatus();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unable to upload resume: " + e.getMessage()
                    + "\n(This action needs the UPLOAD_RESUME command, which the server may not support yet.)");
        }
    }

    private void downloadResume() {
        try {
            SocketClient.Response r = client.sendRequest("GET_RESUME", token);
            if (!r.success) throw new Exception(r.payload);
            String[] x = r.payload.split("\\|", -1);
            if (x.length < 2) throw new Exception("No resume on file.");
            String filename = x[0];
            byte[] bytes = Base64.getDecoder().decode(x[1]);
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File(filename));
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            Files.write(chooser.getSelectedFile().toPath(), bytes);
            JOptionPane.showMessageDialog(this, "Resume saved.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unable to download resume: " + e.getMessage()
                    + "\n(This action needs the GET_RESUME command, which the server may not support yet.)");
        }
    }

    // =========================================================
    // SHARED
    // =========================================================

    private void refresh() {
        loadProfile();
        loadJobs();
        loadApps();
        loadNotes();
    }

    private void logout() {
        try { if (token != null) client.sendRequest("LOGOUT", token); } catch (Exception ignored) {}
        dispose();
        new LoginFrame().setVisible(true);
    }

    private static double parseD(String s) { try { return Double.parseDouble(s); } catch (Exception e) { return -1; } }
    private static int parseI(String s) { try { return Integer.parseInt(s); } catch (Exception e) { return 0; } }

    /** Sort key for "soonest deadline first": unparsable/blank deadlines sort last. */
    private static String deadlineRank(String value) {
        if (value == null || value.isBlank()) return "9999-99-99";
        try { return LocalDate.parse(value.length() >= 10 ? value.substring(0, 10) : value).toString(); }
        catch (Exception e) { return "9999-99-99"; }
    }

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

    private static JPanel page() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(BG());
        p.setBorder(new EmptyBorder(20, 20, 20, 20));
        return p;
    }

    private static Color BG() { return new Color(247, 248, 245); }
    private static Color DARK() { return new Color(20, 92, 48); }
    private static Color TEXT() { return new Color(38, 50, 43); }
    private static Color MUTED() { return new Color(105, 115, 108); }

    private static JLabel lbl(String s, int z, boolean b, Color c) {
        JLabel l = new JLabel(s);
        l.setFont(new Font("SansSerif", b ? Font.BOLD : Font.PLAIN, z));
        l.setForeground(c);
        return l;
    }

    private static JButton btn(String s) {
        JButton b = new JButton(s);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static JComboBox<String> box(String... items) { return new JComboBox<>(items); }

    private static DefaultTableModel model(String[] c) {
        return new DefaultTableModel(c, 0) { public boolean isCellEditable(int r, int c) { return false; } };
    }

    private static void style(JTable t) {
        t.setRowHeight(30);
        t.setAutoCreateRowSorter(true);
        t.setFillsViewportHeight(true);
        t.setShowGrid(true);
        t.setGridColor(new Color(205, 212, 207));
        t.setIntercellSpacing(new Dimension(1, 1));
        t.setSelectionBackground(new Color(205, 224, 211));
        t.setSelectionForeground(TEXT());
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        t.setDefaultRenderer(Object.class, center);
        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setPreferredSize(new Dimension(h.getPreferredSize().width, 32));
        h.setDefaultRenderer(new DefaultTableCellRenderer() {{
            setHorizontalAlignment(SwingConstants.CENTER);
            setOpaque(true);
            setBackground(new Color(232, 238, 234));
            setForeground(new Color(38, 50, 43));
            setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(170, 180, 173)));
        }});
    }
}
