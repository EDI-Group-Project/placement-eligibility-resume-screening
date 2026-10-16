package com.placement.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Zero-dependency writer for single-sheet Excel (.xlsx) files.
 *
 * An .xlsx file is a ZIP archive of small XML parts (Office Open XML), so the JDK's
 * java.util.zip is all that is needed - the same approach this module already uses
 * to READ .docx files. Output features: bold shaded header row, frozen header, an
 * auto-filter over the whole table, sensible column widths, real numeric cells
 * (CGPA/score sort and filter as numbers in Excel) and text cells stored as
 * inline strings (so a value such as "=1+1" stays literal text and is never evaluated).
 *
 * Safety: characters that are illegal in XML 1.0 are dropped (resume-derived or typed
 * text can contain them and would otherwise make Excel report a corrupt file), cell text
 * is capped at Excel's limit, and the row count is capped.
 *
 * Supported cell values: String, Integer/Long (integer cell), Double/Float (2-decimal
 * cell), null (empty cell). Anything else is written with toString().
 */
public final class XlsxWriter {

    public static final int MAX_ROWS = 100_000;
    private static final int MAX_CELL_CHARS = 32_000;      // Excel's hard limit is 32,767 characters per cell
    private static final int STYLE_HEADER = 1;
    private static final int STYLE_DECIMAL = 2;

    private static final String XML_DECL = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n";
    private static final String NS_MAIN = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private static final String NS_REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";

    private static final String CONTENT_TYPES = XML_DECL
            + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
            + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
            + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
            + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
            + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
            + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
            + "</Types>";

    private static final String ROOT_RELS = XML_DECL
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"" + NS_REL + "/officeDocument\" Target=\"xl/workbook.xml\"/>"
            + "</Relationships>";

    private static final String WORKBOOK_RELS = XML_DECL
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"" + NS_REL + "/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
            + "<Relationship Id=\"rId2\" Type=\"" + NS_REL + "/styles\" Target=\"styles.xml\"/>"
            + "</Relationships>";

    // cellXfs: 0 = default, 1 = bold header on light-blue fill, 2 = number with 2 decimals (built-in format 2)
    private static final String STYLES = XML_DECL
            + "<styleSheet xmlns=\"" + NS_MAIN + "\">"
            + "<fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font>"
            + "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts>"
            + "<fills count=\"3\"><fill><patternFill patternType=\"none\"/></fill>"
            + "<fill><patternFill patternType=\"gray125\"/></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFD9E1F2\"/><bgColor indexed=\"64\"/></patternFill></fill></fills>"
            + "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>"
            + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
            + "<cellXfs count=\"3\">"
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
            + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"/>"
            + "<xf numFmtId=\"2\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>"
            + "</cellXfs>"
            + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
            + "</styleSheet>";

    private XlsxWriter() {}

    /**
     * @param sheetName worksheet tab name (illegal characters are replaced, max 31 chars)
     * @param headers   column titles (at least one)
     * @param rows      data rows; a short row leaves the remaining cells empty
     */
    public static byte[] write(String sheetName, List<String> headers, List<List<Object>> rows) throws IOException {
        if (headers == null || headers.isEmpty()) {
            throw new IllegalArgumentException("At least one column is required.");
        }
        List<List<Object>> data = rows == null ? List.of() : rows;
        if (data.size() > MAX_ROWS) {
            throw new IllegalArgumentException("Too many rows to export (maximum " + MAX_ROWS + ").");
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            put(zip, "[Content_Types].xml", CONTENT_TYPES);
            put(zip, "_rels/.rels", ROOT_RELS);
            put(zip, "xl/workbook.xml", workbookXml(sheetName));
            put(zip, "xl/_rels/workbook.xml.rels", WORKBOOK_RELS);
            put(zip, "xl/styles.xml", STYLES);
            put(zip, "xl/worksheets/sheet1.xml", sheetXml(headers, data));
        }
        return bytes.toByteArray();
    }

    // ------------------------------------------------------------------

