package com.placement.sockets;

import com.placement.services.resume.ResumeScreeningService;
import com.placement.services.staff.ApplicantService;
import com.placement.services.staff.BatchResumeService;
import com.placement.services.staff.StudentExportService;
import com.placement.testsupport.TestSupport;
import com.placement.testsupport.TestSupport.FakeApplicationDAO;
import com.placement.testsupport.TestSupport.FakeJobService;
import com.placement.testsupport.TestSupport.FakeResumeDAO;
import com.placement.testsupport.TestSupport.FakeStudentService;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.placement.testsupport.TestSupport.student;
import static org.junit.jupiter.api.Assertions.*;

class StaffCommandHandlerTest {

    private static final String RESUME = "Skills\nJava, SQL\nProjects\nPortal\nExperience\nIntern at Acme\n";

    private final FakeStudentService students = new FakeStudentService();
    private final FakeResumeDAO resumes = new FakeResumeDAO();
    private final FakeApplicationDAO applications = new FakeApplicationDAO();
    private StaffCommandHandler staff;
    private ApplicationCommandHandler apply;

    private final String tpo = SessionManager.createSession("TPO", "Officer", 100);
    private final String tpc = SessionManager.createSession("TPC", "Coordinator", 101);
    private final String admin = SessionManager.createSession("Admin", "Root", 102);
    private final String otherStaff = SessionManager.createSession("TPO", "Other", 103);
    private final String studentToken = SessionManager.createSession("STUDENT", "Asha", 1);
    private final String parentToken = SessionManager.createSession("PARENT", "Someone", 5);

    private void setUp() throws Exception {
        if (staff != null) return;
        students.students.add(student(1, "Asha", "B24CE1001", "asha@c.edu", "Computer", 9.1, 0, "9111111111"));
        students.students.add(student(2, "Bhavesh", "B24CE1002", "bhavesh@c.edu", "IT", 8.2, 1, "9222222222"));
        students.students.add(student(3, "Charu", "B24CE1003", "charu@c.edu", "Computer", 7.4, 0, "9333333333"));
        students.eligibleIds.addAll(Set.of(1, 2));
        resumes.put(1, RESUME);
        FakeJobService jobs = new FakeJobService().with(TestSupport.job("J1", "SDE", "java", "sql"));
        ResumeScreeningService rs = new ResumeScreeningService(resumes, applications, Files.createTempDirectory("h"));
        staff = new StaffCommandHandler(new StudentExportService(students, jobs, rs),
                new ApplicantService(students, jobs, applications, rs), new BatchResumeService(rs, students, jobs));
        apply = new ApplicationCommandHandler(jobs, applications);
    }

    private String call(Object handler, String command, String... lines) throws Exception {
        setUp();
        StringWriter out = new StringWriter();
        BufferedReader in = new BufferedReader(new StringReader(String.join("\n", lines) + "\n"));
        PrintWriter w = new PrintWriter(out, true);
        if (handler instanceof StaffCommandHandler h) h.handle(command, in, w);
        else ((ApplicationCommandHandler) handler).handle(command, in, w);
        return out.toString().replace("\r\n", "\n").trim();
    }

    private String staffCall(String command, String... lines) throws Exception {
        setUp();
        return call(staff, command, lines);
    }

    private static final String[] NO_FILTERS = {"", "", "", "", "", ""};

    private static String[] withToken(String token, String... rest) {
        String[] all = new String[rest.length + 1];
        all[0] = token;
        System.arraycopy(rest, 0, all, 1, rest.length);
        return all;
    }

    // ---------------------------------------------------------------- authorization

