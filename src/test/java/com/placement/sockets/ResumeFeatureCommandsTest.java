package com.placement.sockets;

import com.placement.database.ApplicationDAO;
import com.placement.models.Student;
import com.placement.services.resume.ExportService;
import com.placement.services.resume.ResumeScreeningService;
import com.placement.testsupport.Fakes;
import com.placement.testsupport.XlsxReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The five new staff commands, driven through ResumeCommandHandler exactly as ClientHandler drives it (token on the first
 * line, then one value per line). Fake DAOs and services, so no MySQL and no network.
 */
class ResumeFeatureCommandsTest {

    private static final String DENIED = "FAILED|You do not have permission for this operation.";
    private static final int MAX_BASE64_CHARS = ResumeScreeningService.MAX_UPLOAD_BYTES / 3 * 4 + 8;

    private Fakes.ResumeDao resumes;
    private Fakes.ApplicationDao applications;
    private Fakes.Students students;
    private Fakes.Jobs jobs;
    private ResumeCommandHandler handler;
    private Path dir;
    private String admin, tpo, tpc, studentToken, director, dean, faculty;

    @BeforeEach
    void setUp() throws Exception {
        resumes = new Fakes.ResumeDao();
        applications = new Fakes.ApplicationDao();
        students = new Fakes.Students();
        jobs = new Fakes.Jobs();
        dir = Files.createTempDirectory("handler-test");
        handler = new ResumeCommandHandler(students, jobs, new ResumeScreeningService(resumes, applications, dir), applications);

        for (int i = 1; i <= 20; i++) {
            students.all.add(Fakes.student(i, String.format("PRN%03d", i), "Student " + i, "s" + i + "@college.edu",
                    i % 2 == 0 ? "IT" : "CSE", 6.5 + i / 10.0, 2027, i % 3, i % 8 + 1, "98765432" + String.format("%02d", i), "Python,Excel"));
        }
        jobs.jobs.put("JOB-1", Fakes.job("JOB-1", "Acme", "SDE", "java", "sql", "kubernetes"));

        admin = SessionManager.createSession("ADMIN", "Admin", 1);
        tpo = SessionManager.createSession("TPO", "Tpo", 2);
        tpc = SessionManager.createSession("TPC", "Tpc", 3);
        studentToken = SessionManager.createSession("STUDENT", "Student 5", 5);
        director = SessionManager.createSession("DIRECTOR", "Director", 4);
        dean = SessionManager.createSession("DEAN", "Dean", 6);
        faculty = SessionManager.createSession("FACULTY", "Faculty", 7);
    }

    @AfterEach
    void tearDown() {
        for (String t : List.of(admin, tpo, tpc, studentToken, director, dean, faculty)) SessionManager.invalidate(t);
    }

    // ------------------------------------------------------------------ helpers

    private List<String> run(String command, String token, String... requestLines) throws Exception {
        String request = token + "\n" + String.join("\n", requestLines) + (requestLines.length > 0 ? "\n" : "");
        StringWriter out = new StringWriter();
        handler.handle(command, new BufferedReader(new StringReader(request)), new PrintWriter(out, true));
        return out.toString().lines().toList();
    }

    /** A list answer: SUCCESS / count / lines / END. Returns the lines. */
    private static List<String> listBody(List<String> reply) {
        assertEquals("SUCCESS", reply.get(0), reply.toString());
        int n = Integer.parseInt(reply.get(1));
        assertEquals(n + 3, reply.size(), reply.toString());
        assertEquals("END", reply.get(reply.size() - 1));
        return reply.subList(2, 2 + n);
    }

    private record FileReply(String name, byte[] bytes) {}

    /** A file answer: SUCCESS|base64(name)|base64(bytes). */
    private static FileReply fileReply(List<String> reply) {
        assertEquals(1, reply.size(), reply.size() > 3 ? "unexpected multi-line reply" : reply.toString());
        String[] p = reply.get(0).split("\\|");
        assertEquals(3, p.length, "expected SUCCESS|name|file");
        assertEquals("SUCCESS", p[0]);
        return new FileReply(new String(Base64.getDecoder().decode(p[1]), StandardCharsets.UTF_8), Base64.getDecoder().decode(p[2]));
    }

