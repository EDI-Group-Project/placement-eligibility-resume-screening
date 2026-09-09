package com.placement.sockets;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory session store. A token is issued on successful LOGIN and must
 * be sent as the first parameter on every protected command afterward.
 * Without this, the server has no way to know a client ever logged in,
 * since every request opens a brand-new socket.
 */
public class SessionManager {
    private static final Map<String, String> TOKEN_TO_ROLE = new ConcurrentHashMap<>();

    public static String createSession(String role) {
        String token = UUID.randomUUID().toString();
        TOKEN_TO_ROLE.put(token, role);
        return token;
    }

    public static boolean isValid(String token) {
        return token != null && TOKEN_TO_ROLE.containsKey(token);
    }

    public static String getRole(String token) {
        return TOKEN_TO_ROLE.get(token);
    }
}