    @Test
    void everyStaffCommand_refusesForgedTokensStudentsAndUnknownRoles_withoutLeakingData() throws Exception {
        setUp();
        List<String> commands = List.of("EXPORT_STUDENTS", "EXPORT_STUDENTS_XLSX", "JOB_APPLICANTS",
                "JOB_APPLICANTS_XLSX", "BATCH_START", "BATCH_ADD_FILE", "BATCH_STATUS");
        for (String cmd : commands) {
            String forged = staffCall(cmd, "forged-token", "J1", "", "", "", "", "");
            assertEquals("FAILED|Not authenticated. Please log in again.", forged, cmd);

            for (String token : List.of(studentToken, parentToken)) {
                String denied = staffCall(cmd, token, "J1", "", "", "", "", "");
                assertTrue(denied.startsWith("FAILED|Access denied"), cmd + " -> " + denied);
                assertEquals(1, denied.lines().count(), cmd);                 // a single refusal line ...
                assertFalse(denied.contains("Asha"), cmd);                    // ... and no student data
                assertFalse(denied.contains("asha@c.edu"), cmd);
            }
        }
        assertEquals(1, students.students.stream().filter(s -> s.getName().equals("Asha")).count());
    }

    @Test
    void tpoTpcAndAdminAreAllAllowed() throws Exception {
        for (String token : List.of(tpo, tpc, admin)) {
            String r = staffCall("EXPORT_STUDENTS", withToken(token, NO_FILTERS));
            assertTrue(r.startsWith("SUCCESS\n3\n"), r);
        }
    }

    @Test
    void aLoggedOutStaffTokenStopsWorking() throws Exception {
        String t = SessionManager.createSession("TPO", "Temp", 200);
        assertTrue(staffCall("EXPORT_STUDENTS", withToken(t, NO_FILTERS)).startsWith("SUCCESS"));
        SessionManager.invalidate(t);
        assertEquals("FAILED|Not authenticated. Please log in again.", staffCall("EXPORT_STUDENTS", withToken(t, NO_FILTERS)));
    }

    // ---------------------------------------------------------------- feature 1

    @Test
    void exportStudents_listHasTenFieldsPerRowAndRespectsFilters() throws Exception {
        String all = staffCall("EXPORT_STUDENTS", withToken(tpo, NO_FILTERS));
        String[] lines = all.split("\n");
        assertEquals("SUCCESS", lines[0]);
        assertEquals("3", lines[1]);
        assertEquals("END", lines[lines.length - 1]);
        assertEquals(10, lines[2].split("\\|", -1).length);
        assertTrue(lines[2].startsWith("1|B24CE1001|Asha|asha@c.edu|Computer|9.1|0||"));   // no job -> eligibility empty

        // job + eligibility + branch + min score
        String filtered = staffCall("EXPORT_STUDENTS", tpo, "J1", "computer", "8.0", "", "ELIGIBLE", "30");
        String[] f = filtered.split("\n");
        assertEquals("1", f[1]);
        assertTrue(f[2].contains("|Asha|"));
        assertTrue(f[2].contains("|Eligible|"));
    }

    @Test
    void exportStudentsXlsx_isAValidWorkbookWithTheFilteredRows() throws Exception {
        String r = staffCall("EXPORT_STUDENTS_XLSX", tpc, "", "IT", "", "", "", "");
        assertTrue(r.startsWith("SUCCESS|"));
        byte[] xlsx = Base64.getDecoder().decode(r.substring("SUCCESS|".length()));
        Map<String, String> cells = TestSupport.readXlsx(xlsx);
        assertEquals("Name", cells.get("B1"));
        assertEquals("Bhavesh", cells.get("B2"));
        assertFalse(cells.containsKey("B3"));
    }

    @Test
    void exportStudents_badInputGivesClearMessages() throws Exception {
        assertEquals("FAILED|Invalid minimum CGPA.", staffCall("EXPORT_STUDENTS", tpo, "", "", "abc", "", "", ""));
        assertEquals("FAILED|Invalid maximum CGPA.", staffCall("EXPORT_STUDENTS", tpo, "", "", "", "NaN", "", ""));
        assertEquals("FAILED|Eligibility must be ANY, ELIGIBLE or NOT_ELIGIBLE.", staffCall("EXPORT_STUDENTS", tpo, "", "", "", "", "MAYBE", ""));
        assertTrue(staffCall("EXPORT_STUDENTS", tpo, "", "", "", "", "ELIGIBLE", "").contains("Select a job"));
        assertTrue(staffCall("EXPORT_STUDENTS", tpo, "NOPE", "", "", "", "", "").contains("Job not found"));
        assertTrue(staffCall("EXPORT_STUDENTS", tpo, "", "", "9", "8", "", "").startsWith("FAILED|Minimum CGPA cannot be greater"));
    }