    private static String b64(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String resume(String skills) {
        return "Asha Rao\nasha@college.edu 98765 43210\nSkills\n" + skills + "\nProjects\nPlacement Portal\n- Developed a socket server\n"
                + "Experience\nIntern at Acme\nCertifications\nAWS Cloud Practitioner\n";
    }

    private static String[] file(String ref, String name, String base64) {
        return new String[]{ref, name, base64};
    }

    private static String[] batch(String jobId, String count, String[]... files) {
        List<String> lines = new ArrayList<>(List.of(jobId, count));
        for (String[] f : files) lines.addAll(List.of(f));
        return lines.toArray(new String[0]);
    }

    private static String[] txtFiles(int from, int to) {
        List<String[]> files = new ArrayList<>();
        for (int i = from; i <= to; i++) files.add(file(String.valueOf(i), "resume" + i + ".txt", b64(resume("Java, SQL"))));
        return batch("", String.valueOf(to - from + 1), files.toArray(new String[0][]));
    }

    private static String[] resultLine(String line) {
        String[] f = line.split("\\|", -1);
        assertEquals(17, f.length, line);
        assertEquals("RESULT", f[0]);
        return f;
    }

    // ------------------------------------------------------------------ permissions

    /** One valid request per new command, so each can be tried with every role. */
    private Map<String, String[]> validRequests() {
        return Map.of(
                "EXPORT_STUDENTS", new String[]{"", "ALL", "0", "0", "0", "0", "CGPA", "false"},
                "GET_JOB_APPLICANTS", new String[]{"JOB-1"},
                "EXPORT_JOB_APPLICANTS", new String[]{"JOB-1"},
                "UPLOAD_RESUME_BATCH", batch("", "1", file("1", "a.txt", b64(resume("Java")))),
                "GET_RESUME_BATCH", new String[]{"unknown-id"});
    }

    @Test
    void studentsDirectorsDeansAndAnyOtherRole_areRefusedByEveryNewCommand_andNothingHappens() throws Exception {
        for (String token : List.of(studentToken, director, dean, faculty)) {
            for (var request : validRequests().entrySet()) {
                assertEquals(List.of(DENIED), run(request.getKey(), token, request.getValue()),
                        request.getKey() + " as " + SessionManager.getRole(token));
            }
        }
        assertEquals(0, resumes.saves, "a refused batch stores nothing");
        assertNull(students.lastQuery, "a refused export does not even query");
    }

    @Test
    void adminTpoAndTpc_areAllowedOnEveryNewCommand() throws Exception {
        for (String token : List.of(admin, tpo, tpc)) {
            for (var request : validRequests().entrySet()) {
                List<String> reply = run(request.getKey(), token, request.getValue());
                assertNotEquals(List.of(DENIED), reply, request.getKey() + " as " + SessionManager.getRole(token));
                if (request.getKey().equals("GET_RESUME_BATCH")) assertTrue(reply.get(0).startsWith("FAILED|Batch not found"));
                else assertTrue(reply.get(0).startsWith("SUCCESS"), request.getKey() + " -> " + reply.get(0).substring(0, Math.min(80, reply.get(0).length())));
            }
        }
    }

    @Test
    void withoutAValidSession_everyNewCommandSaysNotAuthenticated() throws Exception {
        for (var request : validRequests().entrySet()) {
            assertEquals(List.of("FAILED|Not authenticated. Please log in again."), run(request.getKey(), "forged-token", request.getValue()));
            assertEquals(List.of("FAILED|Not authenticated. Please log in again."), run(request.getKey(), "", request.getValue()));
        }
        assertEquals(0, resumes.saves);
    }

    @Test
    void sessionRoleIsReadFromTheServer_notFromTheRequest() throws Exception {
        // a student cannot talk their way in by sending a staff-looking value anywhere in the request
        assertEquals(List.of(DENIED), run("UPLOAD_RESUME_BATCH", studentToken, batch("TPO", "1", file("ADMIN", "a.txt", b64(resume("Java"))))));
        assertEquals(0, resumes.saves);
    }

    // ------------------------------------------------------------------ Feature 1: export filtered students

    @Test
    void exportStudents_usesTheSameFiltersAsTheStudentTable_andReturnsThoseRowsAsXlsx() throws Exception {
        Student a = students.all.get(1), b = students.all.get(0);
        students.queryResult = List.of(a, b);

        FileReply reply = fileReply(run("EXPORT_STUDENTS", tpo, "cse", "IT", "2027", "7.5", "0", "5", "NAME", "true"));

        // same 8 values, same order, same reading of "0" as SEARCH_SORT_FILTER_STUDENTS in ClientHandler
        assertArrayEquals(new Object[]{"cse", "IT", 2027, 7.5, -1, 5, "NAME", true}, students.lastQuery);
        assertEquals("students_" + LocalDate.now() + ".xlsx", reply.name());
        List<List<String>> rows = XlsxReader.rows(reply.bytes());
        assertEquals(ExportService.STUDENT_COLUMNS, rows.get(0));
        assertEquals(3, rows.size());
        assertEquals(List.of(a.getPrn(), a.getName(), a.getEmail(), a.getDepartment(), String.valueOf(a.getCgpa()),
                String.valueOf(a.getPassingYear()), String.valueOf(a.getBacklogs()), String.valueOf(a.getSemester()),
                a.getPhone(), a.getSkills()), rows.get(1));
        assertEquals(b.getPrn(), rows.get(2).get(0), "the order of the filtered list is kept");
    }

    @Test
    void exportStudents_readsBadOrZeroFilterValuesLikeTheTableDoes() throws Exception {
        fileReply(run("EXPORT_STUDENTS", admin, "", "ALL", "abc", "abc", "x", "", "CGPA", "no"));
        assertArrayEquals(new Object[]{"", "ALL", -1, -1.0, -1, -1, "CGPA", false}, students.lastQuery);

        fileReply(run("EXPORT_STUDENTS", tpc, "", "ALL", "2026", "0", "2", "8", "YEAR", "TRUE"));
        assertArrayEquals(new Object[]{"", "ALL", 2026, 0.0, 2, 8, "YEAR", true}, students.lastQuery);
    }

    @Test
    void exportStudents_neverLeaksThePasswordHash_andNoMatchGivesAHeaderOnlyFile() throws Exception {
        students.queryResult = students.all;
        FileReply all = fileReply(run("EXPORT_STUDENTS", tpo, "", "ALL", "0", "0", "0", "0", "CGPA", "false"));
        for (var part : XlsxReader.parts(all.bytes()).values()) {
            assertFalse(new String(part, StandardCharsets.UTF_8).contains("SECRET-PASSWORD-HASH"));
        }
        assertEquals(21, XlsxReader.rows(all.bytes()).size());

        students.queryResult = List.of();
        assertEquals(1, XlsxReader.rows(fileReply(run("EXPORT_STUDENTS", tpo, "", "ALL", "0", "0", "0", "0", "CGPA", "false")).bytes()).size());
    }

    @Test
    void exportStudents_aTruncatedRequestStillAnswers() throws Exception {
        List<String> reply = run("EXPORT_STUDENTS", tpo, "cse");          // client sent 1 of the 8 values
        assertTrue(reply.get(0).startsWith("SUCCESS|"), reply.get(0));
    }

    // ------------------------------------------------------------------ Feature 3: job applicants

    private Student s(int index) {
        return students.all.get(index - 1);
    }

    private void applicantsForJob1() {
        applications.byJob.put("JOB-1", List.of(
                new ApplicationDAO.Applicant(s(3), 11, "SCREENED", 72.5, "2026-10-01 10:15:00"),
                new ApplicationDAO.Applicant(s(9), 12, "APPLIED", null, "2026-10-02 09:00:00")));
    }

    @Test
    void getJobApplicants_listsOnlyApplicantsWithTheStudentColumnsThenStatusScoreAndDate() throws Exception {
        applicantsForJob1();
        List<String> lines = listBody(run("GET_JOB_APPLICANTS", tpc, "JOB-1"));

        assertEquals(List.of(
                s(3).toProtocolLine() + "|SCREENED|72.5|2026-10-01 10:15:00",
                s(9).toProtocolLine() + "|APPLIED||2026-10-02 09:00:00"), lines);
        assertTrue(lines.get(0).startsWith(s(3).getPrn() + "|" + s(3).getName() + "|"), "starts with the same 10 columns the student table parses");
        assertFalse(String.join("\n", lines).contains(s(4).getName()), "students who did not apply are not listed");
    }

    @Test
    void exportJobApplicants_isTheSameDataAsTheList() throws Exception {
        applicantsForJob1();
        FileReply reply = fileReply(run("EXPORT_JOB_APPLICANTS", admin, "JOB-1"));

        assertEquals("applicants_JOB-1_" + LocalDate.now() + ".xlsx", reply.name());
        List<List<String>> rows = XlsxReader.rows(reply.bytes());
        assertEquals(ExportService.APPLICANT_COLUMNS, rows.get(0));
        assertEquals(3, rows.size());
        assertEquals(List.of(s(3).getPrn(), s(3).getName(), s(3).getEmail(), s(3).getDepartment(), String.valueOf(s(3).getCgpa()),
                String.valueOf(s(3).getPassingYear()), String.valueOf(s(3).getBacklogs()), String.valueOf(s(3).getSemester()),
                s(3).getPhone(), s(3).getSkills(), "SCREENED", "72.5", "2026-10-01 10:15:00"), rows.get(1));
        assertEquals(List.of("APPLIED", "", "2026-10-02 09:00:00"), rows.get(2).subList(10, 13));

        // every row of the list appears in the file, in the same order
        List<String> listPrns = listBody(run("GET_JOB_APPLICANTS", admin, "JOB-1")).stream().map(l -> l.split("\\|")[0]).toList();
        assertEquals(listPrns, rows.subList(1, rows.size()).stream().map(r -> r.get(0)).toList());
    }

    @Test
    void jobApplicants_unknownBlankAndPaddedJobIds() throws Exception {
        applicantsForJob1();
        for (String command : List.of("GET_JOB_APPLICANTS", "EXPORT_JOB_APPLICANTS")) {
            assertEquals(List.of("FAILED|Job not found: NOPE"), run(command, tpo, "NOPE"));
            assertEquals(List.of("FAILED|Job ID is required."), run(command, tpo, "  "));
            assertEquals(List.of("FAILED|Job ID is required."), run(command, tpo));              // nothing sent at all
        }
        assertEquals(2, listBody(run("GET_JOB_APPLICANTS", tpo, "  JOB-1  ")).size(), "surrounding spaces are ignored");
    }

    @Test
    void jobWithNoApplicants_givesAnEmptyListAndAHeaderOnlyFile() throws Exception {
        assertEquals(List.of("SUCCESS", "0", "END"), run("GET_JOB_APPLICANTS", tpo, "JOB-1"));
        assertEquals(1, XlsxReader.rows(fileReply(run("EXPORT_JOB_APPLICANTS", tpo, "JOB-1")).bytes()).size());
    }

    @Test
    void exportJobApplicants_fileNameCannotCarryPathCharacters() throws Exception {
        jobs.jobs.put("../JOB/1 x", Fakes.job("../JOB/1 x", "Acme", "SDE"));
        FileReply reply = fileReply(run("EXPORT_JOB_APPLICANTS", tpo, "../JOB/1 x"));
        assertEquals("applicants_.._JOB_1_x_" + LocalDate.now() + ".xlsx", reply.name());
        assertFalse(reply.name().contains("/") || reply.name().contains("\\"));
    }

    // ------------------------------------------------------------------ Feature 2: batch upload

    @Test
    void batchOf12_answersOneResultPerFileInOrder_withASummary() throws Exception {
        List<String> body = listBody(run("UPLOAD_RESUME_BATCH", tpo, txtFiles(1, 12)));

        String[] header = body.get(0).split("\\|", -1);
        assertEquals("BATCH", header[0]);
        assertFalse(header[1].isBlank(), "batch id");
        assertEquals(List.of("12", "12", "0", "0", ""), List.of(header).subList(2, 7));
        assertEquals(13, body.size());
        for (int i = 1; i <= 12; i++) {
            String[] f = resultLine(body.get(i));
            assertEquals(String.valueOf(i), f[1]);
            assertEquals(String.valueOf(i), f[3]);                          // student id
            assertEquals(String.format("PRN%03d", i), f[4]);
            assertEquals("resume" + i + ".txt", f[6]);
            assertEquals("PROCESSED", f[7]);
            assertTrue(Integer.parseInt(f[8]) > 10, "word count");
            assertFalse(f[9].isEmpty(), "ATS score");
            assertTrue(f[11].contains("java"), "detected skills");
            assertEquals("", f[12], "no job, so no screening columns");
        }
        assertEquals(12, resumes.rows.size());
        assertEquals("resume7.txt", resumes.rows.get(7).fileName());
    }

    @Test
    void batchOf20_isAccepted_and21Declared_isRefused() throws Exception {
        List<String> body = listBody(run("UPLOAD_RESUME_BATCH", admin, txtFiles(1, 20)));
        assertEquals("BATCH", body.get(0).split("\\|")[0]);
        assertEquals(21, body.size());
        assertEquals(20, resumes.rows.size());

        resumes.rows.clear();
        resumes.saves = 0;
        assertEquals(List.of("FAILED|A batch must contain 1 to 20 resumes."), run("UPLOAD_RESUME_BATCH", admin, batch("", "21", file("1", "a.txt", b64(resume("Java"))))));
        assertEquals(0, resumes.saves);
    }

    @Test
    void aRefusedBatch_stillReadsTheFilesTheClientIsSending_soTheReasonReachesIt() throws Exception {
        String[] files = batch("", "21", file("1", "a.txt", b64(resume("Java"))), file("2", "b.txt", b64(resume("Java"))));
        String[] unknownJob = batch("NOPE", "2", file("1", "a.txt", b64(resume("Java"))), file("2", "b.txt", b64(resume("Java"))));
        String[] okRequest = txtFiles(1, 3);
        record Case(String token, String[] lines, String expected) {}
        for (Case c : List.of(
                new Case(studentToken, okRequest, DENIED),
                new Case(director, okRequest, DENIED),
                new Case(tpo, files, "FAILED|A batch must contain 1 to 20 resumes."),
                new Case(tpo, unknownJob, "FAILED|Job not found: NOPE"),
                new Case("stale-token-after-a-server-restart", okRequest, "FAILED|Not authenticated. Please log in again."))) {
            BufferedReader in = new BufferedReader(new StringReader(c.token() + "\n" + String.join("\n", c.lines()) + "\n"));
            StringWriter out = new StringWriter();
            handler.handle("UPLOAD_RESUME_BATCH", in, new PrintWriter(out, true));
            assertEquals(List.of(c.expected()), out.toString().lines().toList());
            assertEquals(-1, in.read(), "nothing may be left unread, or closing the socket would reset the connection: " + c.expected());
        }
        assertEquals(0, resumes.saves);
    }

    @Test
    void batchCount_mustBeANumberFromOneToTwenty() throws Exception {
        for (String count : List.of("0", "-3", "21", "1000", "abc", "", "1.5", " ")) {
            assertEquals(List.of("FAILED|A batch must contain 1 to 20 resumes."),
                    run("UPLOAD_RESUME_BATCH", tpo, batch("", count, file("1", "a.txt", b64(resume("Java"))))), "count=[" + count + "]");
        }
        assertEquals(0, resumes.saves);
    }

    @Test
    void oneBadFileInTheMiddle_failsAloneAndTheNextFilesStillParse() throws Exception {
        String oversize = "A".repeat(MAX_BASE64_CHARS + 1);
        String[] request = batch("", "9",
                file("1", "ok1.txt", b64(resume("Java"))),
                file("2", "notbase64.txt", "%%% not base64 %%%"),
                file("3", "huge.pdf", oversize),                                   // skipped, must not break the lines after it
                file("4", "ok2.txt", b64(resume("SQL"))),
                file("5", "empty.txt", ""),
                file("999", "ghost.txt", b64(resume("Java"))),
                file("6", "bad.exe", b64(resume("Java"))),
                file("7", "ok3.txt", b64(resume("Python"))),
                file("a".repeat(300), "longref.txt", b64(resume("Java"))));

        List<String> body = listBody(run("UPLOAD_RESUME_BATCH", tpc, request));
        List<String> statusAndMessage = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            String[] f = resultLine(body.get(i));
            statusAndMessage.add(f[7] + ": " + f[16]);
        }
        assertEquals(List.of(
                "PROCESSED: ",
                "FAILED: The file could not be decoded.",
                "FAILED: File is too large (maximum 2 MB).",
                "PROCESSED: ",
                "FAILED: File name and file content are required.",
                "FAILED: Student not found: 999",
                "FAILED: Only PDF, DOCX or TXT resumes are accepted.",
                "PROCESSED: ",
                "FAILED: The student reference is too long."), statusAndMessage);
        assertEquals("BATCH", body.get(0).split("\\|")[0]);
        assertEquals(List.of("9", "3", "0", "6"), List.of(body.get(0).split("\\|", -1)).subList(2, 6));
        assertEquals(java.util.Set.of(1, 4, 7), resumes.rows.keySet(), "exactly the three good files were stored");
        assertEquals("", resultLine(body.get(9))[2], "an over-long reference is not echoed back");
    }

