package com.placement.services.resume;

import com.placement.models.JobPosting;
import com.placement.models.Student;
import com.placement.testsupport.Fakes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/** Batch resume upload: every file is processed on its own and reports its own status and analysis. No MySQL. */
class ResumeBatchServiceTest {

    private Fakes.ResumeDao resumes;
    private Fakes.ApplicationDao applications;
    private Path dir;
    private ResumeBatchService batches;
    private List<Student> roster;

    @BeforeEach
    void setUp() throws Exception {
        resumes = new Fakes.ResumeDao();
        applications = new Fakes.ApplicationDao();
        dir = Files.createTempDirectory("batch-test");
        batches = new ResumeBatchService(new ResumeScreeningService(resumes, applications, dir));
        roster = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            roster.add(Fakes.student(i, String.format("PRN%03d", i), "Student " + i, "s" + i + "@college.edu",
                    "CSE", 8.0, 2027, 0, 5, "98765432" + String.format("%02d", i), ""));      // no profile skills: only the resume counts
        }
    }

    // ---------------------------------------------------------------- sample files

    private static byte[] txt(String skills) {
        return resumeText(skills).getBytes(StandardCharsets.UTF_8);
    }

    private static String resumeText(String skills) {
        return "Asha Rao\nasha@college.edu 98765 43210\nSkills\n" + skills + "\nProjects\nPlacement Portal\n- Developed a socket server\n"
                + "Experience\nIntern at Acme\nCertifications\nAWS Cloud Practitioner\n";
    }

    private static byte[] pdf(String skills) throws Exception {
        List<MinimalPdfWriter.Line> lines = new ArrayList<>();
        for (String line : resumeText(skills).split("\n")) lines.add(MinimalPdfWriter.Line.of(line));
        return MinimalPdfWriter.write(lines);
    }

    private static byte[] docx(String skills) throws Exception {
        StringBuilder body = new StringBuilder();
        for (String line : resumeText(skills).split("\n")) body.append("<w:p><w:r><w:t>").append(line).append("</w:t></w:r></w:p>");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write(("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>"
                    + body + "</w:body></w:document>").getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return out.toByteArray();
    }

    private static JobPosting job() {
        return Fakes.job("JOB-1", "Acme", "SDE", "java", "sql", "kubernetes");
    }

    private static String[] fields(ResumeBatchService.Item item) {
        return item.toProtocolLine().split("\\|", -1);
    }

    // ---------------------------------------------------------------- the batch itself

    @Test
    void fifteenMixedFiles_areAllStoredAndAnalysedIndependently() throws Exception {
        ResumeBatchService.Batch batch = batches.start(roster, null);
        for (int i = 1; i <= 15; i++) {
            byte[] bytes = switch (i % 3) { case 0 -> pdf("Java, SQL"); case 1 -> docx("Java, SQL"); default -> txt("Java, SQL"); };
            String name = "resume" + i + switch (i % 3) { case 0 -> ".pdf"; case 1 -> ".docx"; default -> ".txt"; };
            ResumeBatchService.Item item = batch.add(String.valueOf(i), name, bytes, null);
            assertEquals(ResumeBatchService.PROCESSED, item.status(), "file " + i + ": " + item.message());
            assertEquals(i, item.index());
            assertEquals(i, item.student().getStudentId());
            assertNotNull(item.ats(), "every processed resume gets ATS feedback");
            assertTrue(item.resume().getWordCount() > 10);
            assertTrue(item.resume().getExtractedSkills().contains("java"));
            assertNull(item.screening(), "no job given, so no job screening");
        }

        assertEquals(15, resumes.rows.size());
        assertEquals(15, batch.items().size());
        assertEquals(15, batch.count(ResumeBatchService.PROCESSED));
        assertEquals("BATCH|id-1|15|15|0|0|", batch.lines("id-1").get(0));
        assertEquals(16, batch.lines("id-1").size());
        try (var files = Files.list(dir)) { assertEquals(15L, files.count()); }
    }

    @Test
    void oneBadFileNeverStopsTheOthers() throws Exception {
        ResumeBatchService.Batch batch = batches.start(roster, null);
        var ok1 = batch.add("1", "a.txt", txt("Java"), null);
        var wrongType = batch.add("2", "virus.exe", txt("Java"), null);
        var unknownStudent = batch.add("999", "b.txt", txt("Java"), null);
        var unreadFromRequest = batch.add("3", "huge.pdf", null, "File is too large (maximum 2 MB).");
        var duplicate = batch.add("1", "again.txt", txt("SQL"), null);
        var fakePdf = batch.add("4", "fake.pdf", txt("Java"), null);                    // text renamed to .pdf
        var blankRef = batch.add("  ", "c.txt", txt("Java"), null);
        var ok2 = batch.add("PRN005", "d.txt", txt("Java"), null);

        assertEquals(ResumeBatchService.PROCESSED, ok1.status());
        assertEquals(ResumeBatchService.FAILED, wrongType.status());
        assertEquals("Only PDF, DOCX or TXT resumes are accepted.", wrongType.message());
        assertEquals(ResumeBatchService.FAILED, unknownStudent.status());
        assertEquals("Student not found: 999", unknownStudent.message());
        assertEquals(ResumeBatchService.FAILED, unreadFromRequest.status());
        assertEquals("File is too large (maximum 2 MB).", unreadFromRequest.message());
        assertEquals(ResumeBatchService.FAILED, duplicate.status());
        assertTrue(duplicate.message().contains("already has a resume in this batch"));
        assertEquals(ResumeBatchService.FAILED, fakePdf.status());
        assertEquals("The file content does not match its .pdf extension.", fakePdf.message());
        assertEquals(ResumeBatchService.FAILED, blankRef.status());
        assertEquals("Student ID, PRN or email is required.", blankRef.message());
        assertEquals(ResumeBatchService.PROCESSED, ok2.status());
        assertEquals(5, ok2.student().getStudentId());

        assertEquals(2, batch.count(ResumeBatchService.PROCESSED));
        assertEquals(6, batch.count(ResumeBatchService.FAILED));
        assertEquals(java.util.Set.of(1, 5), resumes.rows.keySet(), "only the two good files were stored");
        assertNull(wrongType.resume());
        assertNull(wrongType.ats());
        assertEquals("BATCH|b|8|2|0|6|", batch.lines("b").get(0));
        try (var files = Files.list(dir)) { assertEquals(2L, files.count(), "failed files leave nothing on disk"); }
        // the duplicate did not overwrite the first upload for that student
        assertTrue(resumes.rows.get(1).extractedText().contains("Java"));
        assertEquals("a.txt", resumes.rows.get(1).fileName());
    }

    @Test
    void scannedOrEmptyFile_isStoredButFlaggedUnreadable() {
        ResumeBatchService.Batch batch = batches.start(roster, job());
        ResumeBatchService.Item item = batch.add("7", "scan.txt", "   \n\n  \n".getBytes(StandardCharsets.UTF_8), null);

        assertEquals(ResumeBatchService.UNREADABLE, item.status());
        assertTrue(item.message().startsWith("No readable text found"));
        assertTrue(resumes.rows.containsKey(7), "the file is still stored, as with a single upload");
        assertNull(item.screening(), "nothing to screen");
        assertEquals(1, batch.count(ResumeBatchService.UNREADABLE));
        assertEquals("BATCH|b|1|0|1|0|JOB-1", batch.lines("b").get(0));
    }

    @Test
    void withAJob_eachResumeIsScreenedAndNothingIsWrittenToApplications() throws Exception {
        ResumeBatchService.Batch batch = batches.start(roster, job());
        ResumeBatchService.Item item = batch.add("2", "r.txt", txt("Java, SQL"), null);

        assertEquals(ResumeBatchService.PROCESSED, item.status());
        assertEquals(26.67, item.screening().getSkillScore(), 0.001);                  // 2 of the 3 required skills
        assertTrue(item.screening().getMissingSkills().contains("kubernetes"));
        assertTrue(applications.scores.isEmpty(), "batch screening is read-only: it must not create or change applications");

        String[] f = fields(item);
        assertEquals(17, f.length);
        assertEquals("RESULT", f[0]);
        assertEquals("1", f[1]);                                                       // position in the batch
        assertEquals("2", f[3]);                                                       // student id
        assertEquals("PRN002", f[4]);
        assertEquals("r.txt", f[6]);
        assertEquals("PROCESSED", f[7]);
        assertFalse(f[9].isEmpty());                                                   // ATS score
        assertEquals("26.67", f[13]);                                                  // skill score
        assertTrue(f[14].contains("java") && f[14].contains("sql"));                   // matched
        assertEquals("kubernetes", f[15]);                                             // missing
    }

    @Test
    void studentCanBeNamedByIdPrnOrEmail_andIdWinsOverPrn() {
        assertEquals(7, ResumeBatchService.resolve(roster, "7").getStudentId());
        assertEquals(7, ResumeBatchService.resolve(roster, " 7 ").getStudentId());
        assertEquals(7, ResumeBatchService.resolve(roster, "prn007").getStudentId());
        assertEquals(7, ResumeBatchService.resolve(roster, "S7@COLLEGE.EDU").getStudentId());
        assertNull(ResumeBatchService.resolve(roster, "999"));
        assertNull(ResumeBatchService.resolve(roster, "nobody"));
        assertNull(ResumeBatchService.resolve(roster, ""));
        assertNull(ResumeBatchService.resolve(roster, null));
        assertNull(ResumeBatchService.resolve(null, "7"));

        // a PRN that looks like a number is found when no student has that id; an id still wins when both exist
        Student numericPrn = Fakes.student(500, "3", "Numeric PRN", "np@college.edu", "IT", 7, 2027, 0, 3, "1", "");
        roster.add(numericPrn);
        assertEquals(3, ResumeBatchService.resolve(roster, "3").getStudentId(), "student id 3 beats the student whose PRN is '3'");
        roster.removeIf(s -> s.getStudentId() == 3);
        assertEquals(500, ResumeBatchService.resolve(roster, "3").getStudentId());
    }

    @Test
    void reuploadingForAStudentWithAResume_saysItReplacedIt() throws Exception {
        new ResumeScreeningService(resumes, applications, dir).uploadResume(4, "old.txt", txt("Java"));
        Thread.sleep(5);        // stored file names carry the millisecond, as in ResumePipelineTest
        ResumeBatchService.Item item = batches.start(roster, null).add("4", "new.txt", txt("SQL"), null);

        assertEquals(ResumeBatchService.PROCESSED, item.status());
        assertEquals("Replaced the previous resume.", item.message());
        assertEquals("new.txt", resumes.rows.get(4).fileName());
        try (var files = Files.list(dir)) { assertEquals(1L, files.count(), "the old file is removed"); }
    }

    @Test
    void databaseErrorOnOneFile_failsOnlyThatFileAndLeavesNoOrphanFile() throws Exception {
        resumes.failFor.add(3);
        ResumeBatchService.Batch batch = batches.start(roster, null);
        for (int i = 1; i <= 5; i++) batch.add(String.valueOf(i), "r" + i + ".txt", txt("Java"), null);

        List<String> statuses = batch.items().stream().map(ResumeBatchService.Item::status).toList();
        assertEquals(List.of("PROCESSED", "PROCESSED", "FAILED", "PROCESSED", "PROCESSED"), statuses);
        assertEquals("Could not store this resume. Please try again.", batch.items().get(2).message());
        assertFalse(batch.items().get(2).message().contains("db down"), "internal error text must not reach the client");
        try (var files = Files.list(dir)) { assertEquals(4L, files.count()); }
    }

    @Test
    void analysisFailingAfterAGoodStore_isNotReportedAsAFailedUpload() {
        Fakes.ResumeDao broken = new Fakes.ResumeDao() {
            @Override public synchronized ResumeRecord findByStudentId(int studentId) {
                if (rows.containsKey(studentId)) throw new RuntimeException("read failed");     // only after the save
                return null;
            }
        };
        ResumeBatchService local = new ResumeBatchService(new ResumeScreeningService(broken, applications, dir));
        ResumeBatchService.Item item = local.start(roster, null).add("1", "r.txt", txt("Java"), null);

        assertEquals(ResumeBatchService.PROCESSED, item.status(), "the resume IS stored");
        assertTrue(broken.rows.containsKey(1));
        assertEquals("The analysis could not be completed.", item.message());
        assertNull(item.ats());
        assertEquals("", fields(item)[9]);
    }

    // ---------------------------------------------------------------- protocol line

    @Test
    void protocolLine_hasAFixedFieldCountAndNoSeparatorsOrLineBreaksInsideFields() {
        Student s = Fakes.student(1, "PRN|001", "Na|me", "a@b.c", "CSE", 8, 2027, 0, 5, "1", "");
        var item = new ResumeBatchService.Item(3, "ref|1", s, "a|b.pdf", ResumeBatchService.FAILED,
                "line one\nline two\r|pipe", null, null, null);
        String line = item.toProtocolLine();

        assertFalse(line.contains("\n") || line.contains("\r"));
        String[] f = line.split("\\|", -1);
        assertEquals(17, f.length);
        assertEquals("ref/1", f[2]);
        assertEquals("PRN/001", f[4]);
        assertEquals("Na/me", f[5]);
        assertEquals("a/b.pdf", f[6]);
        assertEquals("line one line two /pipe", f[16]);
    }

    // ---------------------------------------------------------------- recent batches

    @Test
    void recentBatches_canBeLookedUpAgainAndOnlyTheLatest50AreKept() {
        List<String> first = List.of("BATCH|first|1|1|0|0|", "RESULT|1");
        ResumeBatchService.remember("first-id", first);
        assertEquals(first, ResumeBatchService.recall("first-id"));
        assertNull(ResumeBatchService.recall("never-existed"));
        assertNull(ResumeBatchService.recall(null));

        for (int i = 0; i < 60; i++) ResumeBatchService.remember("many-" + i, List.of("BATCH|" + i));
        assertNull(ResumeBatchService.recall("first-id"), "pushed out by newer batches");
        assertNull(ResumeBatchService.recall("many-0"));
        assertEquals(List.of("BATCH|59"), ResumeBatchService.recall("many-59"));
        assertEquals(List.of("BATCH|10"), ResumeBatchService.recall("many-10"));       // 60 stored, last 50 kept: many-10 .. many-59

        assertNotEquals(ResumeBatchService.newBatchId(), ResumeBatchService.newBatchId());
    }
}
