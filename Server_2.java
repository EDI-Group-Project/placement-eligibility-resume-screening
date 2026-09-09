package com.placement.sockets;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server_2 {

    private static final int PORT = 5000;
    private static final int THREAD_POOL_SIZE = 10;

    public static void main(String[] args) {
        System.out.println("[SERVER] Starting Placement Portal Backend Server on port " + PORT + "...");
        
        // Initialize mock data layer
        DataStore_2.init();

        ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[SERVER] Listening for UI client connections...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[SERVER] New client connected: " + clientSocket.getInetAddress());
                threadPool.execute(new ClientHandler_2(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("[SERVER ERROR] Critical failure in socket server: " + e.getMessage());
        } finally {
            threadPool.shutdown();
        }
    }
}
