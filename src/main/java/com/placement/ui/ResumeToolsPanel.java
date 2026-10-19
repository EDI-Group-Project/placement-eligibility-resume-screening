package com.placement.ui;

import com.placement.sockets.SocketClient;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** Resume exports, applicants and batch tools for ADMIN/TPO/TPC. */
public final class ResumeToolsPanel extends JPanel {
    private static final int MAX_FILE_BYTES = 2 * 1024 * 1024;
    private final String role;
    private final String token;
    private final SocketClient client = new SocketClient();

    private final JTextField studentSearch = new JTextField(12);
    private final JComboBox<String> studentDept = new JComboBox<>(new String[]{"ALL", "CSE", "IT", "ECE", "MECH", "CIVIL"});
    private final JComboBox<String> studentYear = new JComboBox<>(new String[]{"ALL", "2026", "2027", "2028", "2029"});
    private final JComboBox<String> minCgpa = new JComboBox<>(new String[]{"ALL", "6", "7", "8", "9"});
    private final JComboBox<String> maxBacklogs = new JComboBox<>(new String[]{"ALL", "0", "1", "2", "3"});
    private final JComboBox<String> semester = new JComboBox<>(new String[]{"ALL", "1", "2", "3", "4", "5", "6", "7", "8"});
    private final JComboBox<String> sort = new JComboBox<>(new String[]{"CGPA DESC", "CGPA ASC", "NAME ASC", "NAME DESC", "YEAR ASC", "BACKLOGS ASC"});

    private final DefaultTableModel studentPreviewModel = new DefaultTableModel(new String[]{
            "PRN", "Name", "Email", "Department", "CGPA", "Year", "Backlogs", "Semester", "Phone", "Skills"
    }, 0) { @Override public boolean isCellEditable(int row, int column) { return false; } };
    private final JTable studentPreviewTable = new JTable(studentPreviewModel);

