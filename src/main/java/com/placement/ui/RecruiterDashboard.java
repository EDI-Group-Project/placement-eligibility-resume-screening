package com.placement.ui;

import com.placement.sockets.SocketClient;
import javax.swing.*;
import java.awt.*;
import java.util.List;

public class RecruiterDashboard extends JFrame {
    private final String displayName;
    private final DefaultListModel<String> jobsModel = new DefaultListModel<>();

    public RecruiterDashboard(String displayName) {
        this.displayName = displayName;
        setTitle("Placement Eligibility Portal - Recruiter");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(16, 16));

        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(20,20,10,20));
        JLabel title = new JLabel("Recruiter Dashboard");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        JLabel user = new JLabel(displayName);
        user.setFont(new Font("SansSerif", Font.PLAIN, 13));
        header.add(title, BorderLayout.WEST);
        header.add(user, BorderLayout.EAST);

        JList<String> jobs = new JList<>(jobsModel);
        jobs.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JPanel center = new JPanel(new BorderLayout(8,8));
        center.setBorder(BorderFactory.createEmptyBorder(10,20,10,20));
        center.add(new JLabel("Active Job Postings"), BorderLayout.NORTH);
        center.add(new JScrollPane(jobs), BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refresh = new JButton("Refresh");
        JButton logout = new JButton("Logout");
        refresh.addActionListener(e -> loadJobs());
        logout.addActionListener(e -> { dispose(); new LoginFrame().setVisible(true); });
        footer.add(refresh);
        footer.add(logout);

        add(header, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        loadJobs();
    }

    private void loadJobs() {
        jobsModel.clear();
        try {
            SocketClient.ListResponse r = new SocketClient().sendListRequest("GET_JOBS");
            if (!r.success) {
                jobsModel.addElement("Unable to load jobs: " + r.errorMessage);
                return;
            }
            for (String line : r.lines) {
                String[] p = line.split("\\|", -1);
                if (p.length >= 8) {
                    jobsModel.addElement(p[1] + " | " + p[2] + " | Min CGPA " + p[4] +
                            " | Deadline " + p[p.length - 1]);
                }
            }
            if (jobsModel.isEmpty()) jobsModel.addElement("No active job postings.");
        } catch (Exception e) {
            jobsModel.addElement("Server error: " + e.getMessage());
        }
    }
}
