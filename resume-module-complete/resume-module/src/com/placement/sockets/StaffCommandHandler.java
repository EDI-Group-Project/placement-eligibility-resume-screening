package com.placement.sockets;

import com.placement.services.resume.ResumeScreeningService;
import com.placement.services.staff.ApplicantRow;
import com.placement.services.staff.ApplicantService;
import com.placement.services.staff.BatchResumeService;
import com.placement.services.staff.StudentExportService;
import com.placement.services.staff.StudentFilter;
import com.placement.services.staff.StudentRow;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Socket commands for TPO / TPC / Admin ONLY. Every command first checks, on the server, that the session
 * token is valid AND belongs to a staff account ({@link RolePolicy#isStaff}); a student (or anyone else) is
 * refused before a single parameter is read. ClientHandler delegates these commands here.
 *
 * Protocol (token is always the first line after the command; optional values are sent as an EMPTY line):
 *
 *   EXPORT_STUDENTS        token, jobId, branchesCsv, minCgpa, maxCgpa, eligibility, minResumeScore
 *        -> list: studentId|prn|name|email|branch|cgpa|backlogs|eligibility|resumeScore|hasResume
 *   EXPORT_STUDENTS_XLSX   same parameters -> SUCCESS|<base64 of the .xlsx file>
 *        eligibility = ANY | ELIGIBLE | NOT_ELIGIBLE;  eligibility and minResumeScore need a jobId
 *
 *   JOB_APPLICANTS         token, jobId
 *        -> list: studentId|prn|name|email|phone|branch|cgpa|backlogs|eligibility|status|appliedAt|resumeScore
 *   JOB_APPLICANTS_XLSX    token, jobId -> SUCCESS|<base64 of the .xlsx file>
 *
 *   BATCH_START            token, jobId, fileCount(1-20)        -> SUCCESS|<batchId>
 *   BATCH_ADD_FILE         token, batchId, fileName, base64     -> SUCCESS|<position in batch>
 *   BATCH_STATUS           token, batchId
 *        -> list: SUMMARY|expected|added|done|failed|inProgress|complete
 *                 ITEM|position|fileName|state|parseStatus|prn|studentName|matchScore|eligibility|message
 *
 * Lists use the SocketClient.sendListRequest format (SUCCESS / count / lines / END). Every line is read with a
 * length limit; database / unexpected errors never leak details to the client.
 */
public class StaffCommandHandler {

    private static final Set<String> COMMANDS = Set.of("EXPORT_STUDENTS", "EXPORT_STUDENTS_XLSX",
            "JOB_APPLICANTS", "JOB_APPLICANTS_XLSX", "BATCH_START", "BATCH_ADD_FILE", "BATCH_STATUS");

    private static final int MAX_BASE64_CHARS = ResumeScreeningService.MAX_UPLOAD_BYTES / 3 * 4 + 8;

    private final StudentExportService exportService;
    private final ApplicantService applicantService;
    private final BatchResumeService batchService;

    /** Shared, lazily-created instances (batches must outlive a single connection). */
    private static final class Defaults {
        static final StudentExportService EXPORT = new StudentExportService();
        static final ApplicantService APPLICANTS = new ApplicantService();
        static final BatchResumeService BATCHES = new BatchResumeService();
    }

    public StaffCommandHandler() {
        this(Defaults.EXPORT, Defaults.APPLICANTS, Defaults.BATCHES);
    }

    public StaffCommandHandler(StudentExportService exportService, ApplicantService applicantService,
                               BatchResumeService batchService) {
        this.exportService = exportService;
        this.applicantService = applicantService;
        this.batchService = batchService;
    }

    public boolean handles(String command) {
        return COMMANDS.contains(command);
    }

    public void handle(String command, BufferedReader reader, PrintWriter writer) {
        try {
            String token = ProtocolIO.readBoundedLine(reader, ProtocolIO.MAX_SHORT_LINE);
            if (!SessionManager.isValid(token)) {
                writer.println("FAILED|Not authenticated. Please log in again.");
                return;
            }
            if (!RolePolicy.isStaff(token)) {
                writer.println("FAILED|" + RolePolicy.STAFF_DENIED_MESSAGE);
                return;
            }
            int accountId = SessionManager.getAccountId(token);

            switch (command) {
                case "EXPORT_STUDENTS" -> exportStudents(reader, writer, false);
                case "EXPORT_STUDENTS_XLSX" -> exportStudents(reader, writer, true);
                case "JOB_APPLICANTS" -> jobApplicants(reader, writer, false);
                case "JOB_APPLICANTS_XLSX" -> jobApplicants(reader, writer, true);
                case "BATCH_START" -> batchStart(reader, writer, accountId);
                case "BATCH_ADD_FILE" -> batchAddFile(reader, writer, accountId);
                case "BATCH_STATUS" -> batchStatus(reader, writer);
                default -> writer.println("FAILED|Unknown command: " + command);
            }
        } catch (ProtocolIO.LineTooLongException e) {
            writer.println("FAILED|The request is too large.");
        } catch (IllegalArgumentException e) {
            writer.println("FAILED|" + e.getMessage());                  // validation messages are written for end users
        } catch (SQLException e) {
            System.out.println("Database error in " + command + ": " + e.getMessage());
            writer.println("FAILED|Could not complete the request. Please try again.");
        } catch (Exception e) {
            System.out.println("Unexpected error in " + command + ": " + e);
            writer.println("FAILED|Could not complete the request. Please try again.");
        }
    }

    // ------------------------------------------------------------------
    // Feature 1: filtered student export
    // ------------------------------------------------------------------

    private void exportStudents(BufferedReader reader, PrintWriter writer, boolean xlsx) throws Exception {
        StudentFilter filter = readFilter(reader);
        List<StudentRow> rows = exportService.search(filter);

        if (xlsx) {
            writer.println("SUCCESS|" + Base64.getEncoder().encodeToString(exportService.toXlsx(rows)));
            return;
        }
        List<String> lines = new ArrayList<>();
        for (StudentRow r : rows) {
            lines.add(r.studentId() + "|" + ProtocolIO.field(r.prn()) + "|" + ProtocolIO.field(r.name()) + "|"
                    + ProtocolIO.field(r.email()) + "|" + ProtocolIO.field(r.branch()) + "|" + r.cgpa() + "|"
                    + r.backlogs() + "|" + ProtocolIO.field(r.eligibility()) + "|"
                    + (r.resumeScore() == null ? "" : r.resumeScore()) + "|" + r.hasResume());
        }
        ProtocolIO.writeList(writer, lines);
    }

    private StudentFilter readFilter(BufferedReader reader) throws IOException {
        String jobId = ProtocolIO.readOptional(reader);
        String branches = ProtocolIO.readOptional(reader);
        String minCgpa = ProtocolIO.readOptional(reader);
        String maxCgpa = ProtocolIO.readOptional(reader);
        String eligibility = ProtocolIO.readOptional(reader);
        String minScore = ProtocolIO.readOptional(reader);
        return new StudentFilter(jobId.isEmpty() ? null : jobId, parseBranches(branches),
                parseNumber(minCgpa, "minimum CGPA"), parseNumber(maxCgpa, "maximum CGPA"),
                parseEligibility(eligibility), parseNumber(minScore, "minimum resume score"));
    }

    private static Set<String> parseBranches(String csv) {
        Set<String> branches = new HashSet<>();
        for (String part : csv.split(",")) {
            String b = part.trim().toLowerCase(Locale.ROOT);
            if (!b.isEmpty()) branches.add(b);
        }
        return branches;
    }

    private static Double parseNumber(String raw, String label) {
        if (raw.isEmpty()) return null;
        try {
            double v = Double.parseDouble(raw);
            if (!Double.isFinite(v)) throw new NumberFormatException();
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + label + ".");
        }
    }

    private static StudentFilter.Eligibility parseEligibility(String raw) {
        if (raw.isEmpty()) return StudentFilter.Eligibility.ANY;
        try {
            return StudentFilter.Eligibility.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Eligibility must be ANY, ELIGIBLE or NOT_ELIGIBLE.");
        }
    }

    // ------------------------------------------------------------------
    // Feature 3: applicants of one job
    // ------------------------------------------------------------------

    private void jobApplicants(BufferedReader reader, PrintWriter writer, boolean xlsx) throws Exception {
        String jobId = ProtocolIO.readOptional(reader);
        List<ApplicantRow> rows = applicantService.getApplicants(jobId);

        if (xlsx) {
            writer.println("SUCCESS|" + Base64.getEncoder().encodeToString(applicantService.toXlsx(rows)));
            return;
        }
        List<String> lines = new ArrayList<>();
        for (ApplicantRow r : rows) {
            lines.add(r.studentId() + "|" + ProtocolIO.field(r.prn()) + "|" + ProtocolIO.field(r.name()) + "|"
                    + ProtocolIO.field(r.email()) + "|" + ProtocolIO.field(r.phone()) + "|"
                    + ProtocolIO.field(r.branch()) + "|" + r.cgpa() + "|" + r.backlogs() + "|"
                    + ProtocolIO.field(r.eligibility()) + "|" + ProtocolIO.field(r.applicationStatus()) + "|"
                    + ProtocolIO.field(r.appliedAt()) + "|" + (r.resumeScore() == null ? "" : r.resumeScore()));
        }
        ProtocolIO.writeList(writer, lines);
    }

    // ------------------------------------------------------------------
    // Feature 2: batch resume upload
    // ------------------------------------------------------------------

    private void batchStart(BufferedReader reader, PrintWriter writer, int accountId) throws Exception {
        String jobId = ProtocolIO.readOptional(reader);
        String countRaw = ProtocolIO.readOptional(reader);
        int count;
        try {
            count = Integer.parseInt(countRaw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number of files.");
        }
        writer.println("SUCCESS|" + batchService.startBatch(accountId, jobId, count));
    }

    private void batchAddFile(BufferedReader reader, PrintWriter writer, int accountId) throws Exception {
        String batchId = ProtocolIO.readOptional(reader);
        String fileName = ProtocolIO.readBoundedLine(reader, ProtocolIO.MAX_SHORT_LINE);
        String base64 = ProtocolIO.readBoundedLine(reader, MAX_BASE64_CHARS);
        if (batchId.isEmpty() || fileName == null || fileName.isBlank() || base64 == null || base64.isBlank()) {
            writer.println("FAILED|Batch id, file name and file content are required.");
            return;
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64.trim());
        } catch (IllegalArgumentException e) {
            writer.println("FAILED|The file could not be decoded.");
            return;
        }
        writer.println("SUCCESS|" + batchService.addFile(batchId, accountId, fileName, bytes));
    }

    private void batchStatus(BufferedReader reader, PrintWriter writer) {
        String batchId;
        try {
            batchId = ProtocolIO.readOptional(reader);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid batch id.");
        }
        BatchResumeService.BatchView view = batchService.status(batchId);
        List<String> lines = new ArrayList<>();
        lines.add("SUMMARY|" + view.expected() + "|" + view.added() + "|" + view.done() + "|" + view.failed()
                + "|" + view.inProgress() + "|" + view.complete());
        for (BatchResumeService.ItemView i : view.items()) {
            lines.add("ITEM|" + i.index() + "|" + ProtocolIO.field(i.fileName()) + "|" + i.state() + "|"
                    + i.parseStatus() + "|" + ProtocolIO.field(i.studentPrn()) + "|" + ProtocolIO.field(i.studentName())
                    + "|" + (i.matchScore() == null ? "" : i.matchScore()) + "|" + ProtocolIO.field(i.eligibility())
                    + "|" + ProtocolIO.field(i.message()));
        }
        ProtocolIO.writeList(writer, lines);
    }
}
