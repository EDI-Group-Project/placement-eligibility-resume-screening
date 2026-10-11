import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AdminDashboard extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);

    private final List<String[]> students = new ArrayList<>();
    private final List<String[]> jobs = new ArrayList<>();
    private final List<String[]> users = new ArrayList<>();

    private DefaultTableModel studentModel;
    private DefaultTableModel jobModel;
    private DefaultTableModel userModel;

    public AdminDashboard() {

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

        JPanel top = new JPanel(
                new GridLayout(1, 4, 15, 15)
        );

        top.setOpaque(false);

        top.add(statCard(
                "Students",
                String.valueOf(students.size())
        ));

        top.add(statCard(
                "Job Postings",
                String.valueOf(jobs.size())
        ));

        top.add(statCard(
                "Users",
                String.valueOf(users.size())
        ));

        top.add(statCard(
                "Eligible Students",
                "2"
        ));

        panel.add(top, BorderLayout.NORTH);

        JTextArea information = new JTextArea();

        information.setEditable(false);
        information.setLineWrap(true);
        information.setWrapStyleWord(true);

        information.setText(
                "ADMIN PRIVILEGES\n\n" +
                "The Admin portal provides access to the complete " +
                "frontend administration area.\n\n" +
                "Available modules:\n" +
                "• Job postings\n" +
                "• Eligible student management\n" +
                "• Student management\n" +
                "• User management\n" +
                "• TPO management\n" +
                "• TPC management\n" +
                "• Director management\n" +
                "• Dean management\n" +
                "• Reports\n" +
                "• Notifications\n\n" +
                "All controls in this dashboard are frontend UI controls. " +
                "Existing backend APIs can be connected to these actions " +
                "without changing the dashboard structure."
        );

        information.setFont(
                new Font("SansSerif", Font.PLAIN, 15)
        );

        information.setBackground(Color.WHITE);
        information.setBorder(
                new EmptyBorder(20, 20, 20, 20)
        );

        panel.add(
                new JScrollPane(information),
                BorderLayout.CENTER
        );

        return panel;
    }

    private JPanel statCard(String title, String value) {

        JPanel card = new JPanel();

        card.setBackground(Color.WHITE);
        card.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                new Color(220, 225, 220)
                        ),
                        new EmptyBorder(15, 15, 15, 15)
                )
        );

        card.setLayout(
                new BoxLayout(card, BoxLayout.Y_AXIS)
        );

        JLabel valueLabel = new JLabel(value);

        valueLabel.setFont(
                new Font("SansSerif", Font.BOLD, 30)
        );

        JLabel titleLabel = new JLabel(title);

        titleLabel.setFont(
                new Font("SansSerif", Font.PLAIN, 14)
        );

        card.add(valueLabel);
        card.add(Box.createVerticalStrut(5));
        card.add(titleLabel);

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

        JLabel title = new JLabel(
                "Reports & Analysis"
        );

        title.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        22
                )
        );

        panel.add(title, BorderLayout.NORTH);

        JTextArea report = new JTextArea();

        report.setEditable(false);

        report.setFont(
                new Font(
                        "Monospaced",
                        Font.PLAIN,
                        14
                )
        );

        report.setText(
                "PLACEMENT REPORT\n\n" +
                "Total Students       : " + students.size() + "\n" +
                "Total Job Postings   : " + jobs.size() + "\n" +
                "Total Users          : " + users.size() + "\n" +
                "Eligible Students    : 2\n" +
                "Applications         : Available from backend\n\n" +
                "Reports supported by the existing system can be connected " +
                "to this frontend without changing the UI."
        );

        panel.add(
                new JScrollPane(report),
                BorderLayout.CENTER
        );

        return panel;
    }

    private JPanel createNotificationsPanel() {

        JPanel panel = basePanel();

        JLabel title = new JLabel(
                "Notifications"
        );

        title.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        22
                )
        );

        JTextArea area = new JTextArea();

        area.setEditable(false);

        area.setText(
                "Notification Management\n\n" +
                "• Send eligibility notifications\n" +
                "• Send placement drive notifications\n" +
                "• Notify eligible students\n" +
                "• View notification history\n\n" +
                "The existing backend notification functionality can " +
                "be connected here."
        );

        area.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        15
                )
        );

        panel.add(title, BorderLayout.NORTH);

        panel.add(
                new JScrollPane(area),
                BorderLayout.CENTER
        );

        return panel;
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