package com.placement.services.resume;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Zero-dependency .docx (Office Open XML) text extractor.
 *
 * A .docx file is a ZIP archive; the body lives in "word/document.xml". ZIP
 * reading (java.util.zip) and streaming XML parsing (javax.xml.stream, StAX)
 * are both in the JDK, so no Apache POI is needed.
 *
 * Hardening, because the file is untrusted user input:
 *   - Streaming (StAX) parse instead of a DOM: memory stays flat and there is
 *     no recursion, so a deeply nested document cannot overflow the stack.
 *   - DTDs and external entities are disabled (XXE / "billion laughs").
 *   - The decompressed size of document.xml is capped, and so is the total
 *     amount of data inflated while walking the ZIP, so a tiny "zip bomb"
 *     upload cannot exhaust memory or CPU.
 *   - Any failure returns "" (the service layer treats that as "nothing parsed").
 *
 * Word stores text-box content twice (mc:Choice AND mc:Fallback); the Fallback
 * copy is skipped so such text is not duplicated.
 *
 * Package-private: only used internally by {@link ResumeParserService}.
 */
final class DocxTextExtractor {

    private static final int MAX_DOCUMENT_XML_BYTES = 8 * 1024 * 1024;
    private static final long MAX_TOTAL_INFLATED_BYTES = 64L * 1024 * 1024;
    private static final int MAX_TEXT_CHARS = 1_000_000;

    private DocxTextExtractor() {}

    static String extract(byte[] docxBytes) throws IOException {
        byte[] documentXml = readZipEntry(docxBytes, "word/document.xml");
        if (documentXml == null) {
            return "";   // not a docx package, unexpected structure, or over the size limits
        }
        try {
            return extractText(documentXml);
        } catch (Exception e) {
            return "";   // malformed XML etc. - never let a bad file crash the caller
        }
    }

    /** Returns the entry's bytes, or null if absent / too large / the archive inflates suspiciously much. */
    private static byte[] readZipEntry(byte[] zipBytes, String entryName) throws IOException {
        long inflated = 0;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            byte[] scratch = new byte[8192];
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(entryName)) {
                    return readCapped(zis, MAX_DOCUMENT_XML_BYTES);
                }
                int read;   // not the entry we want: drain it, but within a global budget
                while ((read = zis.read(scratch)) != -1) {
                    inflated += read;
                    if (inflated > MAX_TOTAL_INFLATED_BYTES) return null;
                }
            }
        }
        return null;
    }

    private static byte[] readCapped(InputStream in, int max) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long total = 0;
        int read;
        while ((read = in.read(buffer)) != -1) {
            total += read;
            if (total > max) return null;
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    /**
     * Collects every &lt;w:t&gt; run's text, a newline at each paragraph end,
     * a tab for &lt;w:tab/&gt; and a newline for &lt;w:br/&gt; / &lt;w:cr/&gt;, so section
     * headers and bullet lines stay on their own lines for the section detector.
     */
    private static String extractText(byte[] xmlBytes) throws Exception {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);

        XMLStreamReader reader = factory.createXMLStreamReader(new ByteArrayInputStream(xmlBytes));
        StringBuilder sb = new StringBuilder();
        boolean inText = false;
        int fallbackDepth = 0;
        try {
            while (reader.hasNext() && sb.length() < MAX_TEXT_CHARS) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    String name = reader.getLocalName();
                    if (fallbackDepth > 0) {
                        fallbackDepth++;
                    } else if ("Fallback".equals(name)) {
                        fallbackDepth = 1;
                    } else if ("t".equals(name)) {
                        inText = true;
                    } else if ("tab".equals(name)) {
                        sb.append('\t');
                    } else if ("br".equals(name) || "cr".equals(name)) {
                        sb.append('\n');
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    String name = reader.getLocalName();
                    if (fallbackDepth > 0) {
                        fallbackDepth--;
                    } else if ("t".equals(name)) {
                        inText = false;
                    } else if ("p".equals(name)) {
                        sb.append('\n');
                    }
                } else if ((event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA)
                        && inText && fallbackDepth == 0) {
                    sb.append(reader.getText());
                }
            }
        } finally {
            reader.close();
        }
        return sb.toString();
    }
}
