package com.placement.sockets;

import com.placement.models.JobPosting;
import com.placement.models.Student;
import com.placement.models.resume.AtsFeedbackResult;
import com.placement.models.resume.RankedCandidate;
import com.placement.models.resume.ResumeData;
import com.placement.models.resume.ScreeningResult;
import com.placement.services.JobService;
import com.placement.services.StudentService;
import com.placement.services.resume.ResumeScreeningService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;

/**
 * Socket commands for the resume module. ClientHandler delegates to this class
 * for the four commands below, so ClientHandler itself barely changes.
 *
 * Protocol (same style as the rest of the server: one value per line, token first):
 *   UPLOAD_RESUME     token, fileName, base64Content   -> SUCCESS|READABLE|<wordCount>  or  SUCCESS|UNREADABLE|0
 *   GET_ATS_FEEDBACK  token                            -> list: SCORE|n, POSITIVE|.., WARNING|.., SUGGESTION|..
 *   SCREEN_STUDENT    token, jobId, studentId          -> list: TOTAL|, SKILL|, PROJECTS|, EXPERIENCE|, CERTIFICATIONS|,
 *                                                         KEYWORDS|, ATS|, MATCHED|csv, MISSING|csv, STRENGTH|.., IMPROVE|..
 *   RANK_CANDIDATES   token, jobId                     -> list: studentId|name|totalScore|skillScore  (best first)
 *   (list = SUCCESS / count / <count lines> / END, read by SocketClient.sendListRequest)
 *
 * Security rules enforced here:
 *   - every command needs a valid session token;
 *   - a STUDENT's identity always comes from the session, never from a client-supplied id, and a
 *     student can only upload / view their own data; ranking is staff-only;
 *   - every line is read with a length limit, so a client cannot exhaust memory by sending an
 *     endless line (BufferedReader.readLine() has no limit).
 */
public class ResumeCommandHandler {

    private static final Set<String> COMMANDS = Set.of("UPLOAD_RESUME", "GET_ATS_FEEDBACK", "SCREEN_STUDENT", "RANK_CANDIDATES", "DOWNLOAD_RESUME", "GET_RESUME_STATUS");

    /** Must equal what LoginResult.getAccountType() returns for a student account (compared case-insensitively). */
    private static final String STUDENT_ROLE = "STUDENT";

    private static final int MAX_SHORT_LINE = 200;
    private static final int MAX_BASE64_CHARS = ResumeScreeningService.MAX_UPLOAD_BYTES / 3 * 4 + 8;

    private static final ResumeScreeningService SERVICE = new ResumeScreeningService();   // stateless, shared

    private final StudentService studentService;
    private final JobService jobService;

    public ResumeCommandHandler(StudentService studentService, JobService jobService) {
        this.studentService = studentService;
        this.jobService = jobService;
    }

    public boolean handles(String command) {
        return COMMANDS.contains(command);
    }

    public void handle(String command, BufferedReader reader, PrintWriter writer) throws Exception {
        try {
            String token = readBoundedLine(reader, MAX_SHORT_LINE);
            if (!SessionManager.isValid(token)) {
                writer.println("FAILED|Not authenticated. Please log in again.");
                return;
            }
            boolean isStudent = STUDENT_ROLE.equalsIgnoreCase(SessionManager.getRole(token));
            int accountId = SessionManager.getAccountId(token);

            switch (command) {
                case "UPLOAD_RESUME" -> upload(reader, writer, isStudent, accountId);
                case "GET_ATS_FEEDBACK" -> atsFeedback(writer, isStudent, accountId);
                case "SCREEN_STUDENT" -> screen(reader, writer, isStudent, accountId, token);
                case "RANK_CANDIDATES" -> rank(reader, writer, isStudent, token);
                case "DOWNLOAD_RESUME" -> download(reader, writer, isStudent, accountId, token);
                case "GET_RESUME_STATUS" -> resumeStatus(writer, isStudent, accountId);
                default -> writer.println("FAILED|Unknown command: " + command);
            }
        } catch (LineTooLongException e) {
            writer.println("FAILED|The request is too large.");
        } catch (IllegalArgumentException e) {
            writer.println("FAILED|" + e.getMessage());            // validation messages are written for end users
        } catch (SQLException e) {
            System.out.println("Database error in " + command + ": " + e.getMessage());
            writer.println("FAILED|Could not complete the request. Please try again.");
        }
    }

