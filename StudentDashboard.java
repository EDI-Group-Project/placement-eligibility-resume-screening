import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class StudentDashboard extends JFrame {

    private final String studentName;
    private final String studentEmail;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);

    private final List<String[]> jobs = new ArrayList<>();
    private final List<String[]> applications = new ArrayList<>();
    private final List<String[]> notifications = new ArrayList<>();

    private DefaultTableModel jobsModel;
    private DefaultTableModel applicationsModel;
    private DefaultTableModel notificationsModel;

    // -------------------------------
    // FRONTEND-ONLY RESUME VARIABLE
    // -------------------------------

    private File selectedResume;

    private JLabel resumeStatusDashboard;
    private JLabel resumeStatusProfile;

    public StudentDashboard(
            String studentName,
            String studentEmail
    ) {

        this.studentName =
                studentName == null ||
                studentName.isBlank()
                        ? "Student"
                        : studentName;

        this.studentEmail =
                studentEmail == null
                        ? ""
                        : studentEmail;

        setTitle(
                "Placement Eligibility Portal - Student Dashboard"
        );

        setSize(1250, 750);

        setMinimumSize(
                new Dimension(1100, 650)
        );

        setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE
        );

        setLocationRelativeTo(null);

        loadSampleData();

        buildUI();
    }

    private void loadSampleData() {

        jobs.add(new String[]{
                "JOB001",
                "TCS",
                "Software Developer",
                "7 LPA",
                "7.0",
                "Computer, IT",
                "30-10-2026"
        });

        jobs.add(new String[]{
                "JOB002",
                "Infosys",
                "System Engineer",
                "6 LPA",
                "6.5",
                "Computer, IT, ENTC",
                "05-11-2026"
        });

        applications.add(new String[]{
                "JOB001",
                "TCS",
                "Software Developer",
                "Applied",
                "Pending"
        });

        notifications.add(new String[]{
                "TCS placement drive registration is open.",
                "30-09-2026"
        });

        notifications.add(new String[]{
                "Update your student profile before applying.",
                "28-09-2026"
        });
    }

    private void buildUI() {

        JPanel root = new JPanel(
                new BorderLayout()
        );

        root.add(
                createSidebar(),
                BorderLayout.WEST
        );

        JPanel main = new JPanel(
                new BorderLayout()
        );

        main.add(
                createHeader(),
                BorderLayout.NORTH
        );

        contentPanel.setBackground(
                new Color(247, 248, 245)
        );

        contentPanel.add(
                createDashboard(),
                "DASHBOARD"
        );

        contentPanel.add(
                createJobsPanel(),
                "JOBS"
        );

        contentPanel.add(
                createApplicationsPanel(),
                "APPLICATIONS"
        );

        contentPanel.add(
                createNotificationsPanel(),
                "NOTIFICATIONS"
        );

        contentPanel.add(
                createProfilePanel(),
                "PROFILE"
        );

        main.add(
                contentPanel,
                BorderLayout.CENTER
        );

        root.add(
                main,
                BorderLayout.CENTER
        );

        setContentPane(root);
    }

    private JPanel createSidebar() {

        JPanel sidebar = new JPanel();

        sidebar.setPreferredSize(
                new Dimension(220, 750)
        );

        sidebar.setBackground(
                new Color(20, 92, 48)
        );

        sidebar.setBorder(
                new EmptyBorder(
                        25,
                        15,
                        20,
                        15
                )
        );

        sidebar.setLayout(
                new BoxLayout(
                        sidebar,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel title = new JLabel("PLACEMENT");

        title.setForeground(Color.WHITE);

        title.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        22
                )
        );

        JLabel subtitle =
                new JLabel("STUDENT PORTAL");

        subtitle.setForeground(
                new Color(220, 245, 225)
        );

        subtitle.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        12
                )
        );

        sidebar.add(title);
        sidebar.add(subtitle);

        sidebar.add(
                Box.createVerticalStrut(30)
        );

        addMenuButton(
                sidebar,
                "Dashboard",
                "DASHBOARD"
        );

        addMenuButton(
                sidebar,
                "Placement Drives",
                "JOBS"
        );

        addMenuButton(
                sidebar,
                "My Applications",
                "APPLICATIONS"
        );

        addMenuButton(
                sidebar,
                "Notifications",
                "NOTIFICATIONS"
        );

        addMenuButton(
                sidebar,
                "My Profile",
                "PROFILE"
        );

        sidebar.add(
                Box.createVerticalGlue()
        );

        JButton logout =
                createMenuButton("Logout");

        logout.addActionListener(e -> {

            int result =
                    JOptionPane.showConfirmDialog(
                            this,
                            "Are you sure you want to logout?",
                            "Logout",
                            JOptionPane.YES_NO_OPTION
                    );

            if (result ==
                    JOptionPane.YES_OPTION) {

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

        JButton button =
                createMenuButton(text);

        button.addActionListener(
                e -> cardLayout.show(
                        contentPanel,
                        card
                )
        );

        sidebar.add(button);
    }

    private JButton createMenuButton(
            String text
    ) {

        JButton button =
                new JButton(text);

        button.setMaximumSize(
                new Dimension(190, 42)
        );

        button.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        button.setHorizontalAlignment(
                SwingConstants.LEFT
        );

        button.setForeground(Color.WHITE);

        button.setBackground(
                new Color(20, 92, 48)
        );

        button.setBorderPainted(false);

        button.setFocusPainted(false);

        return button;
    }

    private JPanel createHeader() {

        JPanel header =
                new JPanel(
                        new BorderLayout()
                );

        header.setPreferredSize(
                new Dimension(0, 70)
        );

        header.setBackground(Color.WHITE);

        header.setBorder(
                new EmptyBorder(
                        15,
                        25,
                        15,
                        25
                )
        );

        JLabel title =
                new JLabel("Student Dashboard");

        title.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        22
                )
        );

        JLabel name =
                new JLabel(studentName);

        name.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        14
                )
        );

        header.add(
                title,
                BorderLayout.WEST
        );

        header.add(
                name,
                BorderLayout.EAST
        );

        return header;
    }

    private JPanel createDashboard() {

        JPanel panel = basePanel();

        JLabel welcome =
                new JLabel(
                        "Welcome, " + studentName
                );

        welcome.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        22
                )
        );

        panel.add(
                welcome,
                BorderLayout.NORTH
        );

        JPanel body = new JPanel();

        body.setOpaque(false);

        body.setLayout(
                new BoxLayout(
                        body,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel description =
                new JLabel(
                        "View placement drives, apply to jobs and manage your resume."
                );

        body.add(description);

        body.add(
                Box.createVerticalStrut(20)
        );

        body.add(
                createResumeCard()
        );

        body.add(
                Box.createVerticalStrut(20)
        );

        JPanel stats =
                new JPanel(
                        new GridLayout(
                                1,
                                3,
                                15,
                                15
                        )
                );

        stats.setOpaque(false);

        stats.add(
                statCard(
                        "Available Jobs",
                        String.valueOf(jobs.size())
                )
        );

        stats.add(
                statCard(
                        "Applications",
                        String.valueOf(
                                applications.size()
                        )
                )
        );

        stats.add(
                statCard(
                        "Notifications",
                        String.valueOf(
                                notifications.size()
                        )
                )
        );

        body.add(stats);

        panel.add(
                body,
                BorderLayout.CENTER
        );

        return panel;
    }

    private JPanel createResumeCard() {

        JPanel card =
                new JPanel(
                        new BorderLayout(
                                15,
                                10
                        )
                );

        card.setBackground(Color.WHITE);

        card.setBorder(
                new EmptyBorder(
                        18,
                        18,
                        18,
                        18
                )
        );

        JPanel text =
                new JPanel();

        text.setOpaque(false);

        text.setLayout(
                new BoxLayout(
                        text,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel title =
                new JLabel("Resume");

        title.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        18
                )
        );

        text.add(title);

        text.add(
                Box.createVerticalStrut(6)
        );

        JLabel description =
                new JLabel(
                        "Add your resume for placement applications."
                );

        text.add(description);

        resumeStatusDashboard =
                new JLabel(
                        "No resume selected"
                );

        resumeStatusDashboard.setBorder(
                new EmptyBorder(
                        8,
                        0,
                        0,
                        0
                )
        );

        text.add(
                resumeStatusDashboard
        );

        JPanel buttons =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT
                        )
                );

        buttons.setOpaque(false);

        JButton choose =
                new JButton(
                        "Add / Change Resume"
                );

        JButton open =
                new JButton(
                        "Open Resume"
                );

        choose.addActionListener(
                e -> chooseResume()
        );

        open.addActionListener(
                e -> openResume()
        );

        buttons.add(choose);
        buttons.add(open);

        card.add(
                text,
                BorderLayout.CENTER
        );

        card.add(
                buttons,
                BorderLayout.EAST
        );

        return card;
    }

    private JPanel statCard(
            String title,
            String value
    ) {

        JPanel panel =
                new JPanel();

        panel.setBackground(Color.WHITE);

        panel.setBorder(
                new EmptyBorder(
                        15,
                        15,
                        15,
                        15
                )
        );

        panel.setLayout(
                new BoxLayout(
                        panel,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel valueLabel =
                new JLabel(value);

        valueLabel.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        28
                )
        );

        JLabel titleLabel =
                new JLabel(title);

        panel.add(valueLabel);

        panel.add(
                Box.createVerticalStrut(5)
        );

        panel.add(titleLabel);

        return panel;
    }

    // ==========================================================
    // RESUME FUNCTIONALITY
    // ==========================================================

    private void chooseResume() {

        JFileChooser chooser =
                new JFileChooser();

        chooser.setDialogTitle(
                "Select Resume"
        );

        chooser.setFileFilter(
                new javax.swing.filechooser
                        .FileNameExtensionFilter(
                                "Resume Files (*.pdf, *.doc, *.docx)",
                                "pdf",
                                "doc",
                                "docx"
                        )
        );

        chooser.setAcceptAllFileFilterUsed(
                false
        );

        int result =
                chooser.showOpenDialog(this);

        if (result !=
                JFileChooser.APPROVE_OPTION) {

            return;
        }

        File file =
                chooser.getSelectedFile();

        if (file == null ||
                !file.isFile()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please select a valid resume.",
                    "Invalid Resume",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        String filename =
                file.getName().toLowerCase();

        boolean valid =
                filename.endsWith(".pdf") ||
                filename.endsWith(".doc") ||
                filename.endsWith(".docx");

        if (!valid) {

            JOptionPane.showMessageDialog(
                    this,
                    "Only PDF, DOC and DOCX files are allowed.",
                    "Invalid File",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        selectedResume = file;

        updateResumeStatus();

        JOptionPane.showMessageDialog(
                this,
                "Resume selected successfully:\n\n"
                        + file.getName()
                        + "\n\n"
                        + "Frontend-only feature: "
                        + "the resume is not uploaded to the backend.",
                "Resume Added",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void openResume() {

        if (selectedResume == null) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please add a resume first.",
                    "Resume",
                    JOptionPane.INFORMATION_MESSAGE
            );

            return;
        }

        if (!selectedResume.exists()) {

            selectedResume = null;

            updateResumeStatus();

            JOptionPane.showMessageDialog(
                    this,
                    "The selected resume could not be found.",
                    "Resume",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        if (!Desktop.isDesktopSupported()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Opening files is not supported on this system.",
                    "Resume",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        try {

            Desktop
                    .getDesktop()
                    .open(selectedResume);

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Could not open the resume:\n"
                            + e.getMessage(),
                    "Resume",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void updateResumeStatus() {

        String text;

        if (selectedResume == null) {

            text = "No resume selected";

        } else {

            text =
                    "Selected: "
                            + selectedResume.getName();
        }

        if (resumeStatusDashboard != null) {

            resumeStatusDashboard.setText(text);
        }

        if (resumeStatusProfile != null) {

            resumeStatusProfile.setText(text);
        }
    }

    // ==========================================================
    // JOBS
    // ==========================================================

    private JPanel createJobsPanel() {

        JPanel panel = basePanel();

        JPanel top =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT
                        )
                );

        top.setOpaque(false);

        JButton refresh =
                new JButton("Refresh");

        JButton apply =
                new JButton(
                        "Apply to Selected Job"
                );

        top.add(refresh);
        top.add(apply);

        String[] columns = {
                "Job ID",
                "Company",
                "Role",
                "Package",
                "Min CGPA",
                "Branches",
                "Deadline"
        };

        jobsModel =
                new DefaultTableModel(
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

        JTable table =
                new JTable(jobsModel);

        refreshJobs();

        refresh.addActionListener(
                e -> refreshJobs()
        );

        apply.addActionListener(
                e -> applyToJob(table)
        );

        panel.add(
                top,
                BorderLayout.NORTH
        );

        panel.add(
                new JScrollPane(table),
                BorderLayout.CENTER
        );

        return panel;
    }

    private void refreshJobs() {

        if (jobsModel == null) {
            return;
        }

        jobsModel.setRowCount(0);

        for (String[] job : jobs) {

            jobsModel.addRow(
                    job
            );
        }
    }

    private void applyToJob(
            JTable table
    ) {

        int row =
                table.getSelectedRow();

        if (row < 0) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please select a job first.",
                    "Apply",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        int index =
                table.convertRowIndexToModel(
                        row
                );

        String[] job =
                jobs.get(index);

        applications.add(
                new String[]{
                        job[0],
                        job[1],
                        job[2],
                        "Applied",
                        "Pending"
                }
        );

        JOptionPane.showMessageDialog(
                this,
                "Application submitted successfully.",
                "Application",
                JOptionPane.INFORMATION_MESSAGE
        );

        refreshApplications();
    }

    // ==========================================================
    // APPLICATIONS
    // ==========================================================

    private JPanel createApplicationsPanel() {

        JPanel panel = basePanel();

        JButton refresh =
                new JButton("Refresh");

        refresh.addActionListener(
                e -> refreshApplications()
        );

        panel.add(
                refresh,
                BorderLayout.NORTH
        );

        String[] columns = {
                "Job ID",
                "Company",
                "Role",
                "Status",
                "Result"
        };

        applicationsModel =
                new DefaultTableModel(
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

        JTable table =
                new JTable(
                        applicationsModel
                );

        refreshApplications();

        panel.add(
                new JScrollPane(table),
                BorderLayout.CENTER
        );

        return panel;
    }

    private void refreshApplications() {

        if (applicationsModel == null) {
            return;
        }

        applicationsModel.setRowCount(0);

        for (String[] application :
                applications) {

            applicationsModel.addRow(
                    application
            );
        }
    }

    // ==========================================================
    // NOTIFICATIONS
    // ==========================================================

    private JPanel createNotificationsPanel() {

        JPanel panel = basePanel();

        String[] columns = {
                "Message",
                "Date"
        };

        notificationsModel =
                new DefaultTableModel(
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

        JTable table =
                new JTable(
                        notificationsModel
                );

        refreshNotifications();

        panel.add(
                new JScrollPane(table),
                BorderLayout.CENTER
        );

        return panel;
    }

    private void refreshNotifications() {

        if (notificationsModel == null) {
            return;
        }

        notificationsModel.setRowCount(0);

        for (String[] notification :
                notifications) {

            notificationsModel.addRow(
                    notification
            );
        }
    }

    // ==========================================================
    // PROFILE
    // ==========================================================

    private JPanel createProfilePanel() {

        JPanel panel = basePanel();

        JPanel wrapper =
                new JPanel();

        wrapper.setOpaque(false);

        wrapper.setLayout(
                new BoxLayout(
                        wrapper,
                        BoxLayout.Y_AXIS
                )
        );

        JPanel information =
                new JPanel(
                        new GridLayout(
                                2,
                                2,
                                10,
                                10
                        )
                );

        information.setBackground(
                Color.WHITE
        );

        information.setBorder(
                new EmptyBorder(
                        20,
                        20,
                        20,
                        20
                )
        );

        information.add(
                new JLabel("Name")
        );

        JTextField name =
                new JTextField(
                        studentName
                );

        name.setEditable(false);

        information.add(name);

        information.add(
                new JLabel("Email")
        );

        JTextField email =
                new JTextField(
                        studentEmail
                );

        email.setEditable(false);

        information.add(email);

        wrapper.add(information);

        wrapper.add(
                Box.createVerticalStrut(20)
        );

        JPanel resume =
                new JPanel(
                        new BorderLayout(
                                10,
                                10
                        )
                );

        resume.setBackground(Color.WHITE);

        resume.setBorder(
                new EmptyBorder(
                        20,
                        20,
                        20,
                        20
                )
        );

        JLabel title =
                new JLabel("My Resume");

        title.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        18
                )
        );

        resumeStatusProfile =
                new JLabel(
                        "No resume selected"
                );

        JPanel buttons =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT
                        )
                );

        buttons.setOpaque(false);

        JButton choose =
                new JButton(
                        "Add / Change Resume"
                );

        JButton open =
                new JButton(
                        "Open Resume"
                );

        choose.addActionListener(
                e -> chooseResume()
        );

        open.addActionListener(
                e -> openResume()
        );

        buttons.add(choose);
        buttons.add(open);

        resume.add(
                title,
                BorderLayout.WEST
        );

        resume.add(
                resumeStatusProfile,
                BorderLayout.CENTER
        );

        resume.add(
                buttons,
                BorderLayout.EAST
        );

        wrapper.add(resume);

        JLabel note =
                new JLabel(
                        "Note: Resume selection is frontend-only. "
                                + "No backend or database changes were made."
                );

        note.setBorder(
                new EmptyBorder(
                        10,
                        0,
                        0,
                        0
                )
        );

        wrapper.add(note);

        panel.add(
                wrapper,
                BorderLayout.NORTH
        );

        return panel;
    }

    private JPanel basePanel() {

        JPanel panel =
                new JPanel(
                        new BorderLayout(
                                15,
                                15
                        )
                );

        panel.setBackground(
                new Color(
                        247,
                        248,
                        245
                )
        );

        panel.setBorder(
                new EmptyBorder(
                        20,
                        20,
                        20,
                        20
                )
        );

        return panel;
    }

    // ==========================================================
    // MAIN
    // ==========================================================

    public static void main(
            String[] args
    ) {

        SwingUtilities.invokeLater(() -> {

            try {

                UIManager.setLookAndFeel(
                        UIManager
                                .getSystemLookAndFeelClassName()
                );

            } catch (Exception ignored) {
            }

            StudentDashboard dashboard =
                    new StudentDashboard(
                            "Srushti",
                            "srushti@gmail.com"
                    );

            dashboard.setVisible(true);
        });
    }
}