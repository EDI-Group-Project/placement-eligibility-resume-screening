package com.placement.services.resume;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Zero-dependency PDF writer for simple, plain-text, single-column reports.
 *
 * Hand-written against the PDF 1.4 object/xref/trailer structure (no
 * external library) - deliberately minimal: left-aligned text only, one of
 * two standard (non-embedded) base-14 fonts per line (Helvetica / Helvetica-
 * Bold), automatic pagination. That's all {@code ResumeReportPdfService}
 * needs for a candidate screening report, so a full layout engine (which is
 * what a real PDF library like Apache PDFBox would otherwise be used for)
 * isn't necessary here.
 *
 * Package-private: only used internally by {@link ResumeReportPdfService}.
 */
final class MinimalPdfWriter {

    /** One line of report content. */
    record Line(String text, boolean bold, boolean extraSpaceBefore) {
        static Line of(String text) { return new Line(text, false, false); }
        static Line bold(String text) { return new Line(text, true, false); }
        static Line spaced(String text) { return new Line(text, false, true); }
        static Line boldSpaced(String text) { return new Line(text, true, true); }
    }

    private static final double PAGE_WIDTH = 612;  // US Letter, points
    private static final double PAGE_HEIGHT = 792;
    private static final double MARGIN_LEFT = 50;
    private static final double MARGIN_TOP = 740;
    private static final double MARGIN_BOTTOM = 50;
    private static final double LINE_HEIGHT = 14;
    private static final double EXTRA_GAP = 8;
    private static final double NORMAL_SIZE = 10;
    private static final double BOLD_SIZE = 11;
    private static final int MAX_CHARS_NORMAL = 95;  // keeps a 10pt Helvetica line inside the page margins
    private static final int MAX_CHARS_BOLD = 85;
    private static final Charset WIN_1252 = Charset.forName("windows-1252");

    private MinimalPdfWriter() {}