    @Test
    void anAbsurdlyLongLine_abandonsTheRequestInsteadOfBufferingIt() throws Exception {
        String[] request = batch("", "2",
                file("1", "ok.txt", b64(resume("Java"))),
                file("2", "huge.txt", "A".repeat(17 * 1024 * 1024)));              // beyond the 16 MB skip limit
        assertEquals(List.of("FAILED|The request is too large."), run("UPLOAD_RESUME_BATCH", tpo, request));
    }

    @Test
    void withAJob_resultsCarryTheScreeningScoreAndNothingIsWrittenToApplications() throws Exception {
        String[] request = batch("JOB-1", "2",
                file("1", "a.txt", b64(resume("Java, SQL"))),
                file("PRN002", "b.txt", b64(resume("Java"))));
        List<String> body = listBody(run("UPLOAD_RESUME_BATCH", tpo, request));

        assertEquals("JOB-1", body.get(0).split("\\|", -1)[6]);
        String[] first = resultLine(body.get(1));
        assertEquals("PROCESSED", first[7]);
        assertFalse(first[12].isEmpty(), "total score");
        assertEquals("26.67", first[13], "skill score: 2 of the 3 required skills");
        assertTrue(first[14].contains("java") && first[14].contains("sql"));
        assertEquals("kubernetes", first[15]);
        assertTrue(Double.parseDouble(resultLine(body.get(2))[12]) < Double.parseDouble(first[12]),
                "one skill scores lower than two: " + body.get(1) + " vs " + body.get(2));
        assertTrue(applications.scores.isEmpty(), "batch upload must not create or change any application");
    }

