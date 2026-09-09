package com.placement.sockets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.placement.models.JobPosting;
import com.placement.models.LoginResult;
import com.placement.models.Notification;
import com.placement.models.Student;
import com.placement.services.AuthService;
import com.placement.services.JobService;
import com.placement.services.NotificationService;
import com.placement.services.RegistrationService;
import com.placement.services.StudentService;

public class ClientHandler implements Runnable {

    private final Socket clientSocket;

    private final AuthService authService = new AuthService();
    private final RegistrationService registrationService = new RegistrationService();
    private final JobService jobService = new JobService();
    private final StudentService studentService = new StudentService();
    private final NotificationService notificationService = new NotificationService();

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try (Socket socket = clientSocket;
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {

            String command = reader.readLine();
            if (command == null) return;

            switch (command) {
                case "LOGIN" -> handleLogin(reader, writer);
                case "REGISTER_STUDENT" -> handleStudentRegistration(reader, writer);
                case "GET_JOBS" -> handleGetJobs(writer);
                case "ADD_JOB" -> handleAddJob(reader, writer);
                case "GET_STUDENTS" -> handleGetStudents(reader, writer);
                case "GET_ELIGIBLE_STUDENTS" -> handleGetEligibleStudents(reader, writer);
                case "SEND_NOTIFICATION" -> handleSendNotification(reader, writer);
                case "SEND_GENERAL_NOTIFICATION" -> handleSendGeneralNotification(reader, writer);
                case "GET_NOTIFICATIONS" -> handleGetNotifications(reader, writer);
                case "GET_DASHBOARD_STATS" -> handleGetDashboardStats(reader, writer);
                default -> writer.println("FAILED|Unknown command: " + command);
            }

        } catch (NumberFormatException e) {
            System.out.println("Client sent invalid numeric data: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Unexpected error handling client: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // AUTH HELPERS
    // ------------------------------------------------------------------

    private String requireToken(BufferedReader reader, PrintWriter writer) throws IOException {
        String token = reader.readLine();
        if (!SessionManager.isValid(token)) {
            writer.println("FAILED|Not authenticated. Please log in again.");
            return null;
        }
        return token;
    }

    private boolean isMissing(String value, String fieldName, PrintWriter writer) {
        if (value == null || value.isBlank()) {
            writer.println("FAILED|" + fieldName + " is required.");
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------
    // LOGIN / REGISTRATION
    // ------------------------------------------------------------------

    private void handleLogin(BufferedReader reader, PrintWriter writer) throws IOException {
        String role = reader.readLine();
        String email = reader.readLine();
        String password = reader.readLine();

        if (isMissing(role, "Role", writer)) return;
        if (isMissing(email, "Email", writer)) return;
        if (isMissing(password, "Password", writer)) return;

        LoginResult result = authService.login(role, email, password);

        if (result.isSuccess()) {
            String token = SessionManager.createSession(role);
            writer.println("SUCCESS|" + token + "|" + result.getDisplayName());
        } else {
            writer.println("FAILED|" + result.getMessage());
        }

        System.out.println("Login attempt [" + role + "/" + email + "]: "
                + (result.isSuccess() ? "SUCCESS" : "FAILED - " + result.getMessage()));
    }

    private void handleStudentRegistration(BufferedReader reader, PrintWriter writer) throws IOException {
        Student student = new Student();
        student.setName(reader.readLine());
        student.setEmail(reader.readLine());
        String password = reader.readLine();
        student.setDepartment(reader.readLine());
        student.setCgpa(safeDouble(reader.readLine()));
        student.setPassingYear(safeInt(reader.readLine()));
        student.setBacklogs(safeInt(reader.readLine()));
        student.setSemester(safeInt(reader.readLine()));
        student.setPhone(reader.readLine());
        student.setSkills(reader.readLine());

        if (isMissing(student.getName(), "Name", writer)) return;
        if (isMissing(student.getEmail(), "Email", writer)) return;
        if (isMissing(password, "Password", writer)) return;

        String result = registrationService.registerStudent(student, password);
        writer.println(result);
    }

    // ------------------------------------------------------------------
    // JOB POSTINGS (public read, protected write)
    // ------------------------------------------------------------------

    private void handleGetJobs(PrintWriter writer) {
        try {
            List<JobPosting> jobs = jobService.getAllJobs();
            writer.println("SUCCESS");
            writer.println(jobs.size());
            for (JobPosting j : jobs) writer.println(j.toProtocolLine());
            writer.println("END");
        } catch (Exception e) {
            writer.println("FAILED|Unable to retrieve jobs.");
        }
    }

    private void handleAddJob(BufferedReader reader, PrintWriter writer) throws IOException {
        String token = requireToken(reader, writer);
        if (token == null) return;

        try {
            String company = reader.readLine();
            String role = reader.readLine();
            String minPackage = reader.readLine();
            double minCgpa = safeDouble(reader.readLine());
            String branchesLine = reader.readLine();
            int maxBacklogs = safeInt(reader.readLine());
            String deadline = reader.readLine();

            if (isMissing(company, "Company", writer)) return;
            if (isMissing(role, "Role", writer)) return;
            if (isMissing(branchesLine, "Branches", writer)) return;

            List<String> branches = Arrays.asList(branchesLine.split(","));
            JobPosting job = jobService.addJob(company, role, minPackage, minCgpa, branches, maxBacklogs, deadline);
            writer.println("SUCCESS|" + job.getId());
        } catch (NumberFormatException e) {
            writer.println("FAILED|Invalid numeric value for CGPA or backlogs.");
        }
    }

    // ------------------------------------------------------------------
    // STUDENTS (protected)
    // ------------------------------------------------------------------

    private void handleGetStudents(BufferedReader reader, PrintWriter writer) throws IOException {
        String token = requireToken(reader, writer);
        if (token == null) return;

        try {
            List<Student> students = studentService.getAllStudents();
            writer.println("SUCCESS");
            writer.println(students.size());
            for (Student s : students) writer.println(s.toProtocolLine());
            writer.println("END");
        } catch (Exception e) {
            writer.println("FAILED|Unable to retrieve students.");
        }
    }

    private void handleGetEligibleStudents(BufferedReader reader, PrintWriter writer) throws IOException {
        String token = requireToken(reader, writer);
        if (token == null) return;

        String jobId = reader.readLine();
        if (isMissing(jobId, "Job ID", writer)) return;

        JobPosting job = jobService.getJob(jobId);
        if (job == null) {
            writer.println("FAILED|Job not found: " + jobId);
            return;
        }
        List<Student> eligible = studentService.getEligibleUnnotifiedStudents(job);
        writer.println("SUCCESS");
        writer.println(eligible.size());
        for (Student s : eligible) writer.println(s.toProtocolLine());
        writer.println("END");
    }

    // ------------------------------------------------------------------
    // NOTIFICATIONS (protected)
    // ------------------------------------------------------------------

    private void handleSendNotification(BufferedReader reader, PrintWriter writer) throws IOException {
        String token = requireToken(reader, writer);
        if (token == null) return;

        String jobId = reader.readLine();
        String prnsCsv = reader.readLine();

        if (isMissing(jobId, "Job ID", writer)) return;
        if (isMissing(prnsCsv, "Selected students", writer)) return;

        JobPosting job = jobService.getJob(jobId);
        if (job == null) {
            writer.println("FAILED|Job not found: " + jobId);
            return;
        }

        List<String> prns = new ArrayList<>(Arrays.asList(prnsCsv.split(",")));
        Notification n = notificationService.sendJobEligibilityNotification(job, prns);
        writer.println("SUCCESS|" + n.getId() + "|" + prns.size());
    }

    private void handleSendGeneralNotification(BufferedReader reader, PrintWriter writer) throws IOException {
        String token = requireToken(reader, writer);
        if (token == null) return;

        String subject = reader.readLine();
        String message = reader.readLine();
        String recipientGroup = reader.readLine();

        if (isMissing(subject, "Subject", writer)) return;

        List<String> recipients = recipientGroup != null && recipientGroup.startsWith("All")
                ? studentService.getAllStudents().stream().map(Student::getPrn).toList()
                : List.of();

        Notification n = notificationService.sendGeneralNotification(subject, message, recipients);
        writer.println("SUCCESS|" + n.getId() + "|" + recipients.size());
    }

    private void handleGetNotifications(BufferedReader reader, PrintWriter writer) throws IOException {
        String token = requireToken(reader, writer);
        if (token == null) return;

        List<Notification> log = notificationService.getHistory();
        writer.println("SUCCESS");
        writer.println(log.size());
        for (Notification n : log) writer.println(n.toProtocolLine());
        writer.println("END");
    }

    // ------------------------------------------------------------------
    // DASHBOARD STATS (protected)
    // ------------------------------------------------------------------

    private void handleGetDashboardStats(BufferedReader reader, PrintWriter writer) throws IOException {
        String token = requireToken(reader, writer);
        if (token == null) return;

        List<JobPosting> jobs = jobService.getAllJobs();
        List<Student> students = studentService.getAllStudents();

        int pending = 0;
        for (JobPosting job : jobs) {
            pending += studentService.getEligibleUnnotifiedStudents(job).size();
        }
        int sent = notificationService.getHistory().size();

        writer.println("SUCCESS|" + jobs.size() + "|" + pending + "|" + sent + "|" + students.size());
    }

    private double safeDouble(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 0.0; }
    }

    private int safeInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }
}
