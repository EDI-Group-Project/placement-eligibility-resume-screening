package com.placement.services.resume;

import com.placement.testsupport.XlsxReader;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** The hand-written .xlsx must be a well-formed package whose cells read back exactly as written. */
class XlsxWriterTest {

    private static List<Object> row(Object... cells) {
        return new ArrayList<>(Arrays.asList(cells));
    }

    @Test
    void workbook_hasEveryRequiredPartAndEachPartIsWellFormedXml() throws Exception {
        byte[] xlsx = XlsxWriter.write("Students", List.of("A", "B"), List.of(row("x", 1)));

        assertEquals('P', xlsx[0]);                                               // a zip: "PK"
        assertEquals('K', xlsx[1]);
        Map<String, byte[]> parts = XlsxReader.parts(xlsx);
        assertEquals(List.of("[Content_Types].xml", "_rels/.rels", "xl/workbook.xml", "xl/_rels/workbook.xml.rels",
                "xl/styles.xml", "xl/worksheets/sheet1.xml"), List.copyOf(parts.keySet()));
        for (byte[] part : parts.values()) assertNotNull(XlsxReader.xml(part));   // throws if any part is not well-formed
        assertEquals("Students", XlsxReader.xml(parts.get("xl/workbook.xml"))
                .getElementsByTagNameNS(XlsxReader.MAIN_NS, "sheet").item(0).getAttributes().getNamedItem("name").getNodeValue());
    }

    @Test
    void cells_textNumbersAndEmptyCellsReadBackExactly() throws Exception {
        byte[] xlsx = XlsxWriter.write("S", List.of("Name", "CGPA", "Phone"),
                List.of(row("Aarav", 8.5, null), row("Priya", 9, "98765"), row("Ravi", 7.10, "")));

        assertEquals(List.of(
                List.of("Name", "CGPA", "Phone"),
                List.of("Aarav", "8.5", ""),
                List.of("Priya", "9", "98765"),
                List.of("Ravi", "7.1", "")), XlsxReader.rows(xlsx));

        assertNull(XlsxReader.cell(xlsx, "C2"), "a null value must leave the cell out");
        assertNull(XlsxReader.cell(xlsx, "B2").getAttributeNode("t"), "numbers are numeric cells, not text");
        assertEquals("inlineStr", XlsxReader.cell(xlsx, "A2").getAttribute("t"));
        assertEquals("inlineStr", XlsxReader.cell(xlsx, "C3").getAttribute("t"));    // "98765" stays text (phone numbers keep leading zeros)
        assertEquals("1", XlsxReader.cell(xlsx, "A1").getAttribute("s"));            // header cells use the bold style
        assertTrue(XlsxReader.cell(xlsx, "A2").getAttribute("s").isEmpty(), "data cells are not bold");
    }

    @Test
    void header_isFrozenAndColumnsAreSized() throws Exception {
        byte[] xlsx = XlsxWriter.write("S", List.of("Short", "A much longer heading than the minimum width"),
                List.of(row("x", "y")));
        var sheet = XlsxReader.sheet(xlsx);
        Element pane = (Element) sheet.getElementsByTagNameNS(XlsxReader.MAIN_NS, "pane").item(0);
        assertEquals("frozen", pane.getAttribute("state"));
        assertEquals("1", pane.getAttribute("ySplit"));

        var cols = sheet.getElementsByTagNameNS(XlsxReader.MAIN_NS, "col");
        assertEquals(2, cols.getLength());
        assertEquals("8", ((Element) cols.item(0)).getAttribute("width"));               // never narrower than 8
        assertEquals("46", ((Element) cols.item(1)).getAttribute("width"));              // text length 44 + 2
    }