    // ------------------------------------------------------------------

    private void upload(BufferedReader reader, PrintWriter writer, boolean isStudent, int studentId) throws Exception {
        if (!isStudent) {
            writer.println("FAILED|Only students can upload a resume.");
            return;
        }
        String fileName = readBoundedLine(reader, MAX_SHORT_LINE);
        String base64 = readBoundedLine(reader, MAX_BASE64_CHARS);
        if (fileName == null || fileName.isBlank() || base64 == null || base64.isBlank()) {
            writer.println("FAILED|File name and file content are required.");
            return;
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64.trim());
        } catch (IllegalArgumentException e) {
            writer.println("FAILED|The file could not be decoded.");
            return;
        }
        ResumeData data = SERVICE.uploadResume(studentId, fileName, bytes);
        writer.println(data.isEmpty() ? "SUCCESS|UNREADABLE|0" : "SUCCESS|READABLE|" + data.getWordCount());
    }

    private void atsFeedback(PrintWriter writer, boolean isStudent, int studentId) throws Exception {
        if (!isStudent) {
            writer.println("FAILED|ATS feedback is available to students for their own resume.");
            return;
        }
        AtsFeedbackResult ats = SERVICE.atsFeedback(studentId);
        List<String> lines = new ArrayList<>();
        lines.add("SCORE|" + ats.getAtsScore());
        for (String s : ats.getPositives()) lines.add("POSITIVE|" + oneLine(s));
        for (String s : ats.getWarnings()) lines.add("WARNING|" + oneLine(s));
        for (String s : ats.getSuggestions()) lines.add("SUGGESTION|" + oneLine(s));
        writeList(writer, lines);
    }

    private void screen(BufferedReader reader, PrintWriter writer, boolean isStudent, int accountId, String token) throws Exception {
        if (!isStudent && !Set.of("ADMIN", "TPO", "TPC").contains(SessionManager.getRole(token))) {
            writer.println("FAILED|Not allowed."); return;
        }
        String jobId = readBoundedLine(reader, MAX_SHORT_LINE);
        String studentIdRaw = readBoundedLine(reader, MAX_SHORT_LINE);
        if (jobId == null || jobId.isBlank()) {
            writer.println("FAILED|Job ID is required.");
            return;
        }
        int studentId = isStudent ? accountId : resolveStudentId(studentIdRaw);   // a student can only ever screen THEMSELVES

        JobPosting job = jobService.getJob(jobId.trim());
        if (job == null) {
            writer.println("FAILED|Job not found: " + jobId);
            return;
        }
        Student student = studentService.findStudentById(studentId);
        if (student == null) {
            writer.println("FAILED|Student not found.");
            return;
        }

        ResumeScreeningService.Outcome outcome = isStudent ? SERVICE.screen(student, job) : SERVICE.screenAndSave(student, job);
        ScreeningResult r = outcome.screening();
        List<String> lines = new ArrayList<>();
        lines.add("TOTAL|" + r.getTotalScore());
        lines.add("SKILL|" + r.getSkillScore());
        lines.add("PROJECTS|" + r.getProjectScore());
        lines.add("EXPERIENCE|" + r.getExperienceScore());
        lines.add("CERTIFICATIONS|" + r.getCertificationScore());
        lines.add("KEYWORDS|" + r.getKeywordScore());
        lines.add("ATS|" + outcome.ats().getAtsScore());
        lines.add("MATCHED|" + String.join(",", r.getMatchedSkills()));
        lines.add("MISSING|" + String.join(",", r.getMissingSkills()));
        for (String s : r.getStrengths()) lines.add("STRENGTH|" + oneLine(s));
        for (String s : r.getImprovementAreas()) lines.add("IMPROVE|" + oneLine(s));
        writeList(writer, lines);
    }

    private void rank(BufferedReader reader, PrintWriter writer, boolean isStudent, String token) throws Exception {
        if (isStudent || !Set.of("ADMIN", "TPO", "TPC").contains(SessionManager.getRole(token))) {
            writer.println("FAILED|Not allowed.");
            return;
        }
        String jobId = readBoundedLine(reader, MAX_SHORT_LINE);
        if (jobId == null || jobId.isBlank()) {
            writer.println("FAILED|Job ID is required.");
            return;
        }
        JobPosting job = jobService.getJob(jobId.trim());
        if (job == null) {
            writer.println("FAILED|Job not found: " + jobId);
            return;
        }
        List<RankedCandidate> ranked = SERVICE.rank(studentService.getEligibleUnnotifiedStudents(job), job);
        List<String> lines = new ArrayList<>();
        for (RankedCandidate c : ranked) {
            lines.add(c.getStudent().getStudentId() + "|" + oneLine(c.getName()) + "|" + c.getTotalScore()
                    + "|" + c.getScreening().getSkillScore());
        }
        writeList(writer, lines);
    }

    // ------------------------------------------------------------------


    private void resumeStatus(PrintWriter writer, boolean isStudent, int studentId) throws Exception {
        if (!isStudent) { writer.println("FAILED|This status endpoint is for students."); return; }
        com.placement.database.ResumeDAO.ResumeRecord record = new com.placement.database.ResumeDAO().findByStudentId(studentId);
        writer.println(record == null ? "SUCCESS|MISSING" : "SUCCESS|AVAILABLE|" + oneLine(record.fileName()));
    }

    private void download(BufferedReader reader, PrintWriter writer, boolean isStudent, int accountId, String token) throws Exception {
        String raw = readBoundedLine(reader, MAX_SHORT_LINE);
        int target = isStudent ? accountId : resolveStudentId(raw);
        String role = SessionManager.getRole(token);
        if (!isStudent && !Set.of("ADMIN", "TPO", "TPC", "DIRECTOR", "DEAN").contains(role)) {
            writer.println("FAILED|Not allowed."); return;
        }
        com.placement.database.ResumeDAO.ResumeRecord record = new com.placement.database.ResumeDAO().findByStudentId(target);
        if (record == null) { writer.println("FAILED|No resume uploaded."); return; }
        java.nio.file.Path file = java.nio.file.Paths.get(record.filePath()).normalize();
        if (!java.nio.file.Files.isRegularFile(file)) { writer.println("FAILED|Stored resume file is missing."); return; }
        String encoded = Base64.getEncoder().encodeToString(java.nio.file.Files.readAllBytes(file));
        writer.println("SUCCESS|" + Base64.getEncoder().encodeToString(record.fileName().getBytes(java.nio.charset.StandardCharsets.UTF_8)) + "|" + encoded);
    }

    private static void writeList(PrintWriter writer, List<String> lines) {
        writer.println("SUCCESS");
        writer.println(lines.size());
        for (String line : lines) writer.println(line);
        writer.println("END");
    }

    private int resolveStudentId(String raw) throws Exception {
        String value = raw == null ? "" : raw.trim();
        try { return Integer.parseInt(value); } catch (NumberFormatException ignored) { }
        if (value.isBlank()) throw new IllegalArgumentException("Student ID or PRN is required.");
        for (Student student : studentService.getAllStudents()) {
            if (value.equalsIgnoreCase(student.getPrn()) || value.equalsIgnoreCase(student.getEmail())) return student.getStudentId();
        }
        throw new IllegalArgumentException("Student not found. Enter a valid student ID, PRN, or email.");
    }

    private static int parseId(String raw) {
        try {
            return Integer.parseInt(raw == null ? "" : raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid student ID.");
        }
    }

    /** A protocol line must never contain a line break. */
    private static String oneLine(String s) {
        return s == null ? "" : s.replace('\r', ' ').replace('\n', ' ');
    }

    private static final class LineTooLongException extends IOException {
        LineTooLongException() { super("Request line too long."); }
    }

    /** Like BufferedReader.readLine(), but refuses lines longer than {@code maxChars} instead of buffering forever. */
    private static String readBoundedLine(BufferedReader reader, int maxChars) throws IOException {
        StringBuilder sb = new StringBuilder();
        int ch;
        while ((ch = reader.read()) != -1) {
            if (ch == '\n') return sb.toString();
            if (ch != '\r') {
                sb.append((char) ch);
                if (sb.length() > maxChars) throw new LineTooLongException();
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }
}
