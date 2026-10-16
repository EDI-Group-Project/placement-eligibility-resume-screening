package com.placement.sockets;

import com.placement.services.resume.ResumeScreeningService;
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
import java.util.Base64;
import java.util.List;
import java.util.Set;

import static com.placement.testsupport.TestSupport.student;
import static org.junit.jupiter.api.Assertions.*;

class ResumeCommandHandlerTest {

    private static final String RESUME = "Skills\nJava, SQL\nProjects\nPortal\nExperience\nIntern at Acme\n";

    private final FakeStudentService students = new FakeStudentService();
    private final FakeResumeDAO resumes = new FakeResumeDAO();
    private ResumeCommandHandler handler;

    private final String studentToken = SessionManager.createSession("STUDENT", "Asha", 1);
    private final String tpo = SessionManager.createSession("TPO", "Officer", 100);
    private final String parent = SessionManager.createSession("PARENT", "X", 9);

    private String call(String command, String... lines) throws Exception {
        if (handler == null) {
            students.students.add(student(1, "Asha"));
            students.students.add(student(2, "Bhavesh"));
            students.students.add(student(3, "Charu"));
            students.eligibleIds.addAll(Set.of(1, 2));
            resumes.put(1, RESUME);
            resumes.put(2, "Skills\nJava\n");
            FakeJobService jobs = new FakeJobService().with(TestSupport.job("J1", "SDE", "java", "sql"));
            handler = new ResumeCommandHandler(students, jobs,
                    new ResumeScreeningService(resumes, new FakeApplicationDAO(), Files.createTempDirectory("rh")));
        }
        StringWriter out = new StringWriter();
        handler.handle(command, new BufferedReader(new StringReader(String.join("\n", lines) + "\n")), new PrintWriter(out, true));
        return out.toString().replace("\r\n", "\n").trim();
    }

    private static String b64(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void forgedTokensAndUnknownRolesAreRefusedOnEveryCommand() throws Exception {
        for (String cmd : List.of("UPLOAD_RESUME", "GET_ATS_FEEDBACK", "SCREEN_STUDENT", "RANK_CANDIDATES")) {
            assertEquals("FAILED|Not authenticated. Please log in again.", call(cmd, "forged", "J1", "1"), cmd);
            assertEquals("FAILED|Access denied.", call(cmd, parent, "J1", "1"), cmd);
        }
    }

    @Test
    void upload_studentUploadsOwnResume_staffCannotUseTheSingleUpload() throws Exception {
        String ok = call("UPLOAD_RESUME", studentToken, "My Resume.txt", b64(RESUME));
        assertTrue(ok.startsWith("SUCCESS|READABLE|"), ok);
        assertTrue(resumes.rows.containsKey(1));                                  // stored for the SESSION's student
        assertTrue(call("UPLOAD_RESUME", tpo, "r.txt", b64(RESUME)).startsWith("FAILED|Only students"));
        assertEquals("FAILED|The file could not be decoded.", call("UPLOAD_RESUME", studentToken, "r.txt", "%%%"));
        assertTrue(call("UPLOAD_RESUME", studentToken, "r.exe", b64("MZ")).startsWith("FAILED|Only PDF, DOCX or TXT"));
        assertEquals("FAILED|The request is too large.", call("UPLOAD_RESUME", studentToken, "r.txt", "A".repeat(3_000_000)));
    }

    @Test
    void atsFeedback_isForStudentsOwnResumeOnly() throws Exception {
        String r = call("GET_ATS_FEEDBACK", studentToken);
        assertTrue(r.startsWith("SUCCESS\n"));
        assertTrue(r.contains("SCORE|"));
        assertTrue(call("GET_ATS_FEEDBACK", tpo).startsWith("FAILED|ATS feedback is available to students"));
    }

    @Test
    void screenStudent_aStudentCanOnlyScreenThemselves_staffCanScreenAnyone() throws Exception {
        String own = call("SCREEN_STUDENT", studentToken, "J1", "3");             // asks for student 3 ...
        assertTrue(own.startsWith("SUCCESS\n"), own);
        assertEquals(1, students.lastLookup);                                      // ... but the session's student (1) is used

        assertTrue(call("SCREEN_STUDENT", tpo, "J1", "2").startsWith("SUCCESS\n"));
        assertEquals(2, students.lastLookup);                                      // staff may choose

        assertEquals("FAILED|Invalid student ID.", call("SCREEN_STUDENT", tpo, "J1", "abc"));
        assertEquals("FAILED|Student not found.", call("SCREEN_STUDENT", tpo, "J1", "77"));
        assertTrue(call("SCREEN_STUDENT", tpo, "NOPE", "2").startsWith("FAILED|Job not found"));
        assertEquals("FAILED|Job ID is required.", call("SCREEN_STUDENT", tpo, "", "2"));
    }

    @Test
    void rank_isStaffOnly_andBestScoreFirst() throws Exception {
        assertTrue(call("RANK_CANDIDATES", studentToken, "J1").startsWith("FAILED|Access denied. Only TPO"));
        String r = call("RANK_CANDIDATES", tpo, "J1");
        String[] lines = r.split("\n");
        assertEquals("SUCCESS", lines[0]);
        assertEquals("2", lines[1]);                                               // the 2 eligible students
        assertTrue(lines[2].startsWith("1|Asha|"));                                // both skills beats one skill
        assertTrue(lines[3].startsWith("2|Bhavesh|"));
    }
}
