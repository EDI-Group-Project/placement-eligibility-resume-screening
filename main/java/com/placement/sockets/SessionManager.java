package com.placement.sockets;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionManager {
    public record Session(String role, String displayName, int accountId) {}
    private static final Map<String, Session> SESSIONS = new ConcurrentHashMap<>();
    private SessionManager(){}

    public static String createSession(String role,String displayName,int accountId){
        String token=UUID.randomUUID().toString();
        SESSIONS.put(token,new Session(role,displayName,accountId));
        return token;
    }
    public static boolean isValid(String token){return token!=null&&SESSIONS.containsKey(token);}
    public static String getRole(String token){Session s=SESSIONS.get(token);return s==null?null:s.role();}
    public static int getAccountId(String token){Session s=SESSIONS.get(token);return s==null?0:s.accountId();}
    public static String getDisplayName(String token){Session s=SESSIONS.get(token);return s==null?null:s.displayName();}
    public static void invalidate(String token){if(token!=null)SESSIONS.remove(token);}
}
