package com.placement.sockets;

import com.placement.database.ApplicationDAO;
import com.placement.models.JobPosting;
import com.placement.models.Student;
import com.placement.models.resume.AtsFeedbackResult;
import com.placement.models.resume.RankedCandidate;
import com.placement.models.resume.ResumeData;
import com.placement.models.resume.ScreeningResult;
import com.placement.services.JobService;
import com.placement.services.StudentService;
import com.placement.services.resume.ExportService;
import com.placement.services.resume.ResumeBatchService;
import com.placement.services.resume.ResumeScreeningService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Socket commands for the resume module. ClientHandler delegates to this class
 * for the commands below, so ClientHandler itself barely changes.
 *
 * Protocol (same style as the rest of the server: one value per line, token first):
 *   UPLOAD_RESUME     token, fileName, base64Content   -> SUCCESS|READABLE|<wordCount>  or  SUCCESS|UNREADABLE|0
 *   GET_ATS_FEEDBACK  token                            -> list: SCORE|n, POSITIVE|.., WARNING|.., SUGGESTION|..
 *   SCREEN_STUDENT    token, jobId, studentId          -> list: TOTAL|, SKILL|, PROJECTS|, EXPERIENCE|, CERTIFICATIONS|,
 *                                                         KEYWORDS|, ATS|, MATCHED|csv, MISSING|csv, STRENGTH|.., IMPROVE|..
 *   RANK_CANDIDATES   token, jobId                     -> list: studentId|name|totalScore|skillScore  (best first)
 *   (list = SUCCESS / count / <count lines> / END, read by SocketClient.sendListRequest)
 *
 * Staff commands (ADMIN, TPO, TPC only - students and Director / Dean are refused):
 *   EXPORT_STUDENTS       token, search, dept, year, minCgpa, maxBacklogs, semester, sortField, ascending
 *                         (the same 8 filters, in the same order, as SEARCH_SORT_FILTER_STUDENTS)
 *                                                        -> SUCCESS|<base64 file name>|<base64 .xlsx>   (same shape as DOWNLOAD_RESUME)
 *   GET_JOB_APPLICANTS    token, jobId                  -> list: prn|name|email|dept|cgpa|year|backlogs|semester|phone|skills
 *                                                         |applicationStatus|screeningScore|appliedAt   (students with an application)
 *   EXPORT_JOB_APPLICANTS token, jobId                  -> SUCCESS|<base64 file name>|<base64 .xlsx>   (same rows as GET_JOB_APPLICANTS)
 *   UPLOAD_RESUME_BATCH   token, jobId (may be empty), count (1-20), then count x [studentRef, fileName, base64Content]
 *                                                        -> list: BATCH|batchId|total|processed|unreadable|failed|jobId, then one
 *                                                           RESULT|... line per file (see ResumeBatchService.Item). studentRef = student id, PRN or email.
 *   GET_RESUME_BATCH      token, batchId                -> the same list again (recent batches only, kept in server memory)
 *
 * Security rules enforced here:
 *   - every command needs a valid session token;
 *   - a STUDENT's identity always comes from the session, never from a client-supplied id, and a
 *     student can only upload / view their own data; ranking is staff-only;
 *   - every line is read with a length limit, so a client cannot exhaust memory by sending an
 *     endless line (BufferedReader.readLine() has no limit).
 */
public class ResumeCommandHandler {

    private static final Set<String> COMMANDS = Set.of("UPLOAD_RESUME", "GET_ATS_FEEDBACK", "SCREEN_STUDENT", "RANK_CANDIDATES", "DOWNLOAD_RESUME", "GET_RESUME_STATUS",
            "EXPORT_STUDENTS", "GET_JOB_APPLICANTS", "EXPORT_JOB_APPLICANTS", "UPLOAD_RESUME_BATCH", "GET_RESUME_BATCH");

    /** Must equal what LoginResult.getAccountType() returns for a student account (compared case-insensitively). */
    private static final String STUDENT_ROLE = "STUDENT";

    /** Roles allowed to use the staff commands above (same trio as ADD_JOB, SCREEN_STUDENT and RANK_CANDIDATES). */
    private static final Set<String> STAFF_ROLES = Set.of("ADMIN", "TPO", "TPC");

