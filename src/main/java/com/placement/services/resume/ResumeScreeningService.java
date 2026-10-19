package com.placement.services.resume;

import com.placement.algorithms.CandidateMergeSort;
import com.placement.database.ApplicationDAO;
import com.placement.database.ResumeDAO;
import com.placement.models.JobPosting;
import com.placement.models.Student;
import com.placement.models.resume.AtsFeedbackResult;
import com.placement.models.resume.RankedCandidate;
import com.placement.models.resume.ResumeData;
import com.placement.models.resume.ScreeningResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Single entry point for the resume module: ties together the parser, the
 * two DAOs, screening, ATS feedback, ranking and the PDF report so callers
 * (the socket layer) never have to orchestrate them by hand.
 *
 * Flow:
 *   upload  -> validate -> parse ONCE -> store file on disk + path/text in DB
 *   screen  -> read cached text from DB (never re-opens the file) -> score
 *   rank    -> screen every candidate -> CandidateMergeSort
 *
 * Validation failures throw IllegalArgumentException with a message that is
 * safe to show to the student. Stateless and thread-safe (every DAO call opens
 * its own connection), so one shared instance can serve all client threads.
 */
public class ResumeScreeningService {

    public static final int MAX_UPLOAD_BYTES = ResumeParserService.MAX_FILE_BYTES;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "docx", "txt");

    /** Everything produced by screening one candidate against one job. */
    public record Outcome(ResumeData resume, ScreeningResult screening, AtsFeedbackResult ats) {}

    private final ResumeDAO resumeDAO;
    private final ApplicationDAO applicationDAO;
    private final ResumeParserService parser = new ResumeParserService();
    private final ScreeningService screeningService = new ScreeningService();
    private final AtsFeedbackService atsService = new AtsFeedbackService();
    private final ResumeReportPdfService reportService = new ResumeReportPdfService();
    private final Path storageDir;

    public ResumeScreeningService() {
        this(new ResumeDAO(), new ApplicationDAO(), Paths.get("resumes"));
    }

    public ResumeScreeningService(ResumeDAO resumeDAO, ApplicationDAO applicationDAO, Path storageDir) {
        this.resumeDAO = resumeDAO;
        this.applicationDAO = applicationDAO;
        this.storageDir = storageDir;
    }

    // ------------------------------------------------------------------
    // upload
    // ------------------------------------------------------------------

    /**
     * Validates, parses and stores a resume. The returned ResumeData may be
     * {@code isEmpty()} (e.g. a scanned PDF) - the file is still saved so the
     * student can be told exactly why nothing could be read.
     */
    public ResumeData uploadResume(int studentId, String originalFileName, byte[] bytes) throws IOException, SQLException {
        if (studentId <= 0) throw new IllegalArgumentException("Invalid student.");
        if (bytes == null || bytes.length == 0) throw new IllegalArgumentException("The uploaded file is empty.");
        if (bytes.length > MAX_UPLOAD_BYTES) {
            throw new IllegalArgumentException("File is too large (maximum " + (MAX_UPLOAD_BYTES / (1024 * 1024)) + " MB).");
        }
        String baseName = baseName(originalFileName);
        String ext = extensionOf(baseName);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Only PDF, DOCX or TXT resumes are accepted.");
        }
        if (!contentMatchesExtension(bytes, ext)) {
            throw new IllegalArgumentException("The file content does not match its ." + ext + " extension.");
        }

        ResumeData parsed = parser.parse(bytes, baseName, studentId);   // parse once; cached in the DB below

        // Read the previous record BEFORE touching the disk: if the DB is down, nothing has been written yet.
        ResumeDAO.ResumeRecord previous = resumeDAO.findByStudentId(studentId);

        Files.createDirectories(storageDir);
        Path target = storageDir.resolve("student_" + studentId + "_" + System.currentTimeMillis() + "." + ext);
        Files.write(target, bytes);
        try {
            resumeDAO.saveOrUpdate(studentId, sanitize(baseName), target.toString(), parsed.getRawText());
        } catch (SQLException | RuntimeException e) {
            Files.deleteIfExists(target);   // do not leave an orphan file when the DB write failed
            throw e;
        }
        deleteOldFile(previous);
        return parsed;
    }

    // ------------------------------------------------------------------
    // screening / ranking / report
    // ------------------------------------------------------------------

    /** Cached resume for a student, or {@code ResumeData.empty} when none was uploaded. No file access. */
    public ResumeData loadResume(int studentId) throws SQLException {
        ResumeDAO.ResumeRecord record = resumeDAO.findByStudentId(studentId);
        if (record == null) return ResumeData.empty(studentId, "");
        return parser.fromCachedText(record.extractedText(), studentId, record.fileName());
    }

    /** True when the student already has a resume on file (lets a staff upload say it replaced one). */
    public boolean hasResume(int studentId) throws SQLException {
        return resumeDAO.existsForStudent(studentId);
    }

    /** ATS-friendliness feedback for a student's current resume (independent of any job). */
    public AtsFeedbackResult atsFeedback(int studentId) throws SQLException {
        return atsService.evaluate(loadResume(studentId));
    }

    /** Screens one student for one job. Read-only: writes nothing to the database. */
    public Outcome screen(Student student, JobPosting job) throws SQLException {
        ResumeData resume = loadResume(student.getStudentId());
        return new Outcome(resume, screeningService.screen(student, job, resume), atsService.evaluate(resume));
    }

    /**
     * Same as {@link #screen} but also stores the score in applications.screening_score.
     * The score is saved only on an application the student already made; screening a student who has not
     * applied returns the result but does not create an application (see ApplicationDAO.saveScreeningScore).
     */
    public Outcome screenAndSave(Student student, JobPosting job) throws SQLException {
        Outcome outcome = screen(student, job);
        if (!outcome.resume().isEmpty() && job != null && job.getId() != null) {
            applicationDAO.saveScreeningScore(job.getId(), student.getStudentId(), outcome.screening().getTotalScore());
        }
        return outcome;
    }

    /** Ranks the given candidates for a job: score descending, name ascending (stable merge sort). */
    public List<RankedCandidate> rank(List<Student> candidates, JobPosting job) throws SQLException {
        List<RankedCandidate> ranked = new ArrayList<>();
        if (candidates != null) {
            for (Student student : candidates) {
                ResumeData resume = loadResume(student.getStudentId());
                ranked.add(new RankedCandidate(student, screeningService.screen(student, job, resume)));
            }
        }
        return CandidateMergeSort.sort(ranked);   // candidates with no resume score 0 and land at the bottom
    }

    /** Builds the PDF screening report. Eligibility comes from the caller's EligibilityDAO result. */
    public byte[] buildReport(Student student, JobPosting job, boolean eligible, List<String> eligibilityReasons)
            throws IOException, SQLException {
        Outcome outcome = screen(student, job);
        return reportService.generateReport(student, job, outcome.resume(), outcome.screening(),
                outcome.ats(), eligible, eligibilityReasons);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    /** Strips any directory part a client may have put in the file name ("../../x.pdf", "C:\\x.pdf"). */
    private static String baseName(String name) {
        if (name == null) return "";
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        return (slash >= 0 ? name.substring(slash + 1) : name).strip();
    }

    private static String extensionOf(String baseName) {
        int dot = baseName.lastIndexOf('.');
        return dot < 0 ? "" : baseName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String sanitize(String baseName) {
        String clean = baseName.replaceAll("[^A-Za-z0-9._ -]", "_");
        return clean.length() > 100 ? clean.substring(clean.length() - 100) : clean;
    }

    /** Cheap magic-number check so a renamed executable/archive is rejected up front. */
    private static boolean contentMatchesExtension(byte[] bytes, String ext) {
        switch (ext) {
            case "pdf": {
                int limit = Math.min(bytes.length, 1024);
                return new String(bytes, 0, limit, StandardCharsets.ISO_8859_1).contains("%PDF");
            }
            case "docx":
                return bytes.length > 4 && bytes[0] == 'P' && bytes[1] == 'K';
            case "txt": {
                int limit = Math.min(bytes.length, 4096);
                for (int i = 0; i < limit; i++) if (bytes[i] == 0) return false;   // binary data is not a text resume
                return true;
            }
            default:
                return false;
        }
    }

    /** Removes the student's previous file, but only if it lives inside our own storage folder. */
    private void deleteOldFile(ResumeDAO.ResumeRecord previous) {
        if (previous == null || previous.filePath() == null) return;
        try {
            Path old = Paths.get(previous.filePath()).toAbsolutePath().normalize();
            if (old.startsWith(storageDir.toAbsolutePath().normalize())) {
                Files.deleteIfExists(old);
            }
        } catch (Exception ignored) {
            // an undeletable old file must never fail a successful upload
        }
    }
}
