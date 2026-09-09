package com.placement.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class DirectorDashboard extends JFrame {

    // ================= COLORS =================

    private static final Color SIDEBAR_GREEN = new Color(20, 83, 45);
    private static final Color BG_GRAY = new Color(243, 244, 246);
    private static final Color LIGHT_GREEN = new Color(230, 240, 233);
    private static final Color DARK_TEXT = new Color(31, 41, 35);
    private static final Color GREY_TEXT = new Color(107, 114, 110);
    private static final Color BORDER = new Color(220, 225, 222);

    // ================= CONSTRUCTOR =================

    public DirectorDashboard() {

        setTitle("Placement Eligibility Portal - Director Dashboard");
        setSize(1000, 620);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        initComponents();
    }

    // ================= INITIALIZE COMPONENTS =================

    private void initComponents() {

        // =====================================================
        // SIDEBAR
        // =====================================================

        JPanel sidebar = new JPanel();

        sidebar.setBackground(SIDEBAR_GREEN);
        sidebar.setPreferredSize(new Dimension(220, getHeight()));

        sidebar.setLayout(
                new BoxLayout(sidebar, BoxLayout.Y_AXIS)
        );

        sidebar.setBorder(
                BorderFactory.createEmptyBorder(
                        30, 20, 30, 20
                )
        );

        // Portal title

        JLabel title = new JLabel("PLACEMENT");

        title.setForeground(Color.WHITE);
        title.setFont(
                new Font("SansSerif", Font.BOLD, 22)
        );

        JLabel subtitle = new JLabel("ELIGIBILITY PORTAL");

        subtitle.setForeground(
                new Color(200, 220, 210)
        );

        subtitle.setFont(
                new Font("SansSerif", Font.PLAIN, 11)
        );

        sidebar.add(title);
        sidebar.add(subtitle);

        sidebar.add(
                Box.createVerticalStrut(35)
        );

        // Sidebar buttons

        JButton dashboardButton =
                createSidebarButton("Dashboard");

        JButton placementButton =
                createSidebarButton("Placement Overview");

        JButton companiesButton =
                createSidebarButton("Companies");

        JButton studentsButton =
                createSidebarButton("Students");

        JButton reportsButton =
                createSidebarButton("Reports");

        JButton announcementsButton =
                createSidebarButton("Announcements");

        sidebar.add(dashboardButton);
        sidebar.add(placementButton);
        sidebar.add(companiesButton);
        sidebar.add(studentsButton);
        sidebar.add(reportsButton);
        sidebar.add(announcementsButton);

        // Push logout to bottom

        sidebar.add(
                Box.createVerticalGlue()
        );

        JButton logoutButton =
                createSidebarButton("Logout");

        sidebar.add(logoutButton);

        // =====================================================
        // RIGHT SIDE
        // =====================================================

        JPanel rightPanel =
                new JPanel(new BorderLayout());

        rightPanel.setBackground(BG_GRAY);

        // =====================================================
        // HEADER
        // =====================================================

        JPanel header = new JPanel(
                new BorderLayout()
        );

        header.setBackground(Color.WHITE);

        header.setBorder(
                BorderFactory.createEmptyBorder(
                        18, 25, 18, 25
                )
        );

        // Greeting

        JPanel greetingPanel = new JPanel();

        greetingPanel.setBackground(Color.WHITE);

        greetingPanel.setLayout(
                new BoxLayout(
                        greetingPanel,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel greeting =
                new JLabel("Good morning, Director");

        greeting.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        21
                )
        );

        greeting.setForeground(DARK_TEXT);

        JLabel greetingDescription =
                new JLabel(
                        "Placement Management Overview"
                );

        greetingDescription.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        12
                )
        );

        greetingDescription.setForeground(GREY_TEXT);

        greetingPanel.add(greeting);

        greetingPanel.add(
                Box.createVerticalStrut(4)
        );

        greetingPanel.add(
                greetingDescription
        );

        header.add(
                greetingPanel,
                BorderLayout.WEST
        );

        // Director profile

        JPanel profilePanel = new JPanel(
                new FlowLayout(
                        FlowLayout.RIGHT,
                        10,
                        0
                )
        );

        profilePanel.setBackground(Color.WHITE);

        JLabel initials = new JLabel("DR");

        initials.setPreferredSize(
                new Dimension(38, 38)
        );

        initials.setHorizontalAlignment(
                SwingConstants.CENTER
        );

        initials.setVerticalAlignment(
                SwingConstants.CENTER
        );

        initials.setOpaque(true);

        initials.setBackground(LIGHT_GREEN);

        initials.setForeground(SIDEBAR_GREEN);

        initials.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        13
                )
        );

        JLabel directorLabel =
                new JLabel("Director");

        directorLabel.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        13
                )
        );

        directorLabel.setForeground(DARK_TEXT);

        profilePanel.add(initials);
        profilePanel.add(directorLabel);

        header.add(
                profilePanel,
                BorderLayout.EAST
        );

        rightPanel.add(
                header,
                BorderLayout.NORTH
        );

        // =====================================================
        // MAIN CONTENT
        // =====================================================

        JPanel content = new JPanel();

        content.setBackground(BG_GRAY);

        content.setBorder(
                BorderFactory.createEmptyBorder(
                        20, 25, 20, 25
                )
        );

        content.setLayout(
                new BoxLayout(
                        content,
                        BoxLayout.Y_AXIS
                )
        );

        // =====================================================
        // STATISTICS CARDS
        // =====================================================

        JPanel cardsPanel = new JPanel(
                new GridLayout(
                        1, 4, 12, 0
                )
        );

        cardsPanel.setBackground(BG_GRAY);

        cardsPanel.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        105
                )
        );

        cardsPanel.add(
                createCard(
                        "Total Students",
                        "1240",
                        "Registered students"
                )
        );

        cardsPanel.add(
                createCard(
                        "Placed Students",
                        "842",
                        "Students placed"
                )
        );

        cardsPanel.add(
                createCard(
                        "Placement Rate",
                        "67.9%",
                        "Overall placement"
                )
        );

        cardsPanel.add(
                createCard(
                        "Active Companies",
                        "38",
                        "Recruiting companies"
                )
        );

        content.add(cardsPanel);

        content.add(
                Box.createVerticalStrut(18)
        );

        // =====================================================
        // MIDDLE SECTION
        // =====================================================

        JPanel middlePanel = new JPanel(
                new BorderLayout(12, 0)
        );

        middlePanel.setBackground(BG_GRAY);

        middlePanel.setPreferredSize(
                new Dimension(900, 265)
        );

        middlePanel.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        265
                )
        );

        // =====================================================
        // PLACEMENT TABLE
        // =====================================================

        JPanel tablePanel =
                createWhitePanel();

        JLabel tableTitle =
                new JLabel(
                        "Placement Overview"
                );

        tableTitle.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        15
                )
        );

        tableTitle.setForeground(DARK_TEXT);

        tablePanel.add(
                tableTitle,
                BorderLayout.NORTH
        );

        String[] columns = {
                "Company",
                "Drive",
                "Selected",
                "Status"
        };

        Object[][] data = {
                {
                        "TCS",
                        "Software Engineer",
                        "56",
                        "Completed"
                },
                {
                        "Infosys",
                        "System Engineer",
                        "42",
                        "Completed"
                },
                {
                        "Accenture",
                        "Associate",
                        "35",
                        "Ongoing"
                },
                {
                        "Cognizant",
                        "Programmer Analyst",
                        "28",
                        "Completed"
                },
                {
                        "Wipro",
                        "Project Engineer",
                        "24",
                        "Ongoing"
                }
        };

        DefaultTableModel model =
                new DefaultTableModel(
                        data,
                        columns
                ) {

                    @Override
                    public boolean isCellEditable(
                            int row,
                            int column) {

                        return false;
                    }
                };

        JTable table = new JTable(model);

        table.setRowHeight(30);

        table.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        12
                )
        );

        table.getTableHeader().setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        12
                )
        );

        table.getTableHeader().setBackground(
                LIGHT_GREEN
        );

        table.getTableHeader().setForeground(
                DARK_TEXT
        );

        table.setGridColor(BORDER);

        table.setShowVerticalLines(false);

        // Center selected and status columns

        DefaultTableCellRenderer centerRenderer =
                new DefaultTableCellRenderer();

        centerRenderer.setHorizontalAlignment(
                SwingConstants.CENTER
        );

        for (int i = 2;
             i < table.getColumnCount();
             i++) {

            table.getColumnModel()
                    .getColumn(i)
                    .setCellRenderer(
                            centerRenderer
                    );
        }

        JScrollPane scrollPane =
                new JScrollPane(table);

        scrollPane.setBorder(
                BorderFactory.createEmptyBorder(
                        12, 0, 0, 0
                )
        );

        tablePanel.add(
                scrollPane,
                BorderLayout.CENTER
        );

        middlePanel.add(
                tablePanel,
                BorderLayout.CENTER
        );

        // =====================================================
        // RECENT ACTIVITY
        // =====================================================

        JPanel activityPanel =
                createWhitePanel();

        activityPanel.setPreferredSize(
                new Dimension(290, 265)
        );

        JLabel activityTitle =
                new JLabel(
                        "Recent Placement Activity"
                );

        activityTitle.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        15
                )
        );

        activityTitle.setForeground(DARK_TEXT);

        activityPanel.add(
                activityTitle,
                BorderLayout.NORTH
        );

        JPanel activityList =
                new JPanel();

        activityList.setBackground(Color.WHITE);

        activityList.setLayout(
                new BoxLayout(
                        activityList,
                        BoxLayout.Y_AXIS
                )
        );

        addActivity(
                activityList,
                "TCS",
                "Drive completed",
                "Today"
        );

        addActivity(
                activityList,
                "Infosys",
                "42 students selected",
                "Yesterday"
        );

        addActivity(
                activityList,
                "Accenture",
                "Drive in progress",
                "2 days ago"
        );

        addActivity(
                activityList,
                "Cognizant",
                "18 students selected",
                "3 days ago"
        );

        activityPanel.add(
                activityList,
                BorderLayout.CENTER
        );

        middlePanel.add(
                activityPanel,
                BorderLayout.EAST
        );

        content.add(middlePanel);

        content.add(
                Box.createVerticalStrut(15)
        );

        // =====================================================
        // ANNOUNCEMENTS
        // =====================================================

        JPanel announcementPanel =
                createWhitePanel();

        JLabel announcementTitle =
                new JLabel(
                        "Important Announcements"
                );

        announcementTitle.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        15
                )
        );

        announcementTitle.setForeground(DARK_TEXT);

        announcementPanel.add(
                announcementTitle,
                BorderLayout.NORTH
        );

        JPanel announcementList =
                new JPanel();

        announcementList.setBackground(Color.WHITE);

        announcementList.setLayout(
                new BoxLayout(
                        announcementList,
                        BoxLayout.Y_AXIS
                )
        );

        addAnnouncement(
                announcementList,
                "Placement review meeting scheduled for Friday."
        );

        addAnnouncement(
                announcementList,
                "All departments must submit placement reports."
        );

        addAnnouncement(
                announcementList,
                "New recruitment drives have been added."
        );

        announcementPanel.add(
                announcementList,
                BorderLayout.CENTER
        );

        content.add(announcementPanel);

        // =====================================================
        // SCROLLABLE CONTENT
        // =====================================================

        JScrollPane contentScroll =
                new JScrollPane(content);

        contentScroll.setBorder(null);

        contentScroll.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );

        contentScroll.getVerticalScrollBar()
                .setUnitIncrement(15);

        rightPanel.add(
                contentScroll,
                BorderLayout.CENTER
        );

        // =====================================================
        // BUTTON ACTIONS
        // =====================================================

        dashboardButton.addActionListener(e ->
                showMessage(
                        "Dashboard",
                        "You are already on the Director Dashboard."
                )
        );

        placementButton.addActionListener(e ->
                showMessage(
                        "Placement Overview",
                        "Placement Overview selected."
                )
        );

        companiesButton.addActionListener(e ->
                showMessage(
                        "Companies",
                        "Company information selected."
                )
        );

        studentsButton.addActionListener(e ->
                showMessage(
                        "Students",
                        "Student information selected."
                )
        );

        reportsButton.addActionListener(e ->
                showMessage(
                        "Reports",
                        "Placement reports selected."
                )
        );

        announcementsButton.addActionListener(e ->
                showMessage(
                        "Announcements",
                        "Announcements selected."
                )
        );

        logoutButton.addActionListener(e -> {

            int choice =
                    JOptionPane.showConfirmDialog(
                            this,
                            "Are you sure you want to logout?",
                            "Logout",
                            JOptionPane.YES_NO_OPTION
                    );

            if (choice ==
                    JOptionPane.YES_OPTION) {

                dispose();
            }
        });

        // =====================================================
        // ADD PANELS
        // =====================================================

        add(
                sidebar,
                BorderLayout.WEST
        );

        add(
                rightPanel,
                BorderLayout.CENTER
        );
    }

    // =========================================================
    // SIDEBAR BUTTON
    // =========================================================

    private JButton createSidebarButton(
            String text) {

        JButton button =
                new JButton(text);

        button.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        button.setMaximumSize(
                new Dimension(
                        220,
                        42
                )
        );

        button.setPreferredSize(
                new Dimension(
                        220,
                        42
                )
        );

        button.setHorizontalAlignment(
                SwingConstants.LEFT
        );

        button.setBorder(
                BorderFactory.createEmptyBorder(
                        0, 15, 0, 5
                )
        );

        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);

        button.setBackground(
                SIDEBAR_GREEN
        );

        button.setForeground(
                Color.WHITE
        );

        button.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        13
                )
        );

        return button;
    }

    // =========================================================
    // CARD
    // =========================================================

    private JPanel createCard(
            String title,
            String value,
            String description) {

        JPanel card = new JPanel();

        card.setBackground(Color.WHITE);

        card.setLayout(
                new BoxLayout(
                        card,
                        BoxLayout.Y_AXIS
                )
        );

        card.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                BORDER
                        ),
                        BorderFactory.createEmptyBorder(
                                15, 15, 15, 15
                        )
                )
        );

        JLabel titleLabel =
                new JLabel(title);

        titleLabel.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        11
                )
        );

        titleLabel.setForeground(
                GREY_TEXT
        );

        JLabel valueLabel =
                new JLabel(value);

        valueLabel.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        23
                )
        );

        valueLabel.setForeground(
                SIDEBAR_GREEN
        );

        JLabel descriptionLabel =
                new JLabel(description);

        descriptionLabel.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        10
                )
        );

        descriptionLabel.setForeground(
                GREY_TEXT
        );

        card.add(titleLabel);

        card.add(
                Box.createVerticalStrut(5)
        );

        card.add(valueLabel);

        card.add(
                Box.createVerticalStrut(3)
        );

        card.add(descriptionLabel);

        return card;
    }

    // =========================================================
    // WHITE PANEL
    // =========================================================

    private JPanel createWhitePanel() {

        JPanel panel =
                new JPanel(new BorderLayout());

        panel.setBackground(Color.WHITE);

        panel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                BORDER
                        ),
                        BorderFactory.createEmptyBorder(
                                15, 15, 15, 15
                        )
                )
        );

        return panel;
    }

    // =========================================================
    // ACTIVITY
    // =========================================================

    private void addActivity(
            JPanel parent,
            String company,
            String activity,
            String date) {

        JPanel item =
                new JPanel(
                        new BorderLayout()
                );

        item.setBackground(Color.WHITE);

        item.setBorder(
                BorderFactory.createEmptyBorder(
                        8, 0, 8, 0
                )
        );

        JPanel left =
                new JPanel();

        left.setBackground(Color.WHITE);

        left.setLayout(
                new BoxLayout(
                        left,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel companyLabel =
                new JLabel(company);

        companyLabel.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        12
                )
        );

        companyLabel.setForeground(
                DARK_TEXT
        );

        JLabel activityLabel =
                new JLabel(activity);

        activityLabel.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        10
                )
        );

        activityLabel.setForeground(
                GREY_TEXT
        );

        left.add(companyLabel);

        left.add(
                Box.createVerticalStrut(2)
        );

        left.add(activityLabel);

        JLabel dateLabel =
                new JLabel(date);

        dateLabel.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        9
                )
        );

        dateLabel.setForeground(
                GREY_TEXT
        );

        item.add(
                left,
                BorderLayout.CENTER
        );

        item.add(
                dateLabel,
                BorderLayout.EAST
        );

        parent.add(item);

        JSeparator separator =
                new JSeparator();

        separator.setForeground(BORDER);

        parent.add(separator);
    }

    // =========================================================
    // ANNOUNCEMENT
    // =========================================================

    private void addAnnouncement(
            JPanel parent,
            String text) {

        JLabel label =
                new JLabel("•  " + text);

        label.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        11
                )
        );

        label.setForeground(DARK_TEXT);

        label.setBorder(
                BorderFactory.createEmptyBorder(
                        5, 0, 5, 0
                )
        );

        parent.add(label);
    }

    // =========================================================
    // MESSAGE
    // =========================================================

    private void showMessage(
            String title,
            String message) {

        JOptionPane.showMessageDialog(
                this,
                message,
                title,
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    // =========================================================
    // MAIN
    // =========================================================

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            DirectorDashboard dashboard =
                    new DirectorDashboard();

            dashboard.setVisible(true);
        });
    }
}