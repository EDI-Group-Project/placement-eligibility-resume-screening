package com.placement.services.resume;

import com.placement.database.ApplicationDAO;
import com.placement.database.ResumeDAO;
import com.placement.models.JobPosting;
import com.placement.models.Student;
import com.placement.models.resume.RankedCandidate;
import com.placement.models.resume.ResumeData;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for the resume pipeline: extractors, parser, skill matching and the
 * ResumeScreeningService flow. Pure in-memory - no MySQL (fake DAOs), no external files.
 */
class ResumePipelineTest {

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private static byte[] deflate(String s) {
        Deflater d = new Deflater();
        d.setInput(s.getBytes(StandardCharsets.ISO_8859_1));
        d.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        while (!d.finished()) out.write(buf, 0, d.deflate(buf));
        return out.toByteArray();
    }

    private static byte[] pdfWithFlateStream(String content) throws Exception {
        byte[] c = deflate(content);
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.write(("%PDF-1.4\n2 0 obj\n<< /Length " + c.length + " /Filter /FlateDecode >>\nstream\n").getBytes(StandardCharsets.ISO_8859_1));
        o.write(c);
        o.write("\nendstream\nendobj\n%%EOF".getBytes(StandardCharsets.ISO_8859_1));
        return o.toByteArray();
    }

    private static Student student(int id, String name, String skills) {
        Student s = new Student();
        s.setStudentId(id);
        s.setName(name);
        s.setSkills(skills);
        return s;
    }

    private static JobPosting job(String... required) {
        JobPosting j = new JobPosting();
        j.setId("JOB-1");
        j.setCompany("Acme");
        j.setRole("SDE");
        j.setRequiredSkills(List.of(required));
        return j;
    }

    private static final String SAMPLE_RESUME =
            "Aarav Shah\naarav@college.edu 98765 43210\nSkills\nJava, SQL, Docker\nProjects\nPlacement Portal\n"
                    + "- Developed a socket server\nResume Screener\n- Built TF-IDF scoring\nExperience\nIntern at Acme\n"
                    + "Certifications\nAWS Cloud Practitioner\n";

    // ---------------------------------------------------------------
    // PdfTextExtractor
    // ---------------------------------------------------------------

    @Test
    void pdf_kerningSplitsAreJoinedAndTmLinesAreSeparated() throws Exception {
        String content = "BT /F1 11 Tf 1 0 0 1 72 720 Tm [(Man)-4(as)-5(wi)-3(n)] TJ ET\n"
                + "BT /F1 11 Tf 1 0 0 1 72 700 Tm [(S)-5(kills)] TJ ET\n"
                + "BT /F1 11 Tf 1 0 0 1 72 686 Tm [(J)5(ava)-250(and)-250(SQL)] TJ ET\n";
        String text = PdfTextExtractor.extract(pdfWithFlateStream(content));
        assertEquals("Manaswin\nSkills\nJava and SQL", text);
    }

    @Test
    void pdf_octalBulletAndEscapedParenthesesAreDecoded() throws Exception {
        String text = PdfTextExtractor.extract(pdfWithFlateStream("BT 1 0 0 1 72 700 Tm (\\225 Built \\(socket\\) app) Tj ET"));
        assertEquals("\u2022 Built (socket) app", text);
    }

    @Test
    void pdf_uncompressedStreamAfterCompressedOneIsNotLost() throws Exception {
        String raw = "BT 1 0 0 1 72 680 Tm (Second stream) Tj ET";
        byte[] first = pdfWithFlateStream("BT 1 0 0 1 72 700 Tm (First stream) Tj ET");
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.write(first);
        o.write(("\n3 0 obj\n<< /Length " + raw.length() + " >>\nstream\n" + raw + "\nendstream\nendobj\n").getBytes(StandardCharsets.ISO_8859_1));
        String text = PdfTextExtractor.extract(o.toByteArray());
        assertTrue(text.contains("First stream"));
        assertTrue(text.contains("Second stream"));
    }

    @Test
    void pdf_fontBinaryAndActualTextAreNotExtracted() throws Exception {
        String font = "junk (GARBAGE FROM FONT) junk";
        String page = "/Span <</ActualText (Skills)>> BDC BT 1 0 0 1 72 700 Tm (Skills) Tj ET EMC";
        String pdf = "%PDF-1.4\n2 0 obj\n<< /Length1 9 /Length " + font.length() + " >>\nstream\n" + font + "\nendstream\nendobj\n"
                + "3 0 obj\n<< /Length " + page.length() + " >>\nstream\n" + page + "\nendstream\nendobj\n";
        assertEquals("Skills", PdfTextExtractor.extract(pdf.getBytes(StandardCharsets.ISO_8859_1)));
    }

