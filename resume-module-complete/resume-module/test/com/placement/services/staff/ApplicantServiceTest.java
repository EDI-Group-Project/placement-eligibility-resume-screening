package com.placement.services.staff;

import com.placement.services.resume.ResumeScreeningService;
import com.placement.testsupport.TestSupport;
import com.placement.testsupport.TestSupport.FakeApplicationDAO;
import com.placement.testsupport.TestSupport.FakeJobService;
import com.placement.testsupport.TestSupport.FakeResumeDAO;
import com.placement.testsupport.TestSupport.FakeStudentService;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.placement.testsupport.TestSupport.student;
import static org.junit.jupiter.api.Assertions.*;

class ApplicantServiceTest {

    private static final String STRONG = "Skills\nJava, SQL\nProjects\nPortal\nScreener\nExperience\nIntern at Acme\nCertifications\nAWS Cloud\n";

    private final FakeStudentService students = new FakeStudentService();
    private final FakeApplicationDAO applications = new FakeApplicationDAO();
    private final FakeResumeDAO resumes = new FakeResumeDAO();
    private ApplicantService service;

    private ApplicantService service() throws Exception {
        if (service != null) return service;
        students.students.add(student(1, "Asha", "P1", "asha@c.edu", "Computer", 9.1, 0, "9111111111"));
        students.students.add(student(2, "Bhavesh", "P2", "bhavesh@c.edu", "IT", 8.2, 1, "9222222222"));   // eligible, NEVER applied
        students.students.add(student(3, "Charu", "P3", "charu@c.edu", "Computer", 7.4, 0, "9333333333"));
        students.students.add(student(4, "Dev", "P4", "dev@c.edu", "Mechanical", 8.9, 2, "9444444444"));
        students.students.add(student(5, "Esha", "P5", "esha@c.edu", "IT", 6.5, 0, "9555555555"));          // never applied
        students.eligibleIds.addAll(Set.of(1, 2));

        applications.addApplication("J1", 1, "APPLIED", null, Timestamp.valueOf(LocalDateTime.of(2026, 9, 1, 10, 30)));
        applications.addApplication("J1", 3, "SHORTLISTED", null, Timestamp.valueOf(LocalDateTime.of(2026, 9, 2, 9, 5)));
        applications.addApplication("J1", 4, "APPLIED", 55.0, null);               // no resume now, but a stored score
        applications.addApplication("J1", 99, "APPLIED", 80.0, null);              // student no longer exists
        applications.addApplication("J2", 5, "APPLIED", null, null);               // applied to a DIFFERENT job

        resumes.put(1, STRONG);
        resumes.put(3, "Skills\nJava\n");

        FakeJobService jobs = new FakeJobService().with(TestSupport.job("J1", "SDE", "java", "sql")).with(TestSupport.job("J2", "QA", "java"));
        jobs.with(TestSupport.job("J3", "Data"));
        ResumeScreeningService rs = new ResumeScreeningService(resumes, applications, Files.createTempDirectory("x"));
        service = new ApplicantService(students, jobs, applications, rs);
        return service;
    }

    @Test
    void onlyStudentsWhoActuallyApplied_areListed_notEveryoneEligible() throws Exception {
        List<ApplicantRow> rows = service().getApplicants("J1");
        Set<String> names = Set.copyOf(rows.stream().map(ApplicantRow::name).toList());
        assertEquals(Set.of("Asha", "Charu", "Dev"), names);
        assertFalse(names.contains("Bhavesh"));          // eligible for the job but never applied
        assertFalse(names.contains("Esha"));             // applied to a different job only
    }

    @Test
    void applicationOfADeletedStudentIsSkippedWithoutError() throws Exception {
        assertEquals(3, service().getApplicants("J1").size());
    }

    @Test
    void rowsCarryEssentialDetails() throws Exception {
        ApplicantRow asha = service().getApplicants("J1").stream().filter(r -> r.name().equals("Asha")).findFirst().orElseThrow();
        assertEquals("P1", asha.prn());
        assertEquals("asha@c.edu", asha.email());
        assertEquals("9111111111", asha.phone());
        assertEquals("Computer", asha.branch());
        assertEquals(9.1, asha.cgpa(), 0.0001);
        assertEquals(0, asha.backlogs());
        assertEquals("APPLIED", asha.applicationStatus());
        assertEquals("2026-09-01 10:30", asha.appliedAt());
        assertEquals(EligibilityProvider.ELIGIBLE_LABEL, asha.eligibility());
        assertNotNull(asha.resumeScore());

        ApplicantRow charu = service().getApplicants("J1").stream().filter(r -> r.name().equals("Charu")).findFirst().orElseThrow();
        assertEquals("SHORTLISTED", charu.applicationStatus());                      // status shown exactly as stored
        assertEquals(EligibilityProvider.NOT_ELIGIBLE_LABEL, charu.eligibility());
    }

    @Test
    void scoreIsComputedFromTheCurrentResume_elseTheStoredScore_elseEmpty() throws Exception {
        List<ApplicantRow> rows = service().getApplicants("J1");
        assertEquals(List.of("Asha", "Dev", "Charu"), rows.stream().map(ApplicantRow::name).toList());   // best first
        assertTrue(rows.get(0).resumeScore() > 55.0);                                // fresh score for a rich resume
        assertEquals(55.0, rows.get(1).resumeScore(), 0.001);                        // Dev: no resume -> stored score
        assertEquals(20.0, rows.get(2).resumeScore(), 0.001);                        // Charu: 1 of 2 skills
    }

    @Test
    void jobWithNoApplicantsGivesAnEmptyListAndAValidEmptyExport() throws Exception {
        assertTrue(service().getApplicants("J3").isEmpty());
        Map<String, String> c = TestSupport.readXlsx(service().toXlsx(List.of()));
        assertEquals("PRN", c.get("A1"));
        assertFalse(c.containsKey("A2"));
    }

    @Test
    void missingOrUnknownJobIsRejected() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> service().getApplicants(null));
        assertThrows(IllegalArgumentException.class, () -> service().getApplicants("  "));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> service().getApplicants("NOPE")).getMessage().contains("Job not found"));
    }

    @Test
    void xlsxHasEveryRequestedColumnAndTheApplicantRows() throws Exception {
        Map<String, String> c = TestSupport.readXlsx(service().toXlsx(service().getApplicants("J1")));
        List<String> expected = List.of("PRN", "Name", "Email", "Phone", "Branch", "CGPA", "Backlogs",
                "Eligibility", "Application Status", "Applied At", "Resume Score");
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), c.get(String.valueOf((char) ('A' + i)) + "1"));
        }
        assertEquals("Asha", c.get("B2"));
        assertEquals("9111111111", c.get("D2"));
        assertEquals("APPLIED", c.get("I2"));
        assertEquals("Dev", c.get("B3"));
        assertEquals("55", c.get("K3").replaceAll("\\.0+$", ""));
        assertFalse(c.containsKey("B5"));                                            // exactly 3 applicants
    }
}
