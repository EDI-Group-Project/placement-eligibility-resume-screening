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
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;

/**
 * Socket commands for the single-student resume workflow. ClientHandler delegates to this class for the
 * four commands below.
 *
 * Protocol (same style as the rest of the server: one value per line, token first):
 *   UPLOAD_RESUME     token, fileName, base64Content   -> SUCCESS|READABLE|<wordCount>  or  SUCCESS|UNREADABLE|0
 *   GET_ATS_FEEDBACK  token                            -> list: SCORE|n, POSITIVE|.., WARNING|.., SUGGESTION|..
 *   SCREEN_STUDENT    token, jobId, studentId          -> list: TOTAL|, SKILL|, PROJECTS|, EXPERIENCE|, CERTIFICATIONS|,
 *                                                         KEYWORDS|, ATS|, MATCHED|csv, MISSING|csv, STRENGTH|.., IMPROVE|..
 *   RANK_CANDIDATES   token, jobId                     -> list: studentId|name|totalScore|skillScore  (best first)
 *   (list = SUCCESS / count / <count lines> / END, read by SocketClient.sendListRequest)
 *
 * Authorization is decided on the server by {@link RolePolicy} (an allow-list; unknown roles are denied):
 *   - UPLOAD_RESUME, GET_ATS_FEEDBACK: students only, and always for THEIR OWN account (the student id comes
 *     from the session, never from the request);
 *   - SCREEN_STUDENT: a student can only screen themselves; staff (TPO/TPC/Admin) can screen any student;
 *   - RANK_CANDIDATES: staff only.
 * Every line is read with a length limit, so a client cannot exhaust memory with an endless line.
 * (Bulk resume upload for staff is in {@link StaffCommandHandler}.)
 */
public class ResumeCommandHandler {

    private static final Set<String> COMMANDS = Set.of("UPLOAD_RESUME", "GET_ATS_FEEDBACK", "SCREEN_STUDENT", "RANK_CANDIDATES");

    private static final int MAX_BASE64_CHARS = ResumeScreeningService.MAX_UPLOAD_BYTES / 3 * 4 + 8;

    private static final class Defaults {
        static final ResumeScreeningService SERVICE = new ResumeScreeningService();   // stateless, shared
    }

    private final StudentService studentService;
    private final JobService jobService;
    private final ResumeScreeningService service;

    public ResumeCommandHandler(StudentService studentService, JobService jobService) {
        this(studentService, jobService, Defaults.SERVICE);
    }

    public ResumeCommandHandler(StudentService studentService, JobService jobService, ResumeScreeningService service) {
        this.studentService = studentService;
        this.jobService = jobService;
        this.service = service;
    }

    public boolean handles(String command) {
        return COMMANDS.contains(command);
    }

    public void handle(String command, BufferedReader reader, PrintWriter writer) throws Exception {
        try {
            String token = ProtocolIO.readBoundedLine(reader, ProtocolIO.MAX_SHORT_LINE);
            if (!SessionManager.isValid(token)) {
                writer.println("FAILED|Not authenticated. Please log in again.");
                return;
            }
            boolean isStudent = RolePolicy.isStudent(token);
            boolean isStaff = RolePolicy.isStaff(token);
            if (!isStudent && !isStaff) {
                writer.println("FAILED|Access denied.");                    // unknown role: deny
                return;
            }
            int accountId = SessionManager.getAccountId(token);

            switch (command) {
                case "UPLOAD_RESUME" -> upload(reader, writer, isStudent, accountId);
                case "GET_ATS_FEEDBACK" -> atsFeedback(writer, isStudent, accountId);
                case "SCREEN_STUDENT" -> screen(reader, writer, isStudent, accountId);
                case "RANK_CANDIDATES" -> rank(reader, writer, isStaff);
                default -> writer.println("FAILED|Unknown command: " + command);
            }
        } catch (ProtocolIO.LineTooLongException e) {
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
            writer.println("FAILED|Only students can upload their own resume here. Staff can use the batch upload.");
            return;
        }
        String fileName = ProtocolIO.readBoundedLine(reader, ProtocolIO.MAX_SHORT_LINE);
        String base64 = ProtocolIO.readBoundedLine(reader, MAX_BASE64_CHARS);
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
        for (String s : ats.getPositives()) lines.add("POSITIVE|" + ProtocolIO.field(s));
        for (String s : ats.getWarnings()) lines.add("WARNING|" + ProtocolIO.field(s));
        for (String s : ats.getSuggestions()) lines.add("SUGGESTION|" + ProtocolIO.field(s));
        ProtocolIO.writeList(writer, lines);
    }

    private void screen(BufferedReader reader, PrintWriter writer, boolean isStudent, int accountId) throws Exception {
        String jobId = ProtocolIO.readOptional(reader);
        String studentIdRaw = ProtocolIO.readOptional(reader);
        if (jobId.isEmpty()) {
            writer.println("FAILED|Job ID is required.");
            return;
        }
        int studentId = isStudent ? accountId : parseId(studentIdRaw);   // a student can only ever screen THEMSELVES

        JobPosting job = jobService.getJob(jobId);
        if (job == null) {
            writer.println("FAILED|Job not found: " + jobId);
            return;
        }
        Student student = studentService.findStudentById(studentId);
        if (student == null) {
            writer.println("FAILED|Student not found.");
            return;
        }

        ResumeScreeningService.Outcome outcome = service.screen(student, job);
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
        for (String s : r.getStrengths()) lines.add("STRENGTH|" + ProtocolIO.field(s));
        for (String s : r.getImprovementAreas()) lines.add("IMPROVE|" + ProtocolIO.field(s));
        ProtocolIO.writeList(writer, lines);
    }

    private void rank(BufferedReader reader, PrintWriter writer, boolean isStaff) throws Exception {
        if (!isStaff) {
            writer.println("FAILED|" + RolePolicy.STAFF_DENIED_MESSAGE);
            return;
        }
        String jobId = ProtocolIO.readOptional(reader);
        if (jobId.isEmpty()) {
            writer.println("FAILED|Job ID is required.");
            return;
        }
        JobPosting job = jobService.getJob(jobId);
        if (job == null) {
            writer.println("FAILED|Job not found: " + jobId);
            return;
        }
        List<RankedCandidate> ranked = service.rank(studentService.getEligibleUnnotifiedStudents(job), job);
        List<String> lines = new ArrayList<>();
        for (RankedCandidate c : ranked) {
            lines.add(c.getStudent().getStudentId() + "|" + ProtocolIO.field(c.getName()) + "|" + c.getTotalScore()
                    + "|" + c.getScreening().getSkillScore());
        }
        ProtocolIO.writeList(writer, lines);
    }

    private static int parseId(String raw) {
        try {
            return Integer.parseInt(raw == null ? "" : raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid student ID.");
        }
    }
}
