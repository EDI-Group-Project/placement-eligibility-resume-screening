package com.placement.integration;

import com.placement.database.ApplicationDAO;
import com.placement.database.DatabaseConnection;
import com.placement.database.ResumeDAO;
import com.placement.models.Student;
import com.placement.services.StudentService;
import com.placement.services.resume.ExportService;
import com.placement.sockets.ClientHandler;
import com.placement.sockets.SocketClient;
import com.placement.testsupport.XlsxReader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * End-to-end tests of the three staff features against a real MySQL database, through the real socket server
 * (ClientHandler) and the real SocketClient. Run with -DRUN_DB_TESTS=true, like DatabaseIntegrationTest.
 *
 * Uses the demo data from sql/02_seed.sql (accounts, students, JOB001). Everything a test creates (applications,
 * resumes and their files) is removed again in tearDown, and a test is skipped rather than touching a student who
 * already has a real resume or application.
 */
class ResumeFeaturesIntegrationTest {

    private static final String JOB = "JOB001";
    private static final String DENIED = "You do not have permission for this operation.";

    private static ServerSocket server;
    private static ExecutorService pool;
    private static SocketClient client;
    private static Map<String, Student> byPrn;
    private static String admin, tpo, tpc, director, dean;

    private final List<Object[]> createdApplications = new ArrayList<>();   // {jobId, studentId}
    private final List<Integer> createdResumes = new ArrayList<>();         // student ids

    // ------------------------------------------------------------------ server and accounts

    @BeforeAll
    static void startServer() throws Exception {
        assumeTrue("true".equalsIgnoreCase(System.getProperty("RUN_DB_TESTS")),
                "Set -DRUN_DB_TESTS=true to execute MySQL integration tests.");
        server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        pool = Executors.newCachedThreadPool();
        Thread acceptor = new Thread(() -> {
            while (!server.isClosed()) {
                try {
                    Socket s = server.accept();
                    s.setSoTimeout(15000);                  // same as Server.java
                    pool.execute(new ClientHandler(s));
                } catch (IOException closed) {
                    return;
                }
            }
        });
        acceptor.setDaemon(true);
        acceptor.start();
        client = new SocketClient("127.0.0.1", server.getLocalPort());

        byPrn = new HashMap<>();
        for (Student s : new StudentService().getAllStudents()) byPrn.put(s.getPrn(), s);
        admin = login("ADMIN", "admin@college.com");
        tpo = login("TPO", "amit@college.com");
        tpc = login("TPC", "neha@college.com");
        director = login("DIRECTOR", "rajesh@college.com");
        dean = login("DEAN", "pooja@college.com");
    }

    @AfterAll
    static void stopServer() throws Exception {
        if (server != null) server.close();
        if (pool != null) pool.shutdownNow();
    }

