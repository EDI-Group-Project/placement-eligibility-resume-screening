package com.placement.services.resume;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Zero-dependency, BEST-EFFORT PDF text extractor for resumes.
 *
 * What it does:
 *   - Finds every "stream ... endstream" object, reads the stream's OWN
 *     dictionary (from its "N G obj" marker to the "stream" keyword - not an
 *     arbitrary 2000-byte window that could belong to a neighbouring object).
 *   - Skips streams that cannot hold page text: images, fonts, xref/object
 *     streams, metadata, embedded files, and filters we do not implement.
 *   - Inflates /FlateDecode streams with java.util.zip.Inflater, with hard
 *     size caps so a small crafted file cannot exhaust server memory.
 *   - Walks the content-stream syntax with a small tokenizer and extracts the
 *     literal strings shown by Tj / TJ / ' / ". Line breaks are rebuilt from
 *     the text position (Td / TD / Tm / T* and the CTM translation), and the
 *     tiny kerning adjustments inside TJ arrays are NOT turned into spaces
 *     (only large negative gaps are) - so "(Sk)5(ills)" becomes "Skills", not
 *     "Sk ills". Octal escapes such as \225 (WinAnsi bullet) are decoded.
 *
 * KNOWN LIMITATIONS (return "" rather than garbage, never throw):
 *   - Type0 / CID fonts that show text as hex strings (&lt;0041...&gt;) need a
 *     ToUnicode CMap to decode; this class skips hex strings. PDFs exported by
 *     some tools (Google Docs, Canva, Chrome "Print to PDF") may therefore
 *     yield no text. Scanned/image-only PDFs likewise have no text.
 *   - Encrypted PDFs and streams using ASCII85/LZW/RunLength filters are skipped.
 *   Production upgrade path: Apache PDFBox. ResumeParserService treats "" as
 *   "nothing readable", and the ATS feedback tells the student what to do.
 *
 * Package-private: only used internally by {@link ResumeParserService}.
 */
final class PdfTextExtractor {

    // ---- resource limits: a resume upload is untrusted input ----
    private static final int MAX_PDF_BYTES = 10 * 1024 * 1024;
    private static final int MAX_STREAM_OUTPUT_BYTES = 8 * 1024 * 1024;       // per decompressed stream
    private static final long MAX_TOTAL_DECODED_BYTES = 24L * 1024 * 1024;    // across all streams
    private static final int MAX_TEXT_CHARS = 1_000_000;

    private static final double LINE_TOLERANCE = 2.0;      // y-change (points) that counts as a new line
    private static final double WORD_GAP_KERN = -180;      // TJ adjustment (1/1000 em) treated as a word space
    private static final double DEFAULT_LEADING = 12.0;

