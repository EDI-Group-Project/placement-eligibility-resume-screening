package com.placement.services.staff;

import com.placement.testsupport.TestSupport;
import com.placement.testsupport.TestSupport.FakeJobService;
import com.placement.testsupport.TestSupport.FakeResumeDAO;
import com.placement.testsupport.TestSupport.FakeStudentService;
import com.placement.services.resume.ResumeScreeningService;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.placement.testsupport.TestSupport.student;
import static org.junit.jupiter.api.Assertions.*;

class StudentExportServiceTest {

    private static final String STRONG_RESUME = "Skills\nJava, SQL\nProjects\nPortal\nScreener\nExperience\nIntern at Acme\nCertifications\nAWS Cloud\n";
    private static final String WEAK_RESUME = "Skills\nJava\n";

    private final FakeStudentService students = new FakeStudentService();
    private final FakeResumeDAO resumes = new FakeResumeDAO();
    private StudentExportService service;

    private StudentExportService service() throws Exception {
        if (service != null) return service;
        students.students.add(student(1, "Asha", "P1", "asha@c.edu", "Computer", 9.1, 0, "9111111111"));
        students.students.add(student(2, "Bhavesh", "P2", "bhavesh@c.edu", "IT", 8.2, 1, "9222222222"));
        students.students.add(student(3, "Charu", "P3", "charu@c.edu", "Computer", 7.4, 0, "9333333333"));
        students.students.add(student(4, "Dev", "P4", "dev@c.edu", "Mechanical", 8.9, 2, "9444444444"));
        students.students.add(student(5, "Esha", "P5", "esha@c.edu", "IT", 6.5, 0, "9555555555"));
        students.eligibleIds.addAll(Set.of(1, 2, 3));
        resumes.put(1, STRONG_RESUME);
        resumes.put(2, WEAK_RESUME);
        resumes.put(4, "Skills\nJava, SQL\n");
        resumes.rows.put(5, new com.placement.database.ResumeDAO.ResumeRecord(9, 5, "scan.pdf", "x", "", new java.sql.Timestamp(0)));   // unreadable resume
        FakeJobService jobs = new FakeJobService().with(TestSupport.job("J1", "SDE", "java", "sql"));
        ResumeScreeningService rs = new ResumeScreeningService(resumes, new TestSupport.FakeApplicationDAO(), Files.createTempDirectory("x"));
        service = new StudentExportService(students, jobs, rs);
        return service;
    }

    private static StudentFilter filter(String job, Set<String> branches, Double minC, Double maxC,
                                        StudentFilter.Eligibility e, Double minScore) {
        return new StudentFilter(job, branches, minC, maxC, e, minScore);
    }

    private static List<String> names(List<StudentRow> rows) {
        return rows.stream().map(StudentRow::name).toList();
    }

    @Test
    void noFilter_returnsEveryoneHighestCgpaFirst_withoutEligibilityOrScore() throws Exception {
        List<StudentRow> rows = service().search(StudentFilter.none());
        assertEquals(List.of("Asha", "Dev", "Bhavesh", "Charu", "Esha"), names(rows));
        for (StudentRow r : rows) {
            assertEquals("", r.eligibility());
            assertNull(r.resumeScore());
        }
        assertTrue(rows.get(0).hasResume());                  // Asha
        assertFalse(rows.get(3).hasResume());                 // Charu has no resume
        assertEquals("P1", rows.get(0).prn());
        assertEquals("Computer", rows.get(0).branch());
    }

    @Test
    void branchFilter_isCaseInsensitiveAndAcceptsSeveralBranches() throws Exception {
        assertEquals(List.of("Asha", "Charu"), names(service().search(filter(null, Set.of("computer"), null, null, StudentFilter.Eligibility.ANY, null))));
        assertEquals(Set.of("Bhavesh", "Esha", "Dev"),
                Set.copyOf(names(service().search(filter(null, Set.of("it", "mechanical"), null, null, StudentFilter.Eligibility.ANY, null)))));
        assertEquals(3, service().search(filter(null, Set.of("it", "mechanical"), null, null, StudentFilter.Eligibility.ANY, null)).size());
    }

    @Test
    void cgpaRange_isInclusiveOnBothEnds() throws Exception {
        List<StudentRow> rows = service().search(filter(null, Set.of(), 7.4, 8.9, StudentFilter.Eligibility.ANY, null));
        assertEquals(List.of("Dev", "Bhavesh", "Charu"), names(rows));
    }