    static byte[] write(List<Line> lines) throws IOException {
        List<List<Line>> pages = paginate(wrapLines(lines));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Long> offsets = new ArrayList<>();

        int fontF1 = 3;
        int fontF2 = 4;
        int firstPageObj = 5;
        int objectsPerPage = 2; // page object + its content stream object
        int totalObjects = 4 + pages.size() * objectsPerPage; // 1 catalog + 1 pages + 2 fonts + per-page

        writeLine(out, "%PDF-1.4");
        writeBytes(out, new byte[]{'%', (byte) 0xE2, (byte) 0xE3, (byte) 0xCF, (byte) 0xD3, '\n'});

        // Object 1: Catalog
        offsets.add((long) out.size());
        writeLine(out, "1 0 obj");
        writeLine(out, "<< /Type /Catalog /Pages 2 0 R >>");
        writeLine(out, "endobj");

        // Object 2: Pages
        StringBuilder kids = new StringBuilder();
        for (int p = 0; p < pages.size(); p++) {
            int pageObjNum = firstPageObj + p * objectsPerPage;
            kids.append(pageObjNum).append(" 0 R ");
        }
        offsets.add((long) out.size());
        writeLine(out, "2 0 obj");
        writeLine(out, "<< /Type /Pages /Kids [ " + kids.toString().trim() + " ] /Count " + pages.size() + " >>");
        writeLine(out, "endobj");

        // Object 3: Font F1 (Helvetica)
        offsets.add((long) out.size());
        writeLine(out, "3 0 obj");
        writeLine(out, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
        writeLine(out, "endobj");

        // Object 4: Font F2 (Helvetica-Bold)
        offsets.add((long) out.size());
        writeLine(out, "4 0 obj");
        writeLine(out, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>");
        writeLine(out, "endobj");

        for (int p = 0; p < pages.size(); p++) {
            int pageObjNum = firstPageObj + p * objectsPerPage;
            int contentObjNum = pageObjNum + 1;

            String content = buildContentStream(pages.get(p));
            byte[] contentBytes = content.getBytes(StandardCharsets.ISO_8859_1);

            // Page object
            offsets.add((long) out.size());
            writeLine(out, pageObjNum + " 0 obj");
            writeLine(out, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + (int) PAGE_WIDTH + " " + (int) PAGE_HEIGHT + "] "
                    + "/Resources << /Font << /F1 " + fontF1 + " 0 R /F2 " + fontF2 + " 0 R >> >> "
                    + "/Contents " + contentObjNum + " 0 R >>");
            writeLine(out, "endobj");

            // Content stream object
            offsets.add((long) out.size());
            writeLine(out, contentObjNum + " 0 obj");
            writeLine(out, "<< /Length " + contentBytes.length + " >>");
            writeLine(out, "stream");
            writeBytes(out, contentBytes);
            if (contentBytes.length == 0 || contentBytes[contentBytes.length - 1] != '\n') {
                writeLine(out, "");
            }
            writeLine(out, "endstream");
            writeLine(out, "endobj");
        }

        long xrefOffset = out.size();
        writeLine(out, "xref");
        writeLine(out, "0 " + (totalObjects + 1));
        writeLine(out, "0000000000 65535 f ");
        for (long offset : offsets) {
            writeLine(out, String.format(Locale.ROOT, "%010d 00000 n ", offset));
        }

        writeLine(out, "trailer");
        writeLine(out, "<< /Size " + (totalObjects + 1) + " /Root 1 0 R >>");
        writeLine(out, "startxref");
        writeLine(out, String.valueOf(xrefOffset));
        writeLine(out, "%%EOF");

        return out.toByteArray();
    }

    /** Hard-wraps any line too long for the page width so text can never run off the right edge. */
    private static List<Line> wrapLines(List<Line> lines) {
        List<Line> wrapped = new ArrayList<>();
        for (Line line : lines) {
            String text = line.text() == null ? "" : line.text();
            int max = line.bold() ? MAX_CHARS_BOLD : MAX_CHARS_NORMAL;
            if (text.length() <= max) {
                wrapped.add(line);
                continue;
            }
            int start = 0;
            boolean first = true;
            while (start < text.length()) {
                int end = Math.min(start + max, text.length());
                if (end < text.length()) {
                    int space = text.lastIndexOf(' ', end);
                    if (space > start) end = space;
                }
                String chunk = text.substring(start, end).strip();
                if (!chunk.isEmpty()) {
                    wrapped.add(new Line(first ? chunk : "  " + chunk, line.bold(), first && line.extraSpaceBefore()));
                    first = false;
                }
                start = end;
                while (start < text.length() && text.charAt(start) == ' ') start++;
            }
        }
        return wrapped;
    }

    private static List<List<Line>> paginate(List<Line> lines) {
        List<List<Line>> pages = new ArrayList<>();
        List<Line> current = new ArrayList<>();
        double y = MARGIN_TOP;

        for (Line line : lines) {
            double gap = line.extraSpaceBefore() ? EXTRA_GAP : 0;
            if (y - gap - LINE_HEIGHT < MARGIN_BOTTOM) {
                pages.add(current);
                current = new ArrayList<>();
                y = MARGIN_TOP;
            }
            y -= gap;
            current.add(line);
            y -= LINE_HEIGHT;
        }
        if (!current.isEmpty() || pages.isEmpty()) {
            pages.add(current);
        }
        return pages;
    }

    private static String buildContentStream(List<Line> pageLines) {
        StringBuilder cs = new StringBuilder();
        cs.append("BT\n");
        double y = MARGIN_TOP;
        for (Line line : pageLines) {
            if (line.extraSpaceBefore()) y -= EXTRA_GAP;
            String font = line.bold() ? "/F2 " + fmt(BOLD_SIZE) : "/F1 " + fmt(NORMAL_SIZE);
            cs.append(font).append(" Tf\n");
            cs.append("1 0 0 1 ").append(fmt(MARGIN_LEFT)).append(' ').append(fmt(y)).append(" Tm\n");
            cs.append('(').append(escape(line.text())).append(") Tj\n");
            y -= LINE_HEIGHT;
        }
        cs.append("ET\n");
        return cs.toString();
    }

    private static String fmt(double value) {
        if (value == Math.floor(value)) return String.valueOf((long) value);
        return String.valueOf(value);
    }

    private static String escape(String text) {
        if (text == null) return "";
        CharsetEncoder encoder = WIN_1252.newEncoder();
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < 32) {
                sb.append(' ');                                   // newline, tab, other control chars
            } else if (c < 128) {
                switch (c) {
                    case '\\' -> sb.append("\\\\");
                    case '(' -> sb.append("\\(");
                    case ')' -> sb.append("\\)");
                    default -> sb.append(c);
                }
            } else if (encoder.canEncode(c)) {
                // WinAnsi byte for this character (bullet, curly quotes, dashes, accented Latin ...);
                // the content stream is written as ISO-8859-1, so (char) of the byte round-trips it.
                sb.append((char) (String.valueOf(c).getBytes(WIN_1252)[0] & 0xFF));
            } else {
                sb.append('?');                                   // not representable in the base-14 fonts
            }
        }
        return sb.toString();
    }

    private static void writeLine(ByteArrayOutputStream out, String line) throws IOException {
        out.write(line.getBytes(StandardCharsets.ISO_8859_1));
        out.write('\n');
    }

    private static void writeBytes(ByteArrayOutputStream out, byte[] bytes) throws IOException {
        out.write(bytes);
    }
}