    // ---------------------------------------------------------------- feature 3

    @Test
    void jobApplicants_listsOnlyThoseWhoApplied_asTableAndExcel() throws Exception {
        setUp();
        applications.addApplication("J1", 1, "APPLIED", null, Timestamp.valueOf("2026-09-01 10:30:00"));
        applications.addApplication("J1", 3, "SHORTLISTED", null, Timestamp.valueOf("2026-09-02 09:05:00"));

        String table = staffCall("JOB_APPLICANTS", tpo, "J1");
        String[] lines = table.split("\n");
        assertEquals("2", lines[1]);                                           // Bhavesh is eligible but did not apply
        assertFalse(table.contains("Bhavesh"));
        assertEquals(12, lines[2].split("\\|", -1).length);
        assertTrue(lines[2].contains("|9111111111|"));                         // phone
        assertTrue(table.contains("|SHORTLISTED|2026-09-02 09:05|"));

        String x = staffCall("JOB_APPLICANTS_XLSX", admin, "J1");
        Map<String, String> cells = TestSupport.readXlsx(Base64.getDecoder().decode(x.substring("SUCCESS|".length())));
        assertEquals("Phone", cells.get("D1"));
        assertEquals("Asha", cells.get("B2"));
        assertEquals("Charu", cells.get("B3"));
        assertFalse(cells.containsKey("B4"));
    }

    @Test
    void jobApplicants_unknownOrMissingJob() throws Exception {
        assertTrue(staffCall("JOB_APPLICANTS", tpo, "NOPE").startsWith("FAILED|Job not found"));
        assertEquals("FAILED|Job ID is required.", staffCall("JOB_APPLICANTS", tpo, ""));
    }

    // ---------------------------------------------------------------- feature 2