    @AfterEach
    void tearDown() throws Exception {
        try (Connection c = DatabaseConnection.getConnection()) {
            for (Object[] a : createdApplications) {
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM applications WHERE job_id=? AND student_id=?")) {
                    ps.setString(1, (String) a[0]);
                    ps.setInt(2, (Integer) a[1]);
                    ps.executeUpdate();
                }
            }
            for (int studentId : createdResumes) {
                String path = null;
                try (PreparedStatement ps = c.prepareStatement("SELECT file_path FROM resumes WHERE student_id=?")) {
                    ps.setInt(1, studentId);
                    try (ResultSet rs = ps.executeQuery()) { if (rs.next()) path = rs.getString(1); }
                }
                if (path != null) Files.deleteIfExists(Paths.get(path));
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM resumes WHERE student_id=?")) {
                    ps.setInt(1, studentId);
                    ps.executeUpdate();
                }
            }
        } finally {
            createdApplications.clear();
            createdResumes.clear();
        }
    }

    // ------------------------------------------------------------------ small helpers

    private static String login(String role, String email) throws IOException {
        SocketClient.Response r = client.sendRequest("LOGIN", role, email, "pass123");
        assertTrue(r.success, "login " + email + ": " + r.payload);
        return r.parts()[0];
    }

    private static String[] with(String token, String... rest) {
        String[] all = new String[rest.length + 1];
        all[0] = token;
        System.arraycopy(rest, 0, all, 1, rest.length);
        return all;
    }

    private static String b64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static byte[] fileBytes(String payload) {
        return Base64.getDecoder().decode(payload.split("\\|")[1]);
    }

    private static String fileName(String payload) {
        return new String(Base64.getDecoder().decode(payload.split("\\|")[0]), StandardCharsets.UTF_8);
    }

    private static String resumeText(String skills) {
        return "Asha Rao\nasha@college.edu 98765 43210\nSkills\n" + skills + "\nProjects\nPlacement Portal\n- Developed a socket server\n"
                + "Experience\nIntern at Acme\nCertifications\nAWS Cloud Practitioner\n";
    }

    private static byte[] docx(String text) throws IOException {
        StringBuilder body = new StringBuilder();
        for (String line : text.split("\n")) body.append("<w:p><w:r><w:t>").append(line).append("</w:t></w:r></w:p>");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write(("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>"
                    + body + "</w:body></w:document>").getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return out.toByteArray();
    }

    private static int idOf(String prn) {
        return byPrn.get(prn).getStudentId();
    }

    private static int countApplications(String jobId, int studentId) throws Exception {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM applications WHERE job_id=? AND student_id=?")) {
            ps.setString(1, jobId);
            ps.setInt(2, studentId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    /** Skips the test instead of touching a student who already has real data; otherwise registers the cleanup. */
    private void claimResume(int studentId) throws Exception {
        assumeFalse(new ResumeDAO().existsForStudent(studentId), "student " + studentId + " already has a resume - not touching it");
        createdResumes.add(studentId);
    }

    private void claimApplication(String jobId, int studentId) throws Exception {
        assumeFalse(new ApplicationDAO().exists(jobId, studentId), "student " + studentId + " already applied - not touching it");
        createdApplications.add(new Object[]{jobId, studentId});
    }

    private void applyAs(String email, String jobId) throws IOException {
        SocketClient.Response r = client.sendRequest("APPLY_JOB", login("STUDENT", email), jobId);
        assertTrue(r.success, r.payload);
    }

    private void uploadAs(String email, String text) throws IOException {
        SocketClient.Response r = client.sendRequest("UPLOAD_RESUME", login("STUDENT", email), "resume.txt", b64(text.getBytes(StandardCharsets.UTF_8)));
        assertTrue(r.success, r.payload);
    }

    private List<String> applicantLines(String token, String jobId) throws IOException {
        SocketClient.ListResponse r = client.sendListRequest("GET_JOB_APPLICANTS", token, jobId);
        assertTrue(r.success, r.errorMessage);
        return r.lines;
    }

    private static String[] fields(String line) {
        return line.split("\\|", -1);
    }

    private static String[] rowFor(List<String> applicantLines, String prn) {
        return applicantLines.stream().map(ResumeFeaturesIntegrationTest::fields).filter(f -> f[0].equals(prn)).findFirst().orElse(null);
    }

    // ------------------------------------------------------------------ Feature 1

    @Test
    void staffCanFilterAndExportStudents_theFileMatchesTheTable_andEveryoneElseIsRefused() throws Exception {
        List<String[]> filterSets = List.of(
                new String[]{"", "CSE", "0", "8.5", "0", "0", "CGPA", "false"},
                new String[]{"patil", "ALL", "2027", "0", "1", "5", "NAME", "true"});
        List<List<String>> expectedPrns = List.of(List.of("PRN016", "PRN004", "PRN006", "PRN001"), List.of("PRN016", "PRN002"));

        for (String token : List.of(tpo, tpc, admin)) {         // TPO is the newly allowed role for the table
            for (int i = 0; i < filterSets.size(); i++) {
                String[] filters = filterSets.get(i);
                SocketClient.ListResponse table = client.sendListRequest("SEARCH_SORT_FILTER_STUDENTS", with(token, filters));
                assertTrue(table.success, table.errorMessage);
                assertEquals(expectedPrns.get(i), table.lines.stream().map(l -> fields(l)[0]).toList());

                SocketClient.Response export = client.sendRequest("EXPORT_STUDENTS", with(token, filters));
                assertTrue(export.success, export.payload);
                assertTrue(fileName(export.payload).matches("students_\\d{4}-\\d{2}-\\d{2}\\.xlsx"), fileName(export.payload));

                List<List<String>> rows = XlsxReader.rows(fileBytes(export.payload));
                assertEquals(ExportService.STUDENT_COLUMNS, rows.get(0));
                assertEquals(table.lines.size() + 1, rows.size(), "the file has exactly the rows of the table");
                for (int r = 0; r < table.lines.size(); r++) {
                    String[] t = fields(table.lines.get(r));
                    List<String> row = rows.get(r + 1);
                    assertEquals(t[0], row.get(0));                                        // PRN
                    assertEquals(t[1], row.get(1));                                        // name
                    assertEquals(t[2], row.get(2));                                        // email
                    assertEquals(t[3], row.get(3));                                        // department
                    assertEquals(Double.parseDouble(t[4]), Double.parseDouble(row.get(4)), 0.0001);   // CGPA
                    assertEquals(Integer.parseInt(t[5]), Integer.parseInt(row.get(5)));    // passing year
                    assertEquals(Integer.parseInt(t[6]), Integer.parseInt(row.get(6)));    // backlogs
                    assertEquals(Integer.parseInt(t[7]), row.get(7).isEmpty() ? 0 : Integer.parseInt(row.get(7)));   // semester
                    assertEquals(t[8], row.get(8));                                        // phone
                    assertEquals(t[9], row.get(9));                                        // skills
                }
            }
        }

        String student = login("STUDENT", "aarav@gmail.com");
        for (String token : List.of(student, director, dean)) {
            SocketClient.Response refused = client.sendRequest("EXPORT_STUDENTS", with(token, filterSets.get(0)));
            assertFalse(refused.success);
            assertEquals(DENIED, refused.payload);
            assertFalse(client.sendListRequest("SEARCH_SORT_FILTER_STUDENTS", with(token, filterSets.get(0))).success, "unchanged for these roles");
        }
    }

    @Test
    void exportWithNoFilters_hasAllStudents_andNoPasswordColumn() throws Exception {
        SocketClient.Response export = client.sendRequest("EXPORT_STUDENTS", with(tpc, "", "ALL", "0", "-1", "0", "0", "NAME", "true"));
        assertTrue(export.success, export.payload);
        List<List<String>> rows = XlsxReader.rows(fileBytes(export.payload));
        assertEquals(byPrn.size() + 1, rows.size());
        for (var part : XlsxReader.parts(fileBytes(export.payload)).values()) {
            String text = new String(part, StandardCharsets.UTF_8);
            assertFalse(text.contains("$"), "no password hash (the demo hashes contain '$')");
        }
    }

    // ------------------------------------------------------------------ Feature 3

    @Test
    void jobApplicantList_hasOnlyStudentsWhoApplied_screeningNeverAddsApplicants_andExcelMatchesTheList() throws Exception {
        int aarav = idOf("PRN001"), priya = idOf("PRN002"), sneha = idOf("PRN004");
        for (int id : new int[]{aarav, priya, sneha}) claimApplication(JOB, id);
        claimResume(aarav);
        claimResume(sneha);
        int baseline = applicantLines(tpo, JOB).size();

        applyAs("aarav@gmail.com", JOB);
        applyAs("priya@gmail.com", JOB);

        // Sneha did NOT apply. Staff screening her must not turn her into an applicant (the old code inserted a 'SCREENED' row).
        uploadAs("sneha@gmail.com", resumeText("Java, SQL, Python"));
        SocketClient.ListResponse screenedNonApplicant = client.sendListRequest("SCREEN_STUDENT", tpc, JOB, "PRN004");
        assertTrue(screenedNonApplicant.success, screenedNonApplicant.errorMessage);
        assertEquals(0, countApplications(JOB, sneha), "screening a non-applicant creates no application");

        // Screening a real applicant stores the score and moves APPLIED -> SCREENED
        uploadAs("aarav@gmail.com", resumeText("Java, SQL, Python"));
        SocketClient.ListResponse screened = client.sendListRequest("SCREEN_STUDENT", tpc, JOB, "PRN001");
        assertTrue(screened.success, screened.errorMessage);
        double total = Double.parseDouble(screened.lines.stream().filter(l -> l.startsWith("TOTAL|")).findFirst().orElseThrow().substring(6));

        for (String token : List.of(tpo, tpc, admin)) {
            List<String> list = applicantLines(token, JOB);
            assertEquals(baseline + 2, list.size());
            assertNull(rowFor(list, "PRN004"), "Sneha never applied");
            String[] a = rowFor(list, "PRN001"), p = rowFor(list, "PRN002");
            assertNotNull(a);
            assertNotNull(p);
            assertEquals(13, a.length);                                                    // 10 student columns + status + score + applied at
            assertEquals("Aarav Sharma", a[1]);
            assertEquals("aarav@gmail.com", a[2]);
            assertEquals("SCREENED", a[10]);
            assertEquals(total, Double.parseDouble(a[11]), 0.01);
            assertTrue(a[12].matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"), a[12]);
            assertEquals("APPLIED", p[10]);
            assertEquals("", p[11], "not screened yet");
        }

        // The Excel file holds the same rows in the same order
        SocketClient.Response export = client.sendRequest("EXPORT_JOB_APPLICANTS", with(admin, JOB));
        assertTrue(export.success, export.payload);
        assertEquals("applicants_" + JOB + "_", fileName(export.payload).substring(0, ("applicants_" + JOB + "_").length()));
        List<List<String>> rows = XlsxReader.rows(fileBytes(export.payload));
        assertEquals(ExportService.APPLICANT_COLUMNS, rows.get(0));
        List<String> list = applicantLines(admin, JOB);
        assertEquals(list.size() + 1, rows.size());
        for (int i = 0; i < list.size(); i++) {
            String[] t = fields(list.get(i));
            List<String> row = rows.get(i + 1);
            assertEquals(t[0], row.get(0));
            assertEquals(t[1], row.get(1));
            assertEquals(t[10], row.get(10));
            assertEquals(t[11].isEmpty() ? "" : String.valueOf(Double.parseDouble(t[11])), row.get(11).isEmpty() ? "" : String.valueOf(Double.parseDouble(row.get(11))));
            assertEquals(t[12], row.get(12));
        }

        // Because screening left no phantom row behind, Sneha can still apply the normal way, and then she is listed
        applyAs("sneha@gmail.com", JOB);
        assertEquals("APPLIED", rowFor(applicantLines(tpo, JOB), "PRN004")[10]);
    }

    @Test
    void jobApplicants_areStaffOnly_andUnknownOrBlankJobsAreReported() throws Exception {
        String student = login("STUDENT", "aarav@gmail.com");
        for (String command : List.of("GET_JOB_APPLICANTS", "EXPORT_JOB_APPLICANTS")) {
            for (String token : List.of(student, director, dean)) {
                SocketClient.Response r = client.sendRequest(command, token, JOB);
                assertFalse(r.success);
                assertEquals(DENIED, r.payload, command);
            }
            for (String token : List.of(tpo, tpc, admin)) {
                SocketClient.Response unknown = client.sendRequest(command, token, "NO-SUCH-JOB");
                assertFalse(unknown.success);
                assertEquals("Job not found: NO-SUCH-JOB", unknown.payload);
                assertEquals("Job ID is required.", client.sendRequest(command, token, "").payload);
            }
        }
        assertFalse(client.sendListRequest("GET_JOB_APPLICANTS", "bad-token", JOB).success);
    }

    @Test
    void applicationDao_screeningScoreOnlyUpdatesExistingApplications_andKeepsManualStatuses() throws Exception {
        ApplicationDAO dao = new ApplicationDAO();
        int rohan = idOf("PRN003"), vivek = idOf("PRN005");
        claimApplication("JOB004", rohan);
        claimApplication("JOB004", vivek);

        dao.saveScreeningScore("JOB004", rohan, 55.5);                       // Rohan never applied
        assertFalse(dao.exists("JOB004", rohan), "no application may appear");

        dao.insert("JOB004", rohan);
        dao.insert("JOB004", vivek);
        dao.saveScreeningScore("JOB004", rohan, 61.25);
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE applications SET status='SHORTLISTED' WHERE job_id='JOB004' AND student_id=?")) {
            ps.setInt(1, vivek);
            ps.executeUpdate();                                              // a status someone set by hand
        }
        dao.saveScreeningScore("JOB004", vivek, 40.0);

        List<ApplicationDAO.Applicant> applicants = dao.findApplicantsByJob("JOB004");
        assertEquals(2, applicants.size());
        assertEquals(List.of(rohan, vivek), applicants.stream().map(a -> a.student().getStudentId()).toList(), "earliest application first");
        ApplicationDAO.Applicant r = applicants.get(0), v = applicants.get(1);
        assertEquals("SCREENED", r.status());
        assertEquals(61.25, r.screeningScore(), 0.001);
        assertEquals("SHORTLISTED", v.status(), "a status set by hand is not overwritten");
        assertEquals(40.0, v.screeningScore(), 0.001);
        assertEquals("Rohan Mehta", r.student().getName());
        assertEquals("PRN003", r.student().getPrn());
        assertNull(r.student().getPassword(), "the password hash is not even loaded");
        assertTrue(dao.findApplicantsByJob("JOB003").isEmpty());
        assertTrue(dao.findApplicantsByJob("NOPE").isEmpty());
    }

    // ------------------------------------------------------------------ Feature 2

    private List<Student> studentsWithoutResume(int wanted) throws Exception {
        ResumeDAO dao = new ResumeDAO();
        List<Student> free = new ArrayList<>();
        for (Student s : byPrn.values().stream().sorted((a, b) -> a.getPrn().compareTo(b.getPrn())).toList()) {
            if (!dao.existsForStudent(s.getStudentId())) free.add(s);
            if (free.size() == wanted) break;
        }
        assumeTrue(free.size() == wanted, "need " + wanted + " students without a resume");
        return free;
    }

    @Test
    void batchUpload_endToEnd_eachFileHasItsOwnStatusAndAnalysis_andStoredResumesWorkWithTheExistingCommands() throws Exception {
        List<Student> free = studentsWithoutResume(11);
        for (Student s : free) {
            claimResume(s.getStudentId());
            claimApplication(JOB, s.getStudentId());        // none expected; if a bug creates one, tearDown still removes it
        }
        Map<Integer, Integer> applicationsBefore = new HashMap<>();
        for (Student s : free) applicationsBefore.put(s.getStudentId(), countApplications(JOB, s.getStudentId()));

        List<String> request = new ArrayList<>(List.of(JOB, "12"));
        byte[] firstFile = null;
        for (int i = 0; i < 9; i++) {                                        // 1-9: good resumes, student named by id / PRN / email in turn
            Student s = free.get(i);
            String ref = switch (i % 3) { case 0 -> String.valueOf(s.getStudentId()); case 1 -> s.getPrn(); default -> s.getEmail(); };
            byte[] bytes = i % 4 == 3 ? docx(resumeText("Java, SQL")) : resumeText("Java, SQL, Python").getBytes(StandardCharsets.UTF_8);
            if (i == 0) firstFile = bytes;
            request.addAll(List.of(ref, "resume" + i + (i % 4 == 3 ? ".docx" : ".txt"), b64(bytes)));
        }
        request.addAll(List.of(free.get(9).getPrn(), "scan.txt", b64("   \n  \n".getBytes(StandardCharsets.UTF_8))));           // 10: nothing readable
        request.addAll(List.of(free.get(10).getPrn(), "virus.exe", b64(resumeText("Java").getBytes(StandardCharsets.UTF_8))));  // 11: rejected type
        request.addAll(List.of("NOBODY-HERE", "ghost.txt", b64(resumeText("Java").getBytes(StandardCharsets.UTF_8))));           // 12: unknown student

        SocketClient.ListResponse reply = client.sendListRequest("UPLOAD_RESUME_BATCH", with(tpo, request.toArray(new String[0])));
        assertTrue(reply.success, reply.errorMessage);
        assertEquals(13, reply.lines.size());
        String[] header = fields(reply.lines.get(0));
        assertEquals("BATCH", header[0]);
        assertEquals(List.of("12", "9", "1", "2", JOB), List.of(header).subList(2, 7));

        List<String> statuses = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            String[] f = fields(reply.lines.get(i));
            assertEquals(17, f.length);
            assertEquals(String.valueOf(i), f[1]);
            statuses.add(f[7]);
            if (f[7].equals("PROCESSED")) {
                assertEquals(String.valueOf(free.get(i - 1).getStudentId()), f[3]);
                assertTrue(Integer.parseInt(f[8]) > 10, "word count");
                assertFalse(f[9].isEmpty(), "ATS score");
                assertFalse(f[12].isEmpty(), "job screening total");
                assertTrue(f[14].contains("java"), "matched skills");
            }
        }
        assertEquals(List.of("PROCESSED", "PROCESSED", "PROCESSED", "PROCESSED", "PROCESSED", "PROCESSED", "PROCESSED", "PROCESSED",
                "PROCESSED", "UNREADABLE", "FAILED", "FAILED"), statuses);
        assertEquals("Only PDF, DOCX or TXT resumes are accepted.", fields(reply.lines.get(11))[16]);
        assertEquals("Student not found: NOBODY-HERE", fields(reply.lines.get(12))[16]);

        ResumeDAO dao = new ResumeDAO();
        for (int i = 0; i < 10; i++) assertTrue(dao.existsForStudent(free.get(i).getStudentId()), "stored: " + free.get(i).getPrn());
        assertFalse(dao.existsForStudent(free.get(10).getStudentId()), "the rejected file stored nothing");
        for (Student s : free) assertEquals(applicationsBefore.get(s.getStudentId()), countApplications(JOB, s.getStudentId()), "a batch never touches applications");

        // another staff member can read the same batch back
        String batchId = header[1];
        SocketClient.ListResponse again = client.sendListRequest("GET_RESUME_BATCH", tpc, batchId);
        assertTrue(again.success, again.errorMessage);
        assertEquals(reply.lines, again.lines);
        assertFalse(client.sendListRequest("GET_RESUME_BATCH", tpc, "no-such-batch").success);

        // resumes stored by the batch are ordinary resumes for the existing commands
        SocketClient.Response download = client.sendRequest("DOWNLOAD_RESUME", tpo, free.get(0).getPrn());
        assertTrue(download.success, download.payload);
        assertArrayEquals(firstFile, fileBytes(download.payload));
        String studentToken = login("STUDENT", free.get(0).getEmail());
        assertEquals("AVAILABLE|resume0.txt", client.sendRequest("GET_RESUME_STATUS", studentToken).payload);
        SocketClient.ListResponse ats = client.sendListRequest("GET_ATS_FEEDBACK", studentToken);
        assertTrue(ats.success, ats.errorMessage);
        assertTrue(ats.lines.get(0).startsWith("SCORE|"));
    }

    @Test
    void batchOf20_isAnsweredWithinTheClientsTenSecondTimeout_andStudentsAreRefused() throws Exception {
        List<Student> free = studentsWithoutResume(20);
        for (Student s : free) claimResume(s.getStudentId());

        List<String> request = new ArrayList<>(List.of("", "20"));
        for (int i = 0; i < 20; i++) {
            request.addAll(List.of(String.valueOf(free.get(i).getStudentId()), "r" + i + ".txt",
                    b64(resumeText("Java, SQL, Python, Docker, Git").repeat(20).getBytes(StandardCharsets.UTF_8))));
        }
        long start = System.nanoTime();
        SocketClient.ListResponse reply = client.sendListRequest("UPLOAD_RESUME_BATCH", with(admin, request.toArray(new String[0])));
        long ms = (System.nanoTime() - start) / 1_000_000;
        System.out.println("[ResumeFeaturesIntegrationTest] 20-resume batch answered in " + ms + " ms");

        assertTrue(reply.success, reply.errorMessage);
        assertEquals(21, reply.lines.size());
        assertEquals(List.of("20", "20", "0", "0"), List.of(fields(reply.lines.get(0))).subList(2, 6));

        String student = login("STUDENT", "aarav@gmail.com");
        for (String token : List.of(student, director, dean)) {
            SocketClient.ListResponse refused = client.sendListRequest("UPLOAD_RESUME_BATCH", with(token, "", "1", "1", "a.txt", b64("Skills\nJava\n".getBytes(StandardCharsets.UTF_8))));
            assertFalse(refused.success);
            assertEquals(DENIED, refused.errorMessage);
        }
        assertEquals(20, countResumes(free));
    }

    @Test
    void aRefusedBigBatch_stillDeliversTheReasonToAClientThatIsStillSending() throws Exception {
        String student = login("STUDENT", "aarav@gmail.com");
        String big = "A".repeat(2_500_000);                                    // 20 files x 2.5 MB = 50 MB, like a full batch
        Map<String, String> cases = new java.util.LinkedHashMap<>();
        cases.put(student + "\n\n20", "FAILED|" + DENIED);                       // a student
        cases.put(tpo + "\n\n21", "FAILED|A batch must contain 1 to 20 resumes.");   // too many (declared 21, 20 sent)
        cases.put(tpo + "\nNO-SUCH-JOB\n20", "FAILED|Job not found: NO-SUCH-JOB");
        cases.put("stale-token\n\n20", "FAILED|Not authenticated. Please log in again.");
        for (var c : cases.entrySet()) {
            try (Socket socket = new Socket("127.0.0.1", server.getLocalPort())) {
                socket.setSoTimeout(30000);
                var out = new java.io.BufferedOutputStream(socket.getOutputStream(), 1 << 16);
                out.write(("UPLOAD_RESUME_BATCH\n" + c.getKey() + "\n").getBytes(StandardCharsets.UTF_8));
                for (int i = 0; i < 20; i++) out.write(((i + 1) + "\nf" + i + ".txt\n" + big + "\n").getBytes(StandardCharsets.UTF_8));
                out.flush();                                                   // throws when the server hung up early
                var in = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                assertEquals(c.getValue(), in.readLine(), c.getKey().replace("\n", " / ").substring(0, 12));
            }
        }
    }

    private static int countResumes(List<Student> students) throws Exception {
        int n = 0;
        for (Student s : students) if (new ResumeDAO().existsForStudent(s.getStudentId())) n++;
        return n;
    }

    // ------------------------------------------------------------------ the existing resume commands still behave

    @Test
    void existingResumeCommandsStillWork() throws Exception {
        int aarav = idOf("PRN001");
        claimResume(aarav);
        claimApplication(JOB, aarav);                        // none expected; if a bug creates one, tearDown still removes it
        String student = login("STUDENT", "aarav@gmail.com");

        assertEquals("MISSING", client.sendRequest("GET_RESUME_STATUS", student).payload);
        SocketClient.Response upload = client.sendRequest("UPLOAD_RESUME", student, "my resume.txt", b64(resumeText("Java, SQL").getBytes(StandardCharsets.UTF_8)));
        assertTrue(upload.success, upload.payload);
        assertTrue(upload.payload.startsWith("READABLE|"));
        assertEquals("AVAILABLE|my resume.txt", client.sendRequest("GET_RESUME_STATUS", student).payload);
        assertTrue(client.sendListRequest("GET_ATS_FEEDBACK", student).success);

        SocketClient.Response staffUpload = client.sendRequest("UPLOAD_RESUME", tpo, "x.txt", b64("Skills\nJava\n".getBytes(StandardCharsets.UTF_8)));
        assertFalse(staffUpload.success);                                    // single upload is still student-only
        assertEquals("Only students can upload a resume.", staffUpload.payload);

        SocketClient.ListResponse screen = client.sendListRequest("SCREEN_STUDENT", tpo, JOB, "PRN001");
        assertTrue(screen.success, screen.errorMessage);
        assertTrue(screen.lines.get(0).startsWith("TOTAL|"));
        assertTrue(client.sendListRequest("RANK_CANDIDATES", tpc, JOB).success);
        assertFalse(client.sendListRequest("RANK_CANDIDATES", student, JOB).success);
        assertTrue(client.sendRequest("DOWNLOAD_RESUME", dean, "PRN001").success);
        assertEquals(0, countApplications(JOB, aarav) , "none of this created an application");
    }
}