    private static final int MAX_SHORT_LINE = 200;
    private static final int MAX_BASE64_CHARS = ResumeScreeningService.MAX_UPLOAD_BYTES / 3 * 4 + 8;
    /** A batch file line longer than the limit is skipped (that file fails, the next ones still parse) up to this many characters, then the request is abandoned. */
    private static final int MAX_SKIPPED_CHARS = 16 * 1024 * 1024;

    private static final ResumeScreeningService SERVICE = new ResumeScreeningService();   // stateless, shared

    private final StudentService studentService;
    private final JobService jobService;
    private final ResumeScreeningService service;
    private final ApplicationDAO applicationDAO;
    private final ResumeBatchService batches;
    private final ExportService exports = new ExportService();

    public ResumeCommandHandler(StudentService studentService, JobService jobService) {
        this(studentService, jobService, SERVICE, new ApplicationDAO());
    }

    /** Lets tests supply their own services / DAO (no database needed); the production constructor above is the one ClientHandler uses. */
    public ResumeCommandHandler(StudentService studentService, JobService jobService,
                                ResumeScreeningService service, ApplicationDAO applicationDAO) {
        this.studentService = studentService;
        this.jobService = jobService;
        this.service = service;
        this.applicationDAO = applicationDAO;
        this.batches = new ResumeBatchService(service);
    }

    public boolean handles(String command) {
        return COMMANDS.contains(command);
    }

