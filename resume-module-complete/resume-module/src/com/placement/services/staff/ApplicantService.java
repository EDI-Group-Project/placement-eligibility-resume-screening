package com.placement.services.staff;

import com.placement.algorithms.CandidateMergeSort;
import com.placement.database.ApplicationDAO;
import com.placement.export.XlsxWriter;
import com.placement.models.JobPosting;
import com.placement.models.Student;
import com.placement.models.resume.RankedCandidate;
import com.placement.models.resume.ScreeningResult;
import com.placement.services.JobService;
import com.placement.services.StudentService;
import com.placement.services.resume.ResumeScreeningService;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Feature 3 - applicants of ONE job for TPO / TPC / Admin (permission is enforced in the socket handler).
 *
 * The applicant list is exactly the rows of the {@code applications} table for that job
 * ({@link ApplicationDAO#findByJob}); students who are merely eligible but never applied are NOT included.
 * Each row is joined (HashMap, O(n + m)) to the student's profile; an application whose student no longer
 * exists is skipped. Best resume score first (CandidateMergeSort), ties by name.
 */
public class ApplicantService {

    public static final List<String> HEADERS = List.of(
            "PRN", "Name", "Email", "Phone", "Branch", "CGPA", "Backlogs",
            "Eligibility", "Application Status", "Applied At", "Resume Score");

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final StudentService studentService;
    private final JobService jobService;
    private final ApplicationDAO applicationDAO;
    private final EligibilityProvider eligibility;
    private final ResumeScreeningService resumeService;

    public ApplicantService() {
        this(new StudentService(), new JobService(), new ApplicationDAO(), new ResumeScreeningService());
    }

    public ApplicantService(StudentService studentService, JobService jobService,
                            ApplicationDAO applicationDAO, ResumeScreeningService resumeService) {
        this(studentService, jobService, applicationDAO, EligibilityProvider.fromStudentService(studentService), resumeService);
    }

    public ApplicantService(StudentService studentService, JobService jobService, ApplicationDAO applicationDAO,
                            EligibilityProvider eligibility, ResumeScreeningService resumeService) {
        this.studentService = studentService;
        this.jobService = jobService;
        this.applicationDAO = applicationDAO;
        this.eligibility = eligibility;
        this.resumeService = resumeService;
    }

    /** @throws IllegalArgumentException if the job id is missing or unknown (message is safe to show) */
    public List<ApplicantRow> getApplicants(String jobId) throws Exception {
        if (jobId == null || jobId.isBlank()) throw new IllegalArgumentException("Job ID is required.");
        JobPosting job = jobService.getJob(jobId.trim());
        if (job == null) throw new IllegalArgumentException("Job not found: " + jobId.trim());

        List<ApplicationDAO.ApplicationRecord> applications =
                applicationDAO.findByJob(job.getId() != null ? job.getId() : jobId.trim());
        if (applications.isEmpty()) return List.of();

        Map<Integer, Student> byId = new HashMap<>();
        for (Student s : studentService.getAllStudents()) byId.put(s.getStudentId(), s);

        List<Student> applicants = new ArrayList<>();
        List<ApplicationDAO.ApplicationRecord> kept = new ArrayList<>();
        for (ApplicationDAO.ApplicationRecord app : applications) {
            Student s = byId.get(app.studentId());
            if (s == null) continue;                       // application of a student that no longer exists
            applicants.add(s);
            kept.add(app);
        }

        Set<Integer> eligibleIds = eligibility.eligibleStudentIds(job);
        ResumeScreeningService.ScoredResumes scored = resumeService.scoreStudents(applicants, job);

        Map<Integer, ApplicantRow> rowsById = new HashMap<>();
        List<RankedCandidate> ranked = new ArrayList<>();
        for (int i = 0; i < applicants.size(); i++) {
            Student s = applicants.get(i);
            ApplicationDAO.ApplicationRecord app = kept.get(i);

            ScreeningResult fresh = scored.scores().get(s.getStudentId());
            // Double.valueOf keeps the type 'Double': mixing a primitive double with a nullable Double in one
            // ?: expression would unbox the null and throw a NullPointerException.
            Double score = fresh != null ? Double.valueOf(fresh.getTotalScore()) : app.screeningScore();   // else the stored score
            ScreeningResult forSorting = fresh != null ? fresh : new ScreeningResult();
            if (fresh == null && score != null) forSorting.setTotalScore(score);
            ranked.add(new RankedCandidate(s, forSorting));

            rowsById.put(s.getStudentId(), new ApplicantRow(s.getStudentId(), nz(s.getPrn()), nz(s.getName()),
                    nz(s.getEmail()), nz(s.getPhone()), nz(s.getDepartment()), s.getCgpa(), s.getBacklogs(),
                    EligibilityProvider.label(eligibleIds.contains(s.getStudentId())),
                    nz(app.status()), formatWhen(app.appliedAt()),
                    score == null ? null : Math.round(score * 100.0) / 100.0));
        }

        List<ApplicantRow> rows = new ArrayList<>();
        for (RankedCandidate c : CandidateMergeSort.sort(ranked)) {
            rows.add(rowsById.get(c.getStudent().getStudentId()));
        }
        return rows;
    }

    /** Excel (.xlsx) bytes for the given rows. */
    public byte[] toXlsx(List<ApplicantRow> rows) throws IOException {
        List<List<Object>> data = new ArrayList<>();
        for (ApplicantRow r : rows) {
            data.add(Arrays.asList(r.prn(), r.name(), r.email(), r.phone(), r.branch(), r.cgpa(), r.backlogs(),
                    r.eligibility(), r.applicationStatus(), r.appliedAt(), r.resumeScore()));
        }
        return XlsxWriter.write("Applicants", HEADERS, data);
    }

    private static String formatWhen(Timestamp t) {
        return t == null ? "" : t.toLocalDateTime().format(WHEN);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
