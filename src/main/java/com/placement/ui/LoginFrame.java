package com.placement.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import com.placement.sockets.SocketClient;
public class LoginFrame extends JFrame {
    // =========================================================
    // GREEN THEME
    // =========================================================
    // Main green from dashboard reference
    private static final Color PRIMARY_GREEN =
            new Color(27, 117, 61);
    private static final Color DARK_GREEN =
            new Color(20, 92, 48);
    private static final Color LIGHT_GREEN =
            new Color(232, 245, 236);
    private static final Color BACKGROUND =
            new Color(247, 248, 245);
    private static final Color CARD =
            Color.WHITE;
    private static final Color TEXT =
            new Color(38, 50, 43);
    private static final Color SECONDARY_TEXT =
            new Color(105, 115, 108);
    private static final Color BORDER =
            new Color(214, 220, 215);
    private static final Color SUCCESS =
            new Color(22, 125, 58);
    private static final Color ERROR =
            new Color(190, 45, 45);
    // =========================================================
    // COMPONENTS
    // =========================================================
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JComboBox<String> roleComboBox;
    private JCheckBox rememberMe;
    private JLabel statusLabel;
    private JButton loginButton;
    private JButton clearButton;
    private String sessionToken;
    private String authenticatedDisplayName;
    // =========================================================
    // CONSTRUCTOR
    // =========================================================
    public LoginFrame() {
        setTitle("Placement Eligibility Portal");
        setSize(1000, 620);
        setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE
        );
        setLocationRelativeTo(null);
        setResizable(false);
        initializeUI();
    }
    // =========================================================
    // MAIN UI
    // =========================================================
    private void initializeUI() {
        JPanel mainPanel =
                new JPanel(new GridLayout(1, 2));
        mainPanel.setBackground(BACKGROUND);
        // LEFT GREEN PANEL
        JPanel leftPanel =
                createLeftPanel();
        // RIGHT LOGIN PANEL
        JPanel rightPanel =
                createRightPanel();
        mainPanel.add(leftPanel);
        mainPanel.add(rightPanel);
        add(mainPanel);
    }
    // =========================================================
    // LEFT GREEN BRANDING PANEL
    // =========================================================
    private JPanel createLeftPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(
                new BoxLayout(
                        panel,
                        BoxLayout.Y_AXIS
                )
        );
        panel.setBackground(PRIMARY_GREEN);
        panel.setBorder(
                new EmptyBorder(
                        60,
                        50,
                        50,
                        50
                )
        );
        // -----------------------------------------------------
        // LOGO / TITLE
        // -----------------------------------------------------
        JLabel logo =
                new JLabel("PLACEMENT");
        logo.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        30
                )
        );
        logo.setForeground(Color.WHITE);
        logo.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );
        panel.add(logo);
        panel.add(
                Box.createVerticalStrut(5)
        );
        JLabel portal =
                new JLabel(
                        "ELIGIBILITY PORTAL"
                );
        portal.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        13
                )
        );
        portal.setForeground(
                new Color(
                        211,
                        235,
                        218
                )
        );
        portal.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );
        panel.add(portal);
        // -----------------------------------------------------
        // SPACE
        // -----------------------------------------------------
        panel.add(
                Box.createVerticalStrut(85)
        );
        // -----------------------------------------------------
        // MAIN MESSAGE
        // -----------------------------------------------------
        JLabel heading =
                new JLabel(
                        "<html>"
                                + "Placement Management<br>"
                                + "Made Simple."
                                + "</html>"
                );
        heading.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        32
                )
        );
        heading.setForeground(
                Color.WHITE
        );
        heading.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );
        panel.add(heading);
        panel.add(
                Box.createVerticalStrut(20)
        );
        JLabel description =
                new JLabel(
                        "<html>"
                                + "A centralized platform for managing<br>"
                                + "students, placement drives and<br>"
                                + "recruitment activities."
                                + "</html>"
                );
        description.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        14
                )
        );
        description.setForeground(
                new Color(
                        218,
                        238,
                        222
                )
        );
        description.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );
        panel.add(description);
        // -----------------------------------------------------
        // PUSH FOOTER TO BOTTOM
        // -----------------------------------------------------
        panel.add(
                Box.createVerticalGlue()
        );
        // -----------------------------------------------------
        // SECURITY
        // -----------------------------------------------------
        JLabel security =
                new JLabel(
                        "●  Secure Role-Based Access"
                );
        security.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        13
                )
        );
        security.setForeground(
                new Color(
                        224,
                        242,
                        227
                )
        );
        security.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );
        panel.add(security);
        panel.add(
                Box.createVerticalStrut(8)
        );
        JLabel connection =
                new JLabel(
                        "TCP Client • Centralized Server"
                );
        connection.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        12
                )
        );
        connection.setForeground(
                new Color(
                        190,
                        225,
                        198
                )
        );
        connection.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );
        panel.add(connection);
        return panel;
    }
    // =========================================================
    // RIGHT LOGIN PANEL
    // =========================================================
    private JPanel createRightPanel() {
        JPanel backgroundPanel =
                new JPanel(
                        new GridBagLayout()
                );
        backgroundPanel.setBackground(
                BACKGROUND
        );
        // -----------------------------------------------------
        // LOGIN CARD
        // -----------------------------------------------------
        RoundedPanel card =
                new RoundedPanel(18);
        card.setBackground(CARD);
        card.setLayout(
                new BoxLayout(
                        card,
                        BoxLayout.Y_AXIS
                )
        );
        card.setPreferredSize(
                new Dimension(
                        410,
                        480
                )
        );
        card.setBorder(
                new EmptyBorder(
                        38,
                        42,
                        35,
                        42
                )
        );
        // -----------------------------------------------------
        // TITLE
        // -----------------------------------------------------
        JLabel title =
                new JLabel(
                        "Welcome Back"
                );
        title.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        27
                )
        );
        title.setForeground(TEXT);
        title.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );
        card.add(title);
        card.add(
                Box.createVerticalStrut(7)
        );
        JLabel subtitle =
                new JLabel(
                        "Sign in to access your placement dashboard"
                );
        subtitle.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        13
                )
        );
        subtitle.setForeground(
                SECONDARY_TEXT
        );
        subtitle.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );
        card.add(subtitle);
        card.add(
                Box.createVerticalStrut(27)
        );
        // =====================================================
        // ROLE
        // =====================================================
        JLabel roleLabel =
                createLabel("Select Role");
        card.add(roleLabel);
        card.add(
                Box.createVerticalStrut(7)
        );
        roleComboBox =
                new JComboBox<>(
                        new String[]{
                                "TPO", "TPC", "Director", "Dean", "Student", "Recruiter"
                        }
                );
        styleComboBox(
                roleComboBox
        );
        roleComboBox.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );
        card.add(roleComboBox);
        card.add(
                Box.createVerticalStrut(17)
        );
        // =====================================================
        // USERNAME
        // =====================================================
        JLabel usernameLabel =
                createLabel(
                        "Username / ID"
                );
        card.add(usernameLabel);
        card.add(
                Box.createVerticalStrut(7)
        );
        usernameField =
                new JTextField();
        styleTextField(
                usernameField
        );
        usernameField.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );
        card.add(usernameField);
        card.add(
                Box.createVerticalStrut(17)
        );
        // =====================================================
        // PASSWORD
        // =====================================================
        JLabel passwordLabel =
                createLabel(
                        "Password"
                );
        card.add(passwordLabel);
        card.add(
                Box.createVerticalStrut(7)
        );
        passwordField =
                new JPasswordField();
        styleTextField(
                passwordField
        );
        passwordField.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );
        card.add(passwordField);
        card.add(
                Box.createVerticalStrut(12)
        );
        // =====================================================
        // REMEMBER ME + FORGOT PASSWORD
        // =====================================================
        JPanel optionsPanel =
                new JPanel(
                        new BorderLayout()
                );
        optionsPanel.setOpaque(false);
        optionsPanel.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        30
                )
        );
        rememberMe =
                new JCheckBox(
                        "Remember me"
                );
        rememberMe.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        12
                )
        );
        rememberMe.setForeground(
                SECONDARY_TEXT
        );
        rememberMe.setOpaque(false);
        optionsPanel.add(
                rememberMe,
                BorderLayout.WEST
        );
        JLabel forgotPassword =
                new JLabel(
                        "<html><u>Forgot Password?</u></html>"
                );
        forgotPassword.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        12
                )
        );
        forgotPassword.setForeground(
                PRIMARY_GREEN
        );
        forgotPassword.setCursor(
                new Cursor(
                        Cursor.HAND_CURSOR
                )
        );
        optionsPanel.add(
                forgotPassword,
                BorderLayout.EAST
        );
        card.add(optionsPanel);
        card.add(
                Box.createVerticalStrut(20)
        );
        // =====================================================
        // LOGIN BUTTON
        // =====================================================
        loginButton =
                new JButton(
                        "LOGIN"
                );
        stylePrimaryButton(
                loginButton
        );
        loginButton.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );
        card.add(loginButton);
        card.add(
                Box.createVerticalStrut(10)
        );
        // =====================================================
        // CLEAR BUTTON
        // =====================================================
        clearButton =
                new JButton(
                        "CLEAR"
                );
        styleSecondaryButton(
                clearButton
        );
        clearButton.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );
        card.add(clearButton);
        card.add(
                Box.createVerticalStrut(13)
        );
        // =====================================================
        // STATUS
        // =====================================================
        statusLabel =
                new JLabel(" ");
        statusLabel.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        12
                )
        );
        statusLabel.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );
        card.add(statusLabel);
        // =====================================================
        // EVENTS
        // =====================================================
        loginButton.addActionListener(
                e -> handleLogin()
        );
        clearButton.addActionListener(
                e -> clearFields()
        );
        passwordField.addActionListener(
                e -> handleLogin()
        );
        backgroundPanel.add(card);
        return backgroundPanel;
    }
    // =========================================================
    // LOGIN HANDLER
    // =========================================================
    private void handleLogin() {
        String username =
                usernameField
                        .getText()
                        .trim();
        String password =
                new String(
                        passwordField
                                .getPassword()
                );
        String role =
                (String)
                        roleComboBox
                                .getSelectedItem();
        // -----------------------------------------------------
        // VALIDATION
        // -----------------------------------------------------
        if (username.isEmpty()) {
            showError(
                    "Please enter your username / ID."
            );
            usernameField.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            showError(
                    "Please enter your password."
            );
            passwordField.requestFocus();
            return;
        }
        // -----------------------------------------------------
        // DISABLE LOGIN
        // -----------------------------------------------------
        loginButton.setEnabled(false);
        showStatus(
                "Authenticating...",
                SECONDARY_TEXT
        );
        // -----------------------------------------------------
        // BACKGROUND AUTHENTICATION
        // -----------------------------------------------------
        SwingWorker<Boolean, Void> worker =
                new SwingWorker<>() {
                    @Override
                    protected Boolean doInBackground() {
                        return authenticate(
                                username,
                                password,
                                role
                        );
                    }
                    @Override
                    protected void done() {
                        try {
                            boolean authenticated =
                                    get();
                            if (authenticated) {
                                showStatus(
                                        "Login successful.",
                                        SUCCESS
                                );
                                openDashboard(
                                        username,
                                        role
                                );
                            } else {
                                showError(
                                        "Invalid credentials or role."
                                );
                                passwordField
                                        .setText("");
                                passwordField
                                        .requestFocus();
                            }
                        } catch (Exception ex) {
                            showError(
                                    "Unable to connect to server."
                            );
                        } finally {
                            loginButton
                                    .setEnabled(true);
                        }
                    }
                };
        worker.execute();
    }
    // =========================================================
    // TEMPORARY AUTHENTICATION
    // =========================================================
    private boolean authenticate(
            String username,
            String password,
            String role) {
        try {
            SocketClient.Response response = new SocketClient().sendRequest(
                    "LOGIN", role, username, password);
            if (!response.success) {
                showError(response.payload);
                return false;
            }
            String[] parts = response.parts();
            if (parts.length < 1 || parts[0].isBlank()) {
                showError("Server returned an invalid session.");
                return false;
            }
            sessionToken = parts[0];
            authenticatedDisplayName = parts.length > 1 ? parts[1] : username;
            return true;
        } catch (Exception ex) {
            showError("Unable to connect to server: " + ex.getMessage());
            return false;
        }
    }
    // =========================================================
    // OPEN DASHBOARD
    // =========================================================
    private void openDashboard(
            String username,
            String role) {
        JFrame dashboard;
        String normalized = role == null ? "" : role.trim().toUpperCase();
        switch (normalized) {
            case "TPO" -> dashboard = new TPODashboard();
            case "TPC" -> dashboard = new TPCDashboard(authenticatedDisplayName, sessionToken);
            case "DIRECTOR" -> dashboard = new DirectorDashboard();
            case "DEAN" -> dashboard = new DeanDashboard();
            case "STUDENT" -> dashboard = new StudentDashboard(authenticatedDisplayName, username);
            case "RECRUITER" -> dashboard = new RecruiterDashboard(authenticatedDisplayName);
            default -> {
                showError("Unsupported role: " + role);
                return;
            }
        }
        dashboard.setVisible(true);
        dispose();
    }
    // =========================================================
    // CLEAR FIELDS
    // =========================================================
    private void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
        roleComboBox.setSelectedIndex(0);
        rememberMe.setSelected(false);
        statusLabel.setText(" ");
        usernameField.requestFocus();
    }
    // =========================================================
    // LABEL STYLE
    // =========================================================
    private JLabel createLabel(
            String text) {
        JLabel label =
                new JLabel(text);
        label.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        13
                )
        );
        label.setForeground(TEXT);
        label.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );
        return label;
    }
    // =========================================================
    // TEXT FIELD STYLE
    // =========================================================
    private void styleTextField(
            JTextField field) {
        field.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        14
                )
        );
        field.setPreferredSize(
                new Dimension(
                        320,
                        42
                )
        );
        field.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        42
                )
        );
        field.setBorder(
                BorderFactory
                        .createCompoundBorder(
                                BorderFactory
                                        .createLineBorder(
                                                BORDER,
                                                1
                                        ),
                                new EmptyBorder(
                                        8,
                                        12,
                                        8,
                                        12
                                )
                        )
        );
        field.setBackground(
                Color.WHITE
        );
        field.setForeground(TEXT);
    }
    // =========================================================
    // COMBO BOX STYLE
    // =========================================================
    private void styleComboBox(
            JComboBox<String> comboBox) {
        comboBox.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        13
                )
        );
        comboBox.setPreferredSize(
                new Dimension(
                        320,
                        42
                )
        );
        comboBox.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        42
                )
        );
        comboBox.setBackground(
                Color.WHITE
        );
        comboBox.setForeground(
                TEXT
        );
        comboBox.setBorder(
                BorderFactory
                        .createLineBorder(
                                BORDER
                        )
        );
    }
    // =========================================================
    // GREEN LOGIN BUTTON
    // =========================================================
    private void stylePrimaryButton(
            JButton button) {
        button.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        13
                )
        );
        button.setForeground(
                Color.WHITE
        );
        button.setBackground(
                PRIMARY_GREEN
        );
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(
                new Dimension(
                        320,
                        43
                )
        );
        button.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        43
                )
        );
        button.setCursor(
                new Cursor(
                        Cursor.HAND_CURSOR
                )
        );
    }
    // =========================================================
    // SECONDARY BUTTON
    // =========================================================
    private void styleSecondaryButton(
            JButton button) {
        button.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        12
                )
        );
        button.setForeground(
                PRIMARY_GREEN
        );
        button.setBackground(
                Color.WHITE
        );
        button.setFocusPainted(false);
        button.setBorder(
                BorderFactory
                        .createLineBorder(
                                PRIMARY_GREEN
                        )
        );
        button.setPreferredSize(
                new Dimension(
                        320,
                        40
                )
        );
        button.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        40
                )
        );
        button.setCursor(
                new Cursor(
                        Cursor.HAND_CURSOR
                )
        );
    }
    // =========================================================
    // ERROR MESSAGE
    // =========================================================
    private void showError(
            String message) {
        statusLabel.setText(
                "✕  " + message
        );
        statusLabel.setForeground(
                ERROR
        );
    }
    // =========================================================
    // STATUS MESSAGE
    // =========================================================
    private void showStatus(
            String message,
            Color color) {
        statusLabel.setText(
                message
        );
        statusLabel.setForeground(
                color
        );
    }
    // =========================================================
    // ROUNDED CARD
    // =========================================================
    static class RoundedPanel
            extends JPanel {
        private final int radius;
        public RoundedPanel(
                int radius) {
            this.radius = radius;
            setOpaque(false);
        }
        @Override
        protected void paintComponent(
                Graphics g) {
            Graphics2D g2 =
                    (Graphics2D)
                            g.create();
            g2.setRenderingHint(
                    RenderingHints
                            .KEY_ANTIALIASING,
                    RenderingHints
                            .VALUE_ANTIALIAS_ON
            );
            g2.setColor(
                    getBackground()
            );
            g2.fill(
                    new RoundRectangle2D.Float(
                            0,
                            0,
                            getWidth(),
                            getHeight(),
                            radius,
                            radius
                    )
            );
            g2.dispose();
            super.paintComponent(g);
        }
    }
    // =========================================================
    // MAIN
    // =========================================================
    public static void main(
            String[] args) {
        try {
            UIManager.setLookAndFeel(
                    UIManager
                            .getSystemLookAndFeelClassName()
            );
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(
                () -> {
                    LoginFrame frame =
                            new LoginFrame();
                    frame.setVisible(true);
                }
        );
    }
}