    @Test
    void unknownJob_refusesTheWholeBatchBeforeStoringAnything() throws Exception {
        assertEquals(List.of("FAILED|Job not found: NOPE"),
                run("UPLOAD_RESUME_BATCH", tpo, batch("NOPE", "1", file("1", "a.txt", b64(resume("Java"))))));
        assertEquals(0, resumes.saves);
    }

    @Test
    void aRequestThatStopsEarly_reportsTheMissingFilesInsteadOfLosingTheBatch() throws Exception {
        // declares 3 files, sends one complete file and then only the reference of the second
        String[] request = {"", "3", "1", "a.txt", b64(resume("Java")), "2"};
        List<String> body = listBody(run("UPLOAD_RESUME_BATCH", tpo, request));

        assertEquals("PROCESSED", resultLine(body.get(1))[7]);
        for (int i = 2; i <= 3; i++) {
            String[] f = resultLine(body.get(i));
            assertEquals("FAILED", f[7]);
            assertEquals("The request ended before this file was received.", f[16]);
        }
        assertEquals(List.of("3", "1", "0", "2"), List.of(body.get(0).split("\\|", -1)).subList(2, 6));
        assertEquals(1, resumes.rows.size());
    }

    @Test
    void getResumeBatch_returnsTheSameAnswerAgain_andUnknownIdsFail() throws Exception {
        List<String> uploaded = listBody(run("UPLOAD_RESUME_BATCH", tpo, txtFiles(1, 10)));
        String batchId = uploaded.get(0).split("\\|")[1];

        assertEquals(uploaded, listBody(run("GET_RESUME_BATCH", tpc, batchId)), "another staff member can look the batch up");
        assertEquals(uploaded, listBody(run("GET_RESUME_BATCH", admin, "  " + batchId + " ")));
        assertTrue(run("GET_RESUME_BATCH", tpo, "no-such-batch").get(0).startsWith("FAILED|Batch not found"));
        assertTrue(run("GET_RESUME_BATCH", tpo, "").get(0).startsWith("FAILED|Batch not found"));
        assertTrue(run("GET_RESUME_BATCH", tpo).get(0).startsWith("FAILED|Batch not found"));
        assertEquals(List.of(DENIED), run("GET_RESUME_BATCH", studentToken, batchId), "students cannot read batch results");
    }

