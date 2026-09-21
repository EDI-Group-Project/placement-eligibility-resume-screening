package com.placement.sockets;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory session store. A token is issued on successful LOGIN and must
 * be sent as the first parameter on every protected command afterward.
 * Updated to store accountType + accountId per session, since ADD_JOB now
 * needs to know which faculty account created a job posting.
 */
public class SessionManager {

    private static final Map<String, SessionInfo> SESSIONS = new ConcurrentHashMap<>();

    private static final class SessionInfo {
        final String accountType;
        final String displayName;
        final int accountId;

        SessionInfo(String accountType, String displayName, int accountId) {
            this.accountType = accountType;
            this.displayName = displayName;
            this.accountId = accountId;
        }
    }

    public static String createSession(String accountType, String displayName, int accountId) {
        String token = UUID.randomUUID().toString();
        SESSIONS.put(token, new SessionInfo(accountType, displayName, accountId));
        return token;
    }

    public static boolean isValid(String token) {
        return token != null && SESSIONS.containsKey(token);
    }

    public static String getAccountType(String token) {
        SessionInfo info = SESSIONS.get(token);
        return info == null ? null : info.accountType;
    }

    public static String getDisplayName(String token) {
        SessionInfo info = SESSIONS.get(token);
        return info == null ? null : info.displayName;
    }

    /** Returns -1 if the token is missing/invalid — callers should have already checked isValid(). */
    public static int getAccountId(String token) {
        SessionInfo info = SESSIONS.get(token);
        return info == null ? -1 : info.accountId;
    }

    public static void invalidate(String token) {
        if (token != null) SESSIONS.remove(token);
    }
}
