package com.placement.export;

import com.placement.testsupport.TestSupport;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class XlsxWriterTest {

    @Test
    void write_producesAllRequiredPackageParts() throws Exception {
        byte[] xlsx = XlsxWriter.write("Students", List.of("A"), List.of());
        for (String part : List.of("[Content_Types].xml", "_rels/.rels", "xl/workbook.xml",
                "xl/_rels/workbook.xml.rels", "xl/styles.xml", "xl/worksheets/sheet1.xml")) {
            assertTrue(TestSupport.entry(xlsx, part).length > 0);
        }
        assertEquals("PK", new String(xlsx, 0, 2, StandardCharsets.ISO_8859_1));      // it is a zip file
    }

    @Test
    void write_headersAndValuesAreReadBackExactly_nullsLeaveEmptyCells() throws Exception {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(Arrays.asList("B24CE1001", "Tushar", 8.9, 2, null));
        rows.add(Arrays.asList("B24CE1002", "Neel", 9.25, 0, "x"));
        Map<String, String> c = TestSupport.readXlsx(
                XlsxWriter.write("S", List.of("PRN", "Name", "CGPA", "Backlogs", "Note"), rows));

        assertEquals("PRN", c.get("A1"));
        assertEquals("Note", c.get("E1"));
        assertEquals("B24CE1001", c.get("A2"));
        assertEquals("8.9", c.get("C2"));
        assertEquals("2", c.get("D2"));
        assertFalse(c.containsKey("E2"));                    // null -> no cell
        assertEquals("9.25", c.get("C3"));
        assertEquals("x", c.get("E3"));
    }

    @Test
    void write_specialCharactersAreEscapedAndIllegalXmlCharactersAreDropped() throws Exception {
        List<List<Object>> rows = List.of(Arrays.asList(
                "Tushar <Borate> & \"Co\" 'x'", "Ctrl\u0001\u0008Char", "emoji \uD83D\uDE00 \u0905\u0928\u093F\u0932", "=1+1", "+91 98765 43210"));
        Map<String, String> c = TestSupport.readXlsx(XlsxWriter.write("S", List.of("a", "b", "c", "d", "e"), rows));

        assertEquals("Tushar <Borate> & \"Co\" 'x'", c.get("A2"));
        assertEquals("CtrlChar", c.get("B2"));               // control characters removed, file stays valid
        assertEquals("emoji \uD83D\uDE00 \u0905\u0928\u093F\u0932", c.get("C2"));
        assertEquals("=1+1", c.get("D2"));                   // stays literal text (stored as a string cell)
        assertEquals("+91 98765 43210", c.get("E2"));
    }

    @Test
    void write_columnLettersContinuePastZ() throws Exception {
        List<String> headers = new ArrayList<>();
        for (int i = 1; i <= 30; i++) headers.add("H" + i);
        Map<String, String> c = TestSupport.readXlsx(XlsxWriter.write("S", headers, List.of()));
        assertEquals("H26", c.get("Z1"));
        assertEquals("H27", c.get("AA1"));
        assertEquals("H30", c.get("AD1"));
        assertEquals("A", XlsxWriter.columnName(0));
        assertEquals("AA", XlsxWriter.columnName(26));
        assertEquals("AZ", XlsxWriter.columnName(51));
        assertEquals("BA", XlsxWriter.columnName(52));
    }

    @Test
    void write_sheetNameIsSanitisedAndShortened() throws Exception {
        assertEquals("Stu dents   2026", TestSupport.sheetName(XlsxWriter.write("Stu[dents]: 2026/*?", List.of("a"), List.of())));
        assertEquals("Sheet1", TestSupport.sheetName(XlsxWriter.write("   ", List.of("a"), List.of())));
        assertEquals(31, TestSupport.sheetName(XlsxWriter.write("x".repeat(80), List.of("a"), List.of())).length());
    }

    @Test
    void write_hasFrozenHeaderAndAutoFilterOverTheWholeTable() throws Exception {
        List<List<Object>> rows = List.of(Arrays.asList("a", "b"), Arrays.asList("c", "d"), Arrays.asList("e", "f"));
        String sheet = new String(TestSupport.entry(XlsxWriter.write("S", List.of("H1", "H2"), rows),
                "xl/worksheets/sheet1.xml"), StandardCharsets.UTF_8);
        assertTrue(sheet.contains("state=\"frozen\""));
        assertTrue(sheet.contains("<autoFilter ref=\"A1:B4\"/>"));
    }

    @Test
    void write_veryLongCellIsCappedBelowExcelsLimit() throws Exception {
        Map<String, String> c = TestSupport.readXlsx(
                XlsxWriter.write("S", List.of("a"), List.of(List.of("y".repeat(40_000)))));
        assertEquals(32_000, c.get("A2").length());
    }

    @Test
    void write_rejectsNoColumnsAndTooManyRows() {
        assertThrows(IllegalArgumentException.class, () -> XlsxWriter.write("S", List.of(), List.of()));
        List<List<Object>> tooMany = new ArrayList<>();
        for (int i = 0; i <= XlsxWriter.MAX_ROWS; i++) tooMany.add(List.of("x"));
        assertThrows(IllegalArgumentException.class, () -> XlsxWriter.write("S", List.of("a"), tooMany));
    }

    @Test
    void write_nanAndInfinityBecomeEmptyCellsNotCorruptXml() throws Exception {
        Map<String, String> c = TestSupport.readXlsx(XlsxWriter.write("S", List.of("a", "b", "c"),
                List.of(Arrays.asList(Double.NaN, Double.POSITIVE_INFINITY, 1.5))));
        assertFalse(c.containsKey("A2"));
        assertFalse(c.containsKey("B2"));
        assertEquals("1.5", c.get("C2"));
    }
}