    @Test
    void eligibilityOrResumeScoreFilterWithoutAJob_isRejectedWithAClearMessage() throws Exception {
        IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class,
                () -> service().search(filter(null, Set.of(), null, null, StudentFilter.Eligibility.ELIGIBLE, null)));
        assertTrue(e1.getMessage().contains("Select a job"));
        assertThrows(IllegalArgumentException.class,
                () -> service().search(filter("  ", Set.of(), null, null, StudentFilter.Eligibility.ANY, 50.0)));
    }

    @Test
    void eligibleFilter_usesTheExistingEligibilityLogicAndLabelsRows() throws Exception {
        List<StudentRow> eligible = service().search(filter("J1", Set.of(), null, null, StudentFilter.Eligibility.ELIGIBLE, null));
        assertEquals(Set.of("Asha", "Bhavesh", "Charu"), Set.copyOf(names(eligible)));
        for (StudentRow r : eligible) assertEquals(EligibilityProvider.ELIGIBLE_LABEL, r.eligibility());

        List<StudentRow> notEligible = service().search(filter("J1", Set.of(), null, null, StudentFilter.Eligibility.NOT_ELIGIBLE, null));
        assertEquals(Set.of("Dev", "Esha"), Set.copyOf(names(notEligible)));
        for (StudentRow r : notEligible) assertEquals(EligibilityProvider.NOT_ELIGIBLE_LABEL, r.eligibility());
    }

    @Test
    void withAJob_resumeScoresAreShownAndRowsAreRankedBestFirst() throws Exception {
        List<StudentRow> rows = service().search(filter("J1", Set.of(), null, null, StudentFilter.Eligibility.ANY, null));
        assertEquals("Asha", rows.get(0).name());                              // richest resume
        assertTrue(rows.get(0).resumeScore() > rows.get(1).resumeScore());
        Map<String, StudentRow> byName = new java.util.HashMap<>();
        for (StudentRow r : rows) byName.put(r.name(), r);
        assertEquals(20.0, byName.get("Bhavesh").resumeScore(), 0.001);        // 1 of 2 skills
        assertNull(byName.get("Charu").resumeScore());                         // no resume
        assertFalse(byName.get("Charu").hasResume());
        assertNull(byName.get("Esha").resumeScore());                          // resume on file but unreadable
        assertTrue(byName.get("Esha").hasResume());
    }

    @Test
    void minResumeScore_dropsLowScoresAndStudentsWithoutAReadableResume() throws Exception {
        List<StudentRow> rows = service().search(filter("J1", Set.of(), null, null, StudentFilter.Eligibility.ANY, 40.0));
        assertEquals(List.of("Asha", "Dev"), names(rows));                      // Dev: both skills = 40, Bhavesh 20, others none
    }

    @Test
    void filtersCombineWithAnd() throws Exception {
        List<StudentRow> rows = service().search(filter("J1", Set.of("computer"), 8.0, null, StudentFilter.Eligibility.ELIGIBLE, 30.0));
        assertEquals(List.of("Asha"), names(rows));
    }

    @Test
    void scoringReadsAllResumesInOneQuery() throws Exception {
        service().search(filter("J1", Set.of(), null, null, StudentFilter.Eligibility.ANY, null));
        assertEquals(1, resumes.findAllCalls);
    }

    @Test
    void unknownJobAndInvalidRanges_areRejected() throws Exception {
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> service().search(filter("NOPE", Set.of(), null, null, StudentFilter.Eligibility.ANY, null))).getMessage().contains("Job not found"));
        assertThrows(IllegalArgumentException.class, () -> service().search(filter(null, Set.of(), 9.0, 8.0, StudentFilter.Eligibility.ANY, null)));
        assertThrows(IllegalArgumentException.class, () -> service().search(filter(null, Set.of(), 11.0, null, StudentFilter.Eligibility.ANY, null)));
        assertThrows(IllegalArgumentException.class, () -> service().search(filter("J1", Set.of(), null, null, StudentFilter.Eligibility.ANY, 101.0)));
    }

    @Test
    void xlsxContainsTheEssentialColumnsAndTheSameRowsAsTheTable() throws Exception {
        List<StudentRow> rows = service().search(filter("J1", Set.of("computer"), null, null, StudentFilter.Eligibility.ANY, null));
        Map<String, String> c = TestSupport.readXlsx(service().toXlsx(rows));
        assertEquals("PRN", c.get("A1"));
        assertEquals("Name", c.get("B1"));
        assertEquals("Email", c.get("C1"));
        assertEquals("Branch", c.get("D1"));
        assertEquals("CGPA", c.get("E1"));
        assertEquals("Eligibility", c.get("G1"));
        assertEquals("Resume Score", c.get("H1"));
        assertEquals("Asha", c.get("B2"));
        assertEquals("asha@c.edu", c.get("C2"));
        assertEquals("9.1", c.get("E2"));
        assertEquals(EligibilityProvider.ELIGIBLE_LABEL, c.get("G2"));
        assertEquals("Charu", c.get("B3"));
        assertFalse(c.containsKey("H3"));                                      // no resume -> empty score cell
        assertEquals("No", c.get("I3"));
        assertEquals(3, c.keySet().stream().filter(k -> k.startsWith("A")).count());   // header + 2 students
    }

    @Test
    void emptyResultStillExportsAValidFileWithOnlyHeaders() throws Exception {
        List<StudentRow> rows = service().search(filter(null, Set.of(), 9.9, null, StudentFilter.Eligibility.ANY, null));
        assertTrue(rows.isEmpty());
        Map<String, String> c = TestSupport.readXlsx(service().toXlsx(rows));
        assertEquals("PRN", c.get("A1"));
        assertFalse(c.containsKey("A2"));
    }
}
