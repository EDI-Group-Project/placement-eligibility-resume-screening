package com.placement.sockets;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private static final int PORT = 5000;
    private static final int POOL_SIZE = 15;
    private static final int CLIENT_TIMEOUT_MS = 15000;

    public static void main(String[] args) {

        ExecutorService pool = Executors.newFixedThreadPool(POOL_SIZE);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            System.out.println("Placement Eligibility Portal server listening on port " + PORT);
            System.out.println("Thread pool size: " + POOL_SIZE);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down server...");
                pool.shutdown();
            }));

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    clientSocket.setSoTimeout(CLIENT_TIMEOUT_MS);
                    System.out.println("Client connected: " + clientSocket.getRemoteSocketAddress());
                    pool.execute(new ClientHandler(clientSocket));
                } catch (IOException e) {
                    System.out.println("Error accepting a client: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        } finally {
            pool.shutdown();
        }
    }
}