    public void handle(String command, BufferedReader reader, PrintWriter writer) throws Exception {
        try {
            String token = readBoundedLine(reader, MAX_SHORT_LINE);
            if (!SessionManager.isValid(token)) {
                writer.println("FAILED|Not authenticated. Please log in again.");
                if (command.equals("UPLOAD_RESUME_BATCH")) {       // e.g. the server restarted: let the client finish sending, so it can read this
                    readBoundedLine(reader, MAX_SHORT_LINE);
                    skipBatchFiles(reader, lenientInt(readBoundedLine(reader, MAX_SHORT_LINE)));
                }
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
                case "EXPORT_STUDENTS" -> exportStudents(reader, writer, token);
                case "GET_JOB_APPLICANTS" -> jobApplicants(reader, writer, token, false);
                case "EXPORT_JOB_APPLICANTS" -> jobApplicants(reader, writer, token, true);
                case "UPLOAD_RESUME_BATCH" -> uploadBatch(reader, writer, token);
                case "GET_RESUME_BATCH" -> getBatch(reader, writer, token);
                default -> writer.println("FAILED|Unknown command: " + command);
            }
        } catch (LineTooLongException e) {
            writer.println("FAILED|The request is too large.");
        } catch (IllegalArgumentException e) {
            writer.println("FAILED|" + e.getMessage());            // validation messages are written for end users
        } catch (SQLException e) {
            System.out.println("Database error in " + command + ": " + e.getMessage());
            writer.println("FAILED|Could not complete the request. Please try again.");
        } catch (Exception e) {                                     // anything else: answer instead of dropping the connection
            System.out.println("Error in " + command + ": " + e);
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
        ResumeData data = service.uploadResume(studentId, fileName, bytes);
        writer.println(data.isEmpty() ? "SUCCESS|UNREADABLE|0" : "SUCCESS|READABLE|" + data.getWordCount());
    }

    private void atsFeedback(PrintWriter writer, boolean isStudent, int studentId) throws Exception {
        if (!isStudent) {
            writer.println("FAILED|ATS feedback is available to students for their own resume.");
            return;
        }
        AtsFeedbackResult ats = service.atsFeedback(studentId);
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

        ResumeScreeningService.Outcome outcome = isStudent ? service.screen(student, job) : service.screenAndSave(student, job);
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
        List<RankedCandidate> ranked = service.rank(studentService.getEligibleUnnotifiedStudents(job), job);
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

    // ------------------------------------------------------------------
    // staff features: Excel exports, job applicants, batch resume upload
    // ------------------------------------------------------------------

    private static final String DENIED = "FAILED|You do not have permission for this operation.";

    private static boolean isStaff(String token) {
        String role = SessionManager.getRole(token);
        return role != null && STAFF_ROLES.contains(role.toUpperCase(Locale.ROOT));
    }

    /** The check every staff command starts with, before it reads anything else from the request. */
    private static boolean requireStaff(String token, PrintWriter writer) {
        if (isStaff(token)) return true;
        writer.println(DENIED);
        return false;
    }

    private void exportStudents(BufferedReader reader, PrintWriter writer, String token) throws Exception {
        if (!requireStaff(token, writer)) return;
        String search = readBoundedLine(reader, MAX_SHORT_LINE);
        String dept = readBoundedLine(reader, MAX_SHORT_LINE);
        int year = lenientInt(readBoundedLine(reader, MAX_SHORT_LINE));
        double minCgpa = lenientDouble(readBoundedLine(reader, MAX_SHORT_LINE));
        int maxBacklogs = lenientInt(readBoundedLine(reader, MAX_SHORT_LINE));
        int semester = lenientInt(readBoundedLine(reader, MAX_SHORT_LINE));
        String sortField = readBoundedLine(reader, MAX_SHORT_LINE);
        boolean ascending = Boolean.parseBoolean(readBoundedLine(reader, MAX_SHORT_LINE));
        // Read exactly like SEARCH_SORT_FILTER_STUDENTS in ClientHandler (0 means "no filter"), so the file always equals the table.
        List<Student> students = studentService.query(search, dept, year == 0 ? -1 : year, minCgpa,
                maxBacklogs == 0 ? -1 : maxBacklogs, semester == 0 ? -1 : semester, sortField, ascending);
        sendFile(writer, "students_" + LocalDate.now() + ".xlsx", exports.studentsWorkbook(students));
    }

    /** GET_JOB_APPLICANTS (asFile = false) and EXPORT_JOB_APPLICANTS (asFile = true): same rows, two shapes. */
    private void jobApplicants(BufferedReader reader, PrintWriter writer, String token, boolean asFile) throws Exception {
        if (!requireStaff(token, writer)) return;
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
        List<ApplicationDAO.Applicant> applicants = applicationDAO.findApplicantsByJob(job.getId());
        if (asFile) {
            String safeId = job.getId().replaceAll("[^A-Za-z0-9._-]", "_");
            sendFile(writer, "applicants_" + safeId + "_" + LocalDate.now() + ".xlsx", exports.applicantsWorkbook(applicants));
            return;
        }
        List<String> lines = new ArrayList<>();
        for (ApplicationDAO.Applicant a : applicants) {     // the usual student columns (Student.toProtocolLine), then 3 more
            lines.add(oneLine(a.student().toProtocolLine() + "|" + a.status() + "|"
                    + (a.screeningScore() == null ? "" : a.screeningScore()) + "|" + a.appliedAt()));
        }
        writeList(writer, lines);
    }

    private void uploadBatch(BufferedReader reader, PrintWriter writer, String token) throws Exception {
        String jobId = readBoundedLine(reader, MAX_SHORT_LINE);
        int count = lenientInt(readBoundedLine(reader, MAX_SHORT_LINE));
        JobPosting job = null;
        String refusal = null;
        if (!isStaff(token)) {
            refusal = DENIED;
        } else if (count < 1 || count > ResumeBatchService.MAX_FILES) {
            refusal = "FAILED|A batch must contain 1 to " + ResumeBatchService.MAX_FILES + " resumes.";
        } else if (jobId != null && !jobId.isBlank()) {
            job = jobService.getJob(jobId.trim());
            if (job == null) refusal = "FAILED|Job not found: " + jobId;
        }
        if (refusal != null) {
            writer.println(refusal);
            skipBatchFiles(reader, count);
            return;
        }
        ResumeBatchService.Batch batch = batches.start(studentService.getAllStudents(), job);
        boolean ended = false;
        for (int i = 0; i < count; i++) {
            BatchFile file = ended ? null : readBatchFile(reader);
            if (file == null) {         // the client stopped sending: report the missing files instead of dropping the whole batch
                ended = true;
                batch.add("", "", null, "The request ended before this file was received.");
            } else {
                batch.add(file.studentRef(), file.fileName(), file.bytes(), file.problem());
            }
        }
        String batchId = ResumeBatchService.newBatchId();
        List<String> lines = batch.lines(batchId);
        ResumeBatchService.remember(batchId, lines);
        writeList(writer, lines);
    }

    private void getBatch(BufferedReader reader, PrintWriter writer, String token) throws Exception {
        if (!requireStaff(token, writer)) return;
        String batchId = readBoundedLine(reader, MAX_SHORT_LINE);
        List<String> lines = batchId == null ? null : ResumeBatchService.recall(batchId.trim());
        if (lines == null) {
            writer.println("FAILED|Batch not found. Only recent batches are kept, and they are cleared when the server restarts.");
            return;
        }
        writeList(writer, lines);
    }

    /**
     * Reads (and drops) the files of a refused batch request. The reason is already sent; closing the connection
     * with the files unread would reset it, and the client would then show a network error instead of the reason.
     * Best effort and bounded: a broken client that declares more files than this is simply cut off.
     */
    private static void skipBatchFiles(BufferedReader reader, int count) {
        try {
            for (int i = 0; i < Math.min(count, 2 * ResumeBatchService.MAX_FILES) * 3; i++) {
                if (readLineOrSkip(reader, MAX_SHORT_LINE) == null) return;
            }
        } catch (IOException ignored) {
            // the answer has been sent; nothing more to report
        }
    }

    /** One file of a batch request. {@code problem} is set when the file could not be read from the request. */
    private record BatchFile(String studentRef, String fileName, byte[] bytes, String problem) {}

    /**
     * Reads one [studentRef, fileName, base64] triple, or returns null when the client stopped sending. A line that is
     * too long makes only this file fail (it is skipped, so the lines of the next file still line up).
     */
    private static BatchFile readBatchFile(BufferedReader reader) throws IOException {
        Line ref = readLineOrSkip(reader, MAX_SHORT_LINE);
        Line name = readLineOrSkip(reader, MAX_SHORT_LINE);
        Line content = readLineOrSkip(reader, MAX_BASE64_CHARS);
        if (ref == null || name == null || content == null) return null;

        String studentRef = ref.tooLong() ? "" : ref.text();
        String fileName = name.tooLong() ? "" : name.text();
        String problem = null;
        byte[] bytes = null;
        if (ref.tooLong()) problem = "The student reference is too long.";
        else if (name.tooLong()) problem = "The file name is too long.";
        else if (content.tooLong()) problem = "File is too large (maximum " + (ResumeScreeningService.MAX_UPLOAD_BYTES / (1024 * 1024)) + " MB).";
        else if (fileName.isBlank() || content.text().isBlank()) problem = "File name and file content are required.";
        else {
            try {
                bytes = Base64.getDecoder().decode(content.text().trim());
            } catch (IllegalArgumentException e) {
                problem = "The file could not be decoded.";
            }
        }
        return new BatchFile(studentRef, fileName, bytes, problem);
    }

    /** Same shape as the DOWNLOAD_RESUME answer: SUCCESS|base64(file name)|base64(file bytes). */
    private static void sendFile(PrintWriter writer, String fileName, byte[] bytes) {
        writer.println("SUCCESS|" + Base64.getEncoder().encodeToString(fileName.getBytes(StandardCharsets.UTF_8))
                + "|" + Base64.getEncoder().encodeToString(bytes));
    }

    // Same leniency as ClientHandler's i() / d(): a missing or malformed number reads as 0 / -1 (= no filter).
    private static int lenientInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }

    private static double lenientDouble(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return -1; }
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

    /** One request line; {@code tooLong} means it was longer than allowed, so it was skipped and carries no text. */
    private record Line(String text, boolean tooLong) {}

    /**
     * Like {@link #readBoundedLine}, but an over-long line is read to its end and reported instead of failing the
     * whole request, so one oversized file in a batch fails alone and the lines after it still parse. A line
     * longer than {@link #MAX_SKIPPED_CHARS} still abandons the request. Returns null at end of input.
     */
    private static Line readLineOrSkip(BufferedReader reader, int maxChars) throws IOException {
        StringBuilder sb = new StringBuilder();
        boolean tooLong = false;
        long seen = 0;
        int ch;
        while ((ch = reader.read()) != -1) {
            if (ch == '\n') return tooLong ? new Line(null, true) : new Line(sb.toString(), false);
            if (ch == '\r') continue;
            if (++seen > MAX_SKIPPED_CHARS) throw new LineTooLongException();
            if (!tooLong) {
                sb.append((char) ch);
                if (sb.length() > maxChars) tooLong = true;
            }
        }
        if (tooLong) return new Line(null, true);
        return sb.length() == 0 ? null : new Line(sb.toString(), false);
    }
}
