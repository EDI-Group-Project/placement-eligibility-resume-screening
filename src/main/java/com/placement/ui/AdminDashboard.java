package com.placement.ui;
import javax.swing.*;

import javax.swing.border.EmptyBorder;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import java.awt.*;

import java.util.ArrayList;

import java.util.List;



public class AdminDashboard extends JFrame {



    private final CardLayout cardLayout = new CardLayout();

    private final JPanel contentPanel = new JPanel(cardLayout);



    private final List<String[]> students = new ArrayList<>();

    private final List<String[]> jobs = new ArrayList<>();

    private final List<String[]> users = new ArrayList<>();
    private final List<String[]> notifications = new ArrayList<>();



    private DefaultTableModel studentModel;

    private DefaultTableModel jobModel;

    private DefaultTableModel userModel;


    public AdminDashboard() {
        this("Admin", "");
    }
    public AdminDashboard(String displayName, String sessionToken) {
        setTitle("Placement Eligibility Portal - Admin Dashboard");
        setSize(1250, 750);
        setMinimumSize(new Dimension(1100, 650));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        loadSampleData();
        buildUI();
    }



    private void loadSampleData() {



        students.add(new String[]{

                "STU001", "Srushti", "srushti@gmail.com",

                "Computer", "8.5", "0", "Eligible"

        });



        students.add(new String[]{

                "STU002", "Rahul", "rahul@gmail.com",

                "IT", "7.8", "1", "Eligible"

        });



        students.add(new String[]{

                "STU003", "Priya", "priya@gmail.com",

                "ENTC", "6.9", "2", "Not Eligible"

        });



        jobs.add(new String[]{

                "JOB001", "TCS", "Software Developer",

                "7 LPA", "7.0", "Computer, IT", "30-10-2026"

        });



        jobs.add(new String[]{

                "JOB002", "Infosys", "System Engineer",

                "6 LPA", "6.5", "Computer, IT, ENTC", "05-11-2026"

        });



        users.add(new String[]{

                "U001", "Admin", "admin", "ADMIN"

        });



        users.add(new String[]{

                "U002", "TPO", "tpo", "TPO"

        });



        users.add(new String[]{

                "U003", "TPC", "tpc", "TPC"

        });



        users.add(new String[]{

                "U004", "Director", "director", "DIRECTOR"

        });



        users.add(new String[]{

                "U005", "Dean", "dean", "DEAN"

        });

    }



    private void buildUI() {



        JPanel root = new JPanel(new BorderLayout());



        root.add(createSidebar(), BorderLayout.WEST);



        JPanel mainPanel = new JPanel(new BorderLayout());



        mainPanel.add(createHeader(), BorderLayout.NORTH);



        contentPanel.setBackground(new Color(245, 247, 246));



        contentPanel.add(createDashboard(), "DASHBOARD");

        contentPanel.add(createJobsPanel(), "JOBS");

        contentPanel.add(createStudentsPanel(), "STUDENTS");

        contentPanel.add(createUsersPanel(), "USERS");

        contentPanel.add(createReportsPanel(), "REPORTS");

        contentPanel.add(createNotificationsPanel(), "NOTIFICATIONS");



        mainPanel.add(contentPanel, BorderLayout.CENTER);



        root.add(mainPanel, BorderLayout.CENTER);



        setContentPane(root);

    }