    private static final byte[] STREAM_KW = "stream".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] ENDSTREAM_KW = "endstream".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PDF_KW = "%PDF".getBytes(StandardCharsets.US_ASCII);
    private static final Charset WIN_1252 = Charset.forName("windows-1252");

    private static final Pattern IMAGE_SUBTYPE = Pattern.compile("/Subtype\\s*/Image(?![A-Za-z])");
    /** Stream-dictionary markers meaning "this stream is not plain page content we can read". */
    private static final String[] SKIP_MARKERS = {
            "/XRef", "/ObjStm", "/Length1", "/Length2", "/Metadata", "/EmbeddedFile", "/Predictor",
            "/ASCII85Decode", "/LZWDecode", "/RunLengthDecode", "/DCTDecode", "/CCITTFaxDecode",
            "/JBIG2Decode", "/JPXDecode", "/Crypt"
    };

    private PdfTextExtractor() {}

    static String extract(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0 || pdfBytes.length > MAX_PDF_BYTES) return "";
        int header = indexOf(pdfBytes, PDF_KW, 0);
        if (header < 0 || header > 1024) return "";   // not a PDF

        StringBuilder out = new StringBuilder();
        long totalDecoded = 0;
        int searchFrom = 0;

        while (out.length() < MAX_TEXT_CHARS) {
            int streamKw = indexOf(pdfBytes, STREAM_KW, searchFrom);
            if (streamKw < 0) break;
            int endstream = indexOf(pdfBytes, ENDSTREAM_KW, streamKw + STREAM_KW.length);
            if (endstream < 0) break;
            searchFrom = endstream + ENDSTREAM_KW.length;

            String dict = dictionaryBefore(pdfBytes, streamKw);
            if (shouldSkip(dict)) continue;

            int start = streamKw + STREAM_KW.length;
            if (start < pdfBytes.length && pdfBytes[start] == '\r') start++;
            if (start < pdfBytes.length && pdfBytes[start] == '\n') start++;
            int end = endstream;
            while (end > start && (pdfBytes[end - 1] == '\n' || pdfBytes[end - 1] == '\r')) end--;
            if (end <= start) continue;

            byte[] raw = Arrays.copyOfRange(pdfBytes, start, end);
            byte[] decoded = dict.contains("/FlateDecode") ? inflateCapped(raw, MAX_STREAM_OUTPUT_BYTES) : raw;
            if (decoded == null) continue;

            totalDecoded += decoded.length;
            if (totalDecoded > MAX_TOTAL_DECODED_BYTES) break;

            String content = new String(decoded, WIN_1252);
            if (!hasOperator(content, "BT")) continue;   // not a page/form content stream

            scanContentStream(content, out);
            out.append('\n');
        }
        return normalize(out);
    }

    // ------------------------------------------------------------------
    // stream selection
    // ------------------------------------------------------------------

    /** The dictionary of the object whose stream starts at {@code streamKw}: text after the LAST "obj" before it. */
    private static String dictionaryBefore(byte[] pdf, int streamKw) {
        int from = Math.max(0, streamKw - 4000);
        String window = new String(pdf, from, streamKw - from, StandardCharsets.ISO_8859_1);
        int obj = window.lastIndexOf("obj");
        return obj >= 0 ? window.substring(obj) : window;
    }

    private static boolean shouldSkip(String dict) {
        if (IMAGE_SUBTYPE.matcher(dict).find()) return true;
        for (String marker : SKIP_MARKERS) {
            if (dict.contains(marker)) return true;
        }
        return false;
    }

    private static byte[] inflateCapped(byte[] compressed, int maxOutput) {
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(compressed);
            ByteArrayOutputStream out = new ByteArrayOutputStream(Math.min(maxOutput, Math.max(256, compressed.length * 3)));
            byte[] buffer = new byte[8192];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                if (count == 0) break;                       // needs input / dictionary: use what we have
                out.write(buffer, 0, count);
                if (out.size() > maxOutput) return null;     // decompression bomb: skip this stream
            }
            return out.toByteArray();
        } catch (DataFormatException e) {
            return null;                                     // not really Flate data / corrupt
        } finally {
            inflater.end();
        }
    }

    // ------------------------------------------------------------------
    // content-stream scanning
    // ------------------------------------------------------------------

    private static final class State {
        double curY, ctmY, leading;
        double lastEmitY = Double.NaN;
        boolean inArray, arrayHasText, pendingSpace;
        int dictDepth;
        final double[] nums = new double[6];   // last numeric operands; nums[5] is the most recent
        int numCount;
        final double[] ctmStack = new double[32];
        int ctmDepth;

        void pushNum(double v) {
            System.arraycopy(nums, 1, nums, 0, 5);
            nums[5] = v;
            if (numCount < 6) numCount++;
        }
    }

    private static void scanContentStream(String s, StringBuilder out) {
        State st = new State();
        int n = s.length();
        int i = 0;
        while (i < n && out.length() < MAX_TEXT_CHARS) {
            char c = s.charAt(i);
            if (c == '(') {
                i = readLiteral(s, i, st, out);
            } else if (c == '<') {
                if (i + 1 < n && s.charAt(i + 1) == '<') {
                    st.dictDepth++;
                    i += 2;
                } else {                                     // hex string: skipped (see class note)
                    int close = s.indexOf('>', i + 1);
                    i = close < 0 ? n : close + 1;
                }
            } else if (c == '>') {
                if (i + 1 < n && s.charAt(i + 1) == '>') {
                    if (st.dictDepth > 0) st.dictDepth--;
                    i += 2;
                } else {
                    i++;
                }
            } else if (c == '[') {
                st.inArray = true;
                st.arrayHasText = false;
                st.pendingSpace = false;
                i++;
            } else if (c == ']') {
                st.inArray = false;
                i++;
            } else if (c == '/') {                           // name: skip it
                i++;
                while (i < n && !isWhitespaceOrDelimiter(s.charAt(i))) i++;
            } else if (c == '%') {                           // comment
                while (i < n && s.charAt(i) != '\n' && s.charAt(i) != '\r') i++;
            } else if (isWhitespaceOrDelimiter(c)) {
                i++;
            } else {
                int start = i;
                while (i < n && !isWhitespaceOrDelimiter(s.charAt(i))) i++;
                String token = s.substring(start, i);
                if (token.equals("ID")) {                    // inline image: binary data up to EI
                    i = inlineImageEnd(s, i);
                } else {
                    handleToken(token, st);
                }
            }
        }
    }

    private static void handleToken(String tok, State st) {
        char f = tok.charAt(0);
        if (f == '+' || f == '-' || f == '.' || (f >= '0' && f <= '9')) {
            try {
                double v = Double.parseDouble(tok);
                st.pushNum(v);
                if (st.inArray && st.arrayHasText && v < WORD_GAP_KERN) st.pendingSpace = true;
                return;
            } catch (NumberFormatException ignored) {
                // not a number after all - fall through and treat as an (unknown) operator
            }
        }
        switch (tok) {
            case "BT" -> st.curY = 0;
            case "Td" -> { if (st.numCount >= 2) st.curY += st.nums[5]; }
            case "TD" -> { if (st.numCount >= 2) { st.curY += st.nums[5]; st.leading = -st.nums[5]; } }
            case "Tm" -> { if (st.numCount >= 6) st.curY = st.nums[5]; }
            case "T*" -> st.curY -= (st.leading != 0 ? st.leading : DEFAULT_LEADING);
            case "TL" -> { if (st.numCount >= 1) st.leading = st.nums[5]; }
            case "q" -> { if (st.ctmDepth < st.ctmStack.length) st.ctmStack[st.ctmDepth++] = st.ctmY; }
            case "Q" -> { if (st.ctmDepth > 0) st.ctmY = st.ctmStack[--st.ctmDepth]; }
            case "cm" -> { if (st.numCount >= 6) st.ctmY += st.nums[5]; }
            default -> { /* other operators do not affect text layout */ }
        }
        st.numCount = 0;
    }

    /** Reads one literal string starting at '(' and emits it; returns the index just after the closing ')'. */
    private static int readLiteral(String s, int i, State st, StringBuilder out) {
        int n = s.length();
        StringBuilder lit = new StringBuilder();
        int depth = 1;
        i++;
        while (i < n && depth > 0) {
            char ch = s.charAt(i);
            if (ch == '\\') {
                if (i + 1 >= n) { i++; break; }
                char nx = s.charAt(i + 1);
                if (nx >= '0' && nx <= '7') {                // octal escape: up to 3 digits
                    int val = 0, digits = 0, j = i + 1;
                    while (j < n && digits < 3 && s.charAt(j) >= '0' && s.charAt(j) <= '7') {
                        val = val * 8 + (s.charAt(j) - '0');
                        j++;
                        digits++;
                    }
                    lit.append(byteToChar(val & 0xFF));
                    i = j;
                } else {
                    switch (nx) {
                        case 'n', 'r', 't', 'b', 'f' -> lit.append(' ');
                        case '\r' -> { if (i + 2 < n && s.charAt(i + 2) == '\n') i++; }   // line continuation
                        case '\n' -> { }
                        default -> lit.append(nx);           // \( \) \\ and unknown escapes
                    }
                    i += 2;
                }
            } else if (ch == '(') {
                depth++;
                lit.append(ch);
                i++;
            } else if (ch == ')') {
                depth--;
                i++;
                if (depth > 0) lit.append(ch);
            } else {
                lit.append(ch);
                i++;
            }
        }

        if (st.dictDepth > 0) return i;                       // strings inside << >> (e.g. /ActualText) are metadata

        // The ' and " operators come AFTER their string but move to the next line BEFORE showing it.
        int k = i;
        while (k < n && Character.isWhitespace(s.charAt(k))) k++;
        if (k < n && (s.charAt(k) == '\'' || s.charAt(k) == '"')) {
            st.curY -= (st.leading != 0 ? st.leading : DEFAULT_LEADING);
        }

        emit(lit, st, out);
        return i;
    }

    private static void emit(CharSequence text, State st, StringBuilder out) {
        if (text.length() == 0) return;
        if (!st.inArray || !st.arrayHasText) {
            startShow(st, out);                               // Tj, or the first piece of a TJ array
        } else if (st.pendingSpace) {
            if (out.length() > 0 && out.charAt(out.length() - 1) != ' ') out.append(' ');
        }
        st.pendingSpace = false;
        out.append(text);
        if (st.inArray) st.arrayHasText = true;
    }

    /** Decides how the next shown text relates to the previous one: new line, same line (space), or start. */
    private static void startShow(State st, StringBuilder out) {
        double y = st.ctmY + st.curY;
        if (out.length() > 0) {
            char last = out.charAt(out.length() - 1);
            if (!Double.isNaN(st.lastEmitY) && Math.abs(y - st.lastEmitY) > LINE_TOLERANCE) {
                while (out.length() > 0 && out.charAt(out.length() - 1) == ' ') out.setLength(out.length() - 1);
                if (out.length() > 0 && out.charAt(out.length() - 1) != '\n') out.append('\n');
            } else if (last != ' ' && last != '\n') {
                out.append(' ');
            }
        }
        st.lastEmitY = y;
    }

    private static char byteToChar(int b) {
        if (b < 0x80 || b > 0x9F) return (char) b;
        return new String(new byte[]{(byte) b}, WIN_1252).charAt(0);   // 0x95 -> bullet, 0x92 -> apostrophe, ...
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static int inlineImageEnd(String s, int from) {
        int idx = from;
        while ((idx = s.indexOf("EI", idx)) >= 0) {
            boolean before = idx > 0 && Character.isWhitespace(s.charAt(idx - 1));
            boolean after = idx + 2 >= s.length() || Character.isWhitespace(s.charAt(idx + 2));
            if (before && after) return idx + 2;
            idx += 2;
        }
        return s.length();
    }

    private static boolean hasOperator(String s, String op) {
        int from = 0;
        while ((from = s.indexOf(op, from)) >= 0) {
            int after = from + op.length();
            boolean beforeOk = from == 0 || isWhitespaceOrDelimiter(s.charAt(from - 1));
            boolean afterOk = after >= s.length() || isWhitespaceOrDelimiter(s.charAt(after));
            if (beforeOk && afterOk) return true;
            from = after;
        }
        return false;
    }

    private static boolean isWhitespaceOrDelimiter(char c) {
        return Character.isWhitespace(c) || c == '\0' || "()<>[]{}/%".indexOf(c) >= 0;
    }

    /** Collapses runs of spaces, drops control characters, trims trailing spaces per line, caps blank lines. */
    private static String normalize(CharSequence in) {
        StringBuilder sb = new StringBuilder(in.length());
        boolean lastSpace = false;
        int newlines = 0;
        for (int i = 0; i < in.length(); i++) {
            char c = in.charAt(i);
            if (c == '\n') {
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ' ') sb.setLength(sb.length() - 1);
                if (newlines < 2) sb.append('\n');
                newlines++;
                lastSpace = false;
            } else if (c == ' ' || c == '\t' || c == '\u00A0') {
                if (!lastSpace && sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n') sb.append(' ');
                lastSpace = true;
            } else if (c < 32 || c == '\uFFFD') {
                // control byte / ligature code / undefined cp1252 byte: drop
            } else {
                sb.append(c);
                lastSpace = false;
                newlines = 0;
            }
        }
        return sb.toString().strip();
    }

    private static int indexOf(byte[] haystack, byte[] needle, int fromIndex) {
        int max = haystack.length - needle.length;
        outer:
        for (int i = Math.max(fromIndex, 0); i <= max; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) continue outer;
            }
            return i;
        }
        return -1;
    }
}