    @Test
    void twoBatchesGetDifferentIds_andEachKeepsItsOwnResults() throws Exception {
        List<String> first = listBody(run("UPLOAD_RESUME_BATCH", tpo, txtFiles(1, 3)));
        List<String> second = listBody(run("UPLOAD_RESUME_BATCH", tpo, txtFiles(4, 5)));
        String id1 = first.get(0).split("\\|")[1], id2 = second.get(0).split("\\|")[1];
        assertNotEquals(id1, id2);
        assertEquals(4, listBody(run("GET_RESUME_BATCH", tpo, id1)).size());
        assertEquals(3, listBody(run("GET_RESUME_BATCH", tpo, id2)).size());
    }

    // ------------------------------------------------------------------ errors

    @Test
    void unexpectedFailures_getAGenericMessage_notAStackTraceOrInternalDetails() throws Exception {
        Fakes.Students broken = new Fakes.Students() {
            @Override public List<Student> query(String s, String d, int y, double c, int b, int sem, String sort, boolean asc) {
                throw new IllegalStateException("secret internal detail");
            }
        };
        ResumeCommandHandler h = new ResumeCommandHandler(broken, jobs, new ResumeScreeningService(resumes, applications, dir), applications);
        StringWriter out = new StringWriter();
        h.handle("EXPORT_STUDENTS", new BufferedReader(new StringReader(tpo + "\n\nALL\n0\n0\n0\n0\nCGPA\ntrue\n")), new PrintWriter(out, true));
        assertEquals("FAILED|Could not complete the request. Please try again.\n", out.toString().replace("\r\n", "\n"));

        Fakes.ApplicationDao dbDown = new Fakes.ApplicationDao() {
            @Override public List<Applicant> findApplicantsByJob(String jobId) throws SQLException { throw new SQLException("jdbc:mysql://secret-host"); }
        };
        ResumeCommandHandler h2 = new ResumeCommandHandler(students, jobs, new ResumeScreeningService(resumes, dbDown, dir), dbDown);
        StringWriter out2 = new StringWriter();
        h2.handle("GET_JOB_APPLICANTS", new BufferedReader(new StringReader(tpo + "\nJOB-1\n")), new PrintWriter(out2, true));
        assertEquals("FAILED|Could not complete the request. Please try again.\n", out2.toString().replace("\r\n", "\n"));
    }

    // ------------------------------------------------------------------ existing commands keep working through the same handler

    @Test
    void existingUploadResume_stillWorksForStudents_andStillRefusesStaff() throws Exception {
        List<String> ok = run("UPLOAD_RESUME", studentToken, "mine.txt", b64(resume("Java, SQL")));
        assertEquals(1, ok.size());
        assertTrue(ok.get(0).startsWith("SUCCESS|READABLE|"), ok.get(0));
        assertEquals("mine.txt", resumes.rows.get(5).fileName(), "stored under the logged-in student, not a client-supplied id");

        assertEquals(List.of("FAILED|Only students can upload a resume."), run("UPLOAD_RESUME", tpo, "x.txt", b64(resume("Java"))));
        assertEquals(1, resumes.rows.size());
    }

    @Test
    void existingGetAtsFeedback_stillWorksForStudents() throws Exception {
        run("UPLOAD_RESUME", studentToken, "mine.txt", b64(resume("Java, SQL")));
        List<String> body = listBody(run("GET_ATS_FEEDBACK", studentToken));
        assertTrue(body.get(0).startsWith("SCORE|"));
        assertEquals(List.of("FAILED|ATS feedback is available to students for their own resume."), run("GET_ATS_FEEDBACK", tpo));
    }
}
