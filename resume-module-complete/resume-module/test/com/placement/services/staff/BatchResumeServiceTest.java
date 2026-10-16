package com.placement.services.staff;

import com.placement.services.resume.ResumeScreeningService;
import com.placement.testsupport.TestSupport;
import com.placement.testsupport.TestSupport.FakeApplicationDAO;
import com.placement.testsupport.TestSupport.FakeJobService;
import com.placement.testsupport.TestSupport.FakeResumeDAO;
import com.placement.testsupport.TestSupport.FakeStudentService;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.placement.testsupport.TestSupport.student;
import static org.junit.jupiter.api.Assertions.*;

class BatchResumeServiceTest {

    private static final String RESUME = "Skills\nJava, SQL\nProjects\nPortal\nExperience\nIntern at Acme\n";

    private final FakeStudentService students = new FakeStudentService();
    private final FakeResumeDAO resumes = new FakeResumeDAO();
    private final FakeApplicationDAO applications = new FakeApplicationDAO();
    private BatchResumeService batches;

    /** 8 students: PRN B24CE1001..1008, e-mail s<n>@college.edu; ids 1..8; 1-3 eligible. */
    private BatchResumeService batches() throws Exception {
        if (batches != null) return batches;
        for (int i = 1; i <= 8; i++) {
            students.students.add(student(i, "Student" + i, "B24CE100" + i, "s" + i + "@college.edu", "Computer", 8.0, 0, "9000000000"));
        }
        students.eligibleIds.addAll(Set.of(1, 2, 3));
        FakeJobService jobs = new FakeJobService().with(TestSupport.job("J1", "SDE", "java", "sql"));
        ResumeScreeningService rs = new ResumeScreeningService(resumes, applications, Files.createTempDirectory("batch"));
        batches = new BatchResumeService(rs, students, jobs);
        return batches;
    }

    private static byte[] txt(String s) { return s.getBytes(StandardCharsets.UTF_8); }

    /** A PDF that passes the signature check but contains no extractable text (like a scanned resume). */
    private static byte[] scannedPdf() {
        return "%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\n%%EOF".getBytes(StandardCharsets.ISO_8859_1);
    }

