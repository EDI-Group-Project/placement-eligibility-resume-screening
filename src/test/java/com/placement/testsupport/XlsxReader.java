package com.placement.testsupport;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Test helper: reads an .xlsx back with nothing but the JDK (zip + DOM), so the tests do not trust the
 * code that wrote the file.
 */
public final class XlsxReader {

    public static final String MAIN_NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";

    private XlsxReader() { }

    /** Every part of the package, by name. */
    public static Map<String, byte[]> parts(byte[] xlsx) throws IOException {
        Map<String, byte[]> out = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(xlsx))) {
            for (ZipEntry e; (e = zip.getNextEntry()) != null; ) out.put(e.getName(), zip.readAllBytes());
        }
        return out;
    }

    /** Strict XML parse (no DOCTYPE allowed): throws when the part is not well-formed. */
    public static Document xml(byte[] part) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return f.newDocumentBuilder().parse(new ByteArrayInputStream(part));
    }

    public static Document sheet(byte[] xlsx) throws Exception {
        return xml(parts(xlsx).get("xl/worksheets/sheet1.xml"));
    }

    /** The first sheet as rows of text, every row as wide as the header row. A missing cell reads "", a numeric cell its stored text. */
    public static List<List<String>> rows(byte[] xlsx) throws Exception {
        NodeList rowNodes = sheet(xlsx).getElementsByTagNameNS(MAIN_NS, "row");
        List<List<String>> rows = new ArrayList<>();
        for (int r = 0; r < rowNodes.getLength(); r++) {
            Map<Integer, String> cells = new LinkedHashMap<>();
            NodeList cs = ((Element) rowNodes.item(r)).getElementsByTagNameNS(MAIN_NS, "c");
            for (int c = 0; c < cs.getLength(); c++) {
                Element cell = (Element) cs.item(c);
                cells.put(columnIndex(cell.getAttribute("r")), text(cell));
            }
            rows.add(fill(cells));
        }
        int width = rows.isEmpty() ? 0 : rows.get(0).size();
        for (List<String> row : rows) while (row.size() < width) row.add("");
        return rows;
    }

    /** The cell element at a reference like "B3", or null when the cell is empty. */
    public static Element cell(byte[] xlsx, String ref) throws Exception {
        NodeList cs = sheet(xlsx).getElementsByTagNameNS(MAIN_NS, "c");
        for (int i = 0; i < cs.getLength(); i++) {
            if (ref.equals(((Element) cs.item(i)).getAttribute("r"))) return (Element) cs.item(i);
        }
        return null;
    }

    public static String text(Element cell) {
        NodeList is = cell.getElementsByTagNameNS(MAIN_NS, "is");
        if (is.getLength() > 0) return is.item(0).getTextContent();
        NodeList v = cell.getElementsByTagNameNS(MAIN_NS, "v");
        return v.getLength() > 0 ? v.item(0).getTextContent() : "";
    }

    private static List<String> fill(Map<Integer, String> cells) {
        int max = cells.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1);
        List<String> out = new ArrayList<>();
        for (int i = 0; i <= max; i++) out.add(cells.getOrDefault(i, ""));
        return out;
    }

    /** "A1" -> 0, "Z9" -> 25, "AA1" -> 26. */
    private static int columnIndex(String ref) {
        int index = 0;
        for (int i = 0; i < ref.length() && Character.isLetter(ref.charAt(i)); i++) index = index * 26 + (ref.charAt(i) - 'A' + 1);
        return index - 1;
    }
}