    @Test
    void text_specialCharactersSurviveAndIllegalOnesAreDropped() throws Exception {
        String tricky = "R&D <b>\"q\"</b> 'a' café 中文 😀";
        byte[] xlsx = XlsxWriter.write("S", List.of("T"), List.of(
                row(tricky), row("a\u0000b\u0008c\u000Bd￾e"), row("lone\uD800surrogate"), row("tab\there\nnewline")));

        List<List<String>> rows = XlsxReader.rows(xlsx);                                  // parsing proves the XML is still valid
        assertEquals(tricky, rows.get(1).get(0));
        assertEquals("abcde", rows.get(2).get(0));                                         // control characters removed
        assertEquals("lonesurrogate", rows.get(3).get(0));
        assertEquals("tab\there\nnewline", rows.get(4).get(0));                            // tab and newline are legal and kept
    }

    @Test
    void text_isNeverWrittenAsAFormula() throws Exception {
        byte[] xlsx = XlsxWriter.write("S", List.of("Skills"), List.of(row("=1+1"), row("+cmd|' /C calc'!A0"), row("@SUM(1)"), row("-2+3")));

        String sheet = new String(XlsxReader.parts(xlsx).get("xl/worksheets/sheet1.xml"), java.nio.charset.StandardCharsets.UTF_8);
        assertFalse(sheet.contains("<f>") || sheet.contains("<f "), "no formula element may be written");
        assertEquals("=1+1", XlsxReader.rows(xlsx).get(1).get(0));
        assertEquals("inlineStr", XlsxReader.cell(xlsx, "A2").getAttribute("t"));
    }

    @Test
    void longTextIsCutAtExcelsCellLimit() throws Exception {
        byte[] xlsx = XlsxWriter.write("S", List.of("T"), List.of(row("x".repeat(40_000))));
        assertEquals(32_767, XlsxReader.rows(xlsx).get(1).get(0).length());
    }

    @Test
    void numbersThatCannotBeStoredLeaveTheCellEmpty() throws Exception {
        byte[] xlsx = XlsxWriter.write("S", List.of("A", "B", "C", "D"),
                List.of(row(Double.NaN, Double.POSITIVE_INFINITY, 1234567.891, 1e-7)));
        assertEquals(List.of("", "", "1234567.891", "0.0000001"), XlsxReader.rows(xlsx).get(1));   // plain decimals, never 1.0E-7
    }

    @Test
    void noRows_stillGivesAValidSheetWithOnlyTheHeader() throws Exception {
        byte[] xlsx = XlsxWriter.write("S", List.of("A", "B"), List.of());
        assertEquals(List.of(List.of("A", "B")), XlsxReader.rows(xlsx));
    }

    @Test
    void sheetName_isMadeSafeForExcel() throws Exception {
        assertEquals("a_b_c___x_", sheetName(XlsxWriter.write("a/b:c*?[x]", List.of("A"), List.of())));
        assertEquals("Sheet1", sheetName(XlsxWriter.write("  ", List.of("A"), List.of())));
        assertEquals("Sheet1", sheetName(XlsxWriter.write(null, List.of("A"), List.of())));
        assertEquals(31, sheetName(XlsxWriter.write("n".repeat(80), List.of("A"), List.of())).length());
        assertEquals("R&D \"x\"", sheetName(XlsxWriter.write("R&D \"x\"", List.of("A"), List.of())));   // escaped in the XML, same after parsing
    }

    @Test
    void columnNames_followExcelNumbering() {
        assertEquals("A", XlsxWriter.columnName(0));
        assertEquals("Z", XlsxWriter.columnName(25));
        assertEquals("AA", XlsxWriter.columnName(26));
        assertEquals("AB", XlsxWriter.columnName(27));
        assertEquals("ZZ", XlsxWriter.columnName(701));
        assertEquals("AAA", XlsxWriter.columnName(702));
    }

    private static String sheetName(byte[] xlsx) throws Exception {
        return ((Element) XlsxReader.xml(XlsxReader.parts(xlsx).get("xl/workbook.xml"))
                .getElementsByTagNameNS(XlsxReader.MAIN_NS, "sheet").item(0)).getAttribute("name");
    }
}