    private BatchResumeService.BatchView await(String id) throws Exception {
        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
            BatchResumeService.BatchView v = batches().status(id);
            if (v.complete()) return v;
            Thread.sleep(20);
        }
        fail("batch did not finish in time");
        return null;
    }

    private static BatchResumeService.ItemView item(BatchResumeService.BatchView v, int index) {
        return v.items().get(index - 1);
    }

    @Test
    void mixedBatch_everyFileGetsItsOwnResult_andFailuresDoNotStopTheRest() throws Exception {
        BatchResumeService s = batches();
        resumes.put(1, "Skills\nOld\n");                          // student 1 already has a resume -> will be replaced
        resumes.failSaveFor.add(8);                               // the database will fail for student 8 only
        String batch = s.startBatch(7, "J1", 12);

        s.addFile(batch, 7, "B24CE1001.txt", txt(RESUME));                    // 1 valid, eligible, replaces old resume
        s.addFile(batch, 7, "B24CE1002_Student2.txt", txt(RESUME));           // 2 valid, eligible (name has extra token)
        s.addFile(batch, 7, "s4@college.edu.txt", txt("Skills\nJava\n"));     // 3 valid, matched by e-mail, not eligible
        s.addFile(batch, 7, "B24CE1005.txt", txt(RESUME));                    // 4 valid
        s.addFile(batch, 7, "B24CE1006.txt", txt(RESUME));                    // 5 valid
        s.addFile(batch, 7, "B24CE9999.pdf", txt("x"));                       // 6 FAIL: no such student
        s.addFile(batch, 7, "B24CE1003.exe", txt("MZ binary"));               // 7 FAIL: unsupported type
        s.addFile(batch, 7, "B24CE1007.pdf", txt("this is plain text not a pdf")); // 8 FAIL: content does not match .pdf
        s.addFile(batch, 7, "B24CE1003.pdf", scannedPdf());                   // 9 stored but UNREADABLE (still DONE)
        s.addFile(batch, 7, "B24CE1004.txt", new byte[0]);                    // 10 FAIL: empty file
        s.addFile(batch, 7, "B24CE1008.txt", txt(RESUME));                    // 11 FAIL: database error for this student
        s.addFile(batch, 7, "Student 3 resume.txt", txt(RESUME));             // 12 FAIL: nothing identifies the student

        BatchResumeService.BatchView v = await(batch);
        assertEquals(12, v.added());
        assertEquals(12, v.items().size());
        assertEquals(0, v.inProgress());
        assertEquals(6, v.done());                                // 1,2,3,4,5 parsed + 9 unreadable
        assertEquals(6, v.failed());                              // 6,7,8,10,11,12

        // --- valid files: parsed, scored for THIS job, eligibility for THIS job
        BatchResumeService.ItemView first = item(v, 1);
        assertEquals(BatchResumeService.ItemState.DONE, first.state());
        assertEquals("PARSED", first.parseStatus());
        assertEquals("B24CE1001", first.studentPrn());
        assertEquals("Student1", first.studentName());
        assertEquals(EligibilityProvider.ELIGIBLE_LABEL, first.eligibility());
        assertTrue(first.matchScore() > 40.0);                    // both required skills + project + experience
        assertTrue(first.message().contains("Replaced"));         // told that the old resume was replaced
        assertEquals("PARSED", item(v, 2).parseStatus());
        assertEquals("B24CE1002", item(v, 2).studentPrn());

        BatchResumeService.ItemView byEmail = item(v, 3);
        assertEquals("B24CE1004", byEmail.studentPrn());
        assertEquals(EligibilityProvider.NOT_ELIGIBLE_LABEL, byEmail.eligibility());
        assertEquals(20.0, byEmail.matchScore(), 0.001);

        // --- each kind of bad file fails ALONE with its own clear reason
        assertEquals(BatchResumeService.ItemState.FAILED, item(v, 6).state());
        assertTrue(item(v, 6).message().contains("PRN or e-mail"));
        assertTrue(item(v, 7).message().contains("Only PDF, DOCX or TXT"));
        assertTrue(item(v, 8).message().contains("does not match"));
        assertEquals("UNREADABLE", item(v, 9).parseStatus());
        assertEquals(BatchResumeService.ItemState.DONE, item(v, 9).state());
        assertNull(item(v, 9).matchScore());
        assertTrue(item(v, 10).message().contains("empty"));
        assertTrue(item(v, 11).message().contains("Processing failed"));        // database error: generic, no internals
        assertFalse(item(v, 11).message().contains("simulated"));
        assertTrue(item(v, 12).message().contains("PRN or e-mail"));

        // --- the good files really were stored; the failed ones were not
        assertTrue(resumes.rows.containsKey(2));
        assertFalse(resumes.rows.containsKey(7));
        assertFalse(resumes.rows.containsKey(8));
        batches().shutdown();
    }

    @Test
    void twentyFilesAreAllProcessed() throws Exception {
        BatchResumeService s = batches();
        for (int i = 9; i <= 28; i++) {                            // 20 more students
            students.students.add(student(i, "Extra" + i, "X" + i, "x" + i + "@college.edu", "IT", 7.0, 0, "9000000000"));
        }
        String batch = s.startBatch(1, "J1", BatchResumeService.MAX_FILES_PER_BATCH);
        for (int i = 9; i <= 28; i++) s.addFile(batch, 1, "X" + i + ".txt", txt(RESUME));
        BatchResumeService.BatchView v = await(batch);
        assertEquals(20, v.done());
        assertEquals(0, v.failed());
        assertEquals(20, v.items().stream().map(BatchResumeService.ItemView::studentPrn).collect(Collectors.toSet()).size());
        s.shutdown();
    }

    @Test
    void batchSizeLimits() throws Exception {
        BatchResumeService s = batches();
        assertThrows(IllegalArgumentException.class, () -> s.startBatch(1, "J1", 0));
        assertThrows(IllegalArgumentException.class, () -> s.startBatch(1, "J1", 21));
        String batch = s.startBatch(1, "J1", 2);
        s.addFile(batch, 1, "B24CE1001.txt", txt(RESUME));
        s.addFile(batch, 1, "B24CE1002.txt", txt(RESUME));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> s.addFile(batch, 1, "B24CE1003.txt", txt(RESUME))).getMessage().contains("full"));
        s.shutdown();
    }

    @Test
    void unknownJobUnknownBatchAndForeignOwnerAreRejected() throws Exception {
        BatchResumeService s = batches();
        assertTrue(assertThrows(IllegalArgumentException.class, () -> s.startBatch(1, "NOPE", 3)).getMessage().contains("Job not found"));
        assertThrows(IllegalArgumentException.class, () -> s.startBatch(1, " ", 3));
        assertThrows(IllegalArgumentException.class, () -> s.status("no-such-batch"));
        assertThrows(IllegalArgumentException.class, () -> s.addFile("no-such-batch", 1, "a.txt", txt("x")));
        String batch = s.startBatch(1, "J1", 3);
        assertTrue(assertThrows(IllegalArgumentException.class, () -> s.addFile(batch, 2, "B24CE1001.txt", txt(RESUME))).getMessage().contains("another user"));
        s.shutdown();
    }

    @Test
    void screeningAFileNeverCreatesAnApplication_butUpdatesTheScoreOfAnExistingOne() throws Exception {
        BatchResumeService s = batches();
        applications.addApplication("J1", 2, "APPLIED", null, new Timestamp(0));      // only student 2 has applied
        String batch = s.startBatch(1, "J1", 2);
        s.addFile(batch, 1, "B24CE1001.txt", txt(RESUME));                             // student 1 did NOT apply
        s.addFile(batch, 1, "B24CE1002.txt", txt(RESUME));                             // student 2 did
        await(batch);
        assertEquals(1, applications.records.size());                                  // no phantom applicant was created
        assertNotNull(applications.records.get(0).screeningScore());                   // applicant's score was stored
        s.shutdown();
    }

    @Test
    void ambiguousPrnMatchesMoreThanOneStudentAndIsRefused() throws Exception {
        students.students.add(student(21, "Twin A", "SAME", "a@c.edu", "IT", 7.0, 0, "9"));
        students.students.add(student(22, "Twin B", "SAME", "b@c.edu", "IT", 7.0, 0, "9"));
        BatchResumeService s = batches();
        String batch = s.startBatch(1, "J1", 2);
        s.addFile(batch, 1, "SAME.txt", txt(RESUME));
        s.addFile(batch, 1, "B24CE1001.txt", txt(RESUME));
        BatchResumeService.BatchView v = await(batch);
        assertTrue(item(v, 1).message().contains("more than one student"));
        assertEquals(BatchResumeService.ItemState.DONE, item(v, 2).state());           // the unambiguous file is unaffected
        s.shutdown();
    }

    @Test
    void fileNamesAreShownSafely() throws Exception {
        BatchResumeService s = batches();
        String batch = s.startBatch(1, "J1", 1);
        s.addFile(batch, 1, "..\\..\\evil|name\nline.exe", txt("x"));
        BatchResumeService.BatchView v = await(batch);
        assertFalse(item(v, 1).fileName().contains("|"));
        assertFalse(item(v, 1).fileName().contains("\n"));
        assertFalse(item(v, 1).fileName().contains(".."));
        s.shutdown();
    }

    @Test
    void statusCountsAddUp() throws Exception {
        BatchResumeService s = batches();
        String batch = s.startBatch(1, "J1", 3);
        s.addFile(batch, 1, "B24CE1001.txt", txt(RESUME));
        s.addFile(batch, 1, "nobody.txt", txt(RESUME));
        BatchResumeService.BatchView partial = s.status(batch);
        assertEquals(3, partial.expected());
        assertEquals(2, partial.added());
        assertFalse(partial.complete());                                               // 1 more file still expected
        s.addFile(batch, 1, "B24CE1002.txt", txt(RESUME));
        BatchResumeService.BatchView v = await(batch);
        assertEquals(v.added(), v.done() + v.failed() + v.inProgress());
        s.shutdown();
    }
}
