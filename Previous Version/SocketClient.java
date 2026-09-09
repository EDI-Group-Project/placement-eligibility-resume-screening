package com.placement.sockets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class SocketClient {

    private final String host;
    private final int port;

    public SocketClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public SocketClient() {
        this("localhost", 5000);
    }

    public static class Response {
        public final boolean success;
        public final String payload;

        Response(boolean success, String payload) {
            this.success = success;
            this.payload = payload;
        }

        public String[] parts() {
            return payload.isEmpty() ? new String[0] : payload.split("\\|");
        }
    }

    public static class ListResponse {
        public final boolean success;
        public final String errorMessage;
        public final List<String> lines;

        private ListResponse(boolean success, String errorMessage, List<String> lines) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.lines = lines;
        }
    }

    public Response sendRequest(String command, String... params) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 5000);
            socket.setSoTimeout(10000);

            try (PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                writer.println(command);
                for (String p : params) writer.println(p);

                String line = reader.readLine();
                if (line == null) {
                    return new Response(false, "Server closed the connection without a response.");
                }
                String[] split = line.split("\\|", 2);
                boolean success = "SUCCESS".equals(split[0]);
                String payload = split.length > 1 ? split[1] : "";
                if (!success && payload.isEmpty()) payload = "Request failed.";
                return new Response(success, payload);
            }
        }
    }

    public ListResponse sendListRequest(String command, String... params) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 5000);
            socket.setSoTimeout(10000);

            try (PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                writer.println(command);
                for (String p : params) writer.println(p);

                String status = reader.readLine();
                if (status == null) {
                    return new ListResponse(false, "Server closed the connection without a response.", List.of());
                }
                if (status.startsWith("FAILED")) {
                    String[] split = status.split("\\|", 2);
                    return new ListResponse(false, split.length > 1 ? split[1] : "Request failed.", List.of());
                }
                if (!status.equals("SUCCESS")) {
                    return new ListResponse(false, "Unexpected response: " + status, List.of());
                }

                String countLine = reader.readLine();
                if (countLine == null) {
                    return new ListResponse(false, "Server closed connection while sending count.", List.of());
                }
                int count = Integer.parseInt(countLine.trim());
                List<String> lines = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    lines.add(reader.readLine());
                }
                reader.readLine(); // consume "END"
                return new ListResponse(true, null, lines);
            }
        }
    }
}