    private static void put(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String workbookXml(String sheetName) {
        return XML_DECL + "<workbook xmlns=\"" + NS_MAIN + "\" xmlns:r=\"" + NS_REL + "\">"
                + "<sheets><sheet name=\"" + escape(safeSheetName(sheetName)) + "\" sheetId=\"1\" r:id=\"rId1\"/></sheets>"
                + "</workbook>";
    }

    private static String safeSheetName(String name) {
        String clean = name == null ? "" : name.replaceAll("[\\[\\]:*?/\\\\]", " ").replace("'", "").trim();
        if (clean.isEmpty()) clean = "Sheet1";
        return clean.length() > 31 ? clean.substring(0, 31) : clean;
    }

    private static String sheetXml(List<String> headers, List<List<Object>> rows) {
        int cols = headers.size();
        StringBuilder sb = new StringBuilder(4096 + rows.size() * cols * 40);
        sb.append(XML_DECL).append("<worksheet xmlns=\"").append(NS_MAIN).append("\">");
        sb.append("<sheetViews><sheetView workbookViewId=\"0\">"
                + "<pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/>"
                + "</sheetView></sheetViews>");
        sb.append("<sheetFormatPr defaultRowHeight=\"15\"/>");

        sb.append("<cols>");
        for (int c = 0; c < cols; c++) {
            sb.append("<col min=\"").append(c + 1).append("\" max=\"").append(c + 1)
                    .append("\" width=\"").append(columnWidth(headers, rows, c)).append("\" customWidth=\"1\"/>");
        }
        sb.append("</cols>");

        sb.append("<sheetData>");
        sb.append("<row r=\"1\">");
        for (int c = 0; c < cols; c++) {
            textCell(sb, c, 1, headers.get(c), STYLE_HEADER);
        }
        sb.append("</row>");
        for (int r = 0; r < rows.size(); r++) {
            int excelRow = r + 2;
            List<Object> row = rows.get(r);
            sb.append("<row r=\"").append(excelRow).append("\">");
            for (int c = 0; c < cols; c++) {
                Object value = (row != null && c < row.size()) ? row.get(c) : null;
                valueCell(sb, c, excelRow, value);
            }
            sb.append("</row>");
        }
        sb.append("</sheetData>");
        sb.append("<autoFilter ref=\"A1:").append(columnName(cols - 1)).append(rows.size() + 1).append("\"/>");
        sb.append("</worksheet>");
        return sb.toString();
    }

    private static void valueCell(StringBuilder sb, int col, int row, Object value) {
        if (value == null) return;
        if (value instanceof Double || value instanceof Float) {
            double d = ((Number) value).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) return;
            sb.append("<c r=\"").append(columnName(col)).append(row).append("\" s=\"").append(STYLE_DECIMAL)
                    .append("\"><v>").append(BigDecimal.valueOf(d).toPlainString()).append("</v></c>");
        } else if (value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte) {
            sb.append("<c r=\"").append(columnName(col)).append(row).append("\"><v>")
                    .append(((Number) value).longValue()).append("</v></c>");
        } else {
            textCell(sb, col, row, value.toString(), 0);
        }
    }

    private static void textCell(StringBuilder sb, int col, int row, String text, int style) {
        String t = text.length() > MAX_CELL_CHARS ? text.substring(0, MAX_CELL_CHARS) : text;
        sb.append("<c r=\"").append(columnName(col)).append(row).append("\"");
        if (style != 0) sb.append(" s=\"").append(style).append("\"");
        sb.append(" t=\"inlineStr\"><is><t xml:space=\"preserve\">").append(escape(t)).append("</t></is></c>");
    }

    /** Width in characters: longest of the header and the first 200 values, clamped to 10..50. */
    private static int columnWidth(List<String> headers, List<List<Object>> rows, int col) {
        int longest = headers.get(col) == null ? 0 : headers.get(col).length();
        int limit = Math.min(rows.size(), 200);
        for (int r = 0; r < limit; r++) {
            List<Object> row = rows.get(r);
            if (row != null && col < row.size() && row.get(col) != null) {
                longest = Math.max(longest, String.valueOf(row.get(col)).length());
            }
        }
        return Math.max(10, Math.min(50, longest + 2));
    }

    /** 0 -> A, 25 -> Z, 26 -> AA ... */
    static String columnName(int index) {
        StringBuilder name = new StringBuilder();
        int n = index;
        while (n >= 0) {
            name.insert(0, (char) ('A' + (n % 26)));
            n = n / 26 - 1;
        }
        return name.toString();
    }

    /** Escapes XML special characters and removes characters that are not allowed in XML 1.0 at all. */
    private static String escape(String s) {
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            boolean legal = cp == 0x9 || cp == 0xA || cp == 0xD
                    || (cp >= 0x20 && cp <= 0xD7FF) || (cp >= 0xE000 && cp <= 0xFFFD)
                    || (cp >= 0x10000 && cp <= 0x10FFFF);
            if (!legal) continue;                       // also drops lone surrogates
            switch (cp) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&apos;");
                default -> out.appendCodePoint(cp);
            }
        }
        return out.toString();
    }
}
