package com.placement.services.staff;

import com.placement.algorithms.CandidateMergeSort;
import com.placement.algorithms.StudentMergeSort;
import com.placement.export.XlsxWriter;
import com.placement.models.JobPosting;
import com.placement.models.Student;
import com.placement.models.resume.RankedCandidate;
import com.placement.models.resume.ScreeningResult;
import com.placement.services.JobService;
import com.placement.services.StudentService;
import com.placement.services.resume.ResumeScreeningService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Feature 1 - filtered student details for TPO / TPC / Admin (permission is enforced in the socket handler).
 *
 * Reuses what already exists: StudentService for the roster, JobService for the job, the project's own
 * eligibility logic (through {@link EligibilityProvider}) and this module's resume screening for the score.
 * Cheap filters (branch, CGPA) run first; resume scoring then reads ALL cached resume texts in one query.
 *
 * Ordering: with a job selected, best resume score first (CandidateMergeSort, ties by name); without a job,
 * highest CGPA first (the project's StudentMergeSort, ties by name).
 */
public class StudentExportService {

    public static final List<String> HEADERS = List.of(
            "PRN", "Name", "Email", "Branch", "CGPA", "Backlogs", "Eligibility", "Resume Score", "Resume Uploaded");

    private final StudentService studentService;
    private final JobService jobService;
    private final EligibilityProvider eligibility;
    private final ResumeScreeningService resumeService;

    public StudentExportService() {
        this(new StudentService(), new JobService(), new ResumeScreeningService());
    }

    public StudentExportService(StudentService studentService, JobService jobService, ResumeScreeningService resumeService) {
        this(studentService, jobService, EligibilityProvider.fromStudentService(studentService), resumeService);
    }

    public StudentExportService(StudentService studentService, JobService jobService,
                                EligibilityProvider eligibility, ResumeScreeningService resumeService) {
        this.studentService = studentService;
        this.jobService = jobService;
        this.eligibility = eligibility;
        this.resumeService = resumeService;
    }

    /** @throws IllegalArgumentException for an invalid filter or an unknown job (message is safe to show) */
    public List<StudentRow> search(StudentFilter filter) throws Exception {
        StudentFilter f = filter == null ? StudentFilter.none() : filter;
        f.validate();

        JobPosting job = null;
        Set<Integer> eligibleIds = Set.of();
        if (f.hasJob()) {
            job = jobService.getJob(f.jobId().trim());
            if (job == null) throw new IllegalArgumentException("Job not found: " + f.jobId().trim());
            eligibleIds = eligibility.eligibleStudentIds(job);
        }

        List<Student> candidates = new ArrayList<>();
        for (Student s : studentService.getAllStudents()) {
            if (!matchesBasicFilters(s, f)) continue;
            if (job != null && f.eligibility() != StudentFilter.Eligibility.ANY) {
                boolean eligible = eligibleIds.contains(s.getStudentId());
                if (eligible != (f.eligibility() == StudentFilter.Eligibility.ELIGIBLE)) continue;
            }
            candidates.add(s);
        }

        List<StudentRow> rows = new ArrayList<>();
        if (job == null) {
            Set<Integer> withResume = resumeService.studentIdsWithResume();
            for (Student s : StudentMergeSort.sort(candidates)) {
                rows.add(toRow(s, "", null, withResume.contains(s.getStudentId())));
            }
            return rows;
        }

        ResumeScreeningService.ScoredResumes scored = resumeService.scoreStudents(candidates, job);
        List<RankedCandidate> ranked = new ArrayList<>();
        for (Student s : candidates) {
            ScreeningResult result = scored.scores().get(s.getStudentId());
            if (f.minResumeScore() != null && (result == null || result.getTotalScore() < f.minResumeScore())) continue;
            ranked.add(new RankedCandidate(s, result != null ? result : new ScreeningResult()));
        }
        for (RankedCandidate c : CandidateMergeSort.sort(ranked)) {
            Student s = c.getStudent();
            ScreeningResult result = scored.scores().get(s.getStudentId());
            rows.add(toRow(s, EligibilityProvider.label(eligibleIds.contains(s.getStudentId())),
                    result == null ? null : result.getTotalScore(), scored.withResume().contains(s.getStudentId())));
        }
        return rows;
    }

    /** Excel (.xlsx) bytes for the given rows. */
    public byte[] toXlsx(List<StudentRow> rows) throws IOException {
        List<List<Object>> data = new ArrayList<>();
        for (StudentRow r : rows) {
            data.add(Arrays.asList(r.prn(), r.name(), r.email(), r.branch(), r.cgpa(), r.backlogs(),
                    r.eligibility(), r.resumeScore(), r.hasResume() ? "Yes" : "No"));
        }
        return XlsxWriter.write("Students", HEADERS, data);
    }

    // ------------------------------------------------------------------

    private boolean matchesBasicFilters(Student s, StudentFilter f) {
        if (!f.branches().isEmpty()) {
            String branch = s.getDepartment() == null ? "" : s.getDepartment().trim().toLowerCase(Locale.ROOT);
            if (!f.branches().contains(branch)) return false;
        }
        if (f.minCgpa() != null && s.getCgpa() < f.minCgpa()) return false;
        if (f.maxCgpa() != null && s.getCgpa() > f.maxCgpa()) return false;
        return true;
    }

    private StudentRow toRow(Student s, String eligibilityLabel, Double score, boolean hasResume) {
        return new StudentRow(s.getStudentId(), nz(s.getPrn()), nz(s.getName()), nz(s.getEmail()),
                nz(s.getDepartment()), s.getCgpa(), s.getBacklogs(), eligibilityLabel,
                score == null ? null : Math.round(score * 100.0) / 100.0, hasResume);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