    private JPanel createSidebar() {



        JPanel sidebar = new JPanel();

        sidebar.setPreferredSize(new Dimension(220, 750));

        sidebar.setBackground(new Color(25, 92, 50));

        sidebar.setBorder(new EmptyBorder(25, 15, 20, 15));



        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));



        JLabel title = new JLabel("PLACEMENT");

        title.setForeground(Color.WHITE);

        title.setFont(new Font("SansSerif", Font.BOLD, 22));



        JLabel subtitle = new JLabel("ADMIN PORTAL");

        subtitle.setForeground(new Color(215, 240, 220));

        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));



        sidebar.add(title);

        sidebar.add(subtitle);



        sidebar.add(Box.createVerticalStrut(30));



        addMenuButton(sidebar, "Dashboard", "DASHBOARD");

        addMenuButton(sidebar, "Job Postings", "JOBS");

        addMenuButton(sidebar, "Students", "STUDENTS");

        addMenuButton(sidebar, "Manage Users", "USERS");

        addMenuButton(sidebar, "Reports", "REPORTS");

        addMenuButton(sidebar, "Notifications", "NOTIFICATIONS");



        sidebar.add(Box.createVerticalGlue());



        JButton logout = createMenuButton("Logout");



        logout.addActionListener(e -> {



            int choice = JOptionPane.showConfirmDialog(

                    this,

                    "Are you sure you want to logout?",

                    "Logout",

                    JOptionPane.YES_NO_OPTION

            );



            if (choice == JOptionPane.YES_OPTION) {

                dispose();

            }

        });



        sidebar.add(logout);



        return sidebar;

    }



    private void addMenuButton(

            JPanel sidebar,

            String text,

            String card

    ) {



        JButton button = createMenuButton(text);



        button.addActionListener(

                e -> cardLayout.show(contentPanel, card)

        );



        sidebar.add(button);

    }



    private JButton createMenuButton(String text) {



        JButton button = new JButton(text);



        button.setMaximumSize(new Dimension(190, 42));

        button.setAlignmentX(Component.LEFT_ALIGNMENT);



        button.setHorizontalAlignment(SwingConstants.LEFT);



        button.setForeground(Color.WHITE);

        button.setBackground(new Color(25, 92, 50));



        button.setBorderPainted(false);

        button.setFocusPainted(false);



        button.setFont(new Font("SansSerif", Font.PLAIN, 14));



        return button;

    }



    private JPanel createHeader() {



        JPanel header = new JPanel(new BorderLayout());



        header.setPreferredSize(new Dimension(0, 70));

        header.setBackground(Color.WHITE);



        header.setBorder(

                new EmptyBorder(15, 25, 15, 25)

        );



        JLabel title = new JLabel("Admin Dashboard");



        title.setFont(

                new Font("SansSerif", Font.BOLD, 22)

        );



        JLabel admin = new JLabel("ADMIN");



        admin.setFont(

                new Font("SansSerif", Font.BOLD, 14)

        );



        header.add(title, BorderLayout.WEST);

        header.add(admin, BorderLayout.EAST);



        return header;

    }



    private JPanel createDashboard() {
        JPanel panel = basePanel();
        panel.setLayout(new BorderLayout(18, 18));

        int totalStudents = students.size();
        int eligibleStudents = countEligibleStudents();
        int notEligibleStudents = totalStudents - eligibleStudents;
        int totalJobs = jobs.size();
        double readiness = totalStudents == 0 ? 0 : eligibleStudents * 100.0 / totalStudents;

        // Clean page header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Dashboard");
        title.setFont(new Font("SansSerif", Font.BOLD, 25));
        JLabel subtitle = new JLabel("A quick overview of placement readiness and current opportunities");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(new Color(105, 112, 108));

        heading.add(title);
        heading.add(Box.createVerticalStrut(4));
        heading.add(subtitle);

        JButton refresh = new JButton("Refresh");
        refresh.setFocusPainted(false);
        refresh.setFont(new Font("SansSerif", Font.BOLD, 12));
        refresh.setPreferredSize(new Dimension(95, 34));
        refresh.addActionListener(e -> refreshDashboardAndReports());

        header.add(heading, BorderLayout.WEST);
        header.add(refresh, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        // Four simple KPI cards
        JPanel kpis = new JPanel(new GridLayout(1, 4, 14, 0));
        kpis.setOpaque(false);
        kpis.add(cleanStatCard("Total Students", String.valueOf(totalStudents), "Registered students"));
        kpis.add(cleanStatCard("Eligible", String.valueOf(eligibleStudents), "Ready for drives"));
        kpis.add(cleanStatCard("Opportunities", String.valueOf(totalJobs), "Active job postings"));
        kpis.add(cleanStatCard("Readiness", String.format("%.0f%%", readiness), "Overall eligibility"));

        // Main analytics area: only the two most useful visuals
        JPanel analytics = new JPanel(new GridLayout(1, 2, 16, 0));
        analytics.setOpaque(false);
        analytics.add(createEligibilityChartPanel(eligibleStudents, notEligibleStudents));
        analytics.add(createBranchChartPanel());

        JPanel center = new JPanel(new BorderLayout(0, 16));
        center.setOpaque(false);
        center.add(kpis, BorderLayout.NORTH);
        center.add(analytics, BorderLayout.CENTER);

        // Compact bottom section - no duplicate dashboard statistics
        JPanel bottom = createWhiteCardPanel();
        bottom.setLayout(new BorderLayout(12, 8));

        JPanel bottomHeader = new JPanel(new BorderLayout());
        bottomHeader.setOpaque(false);
        JLabel opportunitiesTitle = new JLabel("Recent Placement Opportunities");
        opportunitiesTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        JLabel countLabel = new JLabel(totalJobs + " active");
        countLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        countLabel.setForeground(new Color(105, 112, 108));
        bottomHeader.add(opportunitiesTitle, BorderLayout.WEST);
        bottomHeader.add(countLabel, BorderLayout.EAST);
        bottom.add(bottomHeader, BorderLayout.NORTH);

        JPanel jobList = new JPanel();
        jobList.setOpaque(false);
        jobList.setLayout(new BoxLayout(jobList, BoxLayout.Y_AXIS));

        if (jobs.isEmpty()) {
            JLabel empty = new JLabel("No placement opportunities available.");
            empty.setFont(new Font("SansSerif", Font.PLAIN, 13));
            empty.setForeground(new Color(105, 112, 108));
            jobList.add(empty);
        } else {
            int shown = Math.min(jobs.size(), 3);
            for (int i = 0; i < shown; i++) {
                String[] job = jobs.get(i);

                JPanel row = new JPanel(new BorderLayout(12, 0));
                row.setOpaque(false);
                row.setBorder(new EmptyBorder(7, 4, 7, 4));

                JPanel text = new JPanel();
                text.setOpaque(false);
                text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

                JLabel company = new JLabel(job[1] + "  •  " + job[2]);
                company.setFont(new Font("SansSerif", Font.BOLD, 13));

                JLabel details = new JLabel("" + job[3] + "  •  Min CGPA " + job[4] + "  •  Deadline " + job[6]);
                details.setFont(new Font("SansSerif", Font.PLAIN, 11));
                details.setForeground(new Color(105, 112, 108));

                text.add(company);
                text.add(Box.createVerticalStrut(3));
                text.add(details);
                row.add(text, BorderLayout.CENTER);

                JLabel id = new JLabel(job[0]);
                id.setFont(new Font("SansSerif", Font.BOLD, 11));
                id.setForeground(new Color(105, 112, 108));
                row.add(id, BorderLayout.EAST);

                jobList.add(row);
                if (i < shown - 1) {
                    JSeparator separator = new JSeparator();
                    separator.setForeground(new Color(232, 235, 232));
                    jobList.add(separator);
                }
            }
        }

        bottom.add(jobList, BorderLayout.CENTER);

        panel.add(center, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel cleanStatCard(String title, String value, String description) {
        JPanel card = createWhiteCardPanel();
        card.setLayout(new BorderLayout(6, 5));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 229, 225)),
                new EmptyBorder(13, 15, 12, 15)
        ));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 27));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 12));

        JLabel descriptionLabel = new JLabel(description);
        descriptionLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        descriptionLabel.setForeground(new Color(115, 120, 116));

        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        labels.add(titleLabel);
        labels.add(Box.createVerticalStrut(3));
        labels.add(descriptionLabel);

        card.add(valueLabel, BorderLayout.WEST);
        card.add(labels, BorderLayout.CENTER);
        return card;
    }

    private JPanel dashboardMetric(String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        JLabel left = new JLabel(label);
        left.setFont(new Font("SansSerif", Font.PLAIN, 12));
        JLabel right = new JLabel(value);
        right.setFont(new Font("SansSerif", Font.BOLD, 13));
        row.add(left, BorderLayout.WEST);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    private void refreshDashboardAndReports() {
        contentPanel.removeAll();
        contentPanel.add(createDashboard(), "DASHBOARD");
        contentPanel.add(createJobsPanel(), "JOBS");
        contentPanel.add(createStudentsPanel(), "STUDENTS");
        contentPanel.add(createUsersPanel(), "USERS");
        contentPanel.add(createReportsPanel(), "REPORTS");
        contentPanel.add(createNotificationsPanel(), "NOTIFICATIONS");
        contentPanel.revalidate();
        contentPanel.repaint();
        cardLayout.show(contentPanel, "DASHBOARD");
    }

    private JPanel statCard(String title, String value) {

        JPanel card = createWhiteCardPanel();
        card.setLayout(new BorderLayout(8, 8));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 30));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));

        card.add(valueLabel, BorderLayout.CENTER);
        card.add(titleLabel, BorderLayout.SOUTH);

        return card;
    }

    private JPanel createWhiteCardPanel() {
        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 220)),
                new EmptyBorder(14, 14, 14, 14)
        ));
        return card;
    }

    private int countEligibleStudents() {
        int count = 0;

        for (String[] student : students) {
            if (student.length > 6 &&
                    "Eligible".equalsIgnoreCase(student[6])) {
                count++;
            }
        }

        return count;
    }

    private JPanel createEligibilityChartPanel(int eligible, int notEligible) {

        JPanel card = createWhiteCardPanel();
        card.setLayout(new BorderLayout(10, 10));

        JLabel title = new JLabel("Student Eligibility");
        title.setFont(new Font("SansSerif", Font.BOLD, 17));
        card.add(title, BorderLayout.NORTH);

        card.add(new EligibilityChart(eligible, notEligible), BorderLayout.CENTER);

        return card;
    }

    private JPanel createBranchChartPanel() {

        JPanel card = createWhiteCardPanel();
        card.setLayout(new BorderLayout(10, 10));

        JLabel title = new JLabel("Students by Branch");
        title.setFont(new Font("SansSerif", Font.BOLD, 17));
        card.add(title, BorderLayout.NORTH);

        java.util.Map<String, Integer> branchCounts = new java.util.LinkedHashMap<>();

        for (String[] student : students) {
            if (student.length > 3) {
                String branch = student[3];
                branchCounts.put(branch, branchCounts.getOrDefault(branch, 0) + 1);
            }
        }

        card.add(new BranchChart(branchCounts), BorderLayout.CENTER);

        return card;
    }

    private JPanel createJobsPanel() {



        JPanel panel = basePanel();



        JPanel top = new JPanel(

                new FlowLayout(FlowLayout.LEFT)

        );



        top.setOpaque(false);



        JButton add = new JButton("Add Job");

        JButton edit = new JButton("Edit Selected");

        JButton delete = new JButton("Delete Selected");

        JButton eligible = new JButton("View Eligible Students");

        JButton notification = new JButton("Send Notification");



        top.add(add);

        top.add(edit);

        top.add(delete);

        top.add(eligible);

        top.add(notification);



        String[] columns = {

                "Job ID",

                "Company",

                "Role",

                "Package",

                "Min CGPA",

                "Branches",

                "Deadline"

        };



        jobModel = new DefaultTableModel(columns, 0) {



            public boolean isCellEditable(

                    int row,

                    int column

            ) {

                return false;

            }

        };



        JTable table = new JTable(jobModel);



        refreshJobs();



        add.addActionListener(e ->

                addJob()

        );



        edit.addActionListener(e ->

                editJob(table)

        );



        delete.addActionListener(e ->

                deleteJob(table)

        );



        eligible.addActionListener(e ->

                showEligibleStudents()

        );



        notification.addActionListener(e ->

                sendNotification()

        );



        panel.add(top, BorderLayout.NORTH);



        panel.add(

                new JScrollPane(table),

                BorderLayout.CENTER

        );



        return panel;

    }



    private void refreshJobs() {



        if (jobModel == null) return;



        jobModel.setRowCount(0);



        for (String[] job : jobs) {

            jobModel.addRow(job);

        }

    }



    private void addJob() {



        JTextField company = new JTextField();

        JTextField role = new JTextField();

        JTextField pkg = new JTextField();

        JTextField cgpa = new JTextField();

        JTextField branches = new JTextField();

        JTextField deadline = new JTextField();



        JPanel form = new JPanel(

                new GridLayout(6, 2, 8, 8)

        );



        form.add(new JLabel("Company"));

        form.add(company);



        form.add(new JLabel("Role"));

        form.add(role);



        form.add(new JLabel("Package"));

        form.add(pkg);



        form.add(new JLabel("Minimum CGPA"));

        form.add(cgpa);



        form.add(new JLabel("Branches"));

        form.add(branches);



        form.add(new JLabel("Deadline"));

        form.add(deadline);



        int result = JOptionPane.showConfirmDialog(

                this,

                form,

                "Add Job",

                JOptionPane.OK_CANCEL_OPTION

        );



        if (result != JOptionPane.OK_OPTION) {

            return;

        }



        String id = "JOB" + String.format(

                "%03d",

                jobs.size() + 1

        );



        jobs.add(new String[]{

                id,

                company.getText(),

                role.getText(),

                pkg.getText(),

                cgpa.getText(),

                branches.getText(),

                deadline.getText()

        });



        refreshJobs();



        JOptionPane.showMessageDialog(

                this,

                "Job added in the frontend.",

                "Success",

                JOptionPane.INFORMATION_MESSAGE

        );

    }



    private void editJob(JTable table) {



        int row = table.getSelectedRow();



        if (row < 0) {

            showSelectMessage();

            return;

        }



        int index = table.convertRowIndexToModel(row);



        String[] job = jobs.get(index);



        JTextField company = new JTextField(job[1]);

        JTextField role = new JTextField(job[2]);

        JTextField pkg = new JTextField(job[3]);

        JTextField cgpa = new JTextField(job[4]);

        JTextField branches = new JTextField(job[5]);

        JTextField deadline = new JTextField(job[6]);



        JPanel form = new JPanel(

                new GridLayout(6, 2, 8, 8)

        );



        form.add(new JLabel("Company"));

        form.add(company);



        form.add(new JLabel("Role"));

        form.add(role);



        form.add(new JLabel("Package"));

        form.add(pkg);



        form.add(new JLabel("Minimum CGPA"));

        form.add(cgpa);



        form.add(new JLabel("Branches"));

        form.add(branches);



        form.add(new JLabel("Deadline"));

        form.add(deadline);



        int result = JOptionPane.showConfirmDialog(

                this,

                form,

                "Edit Job",

                JOptionPane.OK_CANCEL_OPTION

        );



        if (result != JOptionPane.OK_OPTION) {

            return;

        }



        job[1] = company.getText();

        job[2] = role.getText();

        job[3] = pkg.getText();

        job[4] = cgpa.getText();

        job[5] = branches.getText();

        job[6] = deadline.getText();



        refreshJobs();

    }



    private void deleteJob(JTable table) {



        int row = table.getSelectedRow();



        if (row < 0) {

            showSelectMessage();

            return;

        }



        int index = table.convertRowIndexToModel(row);



        int choice = JOptionPane.showConfirmDialog(

                this,

                "Delete selected job?",

                "Delete",

                JOptionPane.YES_NO_OPTION

        );



        if (choice == JOptionPane.YES_OPTION) {



            jobs.remove(index);



            refreshJobs();

        }

    }



    private void showEligibleStudents() {



        StringBuilder result = new StringBuilder();



        result.append(

                "Eligible Students\n\n"

        );



        for (String[] student : students) {



            if (student[6].equalsIgnoreCase("Eligible")) {



                result.append(

                        student[0]

                ).append(" - ")

                        .append(student[1])

                        .append(" - ")

                        .append(student[4])

                        .append(" CGPA\n");

            }

        }



        JOptionPane.showMessageDialog(

                this,

                result.toString(),

                "Eligible Students",

                JOptionPane.INFORMATION_MESSAGE

        );

    }



    private void sendNotification() {



        JOptionPane.showMessageDialog(

                this,

                "Notification UI opened.\n\n" +

                        "Existing backend notification API can be connected here.",

                "Send Notification",

                JOptionPane.INFORMATION_MESSAGE

        );

    }



    private JPanel createStudentsPanel() {



        JPanel panel = basePanel();



        JPanel top = new JPanel(

                new FlowLayout(FlowLayout.LEFT)

        );



        top.setOpaque(false);



        JButton add = new JButton("Add Student");

        JButton delete = new JButton("Delete Student");

        JButton search = new JButton("Search");



        JTextField searchField = new JTextField(20);



        top.add(add);

        top.add(delete);

        top.add(searchField);

        top.add(search);



        String[] columns = {

                "Student ID",

                "Name",

                "Email",

                "Branch",

                "CGPA",

                "Backlogs",

                "Eligibility"

        };



        studentModel = new DefaultTableModel(

                columns,

                0

        ) {

            public boolean isCellEditable(

                    int row,

                    int column

            ) {

                return false;

            }

        };



        JTable table = new JTable(studentModel);



        refreshStudents();



        add.addActionListener(e ->

                addStudent()

        );



        delete.addActionListener(e -> {



            int row = table.getSelectedRow();



            if (row < 0) {

                showSelectMessage();

                return;

            }



            students.remove(

                    table.convertRowIndexToModel(row)

            );



            refreshStudents();

        });



        search.addActionListener(e -> {



            String query =

                    searchField.getText()

                            .toLowerCase();



            studentModel.setRowCount(0);



            for (String[] student : students) {



                if (String.join(

                        " ",

                        student

                ).toLowerCase().contains(query)) {



                    studentModel.addRow(student);

                }

            }

        });



        panel.add(top, BorderLayout.NORTH);



        panel.add(

                new JScrollPane(table),

                BorderLayout.CENTER

        );



        return panel;

    }



    private void refreshStudents() {



        if (studentModel == null) return;



        studentModel.setRowCount(0);



        for (String[] student : students) {

            studentModel.addRow(student);

        }

    }



    private void addStudent() {



        JTextField id = new JTextField();

        JTextField name = new JTextField();

        JTextField email = new JTextField();

        JTextField branch = new JTextField();

        JTextField cgpa = new JTextField();

        JTextField backlog = new JTextField();



        JPanel form = new JPanel(

                new GridLayout(6, 2, 8, 8)

        );



        form.add(new JLabel("Student ID"));

        form.add(id);



        form.add(new JLabel("Name"));

        form.add(name);



        form.add(new JLabel("Email"));

        form.add(email);



        form.add(new JLabel("Branch"));

        form.add(branch);



        form.add(new JLabel("CGPA"));

        form.add(cgpa);



        form.add(new JLabel("Backlogs"));

        form.add(backlog);



        int result = JOptionPane.showConfirmDialog(

                this,

                form,

                "Add Student",

                JOptionPane.OK_CANCEL_OPTION

        );



        if (result != JOptionPane.OK_OPTION) {

            return;

        }



        students.add(new String[]{

                id.getText(),

                name.getText(),

                email.getText(),

                branch.getText(),

                cgpa.getText(),

                backlog.getText(),

                "Eligible"

        });



        refreshStudents();

    }



    private JPanel createUsersPanel() {



        JPanel panel = basePanel();



        JPanel top = new JPanel(

                new FlowLayout(FlowLayout.LEFT)

        );



        top.setOpaque(false);



        JButton add = new JButton("Add User");

        JButton delete = new JButton("Delete User");



        top.add(add);

        top.add(delete);



        String[] columns = {

                "User ID",

                "Name",

                "Username",

                "Role"

        };



        userModel = new DefaultTableModel(

                columns,

                0

        ) {

            public boolean isCellEditable(

                    int row,

                    int column

            ) {

                return false;

            }

        };



        JTable table = new JTable(userModel);



        refreshUsers();



        add.addActionListener(e ->

                addUser()

        );



        delete.addActionListener(e -> {



            int row = table.getSelectedRow();



            if (row < 0) {

                showSelectMessage();

                return;

            }



            users.remove(

                    table.convertRowIndexToModel(row)

            );



            refreshUsers();

        });



        panel.add(top, BorderLayout.NORTH);



        panel.add(

                new JScrollPane(table),

                BorderLayout.CENTER

        );



        return panel;

    }



    private void refreshUsers() {



        if (userModel == null) return;



        userModel.setRowCount(0);



        for (String[] user : users) {

            userModel.addRow(user);

        }

    }



    private void addUser() {



        JTextField name = new JTextField();

        JTextField username = new JTextField();



        JComboBox<String> role =

                new JComboBox<>(

                        new String[]{

                                "ADMIN",

                                "TPO",

                                "TPC",

                                "DIRECTOR",

                                "DEAN"

                        }

                );



        JPanel form = new JPanel(

                new GridLayout(3, 2, 8, 8)

        );



        form.add(new JLabel("Name"));

        form.add(name);



        form.add(new JLabel("Username"));

        form.add(username);



        form.add(new JLabel("Role"));

        form.add(role);



        int result = JOptionPane.showConfirmDialog(

                this,

                form,

                "Add User",

                JOptionPane.OK_CANCEL_OPTION

        );



        if (result != JOptionPane.OK_OPTION) {

            return;

        }



        users.add(new String[]{

                "U" + String.format(

                        "%03d",

                        users.size() + 1

                ),

                name.getText(),

                username.getText(),

                role.getSelectedItem().toString()

        });



        refreshUsers();

    }



    private JPanel createReportsPanel() {
        JPanel panel = basePanel();
        panel.setLayout(new BorderLayout(18, 18));

        // Modern Header with Status Badge & Actions
        JPanel header = new JPanel(new BorderLayout(15, 0));
        header.setOpaque(false);

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));

        JPanel titleLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleLine.setOpaque(false);
        JLabel title = new JLabel("Reports & Analytics Hub");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(new Color(25, 40, 30));

        // Live Status Badge
        JLabel liveBadge = new JLabel(" LIVE ");
        liveBadge.setFont(new Font("SansSerif", Font.BOLD, 10));
        liveBadge.setForeground(new Color(25, 92, 50));
        liveBadge.setBackground(new Color(215, 240, 220));
        liveBadge.setOpaque(true);
        liveBadge.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

        titleLine.add(title);
        titleLine.add(liveBadge);

        JLabel subtitle = new JLabel("Comprehensive performance telemetry, student metrics, and recruitment channel insights");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(new Color(110, 120, 115));

        heading.add(titleLine);
        heading.add(Box.createVerticalStrut(4));
        heading.add(subtitle);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        JButton refresh = new JButton("Refresh Data");
        JButton export = new JButton("Export Secure PDF/Txt");
       
        styleActionButton(refresh, new Color(240, 244, 241), new Color(40, 60, 50));
        styleActionButton(export, new Color(25, 92, 50), Color.WHITE);

        actions.add(refresh);
        actions.add(export);

        header.add(heading, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        panel.add(createReportsContent(), BorderLayout.CENTER);

        refresh.addActionListener(e -> {
            contentPanel.removeAll();
            contentPanel.add(createDashboard(), "DASHBOARD");
            contentPanel.add(createJobsPanel(), "JOBS");
            contentPanel.add(createStudentsPanel(), "STUDENTS");
            contentPanel.add(createUsersPanel(), "USERS");
            contentPanel.add(createReportsPanel(), "REPORTS");
            contentPanel.add(createNotificationsPanel(), "NOTIFICATIONS");
            contentPanel.revalidate();
            contentPanel.repaint();
            cardLayout.show(contentPanel, "REPORTS");
        });

        export.addActionListener(e -> exportCurrentReport());
        return panel;
    }

    private void styleActionButton(JButton btn, Color bg, Color fg) {
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private JPanel createReportsContent() {
        int totalStudents = students.size();
        int eligible = countEligibleStudents();
        int notEligible = totalStudents - eligible;
        double eligibilityRate = totalStudents == 0 ? 0 : eligible * 100.0 / totalStudents;

        JPanel content = new JPanel(new BorderLayout(16, 16));
        content.setOpaque(false);

        // Top Summary Metric Cards & Progress Gauge
        JPanel topArea = new JPanel(new BorderLayout(12, 12));
        topArea.setOpaque(false);

        JPanel summaryCards = new JPanel(new GridLayout(1, 4, 14, 0));
        summaryCards.setOpaque(false);
        summaryCards.add(reportStatCard("Eligibility Rate", String.format("%.1f%%", eligibilityRate), "Overall readiness score", new Color(25, 92, 50)));
        summaryCards.add(reportStatCard("Eligible Students", String.valueOf(eligible), "Passed criteria bounds", new Color(41, 128, 185)));
        summaryCards.add(reportStatCard("Needs Attention", String.valueOf(notEligible), "Active backlogs / low CGPA", new Color(211, 84, 0)));
        summaryCards.add(reportStatCard("Open Drives", String.valueOf(jobs.size()), "Active corporate postings", new Color(142, 68, 173)));
        topArea.add(summaryCards, BorderLayout.NORTH);

        // Visual Progress Panel
        JPanel performance = createWhiteCardPanel();
        performance.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 230, 220)),
                new EmptyBorder(14, 18, 14, 18)
        ));
        performance.setLayout(new BorderLayout(15, 0));

        JPanel performanceText = new JPanel();
        performanceText.setOpaque(false);
        performanceText.setLayout(new BoxLayout(performanceText, BoxLayout.Y_AXIS));
        JLabel performanceTitle = new JLabel("Batch Academic & Placement Index");
        performanceTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        performanceTitle.setForeground(new Color(30, 45, 35));
        JLabel performanceSub = new JLabel("Real-time aggregated compliance ratio for campus placement eligibility");
        performanceSub.setFont(new Font("SansSerif", Font.PLAIN, 11));
        performanceSub.setForeground(new Color(120, 130, 125));
        performanceText.add(performanceTitle);
        performanceText.add(Box.createVerticalStrut(3));
        performanceText.add(performanceSub);
        performance.add(performanceText, BorderLayout.WEST);

        JPanel progressArea = new JPanel(new BorderLayout(12, 4));
        progressArea.setOpaque(false);
        JProgressBar readiness = new JProgressBar(0, 100);
        readiness.setValue((int) Math.round(eligibilityRate));
        readiness.setStringPainted(true);
        readiness.setString(String.format("%.1f%% Qualified", eligibilityRate));
        readiness.setFont(new Font("SansSerif", Font.BOLD, 11));
        readiness.setPreferredSize(new Dimension(320, 18));
        readiness.setBorderPainted(false);
        readiness.setForeground(new Color(25, 92, 50));
        readiness.setBackground(new Color(230, 242, 235));
       
        progressArea.add(readiness, BorderLayout.CENTER);
        performance.add(progressArea, BorderLayout.EAST);
        topArea.add(performance, BorderLayout.CENTER);

        content.add(topArea, BorderLayout.NORTH);

        // Styled Tabbed Panel for Detailed Reports
        JPanel reportsCard = createWhiteCardPanel();
        reportsCard.setLayout(new BorderLayout(12, 12));
        reportsCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(215, 225, 218)),
                new EmptyBorder(16, 16, 16, 16)
        ));

        JPanel reportsHeader = new JPanel(new BorderLayout());
        reportsHeader.setOpaque(false);
        JLabel sectionTitle = new JLabel("Detailed Analytics & Data Matrix");
        sectionTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        sectionTitle.setForeground(new Color(25, 40, 30));
        JLabel sectionSubtitle = new JLabel("Switch tabs below to view detailed breakdown sheets");
        sectionSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 11));
        sectionSubtitle.setForeground(new Color(115, 125, 120));

        JPanel titleStack = new JPanel();
        titleStack.setOpaque(false);
        titleStack.setLayout(new BoxLayout(titleStack, BoxLayout.Y_AXIS));
        titleStack.add(sectionTitle);
        titleStack.add(Box.createVerticalStrut(2));
        titleStack.add(sectionSubtitle);
        reportsHeader.add(titleStack, BorderLayout.WEST);
        reportsCard.add(reportsHeader, BorderLayout.NORTH);

        JTabbedPane reportTabs = new JTabbedPane();
        reportTabs.setFont(new Font("SansSerif", Font.BOLD, 12));
        reportTabs.setBackground(Color.WHITE);
        reportTabs.setForeground(new Color(25, 92, 50));
        reportTabs.setBorder(new EmptyBorder(8, 0, 0, 0));
       
        reportTabs.addTab("  Student Eligibility Report  ", createStudentEligibilityReport());
        reportTabs.addTab("  Job Opportunities Report  ", createJobOpportunityReport());
        reportTabs.addTab("  Branch Analysis Report  ", createBranchAnalysisReport());
        reportTabs.addTab("  Executive Insights  ", createReportInsights(eligibilityRate, eligible, notEligible));
       
        reportsCard.add(reportTabs, BorderLayout.CENTER);
        content.add(reportsCard, BorderLayout.CENTER);

        return content;
    }

    private JPanel reportStatCard(String title, String value, String description, Color accent) {
        JPanel card = createWhiteCardPanel();
        card.setLayout(new BorderLayout(12, 0));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 230, 220)),
                new EmptyBorder(12, 14, 12, 14)
        ));

        JPanel accentBar = new JPanel();
        accentBar.setBackground(accent);
        accentBar.setPreferredSize(new Dimension(6, 50));
        card.add(accentBar, BorderLayout.WEST);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        titleLabel.setForeground(new Color(100, 110, 105));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        valueLabel.setForeground(new Color(20, 35, 25));

        JLabel descriptionLabel = new JLabel(description);
        descriptionLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        descriptionLabel.setForeground(new Color(130, 140, 135));

        text.add(titleLabel);
        text.add(Box.createVerticalStrut(2));
        text.add(valueLabel);
        text.add(Box.createVerticalStrut(1));
        text.add(descriptionLabel);
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    private JPanel createStudentEligibilityReport() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(10, 5, 5, 5));

        String[] columns = {"Student ID", "Full Name", "Branch", "CGPA", "Backlogs", "Status"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (String[] student : students) {
            model.addRow(new Object[]{
                    student.length > 0 ? student[0] : "",
                    student.length > 1 ? student[1] : "",
                    student.length > 3 ? student[3] : "",
                    student.length > 4 ? student[4] : "",
                    student.length > 5 ? student[5] : "",
                    student.length > 6 ? student[6] : ""
            });
        }

        JTable table = new JTable(model);
        styleReportTable(table);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        JLabel note = new JLabel("Tip: Click any column header to sort student records dynamically.");
        note.setFont(new Font("SansSerif", Font.ITALIC, 11));
        note.setForeground(new Color(110, 120, 115));
        footer.add(note, BorderLayout.WEST);
        panel.add(footer, BorderLayout.SOUTH);
       
        return panel;
    }

    private JPanel createJobOpportunityReport() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(10, 5, 5, 5));

        String[] columns = {"Job ID", "Company", "Role", "Package", "Min CGPA", "Eligible Branches", "Deadline"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (String[] job : jobs) {
            model.addRow(new Object[]{
                    job.length > 0 ? job[0] : "",
                    job.length > 1 ? job[1] : "",
                    job.length > 2 ? job[2] : "",
                    job.length > 3 ? job[3] : "",
                    job.length > 4 ? job[4] : "",
                    job.length > 5 ? job[5] : "",
                    job.length > 6 ? job[6] : ""
            });
        }

        JTable table = new JTable(model);
        styleReportTable(table);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createBranchAnalysisReport() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(10, 5, 5, 5));

        String[] columns = {"Branch Name", "Total Students", "Eligible", "Not Eligible", "Branch Eligibility Ratio"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        java.util.Map<String, Integer> totalByBranch = new java.util.LinkedHashMap<>();
        java.util.Map<String, Integer> eligibleByBranch = new java.util.LinkedHashMap<>();

        for (String[] student : students) {
            if (student.length <= 3) {
                continue;
            }
            String branch = student[3];
            totalByBranch.put(branch, totalByBranch.getOrDefault(branch, 0) + 1);
            if (student.length > 6 && "Eligible".equalsIgnoreCase(student[6])) {
                eligibleByBranch.put(branch, eligibleByBranch.getOrDefault(branch, 0) + 1);
            }
        }

        for (String branch : totalByBranch.keySet()) {
            int total = totalByBranch.get(branch);
            int eligible = eligibleByBranch.getOrDefault(branch, 0);
            int notEligible = total - eligible;
            double rate = total == 0 ? 0 : eligible * 100.0 / total;
            model.addRow(new Object[]{
                    branch,
                    total,
                    eligible,
                    notEligible,
                    String.format("%.1f%%", rate)
            });
        }

        JTable table = new JTable(model);
        styleReportTable(table);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        JLabel note = new JLabel("Analytics: Use this branch-wise distribution matrix to allocate remedial training focus.");
        note.setFont(new Font("SansSerif", Font.ITALIC, 11));
        note.setForeground(new Color(110, 120, 115));
        footer.add(note, BorderLayout.WEST);
        panel.add(footer, BorderLayout.SOUTH);

        return panel;
    }

    private void styleReportTable(JTable table) {
        table.setRowHeight(34);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setShowGrid(true);
        table.setGridColor(new Color(235, 240, 236));
        table.setSelectionBackground(new Color(215, 240, 220));
        table.setSelectionForeground(new Color(20, 40, 30));
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("SansSerif", Font.BOLD, 12));
        header.setBackground(new Color(240, 245, 241));
        header.setForeground(new Color(25, 60, 40));
        header.setPreferredSize(new Dimension(header.getWidth(), 38));

        JScrollPane scroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, table);
        if (scroll != null) {
            scroll.setBorder(BorderFactory.createLineBorder(new Color(215, 225, 218)));
            scroll.setBackground(Color.WHITE);
        }
    }

    private JPanel createReportInsights(double eligibilityRate, int eligible, int notEligible) {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(10, 5, 5, 5));

        JPanel insights = new JPanel();
        insights.setOpaque(false);
        insights.setLayout(new BoxLayout(insights, BoxLayout.Y_AXIS));

        String highestCgpaStudent = "N/A";
        double highestCgpa = -1;
        for (String[] student : students) {
            try {
                double cgpa = Double.parseDouble(student[4]);
                if (cgpa > highestCgpa) {
                    highestCgpa = cgpa;
                    highestCgpaStudent = student[1] + " (CGPA " + student[4] + ")";
                }
            } catch (Exception ignored) {
            }
        }

        addInsightRow(insights, "Overall Batch Eligibility Rate", String.format("%.1f%%", eligibilityRate));
        addInsightRow(insights, "Qualified Candidates Ready for Drives", String.valueOf(eligible));
        addInsightRow(insights, "Candidates Requiring Academic Review", String.valueOf(notEligible));
        addInsightRow(insights, "Top Academic Performer", highestCgpaStudent);
        addInsightRow(insights, "Active Corporate Placement Openings", String.valueOf(jobs.size()));
        addInsightRow(insights, "Authorized Administrative Portal Users", String.valueOf(users.size()));

        insights.add(Box.createVerticalStrut(15));
       
        JPanel narrativeBox = new JPanel(new BorderLayout());
        narrativeBox.setBackground(new Color(245, 248, 246));
        narrativeBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 230, 222)),
                new EmptyBorder(12, 15, 12, 15)
        ));
       
        JTextArea explanation = new JTextArea(
                "Executive Summary & Recommendation:\n" +
                "The placement portal is running optimal verification pipelines. Candidate metrics indicate solid readiness across core IT and Computer branches. Focus should be directed toward mentorship programs for students with active backlogs to improve overall placement conversion rates."
        );
        explanation.setEditable(false);
        explanation.setLineWrap(true);
        explanation.setWrapStyleWord(true);
        explanation.setFont(new Font("SansSerif", Font.PLAIN, 12));
        explanation.setForeground(new Color(50, 65, 55));
        explanation.setBackground(new Color(245, 248, 246));
       
        narrativeBox.add(explanation, BorderLayout.CENTER);
        insights.add(narrativeBox);

        panel.add(new JScrollPane(insights), BorderLayout.CENTER);
        return panel;
    }

    private void addInsightRow(JPanel parent, String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(8, 10, 8, 10));

        JLabel left = new JLabel(label);
        left.setFont(new Font("SansSerif", Font.PLAIN, 12));
        left.setForeground(new Color(70, 80, 75));
       
        JLabel right = new JLabel(value);
        right.setFont(new Font("SansSerif", Font.BOLD, 12));
        right.setForeground(new Color(25, 92, 50));

        row.add(left, BorderLayout.WEST);
        row.add(right, BorderLayout.EAST);
       
        // Add subtle divider line
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(row, BorderLayout.CENTER);
       
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(240, 245, 241));
        wrapper.add(sep, BorderLayout.SOUTH);
       
        parent.add(wrapper);
    }

    private void exportCurrentReport() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Placement Report");
        chooser.setSelectedFile(new java.io.File("Placement_Report.txt"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        int total = students.size();
        int eligible = countEligibleStudents();
        int notEligible = total - eligible;
        double rate = total == 0 ? 0 : eligible * 100.0 / total;

        StringBuilder report = new StringBuilder();
        report.append("PLACEMENT ELIGIBILITY PORTAL - EXECUTIVE REPORT\n");
        report.append("====================================================\n\n");
        report.append("OVERALL SUMMARY METRICS\n");
        report.append("Total Students Registered : ").append(total).append('\n');
        report.append("Eligible Candidates       : ").append(eligible).append('\n');
        report.append("Candidates Needing Review : ").append(notEligible).append('\n');
        report.append(String.format("Batch Eligibility Rate    : %.1f%%\n", rate));
        report.append("Active Job Postings       : ").append(jobs.size()).append('\n');
        report.append("Authorized Portal Users   : ").append(users.size()).append("\n\n");

        report.append("DETAILED STUDENT ROSTER\n");
        report.append("----------------------------------------------------\n");
        for (String[] student : students) {
            report.append(student[0]).append(" | ")
                    .append(student[1]).append(" | ")
                    .append(student[3]).append(" | CGPA ")
                    .append(student[4]).append(" | Status: ")
                    .append(student[6]).append('\n');
        }

        try (java.io.PrintWriter writer = new java.io.PrintWriter(chooser.getSelectedFile())) {
            writer.print(report.toString());
            JOptionPane.showMessageDialog(this,
                    "Report exported successfully to designated file.",
                    "Export Complete",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Unable to export the report.\n" + ex.getMessage(),
                    "Export Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------- SIMPLE FRONTEND CHARTS ----------

    private static class EligibilityChart extends JPanel {

        private final int eligible;
        private final int notEligible;

        EligibilityChart(int eligible, int notEligible) {
            this.eligible = eligible;
            this.notEligible = notEligible;
            setPreferredSize(new Dimension(300, 220));
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            int total = eligible + notEligible;
            int size = Math.min(getWidth(), getHeight()) - 55;
            int x = 25;
            int y = 15;

            if (total == 0) {
                g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
                g2.drawString("No student data available", 55, 105);
                g2.dispose();
                return;
            }

            int eligibleAngle = (int) Math.round(
                    360.0 * eligible / total
            );

            g2.setColor(new Color(45, 125, 70));
            g2.fillArc(x, y, size, size, 90, -eligibleAngle);

            g2.setColor(new Color(220, 95, 80));
            g2.fillArc(x, y, size, size,
                    90 - eligibleAngle,
                    -(360 - eligibleAngle));

            // Donut centre
            g2.setColor(Color.WHITE);
            int hole = size / 2;
            int holeX = x + (size - hole) / 2;
            int holeY = y + (size - hole) / 2;
            g2.fillOval(holeX, holeY, hole, hole);

            double rate = eligible * 100.0 / total;
            String percentage = String.format("%.0f%%", rate);

            g2.setColor(Color.DARK_GRAY);
            g2.setFont(new Font("SansSerif", Font.BOLD, 20));

            int textWidth = g2.getFontMetrics().stringWidth(percentage);
            g2.drawString(
                    percentage,
                    x + (size - textWidth) / 2,
                    y + size / 2 + 7
            );

            int legendY = y + size + 18;

            g2.setColor(new Color(45, 125, 70));
            g2.fillRect(25, legendY - 10, 12, 12);
            g2.setColor(Color.DARK_GRAY);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2.drawString("Eligible (" + eligible + ")", 43, legendY);

            g2.setColor(new Color(220, 95, 80));
            g2.fillRect(155, legendY - 10, 12, 12);
            g2.setColor(Color.DARK_GRAY);
            g2.drawString(
                    "Not Eligible (" + notEligible + ")",
                    173,
                    legendY
            );

            g2.dispose();
        }
    }

    private static class BranchChart extends JPanel {

        private final java.util.Map<String, Integer> branchCounts;

        BranchChart(java.util.Map<String, Integer> branchCounts) {
            this.branchCounts = branchCounts;
            setPreferredSize(new Dimension(350, 220));
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            if (branchCounts.isEmpty()) {
                g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
                g2.drawString("No branch data available", 80, 105);
                g2.dispose();
                return;
            }

            int max = 1;
            for (Integer value : branchCounts.values()) {
                max = Math.max(max, value);
            }

            int left = 80;
            int top = 20;
            int chartWidth = Math.max(150, getWidth() - left - 35);
            int barHeight = 30;
            int gap = 18;

            int index = 0;

            for (java.util.Map.Entry<String, Integer> entry :
                    branchCounts.entrySet()) {

                int y = top + index * (barHeight + gap);
                int barWidth = (int) (
                        chartWidth * entry.getValue() / (double) max
                );

                g2.setColor(new Color(65, 105, 150));
                g2.fillRoundRect(
                        left,
                        y,
                        Math.max(barWidth, 5),
                        barHeight,
                        8,
                        8
                );

                g2.setColor(Color.DARK_GRAY);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
                g2.drawString(entry.getKey(), 10, y + 20);
                g2.drawString(
                        String.valueOf(entry.getValue()),
                        left + barWidth + 8,
                        y + 20
                );

                index++;
            }

            g2.dispose();
        }
    }

    private JPanel createNotificationsPanel() {

        JPanel panel = basePanel();
        panel.setLayout(new BorderLayout(18, 18));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Notification Center");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        JLabel subtitle = new JLabel("Create, preview and manage admin notifications locally");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        heading.add(title);
        heading.add(Box.createVerticalStrut(4));
        heading.add(subtitle);

        JLabel count = new JLabel(notifications.size() + " notification(s)");
        count.setFont(new Font("SansSerif", Font.BOLD, 13));
        header.add(heading, BorderLayout.WEST);
        header.add(count, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        JPanel compose = createWhiteCardPanel();
        compose.setLayout(new BorderLayout(10, 10));
        compose.setPreferredSize(new Dimension(360, 0));

        JLabel composeTitle = new JLabel("Compose Notification");
        composeTitle.setFont(new Font("SansSerif", Font.BOLD, 17));
        compose.add(composeTitle, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 4, 6, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        JTextField subject = new JTextField();
        JComboBox<String> audience = new JComboBox<>(new String[]{
                "All Students", "Eligible Students", "All Users", "Placement Team"
        });
        JComboBox<String> type = new JComboBox<>(new String[]{
                "Placement Drive", "Eligibility Update", "Important Notice", "Reminder"
        });
        JTextArea message = new JTextArea(8, 20);
        message.setLineWrap(true);
        message.setWrapStyleWord(true);
        message.setFont(new Font("SansSerif", Font.PLAIN, 13));

        addFormRow(form, gbc, 0, "Audience", audience);
        addFormRow(form, gbc, 1, "Type", type);
        addFormRow(form, gbc, 2, "Subject", subject);
        addFormRow(form, gbc, 3, "Message", new JScrollPane(message));

        compose.add(form, BorderLayout.CENTER);

        JPanel composeButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        composeButtons.setOpaque(false);
        JButton preview = new JButton("Preview");
        JButton send = new JButton("Send Notification");
        preview.setFocusPainted(false);
        send.setFocusPainted(false);
        composeButtons.add(preview);
        composeButtons.add(send);
        compose.add(composeButtons, BorderLayout.SOUTH);

        JPanel history = createWhiteCardPanel();
        history.setLayout(new BorderLayout(10, 10));
        JLabel historyTitle = new JLabel("Notification History");
        historyTitle.setFont(new Font("SansSerif", Font.BOLD, 17));
        history.add(historyTitle, BorderLayout.NORTH);

        String[] columns = {"Type", "Audience", "Subject", "Message", "Time"};
        DefaultTableModel notificationModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(notificationModel);
        table.setRowHeight(30);
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(3).setPreferredWidth(260);
        refreshNotificationTable(notificationModel);
        history.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel historyActions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        historyActions.setOpaque(false);
        JButton clear = new JButton("Clear History");
        clear.setFocusPainted(false);
        historyActions.add(clear);
        history.add(historyActions, BorderLayout.SOUTH);

        preview.addActionListener(e -> {
            String text = buildNotificationPreview(audience, type, subject, message);
            JOptionPane.showMessageDialog(this,
                    text,
                    "Notification Preview",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        send.addActionListener(e -> {
            if (subject.getText().trim().isEmpty() || message.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Please enter both a subject and a message.",
                        "Incomplete Notification",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            notifications.add(new String[]{
                    type.getSelectedItem().toString(),
                    audience.getSelectedItem().toString(),
                    subject.getText().trim(),
                    message.getText().trim(),
                    java.time.LocalDateTime.now().format(
                            java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))
            });

            refreshNotificationTable(notificationModel);
            count.setText(notifications.size() + " notification(s)");
            subject.setText("");
            message.setText("");

            JOptionPane.showMessageDialog(this,
                    "Notification added to the frontend history.",
                    "Notification Sent",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        clear.addActionListener(e -> {
            if (notifications.isEmpty()) {
                return;
            }
            int choice = JOptionPane.showConfirmDialog(this,
                    "Clear all notification history?",
                    "Clear History",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                notifications.clear();
                refreshNotificationTable(notificationModel);
                count.setText("0 notification(s)");
            }
        });

        panel.add(compose, BorderLayout.WEST);
        panel.add(history, BorderLayout.CENTER);
        return panel;
    }

    private void addFormRow(JPanel form, GridBagConstraints gbc, int row,
                            String label, Component component) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        JLabel fieldLabel = new JLabel(label);
        fieldLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        form.add(fieldLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(component, gbc);
    }

    private String buildNotificationPreview(JComboBox<String> audience,
                                             JComboBox<String> type,
                                             JTextField subject,
                                             JTextArea message) {
        return "NOTIFICATION PREVIEW\n\n" +
                "Type: " + type.getSelectedItem() + "\n" +
                "Audience: " + audience.getSelectedItem() + "\n" +
                "Subject: " + (subject.getText().trim().isEmpty() ? "(No subject)" : subject.getText().trim()) +
                "\n\n" +
                (message.getText().trim().isEmpty() ? "(No message)" : message.getText().trim());
    }

    private void refreshNotificationTable(DefaultTableModel model) {
        model.setRowCount(0);
        for (String[] notification : notifications) {
            model.addRow(notification);
        }
    }

    private JPanel basePanel() {



        JPanel panel = new JPanel(

                new BorderLayout(15, 15)

        );



        panel.setBackground(

                new Color(245, 247, 246)

        );



        panel.setBorder(

                new EmptyBorder(20, 20, 20, 20)

        );



        return panel;

    }



    private void showSelectMessage() {



        JOptionPane.showMessageDialog(

                this,

                "Please select a row first.",

                "Selection Required",

                JOptionPane.WARNING_MESSAGE

        );

    }



    public static void main(String[] args) {



        SwingUtilities.invokeLater(() -> {



            try {

                UIManager.setLookAndFeel(

                        UIManager.getSystemLookAndFeelClassName()

                );

            } catch (Exception ignored) {

            }



            new AdminDashboard().setVisible(true);

        });

    }

}