    @Test
    void pdf_roundTripThroughMinimalPdfWriterKeepsLines() throws Exception {
        byte[] pdf = MinimalPdfWriter.write(List.of(
                MinimalPdfWriter.Line.bold("Skills"), MinimalPdfWriter.Line.of("Java, SQL"),
                MinimalPdfWriter.Line.boldSpaced("Projects"), MinimalPdfWriter.Line.of("Placement Portal")));
        assertEquals("Skills\nJava, SQL\nProjects\nPlacement Portal", PdfTextExtractor.extract(pdf));
    }

    @Test
    void pdf_decompressionBombIsRejectedWithoutError() throws Exception {
        Deflater d = new Deflater();
        ByteArrayOutputStream co = new ByteArrayOutputStream();
        byte[] chunk = new byte[1 << 20];
        byte[] ob = new byte[8192];
        for (int i = 0; i < 40; i++) {                       // 40 MB of zeros -> a few KB compressed
            d.setInput(chunk);
            while (!d.needsInput()) co.write(ob, 0, d.deflate(ob));
        }
        d.finish();
        while (!d.finished()) co.write(ob, 0, d.deflate(ob));
        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        pdf.write(("%PDF-1.4\n2 0 obj\n<< /Filter /FlateDecode >>\nstream\n").getBytes(StandardCharsets.ISO_8859_1));
        pdf.write(co.toByteArray());
        pdf.write("\nendstream\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));
        assertEquals("", PdfTextExtractor.extract(pdf.toByteArray()));
    }

    @Test
    void pdf_notAPdfOrEmptyReturnsEmptyString() {
        assertEquals("", PdfTextExtractor.extract(null));
        assertEquals("", PdfTextExtractor.extract(new byte[0]));
        assertEquals("", PdfTextExtractor.extract("hello world".getBytes(StandardCharsets.ISO_8859_1)));
    }

    // ---------------------------------------------------------------
    // DocxTextExtractor
    // ---------------------------------------------------------------

    private static byte[] docx(String bodyXml) throws Exception {
        ByteArrayOutputStream zo = new ByteArrayOutputStream();
        try (ZipOutputStream z = new ZipOutputStream(zo)) {
            z.putNextEntry(new ZipEntry("word/document.xml"));
            String ns = "xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" "
                    + "xmlns:mc=\"http://schemas.openxmlformats.org/markup-compatibility/2006\"";
            z.write(("<w:document " + ns + "><w:body>" + bodyXml + "</w:body></w:document>").getBytes(StandardCharsets.UTF_8));
            z.closeEntry();
        }
        return zo.toByteArray();
    }

    @Test
    void docx_extractsParagraphsAndSkipsDuplicatedTextBoxFallback() throws Exception {
        byte[] file = docx("<w:p><w:r><w:t>Skills</w:t></w:r></w:p>"
                + "<mc:AlternateContent><mc:Choice><w:p><w:r><w:t>TEXTBOX</w:t></w:r></w:p></mc:Choice>"
                + "<mc:Fallback><w:p><w:r><w:t>TEXTBOX</w:t></w:r></w:p></mc:Fallback></mc:AlternateContent>");
        assertEquals("Skills\nTEXTBOX\n", DocxTextExtractor.extract(file));
    }

    @Test
    void docx_xxeDoctypeIsNotProcessedAndBadInputReturnsEmpty() throws Exception {
        ByteArrayOutputStream zo = new ByteArrayOutputStream();
        try (ZipOutputStream z = new ZipOutputStream(zo)) {
            z.putNextEntry(new ZipEntry("word/document.xml"));
            z.write("<!DOCTYPE d [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><w:document xmlns:w=\"urn:w\"><w:t>&x;</w:t></w:document>".getBytes(StandardCharsets.UTF_8));
            z.closeEntry();
        }
        String text = DocxTextExtractor.extract(zo.toByteArray());
        assertFalse(text.contains("root:"));
        assertEquals("", DocxTextExtractor.extract("not a zip".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void docx_oversizedDocumentXmlIsRejected() throws Exception {
        ByteArrayOutputStream zo = new ByteArrayOutputStream();
        try (ZipOutputStream z = new ZipOutputStream(zo)) {
            z.putNextEntry(new ZipEntry("word/document.xml"));
            byte[] chunk = new byte[1 << 20];
            java.util.Arrays.fill(chunk, (byte) ' ');
            for (int i = 0; i < 20; i++) z.write(chunk);      // 20 MB inflated, > 8 MB cap
            z.closeEntry();
        }
        assertEquals("", DocxTextExtractor.extract(zo.toByteArray()));
    }

    // ---------------------------------------------------------------
    // ResumeParserService / SkillMatcherService
    // ---------------------------------------------------------------

    private static ResumeData parseTxt(String text) {
        return new ResumeParserService().parse(text.getBytes(StandardCharsets.UTF_8), "r.txt", 1);
    }

    @Test
    void parser_phoneDetection_acceptsRealFormatsAndRejectsLongDigitRuns() {
        assertTrue(parseTxt("call 98765 43210 now").isPhoneDetected());
        assertTrue(parseTxt("+91-98765-43210").isPhoneDetected());
        assertTrue(parseTxt("9876543210").isPhoneDetected());
        assertFalse(parseTxt("PRN 230123456789").isPhoneDetected());      // 12-digit roll number
        assertFalse(parseTxt("CGPA 8.9, 2019-2023").isPhoneDetected());
    }

    @Test
    void parser_unknownHeaderDoesNotLeakIntoPreviousSection() {
        ResumeData d = parseTxt("Certifications\nAWS Cloud Practitioner\nPositions of Responsibility\nTreasurer, coding club\n");
        assertEquals("AWS Cloud Practitioner", d.getCertificationsSectionText());
    }

    @Test
    void parser_decoratedHeadersAreRecognised() {
        ResumeData d = parseTxt("-- Projects --\nPlacement Portal\nSKILLS:\nJava\n");
        assertEquals("Placement Portal", d.getProjectsSectionText());
        assertEquals("Java", d.getSkillsSectionText());
    }

    @Test
    void skills_ambiguousWordsOnlyCountInsideSkillsSection() {
        ResumeData prose = parseTxt("Projects\nUsed Spring 2024 notes, a go-getter, express interest, mail me at a@b.net\n");
        assertTrue(prose.getExtractedSkills().isEmpty());
        ResumeData listed = parseTxt("Skills\nC, C++, Go, R\n");
        assertTrue(listed.getExtractedSkills().containsAll(List.of("c", "c++", "go", "r")));
    }

    @Test
    void skills_javaIsNotFoundInsideJavascript() {
        ResumeData d = parseTxt("Skills\nJavaScript, React\n");
        assertTrue(d.getExtractedSkills().contains("javascript"));
        assertFalse(d.getExtractedSkills().contains("java"));
    }

    @Test
    void skillMatch_aliasesAndBlankRequiredEntriesAreHandled() {
        SkillMatcherService.MatchResult m = new SkillMatcherService().match(null,
                student(1, "A", "NodeJS; JS | postgres"), job("node.js", "JavaScript", "PostgreSQL", "", "  "));
        assertEquals(3, m.matchedSkills().size());
        assertTrue(m.missingSkills().isEmpty());
        assertEquals(40.0, m.skillScore(), 0.001);       // blank entries must not lower a full match
    }

    @Test
    void screening_oneProjectWithManyBulletsIsOneProject() {
        ResumeData d = parseTxt("Projects\nPlacement Portal\n- a\n- b\n- c\n- d\nExperience\n- Built x\n- Built y\n");
        ScreeningResultHolder h = new ScreeningResultHolder(d);
        assertEquals(5.0, h.result.getProjectScore(), 0.001);      // 1 title line x 5
        assertEquals(14.0, h.result.getExperienceScore(), 0.001);  // bullets only -> 2 entries x 7
    }

    private static final class ScreeningResultHolder {
        final com.placement.models.resume.ScreeningResult result;
        ScreeningResultHolder(ResumeData d) { result = new ScreeningService().screen(student(1, "A", ""), job(), d); }
    }

    @Test
    void parser_cachedTextKeepsFileTypeAndOversizedInputIsRejected() {
        assertEquals("PDF", new ResumeParserService().fromCachedText("hello", 1, "a.pdf").getFileType());
        assertTrue(new ResumeParserService().parse(new byte[ResumeParserService.MAX_FILE_BYTES + 1], "big.txt", 1).isEmpty());
    }

    // ---------------------------------------------------------------
    // MinimalPdfWriter
    // ---------------------------------------------------------------

    @Test
    void pdfWriter_longLinesAreWrappedAndSpecialCharsDoNotCorruptTheFile() throws Exception {
        String longLine = "word ".repeat(80) + "caf\u00e9 \u2022 \u4e2d\u6587 (paren) back\\slash";
        byte[] pdf = MinimalPdfWriter.write(List.of(MinimalPdfWriter.Line.of(longLine)));
        String asText = new String(pdf, StandardCharsets.ISO_8859_1);
        assertTrue(asText.startsWith("%PDF-1.4"));
        assertTrue(asText.contains("/WinAnsiEncoding"));
        assertTrue(asText.trim().endsWith("%%EOF"));
        String extracted = PdfTextExtractor.extract(pdf);
        assertTrue(extracted.split("\n").length >= 4);               // wrapped over several lines
        assertTrue(extracted.contains("(paren)"));
        assertTrue(extracted.contains("back\\slash"));
    }

    // ---------------------------------------------------------------
    // ResumeScreeningService (fake DAOs, temp folder)
    // ---------------------------------------------------------------

    private static class FakeResumeDAO extends ResumeDAO {
        final Map<Integer, ResumeRecord> rows = new HashMap<>();
        int saves = 0;
        @Override public void saveOrUpdate(int studentId, String fileName, String filePath, String text) throws java.sql.SQLException {
            saves++;
            rows.put(studentId, new ResumeRecord(saves, studentId, fileName, filePath, text, new Timestamp(0)));
        }
        @Override public ResumeRecord findByStudentId(int studentId) throws java.sql.SQLException { return rows.get(studentId); }
    }

    private static class FakeApplicationDAO extends ApplicationDAO {
        final Map<String, Double> scores = new HashMap<>();
        @Override public void saveScreeningScore(String jobId, int studentId, double score) { scores.put(jobId + "/" + studentId, score); }
    }

    @Test
    void service_uploadStoresFileAndCachedTextThenScreeningReadsTheCache() throws Exception {
        Path dir = Files.createTempDirectory("resumes-test");
        FakeResumeDAO resumes = new FakeResumeDAO();
        FakeApplicationDAO apps = new FakeApplicationDAO();
        ResumeScreeningService service = new ResumeScreeningService(resumes, apps, dir);

        ResumeData uploaded = service.uploadResume(7, "../../Aarav Resume.txt", SAMPLE_RESUME.getBytes(StandardCharsets.UTF_8));
        assertFalse(uploaded.isEmpty());
        assertEquals("Aarav Resume.txt", resumes.rows.get(7).fileName());                 // path part stripped
        assertTrue(Files.exists(Path.of(resumes.rows.get(7).filePath())));
        assertTrue(Path.of(resumes.rows.get(7).filePath()).toAbsolutePath().startsWith(dir.toAbsolutePath()));

        ResumeScreeningService.Outcome outcome = service.screenAndSave(student(7, "Aarav", ""), job("java", "sql", "kubernetes"));
        assertEquals(26.67, outcome.screening().getSkillScore(), 0.001);                  // 2 of 3 required
        assertTrue(outcome.screening().getMissingSkills().contains("kubernetes"));
        assertEquals(outcome.screening().getTotalScore(), apps.scores.get("JOB-1/7"), 0.001);
    }

    @Test
    void service_reuploadReplacesOldFileAndFailedDbWriteLeavesNoOrphan() throws Exception {
        Path dir = Files.createTempDirectory("resumes-test2");
        FakeResumeDAO resumes = new FakeResumeDAO();
        ResumeScreeningService service = new ResumeScreeningService(resumes, new FakeApplicationDAO(), dir);

        service.uploadResume(1, "a.txt", "Skills\nJava\n".getBytes(StandardCharsets.UTF_8));
        Path first = Path.of(resumes.rows.get(1).filePath());
        Thread.sleep(5);
        service.uploadResume(1, "b.txt", "Skills\nSQL\n".getBytes(StandardCharsets.UTF_8));
        assertFalse(Files.exists(first));                                                   // old file removed
        try (var files = Files.list(dir)) { assertEquals(1L, files.count()); }

        ResumeScreeningService failing = new ResumeScreeningService(new FakeResumeDAO() {
            @Override public void saveOrUpdate(int s, String f, String p, String t) throws java.sql.SQLException { throw new RuntimeException("db down"); }
        }, new FakeApplicationDAO(), dir);
        try {
            failing.uploadResume(2, "c.txt", "Skills\nJava\n".getBytes(StandardCharsets.UTF_8));
            fail("expected the DB failure to propagate");
        } catch (RuntimeException expected) {
            // fine
        }
        try (var files = Files.list(dir)) { assertEquals(1L, files.count()); }              // no orphan from the failed upload

        ResumeScreeningService readFails = new ResumeScreeningService(new FakeResumeDAO() {
            @Override public ResumeRecord findByStudentId(int s) throws java.sql.SQLException { throw new java.sql.SQLException("db down"); }
        }, new FakeApplicationDAO(), dir);
        assertThrows(java.sql.SQLException.class, () -> readFails.uploadResume(3, "d.txt", "Skills\nJava\n".getBytes(StandardCharsets.UTF_8)));
        try (var files = Files.list(dir)) { assertEquals(1L, files.count()); }              // DB read failure writes nothing either
    }

    @Test
    void service_rejectsBadUploads() throws Exception {
        ResumeScreeningService service = new ResumeScreeningService(new FakeResumeDAO(), new FakeApplicationDAO(),
                Files.createTempDirectory("resumes-test3"));
        byte[] ok = "Skills\nJava\n".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> service.uploadResume(1, "x.exe", ok));
        assertThrows(IllegalArgumentException.class, () -> service.uploadResume(1, "x.pdf", ok));          // text renamed to .pdf
        assertThrows(IllegalArgumentException.class, () -> service.uploadResume(1, "x.txt", new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> service.uploadResume(1, "x.txt", new byte[ResumeScreeningService.MAX_UPLOAD_BYTES + 1]));
        assertThrows(IllegalArgumentException.class, () -> service.uploadResume(0, "x.txt", ok));
        assertThrows(IllegalArgumentException.class, () -> service.uploadResume(1, "x.txt", new byte[]{'a', 0, 'b'}));
    }

    @Test
    void service_rankingPutsBestFirstTiesByNameAndResumelessLast() throws Exception {
        Path dir = Files.createTempDirectory("resumes-test4");
        FakeResumeDAO resumes = new FakeResumeDAO();
        ResumeScreeningService service = new ResumeScreeningService(resumes, new FakeApplicationDAO(), dir);
        service.uploadResume(1, "r.txt", "Skills\nJava, SQL\n".getBytes(StandardCharsets.UTF_8));      // both skills -> 40
        service.uploadResume(2, "r.txt", "Skills\nJava, SQL\n".getBytes(StandardCharsets.UTF_8));      // same -> tie
        service.uploadResume(3, "r.txt", "Skills\nJava\n".getBytes(StandardCharsets.UTF_8));           // one skill -> 20
        List<Student> candidates = new ArrayList<>(List.of(
                student(3, "Chitra", ""), student(4, "NoResume", ""), student(2, "Zara", ""), student(1, "Anil", "")));

        List<RankedCandidate> ranked = service.rank(candidates, job("java", "sql"));
        assertEquals(List.of("Anil", "Zara", "Chitra", "NoResume"), ranked.stream().map(RankedCandidate::getName).toList());
        assertEquals(0.0, ranked.get(3).getTotalScore(), 0.001);
    }

    @Test
    void service_studentWithoutResumeGetsSafeEmptyOutcomeAndReport() throws Exception {
        ResumeScreeningService service = new ResumeScreeningService(new FakeResumeDAO(), new FakeApplicationDAO(),
                Files.createTempDirectory("resumes-test5"));
        ResumeScreeningService.Outcome o = service.screen(student(9, "Nobody", ""), job("java"));
        assertTrue(o.resume().isEmpty());
        assertEquals(0.0, o.screening().getTotalScore(), 0.001);
        assertEquals(0, o.ats().getAtsScore());
        byte[] report = service.buildReport(student(9, "Nobody", ""), job("java"), false, List.of("No resume uploaded."));
        assertTrue(new String(report, StandardCharsets.ISO_8859_1).startsWith("%PDF-1.4"));
    }
}