    private final JComboBox<JobOption> applicantJob = new JComboBox<>();
    private final DefaultTableModel applicantsModel = new DefaultTableModel(new String[]{
            "PRN", "Name", "Email", "Department", "CGPA", "Year", "Backlogs", "Semester", "Phone", "Skills",
            "Application Status", "Screening Score", "Applied At"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable applicantsTable = new JTable(applicantsModel);

    private final JComboBox<JobOption> batchJob = new JComboBox<>();
    private final JTextField batchIdField = new JTextField(24);
    private final JLabel batchStatus = new JLabel("Select resumes to upload a batch of up to 20 files.");
    private final DefaultTableModel batchModel = new DefaultTableModel(new String[]{
            "Index", "Student Reference", "Student ID", "PRN", "Name", "File", "Status", "Words", "ATS Score",
            "ATS Warnings", "Skills", "Screening Total", "Skill Score", "Matched Skills", "Missing Skills", "Message"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable batchTable = new JTable(batchModel);

    public ResumeToolsPanel(String role, String token) {
        this.role = role == null ? "" : role.toUpperCase();
        this.token = token;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Student Export", buildStudentExportTab());
        tabs.addTab("Job Applicants", buildApplicantsTab());
        tabs.addTab("Batch Resume Upload & History", buildBatchTab());
        add(tabs, BorderLayout.CENTER);
        loadJobs();
    }

    private JPanel buildStudentExportTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        JPanel filters = new JPanel(new GridBagLayout());
        filters.setBorder(BorderFactory.createTitledBorder("Filter students"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 7, 5, 7);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        addFilter(filters, c, 0, 0, "Search name / PRN / email", studentSearch);
        addFilter(filters, c, 1, 0, "Department", studentDept);
        addFilter(filters, c, 2, 0, "Passing year", studentYear);
        addFilter(filters, c, 0, 1, "Minimum CGPA", minCgpa);
        addFilter(filters, c, 1, 1, "Maximum backlogs", maxBacklogs);
        addFilter(filters, c, 2, 1, "Semester", semester);
        addFilter(filters, c, 0, 2, "Sort by", sort);

        JButton preview = new JButton("Search / Preview Students");
        preview.addActionListener(e -> previewStudents(preview));
        JButton clear = new JButton("Clear Filters");
        clear.addActionListener(e -> {
            studentSearch.setText(""); studentDept.setSelectedIndex(0); studentYear.setSelectedIndex(0);
            minCgpa.setSelectedIndex(0); maxBacklogs.setSelectedIndex(0); semester.setSelectedIndex(0); sort.setSelectedIndex(0);
            studentPreviewModel.setRowCount(0);
        });
        JButton export = new JButton("Export Filtered Students to Excel (.xlsx)");
        export.addActionListener(e -> exportStudents(export));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        actions.add(preview); actions.add(clear); actions.add(export);
        JPanel north = new JPanel(new BorderLayout(6, 6));
        north.add(filters, BorderLayout.CENTER);
        north.add(actions, BorderLayout.SOUTH);

        studentPreviewTable.setAutoCreateRowSorter(true);
        studentPreviewTable.setFillsViewportHeight(true);
        studentPreviewTable.setRowHeight(24);
        JLabel hint = new JLabel("Search / Preview displays matching database records here. Export creates an Excel file using the same filters.");
        hint.setBorder(BorderFactory.createEmptyBorder(2, 3, 4, 3));
        JPanel center = new JPanel(new BorderLayout(4, 4));
        center.add(hint, BorderLayout.NORTH);
        center.add(new JScrollPane(studentPreviewTable), BorderLayout.CENTER);
        panel.add(north, BorderLayout.NORTH);
        panel.add(center, BorderLayout.CENTER);
        return panel;
    }

    private static void addFilter(JPanel panel, GridBagConstraints c, int column, int row, String label, JComponent field) {
        c.gridx = column; c.gridy = row; c.weightx = 0;
        panel.add(new JLabel(label), c);
        c.gridx = column + 1; c.weightx = 1;
        field.setMinimumSize(new Dimension(100, field.getPreferredSize().height));
        field.setPreferredSize(new Dimension(150, field.getPreferredSize().height));
        panel.add(field, c);
    }

    private void previewStudents(JButton button) {
        button.setEnabled(false);
        try {
            String chosenSort = String.valueOf(sort.getSelectedItem());
            String[] sortParts = chosenSort.split(" ");
            String sortField = sortParts.length > 0 ? sortParts[0] : "CGPA";
            boolean ascending = sortParts.length > 1 && "ASC".equalsIgnoreCase(sortParts[1]);
            SocketClient.ListResponse response = client.sendListRequest("SEARCH_SORT_FILTER_STUDENTS", token,
                    studentSearch.getText().trim(), String.valueOf(studentDept.getSelectedItem()),
                    allToZero(studentYear), "ALL".equals(minCgpa.getSelectedItem()) ? "-1" : String.valueOf(minCgpa.getSelectedItem()),
                    allToZero(maxBacklogs), allToZero(semester), sortField, String.valueOf(ascending));
            if (!response.success) throw new IllegalStateException(response.errorMessage);
            studentPreviewModel.setRowCount(0);
            for (String line : response.lines) {
                String[] fields = line.split("\\|", -1);
                if (fields.length >= 10) studentPreviewModel.addRow(java.util.Arrays.copyOf(fields, 10));
            }
            if (studentPreviewModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "No students matched these filters.", "Student Search", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            showError("Could not search students: " + ex.getMessage());
        } finally { button.setEnabled(true); }
    }

    private JPanel buildApplicantsTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Job")); top.add(applicantJob);
        JButton refresh = new JButton("Load Applicants"); refresh.addActionListener(e -> loadApplicants());
        JButton export = new JButton("Download Applicants Excel"); export.addActionListener(e -> exportApplicants(export));
        top.add(refresh); top.add(export);
        applicantsTable.setAutoCreateRowSorter(true);
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(applicantsTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildBatchTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Optional job screening")); top.add(batchJob);
        JButton upload = new JButton("Choose Resumes & Upload Batch");
        upload.addActionListener(e -> uploadBatch(upload));
        top.add(upload);
        JPanel history = new JPanel(new FlowLayout(FlowLayout.LEFT));
        history.add(new JLabel("Recent Batch ID")); history.add(batchIdField);
        JButton loadHistory = new JButton("Load Batch History");
        loadHistory.addActionListener(e -> loadBatchHistory(loadHistory));
        history.add(loadHistory);
        JPanel north = new JPanel(new BorderLayout()); north.add(top, BorderLayout.NORTH); north.add(history, BorderLayout.SOUTH);
        batchTable.setAutoCreateRowSorter(true);
        panel.add(north, BorderLayout.NORTH);
        panel.add(new JScrollPane(batchTable), BorderLayout.CENTER);
        panel.add(batchStatus, BorderLayout.SOUTH);
        return panel;
    }

    private void loadJobs() {
        try {
            SocketClient.ListResponse response = client.sendListRequest("GET_JOBS", token);
            if (!response.success) throw new IllegalStateException(response.errorMessage);
            applicantJob.removeAllItems(); batchJob.removeAllItems();
            for (String line : response.lines) {
                String[] fields = line.split("\\|", -1);
                if (fields.length < 3) continue;
                JobOption option = new JobOption(fields[0], fields[1] + " — " + fields[2]);
                applicantJob.addItem(option);
                batchJob.addItem(option);
            }
            batchJob.insertItemAt(new JobOption("", "No screening (upload only)"), 0);
            batchJob.setSelectedIndex(0);
        } catch (Exception ex) {
            showError("Could not load jobs: " + ex.getMessage());
        }
    }

    private void exportStudents(JButton button) {
        String chosenSort = String.valueOf(sort.getSelectedItem());
        String[] sortParts = chosenSort.split(" ");
        String sortField = sortParts.length > 0 ? sortParts[0] : "CGPA";
        boolean ascending = sortParts.length > 1 && "ASC".equalsIgnoreCase(sortParts[1]);
        String year = allToZero(studentYear);
        String cgpa = "ALL".equals(minCgpa.getSelectedItem()) ? "-1" : String.valueOf(minCgpa.getSelectedItem());
        String backlogs = allToZero(maxBacklogs);
        String sem = allToZero(semester);
        runFileRequest(button, "EXPORT_STUDENTS", "students_" + java.time.LocalDate.now() + ".xlsx",
                token, studentSearch.getText().trim(), String.valueOf(studentDept.getSelectedItem()), year,
                cgpa, backlogs, sem, sortField, String.valueOf(ascending));
    }

    private void loadApplicants() {
        JobOption job = (JobOption) applicantJob.getSelectedItem();
        if (job == null) { showError("No job is available."); return; }
        applicantsModel.setRowCount(0);
        try {
            SocketClient.ListResponse response = client.sendListRequest("GET_JOB_APPLICANTS", token, job.id);
            if (!response.success) throw new IllegalStateException(response.errorMessage);
            for (String line : response.lines) {
                String[] fields = line.split("\\|", -1);
                if (fields.length >= 13) applicantsModel.addRow(fields);
            }
        } catch (Exception ex) { showError("Could not load applicants: " + ex.getMessage()); }
    }

    private void exportApplicants(JButton button) {
        JobOption job = (JobOption) applicantJob.getSelectedItem();
        if (job == null) { showError("Select a job first."); return; }
        runFileRequest(button, "EXPORT_JOB_APPLICANTS", "applicants_" + job.id + ".xlsx", token, job.id);
    }

    private void uploadBatch(JButton button) {
        JFileChooser chooser = new JFileChooser(new File(System.getProperty("user.home")));
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("Resume files (*.pdf, *.docx, *.txt)", "pdf", "docx", "txt"));
        chooser.setDialogTitle("Choose resume files (1–20 files)");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File[] files = chooser.getSelectedFiles();
        if (files.length < 1 || files.length > 20) { showError("Select between 1 and 20 files."); return; }
        List<String> params = new ArrayList<>();
        params.add(token);
        JobOption selectedJob = (JobOption) batchJob.getSelectedItem();
        params.add(selectedJob == null ? "" : selectedJob.id);
        params.add(String.valueOf(files.length));
        try {
            for (File file : files) {
                String lower = file.getName().toLowerCase(java.util.Locale.ROOT);
                if (!(lower.endsWith(".pdf") || lower.endsWith(".docx") || lower.endsWith(".txt")))
                    throw new IllegalArgumentException("Unsupported file type: " + file.getName());
                if (file.length() > MAX_FILE_BYTES)
                    throw new IllegalArgumentException(file.getName() + " exceeds the 2 MB limit.");
                String ref = JOptionPane.showInputDialog(this,
                        "Enter student ID, PRN, or email for:\n" + file.getName());
                if (ref == null || ref.isBlank()) throw new IllegalArgumentException("Student reference is required for every file.");
                byte[] bytes = Files.readAllBytes(file.toPath());
                params.add(ref.trim());
                params.add(file.getName());
                params.add(Base64.getEncoder().encodeToString(bytes));
            }
        } catch (Exception ex) { showError("Could not prepare batch: " + ex.getMessage()); return; }
        button.setEnabled(false); batchStatus.setText("Uploading and screening resumes. Please wait…");
        SwingWorker<SocketClient.ListResponse, Void> worker = new SwingWorker<>() {
            @Override protected SocketClient.ListResponse doInBackground() throws Exception {
                return client.sendListRequest("UPLOAD_RESUME_BATCH", 180000, params.toArray(new String[0]));
            }
            @Override protected void done() {
                button.setEnabled(true);
                try {
                    SocketClient.ListResponse response = get();
                    if (!response.success) throw new IllegalStateException(response.errorMessage);
                    displayBatchLines(response.lines);
                } catch (Exception ex) { batchStatus.setText("Batch failed."); showError("Batch upload failed: " + rootMessage(ex)); }
            }
        };
        worker.execute();
    }

    private void loadBatchHistory(JButton button) {
        String id = batchIdField.getText().trim();
        if (id.isEmpty()) { showError("Enter the Batch ID shown after an upload."); return; }
        button.setEnabled(false);
        SwingWorker<SocketClient.ListResponse, Void> worker = new SwingWorker<>() {
            @Override protected SocketClient.ListResponse doInBackground() throws Exception {
                return client.sendListRequest("GET_RESUME_BATCH", 30000, token, id);
            }
            @Override protected void done() {
                button.setEnabled(true);
                try {
                    SocketClient.ListResponse response = get();
                    if (!response.success) throw new IllegalStateException(response.errorMessage);
                    displayBatchLines(response.lines);
                } catch (Exception ex) { showError("Could not load batch history: " + rootMessage(ex)); }
            }
        };
        worker.execute();
    }

    private void displayBatchLines(List<String> lines) {
        batchModel.setRowCount(0);
        String batchSummary = "Batch response received.";
        for (String line : lines) {
            String[] fields = line.split("\\|", -1);
            if (fields.length > 0 && "BATCH".equals(fields[0])) {
                // BATCH|batchId|total|processed|unreadable|failed|jobId
                if (fields.length >= 7) {
                    batchIdField.setText(fields[1]);
                    batchSummary = "Batch " + fields[1] + " — total: " + fields[2] + ", processed: " + fields[3]
                            + ", unreadable: " + fields[4] + ", failed: " + fields[5]
                            + (fields[6].isBlank() ? " (no job screening)" : ", job: " + fields[6]);
                }
            } else if (fields.length > 0 && "RESULT".equals(fields[0]) && fields.length >= 17) {
                batchModel.addRow(new Object[]{fields[1], fields[2], fields[3], fields[4], fields[5], fields[6], fields[7],
                        fields[8], fields[9], fields[10], fields[11], fields[12], fields[13], fields[14], fields[15], fields[16]});
            }
        }
        batchStatus.setText(batchSummary);
    }

    private void runFileRequest(JButton button, String command, String fallbackName, String... params) {
        button.setEnabled(false);
        try {
            SocketClient.Response response = client.sendRequest(command, params);
            if (!response.success) throw new IllegalStateException(response.payload);
            String[] parts = response.parts();
            if (parts.length < 2) throw new IllegalStateException("Server returned an invalid file response.");
            String fileName = new String(Base64.getDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            byte[] bytes = Base64.getDecoder().decode(parts[1]);
            JFileChooser chooser = new JFileChooser(new File(System.getProperty("user.home")));
            chooser.setDialogTitle("Save Excel export");
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);
            chooser.addChoosableFileFilter(new FileNameExtensionFilter("Excel workbook (*.xlsx)", "xlsx"));
            chooser.setSelectedFile(new File(fileName.isBlank() ? fallbackName : fileName));
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File destination = chooser.getSelectedFile();
                if (!destination.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".xlsx"))
                    destination = new File(destination.getParentFile(), destination.getName() + ".xlsx");
                Files.write(destination.toPath(), bytes);
                JOptionPane.showMessageDialog(this, "File saved successfully.", "Export complete", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) { showError("Export failed: " + ex.getMessage()); }
        finally { button.setEnabled(true); }
    }

    private static String allToZero(JComboBox<String> box) {
        String value = String.valueOf(box.getSelectedItem());
        return "ALL".equalsIgnoreCase(value) ? "0" : value;
    }
    private static String rootMessage(Exception ex) {
        Throwable cause = ex;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() == null ? cause.toString() : cause.getMessage();
    }
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Resume Tools", JOptionPane.ERROR_MESSAGE);
    }
    private static final class JobOption {
        final String id; final String label;
        JobOption(String id, String label) { this.id = id; this.label = label; }
        @Override public String toString() { return id + " — " + label; }
    }
}
