package com.placement.services.resume;

import com.placement.database.ApplicationDAO;
import com.placement.models.Student;
import com.placement.testsupport.Fakes;
import com.placement.testsupport.XlsxReader;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Column layout and content of the two Excel exports (no database, no sockets). */
class ExportServiceTest {

    private final ExportService exports = new ExportService();

    private static Student aarav() {
        return Fakes.student(1, "PRN001", "Aarav Sharma", "aarav@gmail.com", "CSE", 8.5, 2027, 0, 5, "9876543210", "Java,SQL,Python");
    }

    @Test
    void studentColumns_matchTheStudentProtocolLineOrder() {
        assertEquals(List.of("PRN", "Name", "Email", "Department", "CGPA", "Passing Year", "Backlogs", "Semester", "Phone", "Skills"),
                ExportService.STUDENT_COLUMNS);
        // same order as Student.toProtocolLine(), which the existing student table already parses
        assertEquals("PRN001|Aarav Sharma|aarav@gmail.com|CSE|8.5|2027|0|5|9876543210|Java,SQL,Python", aarav().toProtocolLine());
    }

    @Test
    void studentsWorkbook_hasOneRowPerStudentInGivenOrder() throws Exception {
        Student priya = Fakes.student(2, "PRN002", "Priya Patil", "priya@gmail.com", "IT", 7.8, 2027, 1, 3, "9876543211", "Java");
        byte[] xlsx = exports.studentsWorkbook(List.of(priya, aarav()));

        assertEquals(List.of(
                ExportService.STUDENT_COLUMNS,
                List.of("PRN002", "Priya Patil", "priya@gmail.com", "IT", "7.8", "2027", "1", "3", "9876543211", "Java"),
                List.of("PRN001", "Aarav Sharma", "aarav@gmail.com", "CSE", "8.5", "2027", "0", "5", "9876543210", "Java,SQL,Python")),
                XlsxReader.rows(xlsx));
        assertEquals("Students", sheetName(xlsx));
    }

    @Test
    void studentsWorkbook_neverContainsThePasswordHash() throws Exception {
        byte[] xlsx = exports.studentsWorkbook(List.of(aarav()));
        for (Map.Entry<String, byte[]> part : XlsxReader.parts(xlsx).entrySet()) {
            assertFalse(new String(part.getValue(), StandardCharsets.UTF_8).contains("SECRET-PASSWORD-HASH"), part.getKey());
        }
        assertFalse(ExportService.STUDENT_COLUMNS.stream().anyMatch(c -> c.toLowerCase().contains("password")));
    }

    @Test
    void studentCells_handleMissingValues() throws Exception {
        Student bare = new Student();
        bare.setStudentId(42);                  // no PRN, no semester (the DB column is NULL), nothing else
        List<Object> cells = ExportService.studentCells(bare);
        assertEquals("42", cells.get(0));        // PRN falls back to the student id, like Student.toProtocolLine()
        assertNull(cells.get(1));
        assertNull(cells.get(7), "semester 0 means 'not set' and must export as an empty cell");
        assertEquals(10, cells.size());

        byte[] xlsx = exports.studentsWorkbook(List.of(bare));
        assertEquals("42", XlsxReader.rows(xlsx).get(1).get(0));
    }

    @Test
    void applicantsWorkbook_addsStatusScoreAndDate() throws Exception {
        var screened = new ApplicationDAO.Applicant(aarav(), 11, "SCREENED", 72.5, "2026-10-01 10:15:00");
        var fresh = new ApplicationDAO.Applicant(
                Fakes.student(2, "PRN002", "Priya Patil", "priya@gmail.com", "IT", 7.8, 2027, 1, 3, "9876543211", "Java"),
                12, "APPLIED", null, "2026-10-02 09:00:00");
        byte[] xlsx = exports.applicantsWorkbook(List.of(screened, fresh));

        List<List<String>> rows = XlsxReader.rows(xlsx);
        assertEquals(13, rows.get(0).size());
        assertEquals(List.of("Application Status", "Screening Score", "Applied At"), rows.get(0).subList(10, 13));
        assertEquals(List.of("SCREENED", "72.5", "2026-10-01 10:15:00"), rows.get(1).subList(10, 13));
        assertEquals(List.of("APPLIED", "", "2026-10-02 09:00:00"), rows.get(2).subList(10, 13));    // not screened yet: empty, not 0
        assertEquals("PRN002", rows.get(2).get(0));
        assertEquals("Applicants", sheetName(xlsx));
    }

    @Test
    void emptyLists_giveHeaderOnlyWorkbooks() throws Exception {
        assertEquals(1, XlsxReader.rows(exports.studentsWorkbook(List.of())).size());
        assertEquals(1, XlsxReader.rows(exports.applicantsWorkbook(List.of())).size());
    }

    private static String sheetName(byte[] xlsx) throws Exception {
        return ((org.w3c.dom.Element) XlsxReader.xml(XlsxReader.parts(xlsx).get("xl/workbook.xml"))
                .getElementsByTagNameNS(XlsxReader.MAIN_NS, "sheet").item(0)).getAttribute("name");
    }
}