    private static String b64(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void batch_endToEndOverTheProtocol() throws Exception {
        String start = staffCall("BATCH_START", tpo, "J1", "3");
        assertTrue(start.startsWith("SUCCESS|"), start);
        String batchId = start.substring("SUCCESS|".length());

        assertEquals("SUCCESS|1", staffCall("BATCH_ADD_FILE", tpo, batchId, "B24CE1001.txt", b64(RESUME)));
        assertEquals("SUCCESS|2", staffCall("BATCH_ADD_FILE", tpo, batchId, "unknown-person.txt", b64(RESUME)));
        assertEquals("SUCCESS|3", staffCall("BATCH_ADD_FILE", tpo, batchId, "B24CE1002.exe", b64("MZ")));

        String status = "";
        for (int i = 0; i < 300; i++) {
            status = staffCall("BATCH_STATUS", tpo, batchId);
            if (status.contains("|true\n")) break;
            Thread.sleep(20);
        }
        String[] lines = status.split("\n");
        assertEquals("SUCCESS", lines[0]);
        assertEquals("4", lines[1]);                                           // summary + 3 items
        assertEquals("SUMMARY|3|3|1|2|0|true", lines[2]);
        String ok = lines[3];
        assertTrue(ok.startsWith("ITEM|1|B24CE1001.txt|DONE|PARSED|B24CE1001|Asha|"), ok);
        assertTrue(ok.contains("|Eligible|"));
        assertTrue(lines[4].startsWith("ITEM|2|unknown-person.txt|FAILED|FAILED||||"), lines[4]);
        assertTrue(lines[5].contains("|FAILED|FAILED|B24CE1002|Bhavesh|"), lines[5]);   // matched the student, rejected the type
        assertTrue(resumes.rows.containsKey(1));
    }

    @Test
    void batch_otherStaffCannotAddFilesToSomeoneElsesBatch_butCanViewIt() throws Exception {
        String batchId = staffCall("BATCH_START", tpo, "J1", "2").substring("SUCCESS|".length());
        assertEquals("FAILED|This batch belongs to another user.",
                staffCall("BATCH_ADD_FILE", otherStaff, batchId, "B24CE1001.txt", b64(RESUME)));
        assertTrue(staffCall("BATCH_STATUS", otherStaff, batchId).startsWith("SUCCESS"));
    }

    @Test
    void batch_badRequestsGetClearMessages() throws Exception {
        assertEquals("FAILED|Invalid number of files.", staffCall("BATCH_START", tpo, "J1", "many"));
        assertTrue(staffCall("BATCH_START", tpo, "J1", "21").startsWith("FAILED|A batch must contain between 1 and 20"));
        assertTrue(staffCall("BATCH_START", tpo, "NOPE", "3").startsWith("FAILED|Job not found"));
        String batchId = staffCall("BATCH_START", tpo, "J1", "2").substring("SUCCESS|".length());
        assertEquals("FAILED|The file could not be decoded.", staffCall("BATCH_ADD_FILE", tpo, batchId, "a.txt", "!!!not base64!!!"));
        assertEquals("FAILED|Batch id, file name and file content are required.", staffCall("BATCH_ADD_FILE", tpo, batchId, "", b64("x")));
        assertEquals("FAILED|Batch not found or expired.", staffCall("BATCH_STATUS", tpo, "nope"));
    }

    // ---------------------------------------------------------------- size limits

    @Test
    void anEndlessLineIsRefusedInsteadOfBufferedInMemory() throws Exception {
        assertEquals("FAILED|The request is too large.", staffCall("EXPORT_STUDENTS", tpo, "J".repeat(500), "", "", "", "", ""));
        String batchId = staffCall("BATCH_START", tpo, "J1", "1").substring("SUCCESS|".length());
        long start = System.currentTimeMillis();
        assertEquals("FAILED|The request is too large.", staffCall("BATCH_ADD_FILE", tpo, batchId, "a.txt", "A".repeat(3_000_000)));
        assertTrue(System.currentTimeMillis() - start < 5000);
    }

    // ---------------------------------------------------------------- APPLY_JOB

    @Test
    void applyJob_studentAppliesOnce_andThenAppearsInTheApplicantList() throws Exception {
        setUp();
        assertEquals("SUCCESS|APPLIED", call(apply, "APPLY_JOB", studentToken, "J1"));
        assertEquals("SUCCESS|ALREADY_APPLIED", call(apply, "APPLY_JOB", studentToken, "J1"));
        assertEquals(1, applications.records.size());

        String table = staffCall("JOB_APPLICANTS", tpo, "J1");
        assertTrue(table.startsWith("SUCCESS\n1\n"), table);
        assertTrue(table.contains("|Asha|"));
        assertTrue(table.contains("|APPLIED|"));
    }

    @Test
    void applyJob_isStudentOnly_andUsesTheSessionIdentity() throws Exception {
        setUp();
        assertTrue(call(apply, "APPLY_JOB", tpo, "J1").startsWith("FAILED|Only students"));
        assertTrue(call(apply, "APPLY_JOB", parentToken, "J1").startsWith("FAILED|Only students"));
        assertEquals("FAILED|Not authenticated. Please log in again.", call(apply, "APPLY_JOB", "forged", "J1"));
        assertTrue(call(apply, "APPLY_JOB", studentToken, "NOPE").startsWith("FAILED|Job not found"));
        assertEquals("FAILED|Job ID is required.", call(apply, "APPLY_JOB", studentToken, ""));
        assertEquals(0, applications.records.size());
        call(apply, "APPLY_JOB", studentToken, "J1");
        assertEquals(1, applications.records.get(0).studentId());              // the student in the session, nobody else
    }
}
