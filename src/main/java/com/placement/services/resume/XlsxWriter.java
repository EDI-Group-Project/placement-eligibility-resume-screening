package com.placement.services.resume;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Zero-dependency writer for a one-sheet .xlsx workbook (same idea as {@link MinimalPdfWriter}: the
 * project has no Excel library and two simple reports do not justify adding one).
 *
 * Output: bold header row, frozen header, sized columns. Numbers are real numeric cells; everything
 * else is an inline string. Text is NEVER written as a formula, so a value such as {@code =cmd|...}
 * typed into a student's skills stays plain text when the file is opened in Excel.
 *
 * Package-private: used by {@link ExportService}.
 */
final class XlsxWriter {

    private static final int MAX_CELL_CHARS = 32_767;     // Excel's limit per cell
    private static final int MIN_WIDTH = 8, MAX_WIDTH = 50;

    private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n";
    private static final String MAIN_NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private static final String REL_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    private static final String PKG_REL_NS = "http://schemas.openxmlformats.org/package/2006/relationships";

    private XlsxWriter() { }

    /**
     * @param sheetName worksheet tab name (invalid characters are replaced, cut to Excel's 31 characters)
     * @param headers   first row
     * @param rows      data rows; a cell is a Number (numeric cell), null (empty cell) or anything else (text)
     */
    static byte[] write(String sheetName, List<String> headers, List<List<Object>> rows) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            put(zip, "[Content_Types].xml", XML
                    + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                    + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                    + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                    + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                    + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                    + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
                    + "</Types>");
            put(zip, "_rels/.rels", XML
                    + "<Relationships xmlns=\"" + PKG_REL_NS + "\">"
                    + "<Relationship Id=\"rId1\" Type=\"" + REL_NS + "/officeDocument\" Target=\"xl/workbook.xml\"/>"
                    + "</Relationships>");
            put(zip, "xl/workbook.xml", XML
                    + "<workbook xmlns=\"" + MAIN_NS + "\" xmlns:r=\"" + REL_NS + "\">"
                    + "<sheets><sheet name=\"" + escape(safeSheetName(sheetName)) + "\" sheetId=\"1\" r:id=\"rId1\"/></sheets>"
                    + "</workbook>");
            put(zip, "xl/_rels/workbook.xml.rels", XML
                    + "<Relationships xmlns=\"" + PKG_REL_NS + "\">"
                    + "<Relationship Id=\"rId1\" Type=\"" + REL_NS + "/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                    + "<Relationship Id=\"rId2\" Type=\"" + REL_NS + "/styles\" Target=\"styles.xml\"/>"
                    + "</Relationships>");
            // cellXfs index 1 = bold (used by the header row)
            put(zip, "xl/styles.xml", XML
                    + "<styleSheet xmlns=\"" + MAIN_NS + "\">"
                    + "<fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font>"
                    + "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts>"
                    + "<fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill></fills>"
                    + "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>"
                    + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
                    + "<cellXfs count=\"2\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
                    + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/></cellXfs>"
                    + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
                    + "</styleSheet>");
            put(zip, "xl/worksheets/sheet1.xml", sheet(headers, rows));
        }
        return bytes.toByteArray();
    }

    private static String sheet(List<String> headers, List<List<Object>> rows) {
        int[] width = new int[headers.size()];
        StringBuilder data = new StringBuilder();
        appendRow(data, 1, new ArrayList<Object>(headers), true, width);
        int rowNo = 2;
        for (List<Object> row : rows) appendRow(data, rowNo++, row, false, width);

        StringBuilder cols = new StringBuilder();
        for (int c = 0; c < width.length; c++) {
            int w = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, width[c] + 2));
            cols.append("<col min=\"").append(c + 1).append("\" max=\"").append(c + 1)
                .append("\" width=\"").append(w).append("\" customWidth=\"1\"/>");
        }
        return XML + "<worksheet xmlns=\"" + MAIN_NS + "\">"
                + "<sheetViews><sheetView workbookViewId=\"0\">"
                + "<pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/>"
                + "</sheetView></sheetViews>"
                + (cols.length() > 0 ? "<cols>" + cols + "</cols>" : "")
                + "<sheetData>" + data + "</sheetData></worksheet>";
    }

    private static void appendRow(StringBuilder sb, int rowNo, List<Object> cells, boolean header, int[] width) {
        sb.append("<row r=\"").append(rowNo).append("\">");
        for (int c = 0; c < width.length && c < cells.size(); c++) {
            Object value = cells.get(c);
            if (value == null) continue;
            String ref = columnName(c) + rowNo;
            String number = !header && value instanceof Number n ? numberText(n) : null;
            if (number != null) {
                sb.append("<c r=\"").append(ref).append("\"><v>").append(number).append("</v></c>");
                width[c] = Math.max(width[c], number.length());
            } else if (!(value instanceof Number)) {
                String text = value.toString();
                if (text.length() > MAX_CELL_CHARS) text = text.substring(0, MAX_CELL_CHARS);
                sb.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"").append(header ? " s=\"1\"" : "")
                  .append("><is><t xml:space=\"preserve\">").append(escape(text)).append("</t></is></c>");
                width[c] = Math.max(width[c], text.length());
            }                                                    // NaN / infinite numbers: left empty
        }
        sb.append("</row>");
    }

    /** Plain decimal text for a numeric cell, or null when the value cannot be stored (NaN, infinity). */
    private static String numberText(Number n) {
        if (n instanceof Double || n instanceof Float) {
            double d = n.doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) return null;
            return BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
        }
        return n.toString();
    }

    /** 0 -> A, 25 -> Z, 26 -> AA ... */
    static String columnName(int index) {
        StringBuilder sb = new StringBuilder();
        for (int i = index + 1; i > 0; i = (i - 1) / 26) sb.insert(0, (char) ('A' + (i - 1) % 26));
        return sb.toString();
    }

    private static String safeSheetName(String name) {
        String clean = (name == null ? "" : name).replaceAll("[\\\\/?*\\[\\]:]", "_").strip();
        if (clean.isEmpty()) clean = "Sheet1";
        return clean.length() > 31 ? clean.substring(0, 31) : clean;
    }

    /** Escapes XML and drops characters XML 1.0 cannot carry (control characters, lone surrogates) - they would make Excel report a corrupt file. */
    static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            boolean legal = cp == 0x9 || cp == 0xA || cp == 0xD
                    || (cp >= 0x20 && cp <= 0xD7FF) || (cp >= 0xE000 && cp <= 0xFFFD) || (cp >= 0x10000 && cp <= 0x10FFFF);
            if (!legal) continue;
            switch (cp) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&apos;");
                default -> sb.appendCodePoint(cp);
            }
        }
        return sb.toString();
    }

    private static void put(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
