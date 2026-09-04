package com.placement.ui;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class LoginForm extends JFrame {

    private JComboBox<String> comboRole;
    private JTextField txtEmail;
    private JPasswordField txtPassword;
    private JButton btnLogin;

    private static final Color SIDEBAR_GREEN = new Color(20, 83, 45);
    private static final Color BG_GRAY = new Color(243, 244, 246);

    public LoginForm() {
        setTitle("Placement Eligibility Portal");
        setSize(1000, 620);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        initComponents();
    }

    private void initComponents() {

        JPanel sidebar = new JPanel();
        sidebar.setBackground(SIDEBAR_GREEN);
        sidebar.setPreferredSize(new Dimension(280, getHeight()));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(40, 30, 40, 30));

        JLabel title = new JLabel("PLACEMENT");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 26));

        JLabel subtitle = new JLabel("ELIGIBILITY PORTAL");
        subtitle.setForeground(new Color(200, 220, 210));
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JLabel heading = new JLabel("<html>Placement<br>Management<br>System</html>");
        heading.setForeground(Color.WHITE);
        heading.setFont(new Font("SansSerif", Font.BOLD, 22));
        heading.setBorder(BorderFactory.createEmptyBorder(30, 0, 15, 0));

        JLabel desc = new JLabel("<html>Manage placement activities,<br>eligibility and recruitment<br>from one centralized portal.</html>");
        desc.setForeground(new Color(200, 220, 210));
        desc.setFont(new Font("SansSerif", Font.PLAIN, 13));

        sidebar.add(title);
        sidebar.add(subtitle);
        sidebar.add(heading);
        sidebar.add(desc);

        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setBackground(BG_GRAY);

        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setPreferredSize(new Dimension(420, 400));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JLabel welcome = new JLabel("Welcome Back");
        welcome.setFont(new Font("SansSerif", Font.BOLD, 24));

        JLabel signInDesc = new JLabel("Sign in to your placement account");
        signInDesc.setForeground(Color.GRAY);
        signInDesc.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JLabel roleLabel = new JLabel("Role");
        roleLabel.setFont(new Font("SansSerif", Font.BOLD, 12));

        String[] roles = { "Director", "TPO", "TPC", "Recruiter" };
        comboRole = new JComboBox<>(roles);
        comboRole.setAlignmentX(Component.LEFT_ALIGNMENT);
        comboRole.setMaximumSize(new Dimension(360, 30));

        JLabel emailLabel = new JLabel("Email");
        emailLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        emailLabel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        txtEmail = new JTextField();
        txtEmail.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtEmail.setMaximumSize(new Dimension(360, 30));

        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        passLabel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        txtPassword = new JPasswordField();
        txtPassword.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtPassword.setMaximumSize(new Dimension(360, 30));

        btnLogin = new JButton("LOGIN");
        btnLogin.setBackground(SIDEBAR_GREEN);
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLogin.setMaximumSize(new Dimension(360, 40));
        btnLogin.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        btnLogin.addActionListener(e -> handleLogin());

        JLabel footer = new JLabel("Authorized users only");
        footer.setForeground(Color.GRAY);
        footer.setFont(new Font("SansSerif", Font.PLAIN, 11));
        footer.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        card.add(welcome);
        card.add(signInDesc);
        card.add(roleLabel);
        card.add(Box.createRigidArea(new Dimension(0, 5)));
        card.add(comboRole);
        card.add(emailLabel);
        card.add(Box.createRigidArea(new Dimension(0, 5)));
        card.add(txtEmail);
        card.add(passLabel);
        card.add(Box.createRigidArea(new Dimension(0, 5)));
        card.add(txtPassword);
        card.add(Box.createRigidArea(new Dimension(0, 20)));
        card.add(btnLogin);
        card.add(footer);

        rightPanel.add(card);

        add(sidebar, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);
    }

    private void handleLogin() {

        String role = (String) comboRole.getSelectedItem();
        String email = txtEmail.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter both email and password.",
                    "Missing fields", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try (Socket socket = new Socket("localhost", 5000);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()))) {

            writer.println("LOGIN");
            writer.println(role);
            writer.println(email);
            writer.println(password);

            String response = reader.readLine();
            String[] parts = response.split("\\|", 2);

            if (parts[0].equals("SUCCESS")) {
                JOptionPane.showMessageDialog(this, "Welcome, " + parts[1],
                        "Login successful", JOptionPane.INFORMATION_MESSAGE);
                txtEmail.setText("");
                txtPassword.setText("");
            } else {
                JOptionPane.showMessageDialog(this, parts.length > 1 ? parts[1] : "Login failed.",
                        "Login failed", JOptionPane.ERROR_MESSAGE);
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Could not reach server: " + e.getMessage(),
                    "Connection error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginForm().setVisible(true));
    }
}