package com.placement.integration;

import com.placement.sockets.SocketClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Launches 15 simultaneous clients performing a mix of operations, to prove
 * the server (with its 15-thread pool) handles them concurrently without
 * blocking or crashing.
 *
 * NOTE: Every client authenticates first, since protected commands
 * (GET_STUDENTS, GET_ELIGIBLE_STUDENTS) now require a session token from
 * SessionManager. Replace the placeholder credentials below with a real
 * seeded TPC/TPO test account before running.
 *
 * How to run:
 *   1. Start Server.java first (it must already be listening on port 5000).
 *   2. Compile: mvn -q test-compile
 *   3. Run: mvn -q exec:java -Dexec.classpathScope=test -Dexec.mainClass=com.placement.integration.MultiClientTest
 *   4. Watch the server console print "Client connected" 15 times in quick
 *      succession, and this program print all 15 results plus latency stats.
 */
public class MultiClientTest {

    // Uses the TPC account seeded by sql/02_seed.sql.
    private static final String TEST_ROLE = "TPC";
    private static final String TEST_EMAIL = "neha@college.com";
    private static final String TEST_PASSWORD = "pass123";

    public static void main(String[] args) throws InterruptedException {
        int clientCount = 15;
        CountDownLatch latch = new CountDownLatch(clientCount);
        List<Thread> threads = new ArrayList<>();
        AtomicLong totalLatency = new AtomicLong(0);

        for (int i = 1; i <= clientCount; i++) {
            final int id = i;
            Thread t = new Thread(() -> {
                try {
                    long clientTime = runOneClient(id);
                    totalLatency.addAndGet(clientTime);
                } finally {
                    latch.countDown();
                }
            });
            threads.add(t);
        }

        long start = System.currentTimeMillis();
        threads.forEach(Thread::start);
        latch.await();
        long elapsed = System.currentTimeMillis() - start;

        System.out.println("\nAll " + clientCount + " clients finished in " + elapsed + " ms.");
        System.out.println("Average per-client round-trip latency: "
                + (totalLatency.get() / clientCount) + " ms.");
    }

    /** Returns this client's total round-trip time in ms, for latency reporting. */
    private static long runOneClient(int id) {
        SocketClient client = new SocketClient();
        long clientStart = System.currentTimeMillis();
        try {
            // Every client logs in first — this is required now that GET_STUDENTS
            // and GET_ELIGIBLE_STUDENTS enforce SessionManager tokens.
            SocketClient.Response login = client.sendRequest("LOGIN", TEST_ROLE, TEST_EMAIL, TEST_PASSWORD);
            if (!login.success) {
                System.out.println("[Client " + id + "] LOGIN failed -> " + login.payload);
                return System.currentTimeMillis() - clientStart;
            }

            String[] loginParts = login.parts();
            String token = loginParts.length > 0 ? loginParts[0] : null;
            String displayName = loginParts.length > 1 ? loginParts[1] : "";
            System.out.println("[Client " + id + "] LOGIN -> success | " + displayName);

            switch (id % 4) {
                case 0 -> {
                    // Login itself is this branch's demonstration — nothing further needed.
                }
                case 1 -> {
                    SocketClient.ListResponse r = client.sendListRequest("GET_JOBS");
                    System.out.println("[Client " + id + "] GET_JOBS -> success=" + r.success
                            + " rows=" + r.lines.size());
                }
                case 2 -> {
                    SocketClient.ListResponse r = client.sendListRequest("GET_STUDENTS", token);
                    System.out.println("[Client " + id + "] GET_STUDENTS -> success=" + r.success
                            + " rows=" + r.lines.size());
                }
                case 3 -> {
                    SocketClient.ListResponse r = client.sendListRequest("GET_ELIGIBLE_STUDENTS", token, "JOB001");
                    System.out.println("[Client " + id + "] GET_ELIGIBLE_STUDENTS(JOB001) -> success=" + r.success
                            + " rows=" + r.lines.size());
                }
            }
        } catch (Exception e) {
            System.out.println("[Client " + id + "] ERROR: " + e.getMessage());
        }
        return System.currentTimeMillis() - clientStart;
    }
}
