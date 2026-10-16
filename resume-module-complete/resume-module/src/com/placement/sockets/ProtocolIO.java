package com.placement.sockets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/** Small helpers shared by the command handlers (same line-based protocol as the rest of the server). */
final class ProtocolIO {

    static final int MAX_SHORT_LINE = 200;

    /** Thrown when a client sends a line longer than allowed. */
    static final class LineTooLongException extends IOException {
        private static final long serialVersionUID = 1L;

        LineTooLongException() { super("Request line too long."); }
    }

    private ProtocolIO() {}

    /**
     * Like BufferedReader.readLine(), but refuses lines longer than {@code maxChars} instead of buffering an
     * endless line in memory. Returns null at end of stream.
     */
    static String readBoundedLine(BufferedReader reader, int maxChars) throws IOException {
        StringBuilder sb = new StringBuilder();
        int ch;
        while ((ch = reader.read()) != -1) {
            if (ch == '\n') return sb.toString();
            if (ch != '\r') {
                sb.append((char) ch);
                if (sb.length() > maxChars) throw new LineTooLongException();
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    /** Optional short parameter: a missing or blank line is the empty string. */
    static String readOptional(BufferedReader reader) throws IOException {
        String line = readBoundedLine(reader, MAX_SHORT_LINE);
        return line == null ? "" : line.trim();
    }

    /** SocketClient.sendListRequest format: SUCCESS / count / lines / END. */
    static void writeList(PrintWriter writer, List<String> lines) {
        writer.println("SUCCESS");
        writer.println(lines.size());
        for (String line : lines) writer.println(line);
        writer.println("END");
    }

    /** One value inside a '|'-separated protocol line: no separators and no line breaks. */
    static String field(String value) {
        return value == null ? "" : value.replace('|', '/').replace('\r', ' ').replace('\n', ' ');
    }
}
